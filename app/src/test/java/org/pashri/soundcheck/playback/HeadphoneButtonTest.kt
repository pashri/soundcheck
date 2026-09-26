package org.pashri.soundcheck.playback

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
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.testController

@OptIn(ExperimentalCoroutinesApi::class)
class HeadphoneButtonTest {
    private val arbiter = ToolArbiter()
    private var toggles = 0
    private val toggle: () -> Unit = { toggles++ }

    private fun TestScope.button(controller: WarmupController): HeadphoneButton =
        HeadphoneButton(
            arbiter = arbiter,
            warmup = controller,
            scope = backgroundScope,
            windowMs = PressCounter.WINDOW_MS,
        )

    private fun TestScope.controller(): WarmupController = testController(arbiter = arbiter)

    /** Lets a run of presses end and act. */
    private fun TestScope.runEnds() {
        advanceTimeBy(PressCounter.WINDOW_MS + 1)
        runCurrent()
    }

    private fun TestScope.playStarter(controller: WarmupController) {
        controller.play(programme = StarterProgrammes.WARM_UP, range = VoiceType.TENOR.range)
        runCurrent()
    }

    @Test
    fun `the tool sounding now has the button, else the last to start`() {
        assertEquals(
            PressTarget.METRONOME,
            pressTarget(
                current = Tool.METRONOME,
                last = Tool.METRONOME,
                metronomeOffered = true,
                programmeLoaded = true,
            ),
        )
        assertEquals(
            PressTarget.WARM_UP,
            pressTarget(
                current = Tool.WARM_UP,
                last = Tool.WARM_UP,
                metronomeOffered = true,
                programmeLoaded = true,
            ),
        )
        assertEquals(
            PressTarget.METRONOME,
            pressTarget(
                current = null,
                last = Tool.METRONOME,
                metronomeOffered = true,
                programmeLoaded = true,
            ),
        )
        assertEquals(
            PressTarget.WARM_UP,
            pressTarget(
                current = Tool.TUNER,
                last = Tool.TUNER,
                metronomeOffered = false,
                programmeLoaded = true,
            ),
        )
    }

    @Test
    fun `a tool that can't take presses passes them on, or nobody gets them`() {
        assertEquals(
            PressTarget.WARM_UP,
            pressTarget(
                current = null,
                last = Tool.METRONOME,
                metronomeOffered = false,
                programmeLoaded = true,
            ),
        )
        assertEquals(
            PressTarget.METRONOME,
            pressTarget(
                current = null,
                last = Tool.WARM_UP,
                metronomeOffered = true,
                programmeLoaded = false,
            ),
        )
        assertNull(
            pressTarget(
                current = null,
                last = null,
                metronomeOffered = false,
                programmeLoaded = false,
            ),
        )
    }

    @Test
    fun `while the Metronome runs one press stops it, and the next starts it again`() =
        runTest {
            val button = button(controller())
            button.offerMetronome(toggle)
            arbiter.claim(tool = Tool.METRONOME, onEvicted = {})
            button.press()
            runEnds()
            assertEquals(1, toggles)
            arbiter.release(Tool.METRONOME)
            button.press()
            runEnds()
            assertEquals(2, toggles)
        }

    @Test
    fun `two or three presses leave the Metronome alone`() = runTest {
        val button = button(controller())
        button.offerMetronome(toggle)
        arbiter.claim(tool = Tool.METRONOME, onEvicted = {})
        button.press()
        button.press()
        runEnds()
        repeat(times = 3) { button.press() }
        runEnds()
        assertEquals(0, toggles)
    }

    @Test
    fun `presses reach a playing Programme when the Warm-up started last`() = runTest {
        val controller = controller()
        val button = button(controller)
        button.offerMetronome(toggle)
        playStarter(controller)
        button.press()
        button.press()
        runEnds()
        assertEquals(1, controller.playback.value?.stepIndex)
        assertEquals(0, toggles)
    }

    @Test
    fun `the Metronome started after a Programme takes the button from it`() = runTest {
        val controller = controller()
        val button = button(controller)
        playStarter(controller)
        button.offerMetronome(toggle)
        arbiter.claim(tool = Tool.METRONOME, onEvicted = {})
        runCurrent()
        assertEquals(false, controller.playback.value?.playing)
        button.press()
        runEnds()
        assertEquals(1, toggles)
        assertEquals(false, controller.playback.value?.playing)
    }

    @Test
    fun `next and previous keys reach a loaded Programme even while the Metronome has it`() =
        runTest {
            val controller = controller()
            val button = button(controller)
            playStarter(controller)
            button.offerMetronome(toggle)
            arbiter.claim(tool = Tool.METRONOME, onEvicted = {})
            runCurrent()
            button.next()
            runCurrent()
            assertEquals(1, controller.playback.value?.stepIndex)
            button.previous()
            runCurrent()
            assertEquals(0, controller.playback.value?.stepIndex)
            assertEquals(0, toggles)
        }

    @Test
    fun `a hidden Metronome never starts, and the session is needed only while something is`() =
        runTest {
            val button = button(controller())
            runCurrent()
            assertFalse(button.needed.value)
            button.offerMetronome(toggle)
            runCurrent()
            assertTrue(button.needed.value)
            button.withdrawMetronome(toggle)
            runCurrent()
            assertFalse(button.needed.value)
            button.press()
            runEnds()
            assertEquals(0, toggles)
        }
}
