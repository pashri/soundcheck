package org.pashri.soundcheck.tuner

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.pashri.soundcheck.audio.FakeMicInput
import org.pashri.soundcheck.audio.FakeMicInput.Companion.HOP_MS
import org.pashri.soundcheck.audio.MIC_OPEN_RETRY_MS
import org.pashri.soundcheck.audio.MIC_OPEN_TRIES

@OptIn(ExperimentalCoroutinesApi::class)
class TunerTest {
    private val mic = FakeMicInput()

    private fun TestScope.tuner(): Tuner =
        Tuner(mic, backgroundScope, StandardTestDispatcher(testScheduler))

    private fun TestScope.hops(count: Int) {
        advanceTimeBy(count * HOP_MS)
        runCurrent()
    }

    /** Waits out every try at opening the microphone. */
    private fun TestScope.micGivesUp() {
        advanceTimeBy(MIC_OPEN_RETRY_MS * (MIC_OPEN_TRIES - 1))
        runCurrent()
    }

    private fun tone(hz: Double, hops: Int): FloatArray =
        Signals.sine(hz, size = hops * HOP_SIZE)

    @Test
    fun `it is off until started`() = runTest {
        val tuner = tuner()
        runCurrent()
        assertEquals(TunerState(), tuner.state.value)
        assertEquals(0, mic.timesOpened)
    }

    @Test
    fun `once started it listens with no note until it hears one`() = runTest {
        val tuner = tuner()
        tuner.start()
        hops(5)
        assertEquals(TunerState(mic = MicStatus.Listening, note = null), tuner.state.value)
    }

    @Test
    fun `each open guitar string reads as its note, in tune`() = runTest {
        val strings = mapOf(82.407 to "E2", 110.0 to "A2", 146.832 to "D3")
            .plus(mapOf(195.998 to "G3", 246.942 to "B3", 329.628 to "E4"))
        strings.forEach { (hz, name) ->
            val tuner = tuner()
            tuner.start()
            mic.play(tone(hz, hops = 12))
            hops(12)
            val note = checkNotNull(tuner.state.value.note) { "nothing heard for $name" }
            assertEquals(name, "${note.name}${note.octave}")
            assertEquals(name, 0, note.roundedCents)
            tuner.stop()
            hops(1)
        }
    }

    @Test
    fun `an A2 a little under 110 Hz reads 5 cents flat`() = runTest {
        val tuner = tuner()
        tuner.start()
        mic.play(tone(109.7, hops = 12))
        hops(12)
        val note = checkNotNull(tuner.state.value.note)
        assertEquals("A", note.name)
        assertEquals(-5, note.roundedCents)
    }

    @Test
    fun `when the sound stops the note holds and then clears`() = runTest {
        val tuner = tuner()
        tuner.start()
        mic.play(tone(110.0, hops = 12))
        hops(12 + HOLD_FRAMES - 2)
        assertNotNull(tuner.state.value.note)
        hops(4)
        assertNull(tuner.state.value.note)
        assertEquals(MicStatus.Listening, tuner.state.value.mic)
    }

    @Test
    fun `stopping releases the microphone and clears the note`() = runTest {
        val tuner = tuner()
        tuner.start()
        mic.play(tone(110.0, hops = 12))
        hops(12)
        tuner.stop()
        assertEquals(TunerState(), tuner.state.value)
        hops(1)
        assertEquals(0, mic.openNow)
        hops(10)
        assertEquals(TunerState(), tuner.state.value)
    }

    @Test
    fun `starting twice opens the microphone once`() = runTest {
        val tuner = tuner()
        tuner.start()
        hops(2)
        tuner.start()
        hops(2)
        assertEquals(1, mic.timesOpened)
    }

    @Test
    fun `stopping and starting at once never holds the microphone twice`() = runTest {
        val tuner = tuner()
        tuner.start()
        hops(2)
        tuner.stop()
        tuner.start()
        hops(2)
        assertEquals(2, mic.timesOpened)
        assertEquals(1, mic.mostOpenAtOnce)
        assertEquals(MicStatus.Listening, tuner.state.value.mic)
    }

    @Test
    fun `three quick stop-starts inside one hop never hold two microphones`() = runTest {
        val tuner = tuner()
        tuner.start()
        advanceTimeBy(2 * HOP_MS + 5)
        runCurrent()
        tuner.stop()
        tuner.start()
        runCurrent()
        tuner.stop()
        tuner.start()
        runCurrent()
        hops(3)
        assertEquals(1, mic.mostOpenAtOnce)
    }

    @Test
    fun `stopping from within a hop callback never leaves a note showing`() = runTest {
        val tuner = tuner()
        tuner.start()
        mic.play(tone(110.0, hops = 12))
        hops(11)
        mic.onHop = { tuner.stop() }
        hops(1)
        assertEquals(TunerState(), tuner.state.value)
    }

    @Test
    fun `stopping while the microphone opens leaves it off and closed`() = runTest {
        val tuner = tuner()
        mic.onOpen = { tuner.stop() }
        tuner.start()
        runCurrent()
        assertEquals(TunerState(), tuner.state.value)
        assertEquals(0, mic.openNow)
    }

    @Test
    fun `stopping while a refusing microphone opens does not report it unavailable`() =
        runTest {
            mic.available = false
            val tuner = tuner()
            var tries = 0
            mic.onOpen = { if (++tries == MIC_OPEN_TRIES) tuner.stop() }
            tuner.start()
            micGivesUp()
            assertEquals(TunerState(), tuner.state.value)
        }

    @Test
    fun `a microphone that will not open is reported unavailable`() = runTest {
        mic.available = false
        val tuner = tuner()
        tuner.start()
        micGivesUp()
        assertEquals(TunerState(mic = MicStatus.Unavailable), tuner.state.value)
    }

    @Test
    fun `a microphone that stops working is reported unavailable and released`() = runTest {
        val tuner = tuner()
        tuner.start()
        mic.play(tone(110.0, hops = 12))
        hops(12)
        mic.breakMic()
        hops(1)
        assertEquals(TunerState(mic = MicStatus.Unavailable), tuner.state.value)
        assertEquals(0, mic.openNow)
    }

    @Test
    fun `after a failure starting again listens again`() = runTest {
        mic.available = false
        val tuner = tuner()
        tuner.start()
        micGivesUp()
        mic.available = true
        tuner.start()
        hops(1)
        assertEquals(MicStatus.Listening, tuner.state.value.mic)
    }

    @Test
    fun `the microphone is tried again while the recorder lets go of it`() = runTest {
        mic.available = false
        val tuner = tuner()
        tuner.start()
        advanceTimeBy(150)
        runCurrent()
        assertEquals(TunerState(), tuner.state.value)
        mic.available = true
        advanceTimeBy(100)
        runCurrent()
        assertEquals(MicStatus.Listening, tuner.state.value.mic)
        assertEquals(1, mic.openNow)
        tuner.stop()
    }
}
