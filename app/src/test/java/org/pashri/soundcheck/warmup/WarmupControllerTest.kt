package org.pashri.soundcheck.warmup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.metronome.Metronome
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.piano.FakePianoSource
import org.pashri.soundcheck.piano.Piano

/** Uses Task 5's traced fixture: two Triad Steps in C4–A4 at 120 bpm, ending at 60 100 ms. */
@OptIn(ExperimentalCoroutinesApi::class)
class WarmupControllerTest {
    private val range = Range(lowest = Pitch(60), highest = Pitch(69))
    private val triad = Step(
        pattern = StarterPatterns.TRIAD,
        soundId = SoundId("mim"),
        bpm = 120,
        direction = Direction.START_LOW,
    )
    private val programme =
        Programme(name = "Test", steps = listOf(triad, triad.copy(soundId = SoundId("hum"))))
    private val focus = FakeFocusGate()
    private val arbiter = ToolArbiter()

    private class Rig(val output: FakeSoundOutput, val controller: WarmupController) {
        val playback: Playback? get() = controller.playback.value
    }

    private fun TestScope.rig(): Rig {
        val output = FakeSoundOutput(clockMs = { testScheduler.currentTime })
        val player = ProgrammePlayer(
            output = output,
            piano = Piano(source = FakePianoSource(), output = output),
            announcements = FakeAnnouncements(),
            scope = backgroundScope,
        )
        val controller = WarmupController(
            player = player,
            focus = focus,
            arbiter = arbiter,
            scope = backgroundScope,
        )
        return Rig(output = output, controller = controller)
    }

    private fun TestScope.runUntil(ms: Long) {
        advanceTimeBy(ms - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun `playing takes audio focus and the tool slot`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runCurrent()
        assertTrue(focus.held)
        assertEquals(Tool.WARM_UP, arbiter.current)
        assertEquals(true, rig.playback?.playing)
    }

    @Test
    fun `with focus refused nothing plays and the slot is free`() = runTest {
        focus.grant = false
        val rig = rig()
        rig.controller.play(programme, range)
        runCurrent()
        assertNull(rig.playback)
        assertNull(arbiter.current)
        assertFalse(rig.output.running)
    }

    @Test
    fun `one press pauses and resumes, two go to the next Step, three go back`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runUntil(1_000)
        rig.controller.onPresses(1)
        assertEquals(false, rig.playback?.playing)
        rig.controller.onPresses(1)
        assertEquals(true, rig.playback?.playing)
        runCurrent()
        rig.controller.onPresses(2)
        assertEquals(1, rig.playback?.stepIndex)
        runCurrent()
        rig.controller.onPresses(3)
        assertEquals(0, rig.playback?.stepIndex)
    }

    @Test
    fun `a phone call pauses the Programme and it resumes when the call ends`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runUntil(10_600)
        focus.loseFocus()
        assertEquals(false, rig.playback?.playing)
        assertEquals(1, rig.playback?.iteration)
        runUntil(20_000)
        focus.regainFocus()
        assertEquals(true, rig.playback?.playing)
        assertEquals(2, focus.acquireCount)
    }

    @Test
    fun `regaining focus does not resume a Programme the user paused`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runUntil(1_000)
        rig.controller.pause()
        focus.loseFocus()
        focus.regainFocus()
        assertEquals(false, rig.playback?.playing)
    }

    @Test
    fun `another tool starting pauses the Programme and stops the output at once`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runUntil(1_000)
        arbiter.claim(Tool.METRONOME, onEvicted = {})
        assertEquals(false, rig.playback?.playing)
        assertFalse(rig.output.running)
    }

    @Test
    fun `resuming takes the slot back from the other tool`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runUntil(1_000)
        var evicted = false
        arbiter.claim(Tool.TUNER, onEvicted = { evicted = true })
        rig.controller.resume()
        assertTrue(evicted)
        assertEquals(Tool.WARM_UP, arbiter.current)
        assertEquals(true, rig.playback?.playing)
    }

    @Test
    fun `stopping hands focus back and frees the slot`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runCurrent()
        rig.controller.stop()
        assertFalse(focus.held)
        assertNull(arbiter.current)
        assertNull(rig.playback)
    }

    @Test
    fun `a Programme that ends by itself hands focus back`() = runTest {
        val rig = rig()
        rig.controller.play(programme, range)
        runUntil(61_000)
        assertNull(rig.playback)
        assertFalse(focus.held)
        assertNull(arbiter.current)
    }

    @Test
    fun `skipping or resuming with nothing playing does nothing`() = runTest {
        val rig = rig()
        rig.controller.next()
        rig.controller.previous()
        rig.controller.resume()
        assertNull(rig.playback)
        assertEquals(0, focus.acquireCount)
    }

    @Test
    fun `pausing the Programme and starting the Metronome within the tail leaves it running`() =
        runTest {
            // Every tool shares one physical output, as they do in AppContainer; a late stop
            // scheduled by the Programme's fading pause would otherwise silence the
            // Metronome's own clicks a moment after it started.
            val rig = rig()
            rig.controller.play(programme, range)
            runUntil(1_000)
            rig.controller.pause()
            val metronome = Metronome(rig.output, backgroundScope)
            arbiter.claim(Tool.METRONOME, onEvicted = { metronome.stop() })
            rig.output.start()
            metronome.start(bpm = 120, accentEvery = 4)
            runCurrent()
            advanceTimeBy(151)
            runCurrent()
            assertTrue(rig.output.running)
        }
}
