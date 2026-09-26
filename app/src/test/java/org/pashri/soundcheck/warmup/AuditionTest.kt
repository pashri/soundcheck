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
import org.pashri.soundcheck.piano.FakePianoSource
import org.pashri.soundcheck.piano.Piano

/**
 * The Triad's Demo on a Tenor at 90 bpm: five notes 32 000 frames apart, the last a half
 * note, 192 000 frames in all; with the 100 ms (4 800-frame) start margin it ends at frame
 * 196 800, about 4.1 s after the tap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuditionTest {
    private val focus = FakeFocusGate()
    private val arbiter = ToolArbiter()
    private val triadDemo =
        demoNotes(step = StarterProgrammes.WARM_UP.steps[1], range = VoiceType.TENOR.range)

    private class Rig(
        val output: FakeSoundOutput,
        val audition: Audition,
        val controller: WarmupController,
    )

    private fun TestScope.rig(): Rig {
        val output = FakeSoundOutput(clockMs = { testScheduler.currentTime })
        val piano = Piano(source = FakePianoSource(), output = output)
        val player = ProgrammePlayer(
            output = output,
            piano = piano,
            announcements = FakeAnnouncements(),
            scope = backgroundScope,
        )
        val controller = WarmupController(
            player = player,
            focus = FakeFocusGate(),
            arbiter = arbiter,
            scope = backgroundScope,
        )
        val audition = Audition(
            output = output,
            piano = piano,
            focus = focus,
            arbiter = arbiter,
            scope = backgroundScope,
        )
        return Rig(output = output, audition = audition, controller = controller)
    }

    private fun TestScope.playingProgramme(rig: Rig) {
        rig.controller.play(StarterProgrammes.WARM_UP, VoiceType.TENOR.range)
        runCurrent()
    }

    @Test
    fun `the notes play once on their frames from a moment after the tap`() = runTest {
        val rig = rig()
        assertTrue(rig.audition.play(triadDemo))
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(rig.audition.playing.value)
        assertEquals(
            listOf(4_800L, 36_800L, 68_800L, 100_800L, 132_800L),
            rig.output.scheduled.map { it.frame },
        )
        assertTrue(rig.output.scheduled.all { it.gain == ProgrammePlayer.MELODY_GAIN })
    }

    @Test
    fun `it ends by itself and hands everything back`() = runTest {
        val rig = rig()
        rig.audition.play(triadDemo)
        runCurrent()
        assertTrue(rig.audition.playing.value)
        assertEquals(Tool.AUDITION, arbiter.current)
        assertTrue(focus.held)
        advanceTimeBy(5_000)
        runCurrent()
        assertFalse(rig.audition.playing.value)
        assertNull(arbiter.current)
        assertFalse(focus.held)
        assertFalse(rig.output.running)
    }

    @Test
    fun `a second tap stops it at once`() = runTest {
        val rig = rig()
        rig.audition.play(triadDemo)
        runCurrent()
        rig.audition.stop()
        assertFalse(rig.audition.playing.value)
        assertNull(arbiter.current)
        assertFalse(focus.held)
        assertFalse(rig.output.running)
    }

    @Test
    fun `auditioning pauses a playing Programme`() = runTest {
        val rig = rig()
        playingProgramme(rig)
        assertTrue(rig.audition.play(triadDemo))
        assertEquals(false, rig.controller.playback.value?.playing)
        assertEquals(Tool.AUDITION, arbiter.current)
        assertTrue(rig.output.running)
    }

    @Test
    fun `a Programme resuming stops the audition`() = runTest {
        val rig = rig()
        playingProgramme(rig)
        rig.audition.play(triadDemo)
        runCurrent()
        rig.controller.resume()
        assertFalse(rig.audition.playing.value)
        assertEquals(Tool.WARM_UP, arbiter.current)
        assertEquals(true, rig.controller.playback.value?.playing)
        assertTrue(rig.output.running)
    }

    @Test
    fun `nothing to play changes nothing, not even a playing Programme`() = runTest {
        val rig = rig()
        playingProgramme(rig)
        assertFalse(rig.audition.play(emptyList()))
        assertEquals(true, rig.controller.playback.value?.playing)
        assertEquals(Tool.WARM_UP, arbiter.current)
    }

    @Test
    fun `with focus refused nothing plays and the slot is free`() = runTest {
        focus.grant = false
        val rig = rig()
        assertFalse(rig.audition.play(triadDemo))
        runCurrent()
        assertFalse(rig.audition.playing.value)
        assertNull(arbiter.current)
        assertTrue(rig.output.scheduled.isEmpty())
    }

    @Test
    fun `an output that won't start hands everything back`() = runTest {
        val rig = rig()
        rig.output.startResult = false
        assertFalse(rig.audition.play(triadDemo))
        assertNull(arbiter.current)
        assertFalse(focus.held)
    }

    @Test
    fun `losing focus stops it`() = runTest {
        val rig = rig()
        rig.audition.play(triadDemo)
        runCurrent()
        focus.loseFocus()
        assertFalse(rig.audition.playing.value)
        assertNull(arbiter.current)
    }
}
