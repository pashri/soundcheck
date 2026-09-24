package org.pashri.soundcheck.metronome

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
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.FakeSoundOutput.Scheduled
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.audio.msToFrames

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeTest {
    private val accent = SampleIds.METRONOME_ACCENT
    private val click = SampleIds.METRONOME_CLICK

    private fun TestScope.metronomeWith(output: FakeSoundOutput): Metronome =
        Metronome(output, backgroundScope)

    private fun TestScope.fakeOutput(): FakeSoundOutput =
        FakeSoundOutput(clockMs = { testScheduler.currentTime })

    private fun TestScope.runFor(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private val FakeSoundOutput.frames: List<Long> get() = scheduled.map { it.frame }

    @Test
    fun `it loads its two click sounds when created`() = runTest {
        val output = fakeOutput()
        metronomeWith(output)
        assertEquals(setOf(click, accent), output.loaded.keys)
    }

    @Test
    fun `the first click is accented and lands one start margin after starting`() = runTest {
        val output = fakeOutput()
        metronomeWith(output).start(bpm = 120, accentEvery = 4)
        runCurrent()
        assertEquals(Scheduled(accent, 4_800, Metronome.ACCENT_GAIN), output.scheduled.first())
    }

    @Test
    fun `beats at 120 bpm are exactly half a second apart`() = runTest {
        val output = fakeOutput()
        metronomeWith(output).start(bpm = 120, accentEvery = 4)
        runFor(2_000)
        assertEquals(listOf(4_800L, 28_800L, 52_800L, 76_800L, 100_800L), output.frames)
    }

    @Test
    fun `the slowest and fastest tempos keep exact spacing`() = runTest {
        val slow = fakeOutput()
        metronomeWith(slow).start(bpm = MIN_BPM, accentEvery = null)
        val fast = fakeOutput()
        metronomeWith(fast).start(bpm = MAX_BPM, accentEvery = null)
        runFor(5_000)
        assertTrue(slow.frames.zipWithNext { a, b -> b - a }.all { it == 96_000L })
        assertTrue(fast.frames.zipWithNext { a, b -> b - a }.all { it == 9_600L })
    }

    @Test
    fun `an accent every 4 accents every fourth beat`() = runTest {
        val output = fakeOutput()
        metronomeWith(output).start(bpm = 120, accentEvery = 4)
        runFor(4_000)
        val expected = listOf(accent, click, click, click, accent, click, click, click, accent)
        assertEquals(expected, output.scheduled.map { it.id })
    }

    @Test
    fun `with the accent off no beat is accented`() = runTest {
        val output = fakeOutput()
        metronomeWith(output).start(bpm = 120, accentEvery = null)
        runFor(2_000)
        assertTrue(output.scheduled.all { it.id == click && it.gain == Metronome.CLICK_GAIN })
    }

    @Test
    fun `nothing is scheduled further ahead than the lookahead`() = runTest {
        val output = fakeOutput()
        metronomeWith(output).start(bpm = MAX_BPM, accentEvery = 4)
        repeat(40) {
            runFor(Metronome.TICK_MS)
            val limit = output.framePosition() + msToFrames(Metronome.LOOKAHEAD_MS)
            assertTrue(output.frames.max() < limit)
        }
    }

    @Test
    fun `slowing down keeps the sounded beat and spaces the next by the new tempo`() = runTest {
        val output = fakeOutput()
        val metronome = metronomeWith(output)
        metronome.start(bpm = 120, accentEvery = 4)
        runFor(700)
        metronome.setTempo(60)
        runFor(2_000)
        assertEquals(listOf(4_800L, 28_800L, 76_800L, 124_800L), output.frames)
        assertTrue(output.lateSchedules.isEmpty())
    }

    @Test
    fun `speeding up sharply never schedules a beat in the past`() = runTest {
        val output = fakeOutput()
        val metronome = metronomeWith(output)
        metronome.start(bpm = 30, accentEvery = 4)
        runFor(1_000)
        metronome.setTempo(300)
        runFor(100)
        assertEquals(listOf(4_800L, 48_960L, 58_560L), output.frames)
        assertTrue(output.lateSchedules.isEmpty())
    }

    @Test
    fun `changing the accent starts a new bar on the next beat`() = runTest {
        val output = fakeOutput()
        val metronome = metronomeWith(output)
        metronome.start(bpm = 120, accentEvery = 4)
        runFor(700)
        metronome.setAccent(3)
        runFor(2_000)
        val expected = listOf(accent, click, accent, click, click, accent)
        assertEquals(expected, output.scheduled.map { it.id })
        assertTrue(output.lateSchedules.isEmpty())
    }

    @Test
    fun `starting again replaces the running beat instead of adding a second one`() = runTest {
        val output = fakeOutput()
        val metronome = metronomeWith(output)
        metronome.start(bpm = 120, accentEvery = 4)
        runFor(700)
        metronome.start(bpm = 120, accentEvery = 4)
        runFor(2_000)
        val expected = listOf(4_800L, 28_800L, 38_400L, 62_400L, 86_400L, 110_400L, 134_400L)
        assertEquals(expected, output.frames)
    }

    @Test
    fun `stopping cancels pending clicks and stops the output`() = runTest {
        val output = fakeOutput()
        val metronome = metronomeWith(output)
        metronome.start(bpm = 120, accentEvery = 4)
        runFor(1_000)
        metronome.stop()
        runFor(1_000)
        assertFalse(output.running)
        assertNull(metronome.beat.value)
        assertEquals(listOf(4_800L, 28_800L), output.frames)
    }

    @Test
    fun `the current beat follows the frame position`() = runTest {
        val output = fakeOutput()
        val metronome = metronomeWith(output)
        metronome.start(bpm = 120, accentEvery = 4)
        runFor(150)
        assertEquals(Beat(index = 0, frame = 4_800, accented = true, positionInBar = 0),
            metronome.beat.value)
        runFor(500)
        assertEquals(1, metronome.beat.value?.positionInBar)
    }

    @Test
    fun `if the output will not start nothing is scheduled`() = runTest {
        val output = fakeOutput().apply { startResult = false }
        val metronome = metronomeWith(output)
        assertFalse(metronome.start(bpm = 120, accentEvery = 4))
        runFor(1_000)
        assertTrue(output.scheduled.isEmpty())
    }
}
