package org.pashri.soundcheck.ui.metronome

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM
import org.pashri.soundcheck.metronome.MetronomeController
import org.pashri.soundcheck.metronome.MetronomeStatus
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

    private fun TestScope.controller(
        headphones: HeadphoneButton = newHeadphones(),
        notificationsShown: () -> Boolean = { true },
    ) = MetronomeController(
        output = output,
        focus = focus,
        arbiter = arbiter,
        headphones = headphones,
        scope = backgroundScope,
        notificationsShown = notificationsShown,
    )

    private fun TestScope.viewModel(headphones: HeadphoneButton = newHeadphones()) =
        MetronomeViewModel(metronome = controller(headphones = headphones), clockMs = clock)

    /** Lets a run of headphone presses end and act. */
    private fun TestScope.runEnds() {
        advanceTimeBy(PressCounter.WINDOW_MS + 1)
        runCurrent()
    }

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

    @Test
    fun `leaving the screen while it clicks keeps it clicking`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown()
        viewModel.setBpm(120)
        viewModel.toggle()
        runCurrent()
        viewModel.onHidden()
        advanceTimeBy(2_000)
        assertTrue(state(viewModel).running)
        assertTrue(output.running)
        assertTrue(focus.held)
        assertTrue(output.scheduled.size >= 4)
        viewModel.stop()
    }

    @Test
    fun `off screen the headphone button pauses the Metronome and starts it again`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val metronome = controller(headphones = headphones)
            metronome.show()
            metronome.toggle()
            metronome.hide()
            runCurrent()
            headphones.press()
            runEnds()
            assertEquals(MetronomeStatus(running = false, paused = true), metronome.status.value)
            assertFalse(output.running)
            assertTrue(headphones.needed.value)
            headphones.press()
            runEnds()
            assertEquals(MetronomeStatus(running = true, paused = false), metronome.status.value)
            assertTrue(output.running)
            metronome.stop()
        }

    @Test
    fun `on screen the headphone button ends the Metronome like the Stop button`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val metronome = controller(headphones = headphones)
            metronome.show()
            metronome.toggle()
            headphones.press()
            runEnds()
            assertEquals(MetronomeStatus(), metronome.status.value)
            assertFalse(focus.held)
            assertNull(arbiter.current)
        }

    @Test
    fun `a paused Metronome keeps focus and the slot, like a paused Programme`() =
        runTest(context = dispatcher) {
            val metronome = controller()
            metronome.toggle()
            metronome.pause()
            assertTrue(metronome.status.value.paused)
            assertTrue(focus.held)
            assertEquals(Tool.METRONOME, arbiter.current)
        }

    @Test
    fun `Close ends a paused Metronome and gives the headphone button back`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val metronome = controller(headphones = headphones)
            metronome.toggle()
            metronome.pause()
            runCurrent()
            assertTrue(headphones.needed.value)
            metronome.stop()
            runCurrent()
            assertEquals(MetronomeStatus(), metronome.status.value)
            assertFalse(focus.held)
            assertNull(arbiter.current)
            assertFalse(headphones.needed.value)
            headphones.press()
            runEnds()
            assertFalse(metronome.status.value.running)
        }

    @Test
    fun `another tool starting ends a paused Metronome`() = runTest(context = dispatcher) {
        val headphones = newHeadphones()
        val metronome = controller(headphones = headphones)
        metronome.toggle()
        metronome.pause()
        arbiter.claim(tool = Tool.TUNER, onEvicted = {})
        runCurrent()
        assertEquals(MetronomeStatus(), metronome.status.value)
        assertFalse(focus.held)
        assertFalse(headphones.needed.value)
    }

    @Test
    fun `the notification's Pause and Play toggle a held Metronome, never a closed one`() =
        runTest(context = dispatcher) {
            val metronome = controller()
            metronome.pauseOrResume()
            assertEquals(MetronomeStatus(), metronome.status.value)
            metronome.toggle()
            metronome.pauseOrResume()
            assertTrue(metronome.status.value.paused)
            metronome.pauseOrResume()
            assertTrue(metronome.status.value.running)
            metronome.stop()
        }

    @Test
    fun `another tool starting stops a Metronome whose screen has gone`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val viewModel = viewModel(headphones = headphones)
            viewModel.onShown()
            viewModel.toggle()
            viewModel.onHidden()
            runCurrent()
            arbiter.claim(tool = Tool.WARM_UP, onEvicted = {})
            runCurrent()
            assertFalse(state(viewModel).running)
            assertFalse(output.running)
            assertFalse(focus.held)
            assertFalse(headphones.needed.value)
        }

    @Test
    fun `a Metronome whose screen was cleared away keeps clicking`() =
        runTest(context = dispatcher) {
            val metronome = controller()
            val factory = MetronomeViewModel.Factory(metronome = metronome, clockMs = clock)
            val store = ViewModelStore()
            val provider = ViewModelProvider(store = store, factory = factory)
            val first = provider[MetronomeViewModel::class.java]
            first.onShown()
            first.toggle()
            store.clear()
            advanceTimeBy(1_000)
            assertTrue(output.running)
            val second = MetronomeViewModel(metronome = metronome, clockMs = clock)
            assertTrue(state(second).running)
            second.toggle()
            assertFalse(state(second).running)
            assertFalse(output.running)
        }

    @Test
    fun `with notifications off, an off-screen press ends the Metronome instead of pausing it`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val metronome = controller(headphones = headphones, notificationsShown = { false })
            metronome.toggle()
            metronome.hide()
            headphones.press()
            runEnds()
            assertEquals(MetronomeStatus(), metronome.status.value)
            assertFalse(focus.held)
            assertNull(arbiter.current)
            assertFalse(headphones.needed.value)
        }

    @Test
    fun `back on its screen a paused Metronome shows as paused, and Stop ends it`() =
        runTest(context = dispatcher) {
            val headphones = newHeadphones()
            val viewModel = viewModel(headphones = headphones)
            viewModel.onShown()
            viewModel.toggle()
            viewModel.onHidden()
            headphones.press()
            runEnds()
            viewModel.onShown()
            val paused = state(viewModel)
            assertTrue(paused.paused)
            assertFalse(paused.running)
            assertEquals("Resume", paused.startLabel)
            assertEquals("PAUSED", paused.caption)
            viewModel.stop()
            val ended = state(viewModel)
            assertFalse(ended.paused)
            assertEquals("Start", ended.startLabel)
            assertNull(ended.caption)
            assertFalse(focus.held)
        }

    @Test
    fun `Resume on the screen starts a paused Metronome again`() = runTest(context = dispatcher) {
        val headphones = newHeadphones()
        val viewModel = viewModel(headphones = headphones)
        viewModel.toggle()
        viewModel.onHidden()
        headphones.press()
        runEnds()
        viewModel.onShown()
        viewModel.toggle()
        val resumed = state(viewModel)
        assertTrue(resumed.running)
        assertFalse(resumed.paused)
        assertEquals("Stop", resumed.startLabel)
        viewModel.stop()
    }
}
