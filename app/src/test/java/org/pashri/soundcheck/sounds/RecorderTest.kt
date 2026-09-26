package org.pashri.soundcheck.sounds

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.ExclusiveMic
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeMicInput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.Tuner
import org.pashri.soundcheck.tuner.TunerState

/**
 * The fake microphone delivers one 1 024-frame hop every 21 ms of test time, and silence
 * once its queue runs dry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorderTest {
    private val mic = FakeMicInput()
    private val focus = FakeFocusGate()
    private val arbiter = ToolArbiter()
    private val takes = mutableListOf<Take>()

    private fun TestScope.recorder(input: MicInput = mic): Recorder =
        Recorder(
            mic = input,
            focus = focus,
            arbiter = arbiter,
            scope = backgroundScope,
            worker = StandardTestDispatcher(testScheduler),
        )

    private fun TestScope.tuner(input: MicInput): Tuner =
        Tuner(mic = input, scope = backgroundScope, worker = StandardTestDispatcher(testScheduler))

    /** [hops] hops of 1 024 frames at [level], the sign alternating frame by frame. */
    private fun hops(hops: Int, level: Float): FloatArray =
        FloatArray(size = hops * 1_024) { if (it % 2 == 0) level else -level }

    private fun TestScope.wait(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private fun assertEverythingHandedBack(recorder: Recorder) {
        assertFalse(recorder.recording.value)
        assertEquals(0, mic.openNow)
        assertFalse(focus.held)
        assertNull(arbiter.current)
        assertTrue(recorder.levels.value.isEmpty())
    }

    @Test
    fun `a take is what was said between start and stop, trimmed`() = runTest {
        mic.play(hops(hops = 10, level = 0.001f) + hops(hops = 15, level = 0.3f) +
            hops(hops = 10, level = 0.001f))
        val recorder = recorder()
        assertTrue(recorder.start { takes += it })
        wait(800)
        assertTrue(recorder.recording.value)
        assertTrue(focus.held)
        assertEquals(Tool.RECORDING, arbiter.current)
        recorder.stop()
        wait(100)
        assertEquals(420L, (takes.single() as Take.Kept).lengthMs)
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `a take stops by itself at five seconds and says so`() = runTest {
        mic.play(hops(hops = 240, level = 0.3f))
        val recorder = recorder()
        recorder.start { takes += it }
        wait(6_000)
        val take = takes.single() as Take.Kept
        assertEquals(4_950L, take.lengthMs)
        assertTrue(take.cutShort)
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `a tap with nothing said is too quiet`() = runTest {
        val recorder = recorder()
        recorder.start { takes += it }
        recorder.stop()
        wait(100)
        assertEquals(listOf<Take>(Take.TooQuiet), takes)
    }

    @Test
    fun `another tool starting interrupts the take and throws it away`() = runTest {
        mic.play(hops(hops = 40, level = 0.3f))
        val recorder = recorder()
        recorder.start { takes += it }
        wait(300)
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = {})
        wait(100)
        assertEquals(listOf<Take>(Take.Interrupted), takes)
        assertEquals(Tool.WARM_UP, arbiter.current)
        assertEquals(0, mic.openNow)
        assertFalse(recorder.recording.value)
    }

    @Test
    fun `a call taking audio focus interrupts the take`() = runTest {
        val recorder = recorder()
        recorder.start { takes += it }
        wait(300)
        focus.loseFocus()
        wait(100)
        assertEquals(listOf<Take>(Take.Interrupted), takes)
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `recording takes the one-tool slot from whatever held it`() = runTest {
        var paused = false
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = { paused = true })
        recorder().start { takes += it }
        assertTrue(paused)
    }

    @Test
    fun `a microphone that won't open says so after five tries`() = runTest {
        var tries = 0
        mic.available = false
        mic.onOpen = { tries++ }
        val recorder = recorder()
        recorder.start { takes += it }
        wait(1_000)
        assertEquals(listOf<Take>(Take.MicUnavailable), takes)
        assertEquals(5, tries)
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `the microphone is tried again while the Tuner lets go of it`() = runTest {
        mic.available = false
        val recorder = recorder()
        recorder.start { takes += it }
        wait(150)
        mic.available = true
        wait(150)
        assertTrue(recorder.recording.value)
        assertEquals(1, mic.openNow)
        recorder.stop()
        wait(100)
        assertEquals(listOf<Take>(Take.TooQuiet), takes)
    }

    @Test
    fun `cancelling throws the take away and hands everything back`() = runTest {
        mic.play(hops(hops = 40, level = 0.3f))
        val recorder = recorder()
        recorder.start { takes += it }
        wait(300)
        recorder.cancel()
        wait(100)
        assertTrue(takes.isEmpty())
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `a second start while recording is refused`() = runTest {
        val recorder = recorder()
        assertTrue(recorder.start { takes += it })
        assertFalse(recorder.start { takes += it })
        recorder.stop()
        wait(100)
        assertEquals(1, takes.size)
    }

    @Test
    fun `the level follows what the microphone hears`() = runTest {
        mic.play(hops(hops = 2, level = 0.5f) + hops(hops = 1, level = 0.25f))
        val recorder = recorder()
        recorder.start { takes += it }
        wait(63)
        assertEquals(listOf(0.5f, 0.5f, 0.25f), recorder.levels.value)
        recorder.cancel()
    }

    @Test
    fun `stopping while the microphone is tried gives up at once`() = runTest {
        mic.available = false
        val recorder = recorder()
        recorder.start { takes += it }
        wait(150)
        recorder.stop()
        mic.available = true
        wait(500)
        assertEquals(listOf<Take>(Take.TooQuiet), takes)
        assertEquals(0, mic.timesOpened)
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `another tool starting while the microphone is tried interrupts the take`() = runTest {
        mic.available = false
        val recorder = recorder()
        recorder.start { takes += it }
        wait(150)
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = {})
        mic.available = true
        wait(500)
        assertEquals(listOf<Take>(Take.Interrupted), takes)
        assertEquals(0, mic.timesOpened)
        assertEquals(Tool.WARM_UP, arbiter.current)
        assertFalse(focus.held)
        assertFalse(recorder.recording.value)
    }

    @Test
    fun `a microphone that dies mid-take says so and hands everything back`() = runTest {
        mic.play(hops(hops = 40, level = 0.3f))
        val recorder = recorder()
        recorder.start { takes += it }
        wait(300)
        mic.breakMic()
        wait(100)
        assertEquals(listOf<Take>(Take.MicUnavailable), takes)
        assertEverythingHandedBack(recorder)
    }

    @Test
    fun `the recorder waits for the Tuner to let go of the shared microphone`() = runTest {
        val shared = ExclusiveMic(mic)
        val tuner = tuner(input = shared)
        tuner.start()
        wait(50)
        val recorder = recorder(input = shared)
        recorder.start { takes += it }
        wait(150)
        assertEquals(1, mic.timesOpened)
        tuner.stop()
        wait(150)
        assertEquals(2, mic.timesOpened)
        assertEquals(1, mic.openNow)
        assertEquals(1, mic.mostOpenAtOnce)
        recorder.stop()
        wait(100)
        assertEquals(listOf<Take>(Take.TooQuiet), takes)
    }

    @Test
    fun `the Tuner waits for the recorder to let go of the shared microphone`() = runTest {
        val shared = ExclusiveMic(mic)
        val recorder = recorder(input = shared)
        recorder.start { takes += it }
        wait(50)
        val tuner = tuner(input = shared)
        tuner.start()
        wait(150)
        assertEquals(TunerState(), tuner.state.value)
        assertEquals(1, mic.timesOpened)
        recorder.stop()
        wait(150)
        assertEquals(MicStatus.Listening, tuner.state.value.mic)
        assertEquals(2, mic.timesOpened)
        assertEquals(1, mic.mostOpenAtOnce)
        tuner.stop()
    }

    @Test
    fun `refused audio focus records nothing and hands everything back`() = runTest {
        focus.grant = false
        mic.play(hops(hops = 40, level = 0.3f))
        val recorder = recorder()
        recorder.start { takes += it }
        wait(500)
        assertEquals(listOf<Take>(Take.Interrupted), takes)
        assertEquals(0, mic.timesOpened)
        assertEverythingHandedBack(recorder)
    }
}
