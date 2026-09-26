package org.pashri.soundcheck.ui.metronome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM
import org.pashri.soundcheck.playback.HeadphoneButton
import org.pashri.soundcheck.playback.PressCounter
import org.pashri.soundcheck.warmup.testController

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = { dispatcher.scheduler.currentTime }
    private val output = FakeSoundOutput(clockMs = clock)
    private val focus = FakeFocusGate()
    private val arbiter = ToolArbiter()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.newHeadphones(): HeadphoneButton = HeadphoneButton(
        arbiter = arbiter,
        warmup = testController(arbiter = arbiter),
        scope = backgroundScope,
    )

    private fun TestScope.viewModel(headphones: HeadphoneButton = newHeadphones()) =
        MetronomeViewModel(
            output = output,
            focus = focus,
            clockMs = clock,
            arbiter = arbiter,
            headphones = headphones,
        )

    private fun TestScope.state(viewModel: MetronomeViewModel): MetronomeUiState {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `it starts stopped at 96 bpm with an accent every 4`() = runTest(context = dispatcher) {
        val expected =
            MetronomeUiState(bpm = 96, accentEvery = 4, running = false, beatInBar = null)
        assertEquals(expected, state(viewModel()))
    }

    @Test
    fun `faster and slower move one bpm and stop at the limits`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.faster()
        assertEquals(97, state(viewModel).bpm)
        viewModel.setBpm(MAX_BPM)
        viewModel.faster()
        assertEquals(MAX_BPM, state(viewModel).bpm)
        viewModel.setBpm(MIN_BPM)
        viewModel.slower()
        assertEquals(MIN_BPM, state(viewModel).bpm)
    }

    @Test
    fun `a tempo outside the range is clamped`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.setBpm(1_000)
        assertEquals(MAX_BPM, state(viewModel).bpm)
        viewModel.setBpm(0)
        assertEquals(MIN_BPM, state(viewModel).bpm)
    }

    @Test
    fun `starting takes audio focus and starts the output`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.toggle()
        assertTrue(state(viewModel).running)
        assertTrue(focus.held)
        assertTrue(output.running)
        viewModel.stop()
    }

    @Test
    fun `if audio focus is refused the metronome stays stopped`() = runTest(context = dispatcher) {
        focus.grant = false
        val viewModel = viewModel()
        viewModel.toggle()
        assertFalse(state(viewModel).running)
        assertFalse(output.running)
    }

    @Test
    fun `if the output will not start audio focus is handed back`() =
        runTest(context = dispatcher) {
            output.startResult = false
            val viewModel = viewModel()
            viewModel.toggle()
            assertFalse(state(viewModel).running)
            assertFalse(focus.held)
        }

    @Test
    fun `losing audio focus stops the metronome`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.toggle()
        runCurrent()
        focus.loseFocus()
        assertFalse(state(viewModel).running)
        assertFalse(output.running)
        assertFalse(focus.held)
    }

    @Test
    fun `stopping hands audio focus back`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.toggle()
        runCurrent()
        viewModel.toggle()
        assertFalse(state(viewModel).running)
        assertFalse(focus.held)
    }

    @Test
    fun `four taps half a second apart set 120 bpm`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        repeat(times = 4) {
            viewModel.tap()
            advanceTimeBy(500)
        }
        assertEquals(120, state(viewModel).bpm)
    }

    @Test
    fun `changing tempo while running respaces the clicks`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.setBpm(120)
        viewModel.toggle()
        advanceTimeBy(700)
        runCurrent()
        viewModel.setBpm(60)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(4_800L, 28_800L, 76_800L, 124_800L), output.scheduled.map { it.frame })
        viewModel.stop()
    }

    @Test
    fun `the beat indicator follows the sounding beat`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.setBpm(120)
        viewModel.toggle()
        advanceTimeBy(650)
        assertEquals(1, state(viewModel).beatInBar)
        viewModel.stop()
    }

    @Test
    fun `with the accent off there is one beat per bar`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.setAccent(null)
        val current = state(viewModel)
        assertEquals(1, current.beatsInBar)
        assertEquals("NO ACCENT", current.accentLabel)
    }

    @Test
    fun `with the accent off the beat index still advances beat to beat`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            viewModel.setAccent(null)
            viewModel.setBpm(120)
            viewModel.toggle()
            advanceTimeBy(100)
            val first = state(viewModel).beatIndex
            advanceTimeBy(500)
            val second = state(viewModel).beatIndex
            advanceTimeBy(500)
            val third = state(viewModel).beatIndex
            assertEquals(0L, first)
            assertEquals(1L, second)
            assertEquals(2L, third)
            viewModel.stop()
        }

    @Test
    fun `starting the metronome stops the tool that was running`() =
        runTest(context = dispatcher) {
            var evicted = false
            arbiter.claim(tool = Tool.WARM_UP, onEvicted = { evicted = true })
            val viewModel = viewModel()
            viewModel.toggle()
            assertTrue(evicted)
            assertEquals(Tool.METRONOME, arbiter.current)
            viewModel.stop()
        }

    @Test
    fun `another tool starting stops the metronome`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.toggle()
        runCurrent()
        arbiter.claim(tool = Tool.TUNER, onEvicted = {})
        assertFalse(state(viewModel).running)
        assertFalse(output.running)
        assertFalse(focus.held)
        assertEquals(Tool.TUNER, arbiter.current)
    }

    @Test
    fun `a shown Metronome starts and stops with the headphone button, a hidden one doesn't`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val viewModel = viewModel(headphones = headphones)
            viewModel.onShown()
            headphones.press()
            advanceTimeBy(PressCounter.WINDOW_MS + 1)
            assertTrue(state(viewModel).running)
            headphones.press()
            advanceTimeBy(PressCounter.WINDOW_MS + 1)
            assertFalse(state(viewModel).running)
            viewModel.onHidden()
            headphones.press()
            advanceTimeBy(PressCounter.WINDOW_MS + 1)
            assertFalse(state(viewModel).running)
        }
}
