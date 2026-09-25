package org.pashri.soundcheck.ui.warmup

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.piano.FakePianoSource
import org.pashri.soundcheck.piano.Piano
import org.pashri.soundcheck.warmup.FakeAnnouncements
import org.pashri.soundcheck.warmup.ProgrammePlayer
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController

@OptIn(ExperimentalCoroutinesApi::class)
class WarmupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val focus = FakeFocusGate()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.controller(): WarmupController {
        val output = FakeSoundOutput(clockMs = { testScheduler.currentTime })
        val player = ProgrammePlayer(
            output = output,
            piano = Piano(source = FakePianoSource(), output = output),
            announcements = FakeAnnouncements(),
            scope = backgroundScope,
        )
        return WarmupController(
            player = player,
            focus = focus,
            arbiter = ToolArbiter(),
            scope = backgroundScope,
        )
    }

    private fun factory(controller: WarmupController): ViewModelProvider.Factory =
        WarmupViewModel.Factory(
            controller = controller,
            programme = StarterProgrammes.WARM_UP,
            range = VoiceType.TENOR.range,
            sounds = StarterSounds.ALL,
        )

    private fun TestScope.viewModel(): WarmupViewModel =
        factory(controller()).create(WarmupViewModel::class.java)

    private fun TestScope.state(viewModel: WarmupViewModel): WarmupUiState {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `it opens on the starter Programme, ready to start`() = runTest(dispatcher) {
        val state = state(viewModel())
        assertEquals("Starter warm-up", state.programmeName)
        assertFalse(state.active)
        assertEquals("Start", state.playLabel)
    }

    @Test
    fun `the play button starts, pauses and resumes the Programme`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.playPause()
        assertTrue(state(viewModel).playing)
        viewModel.playPause()
        assertFalse(state(viewModel).playing)
        assertTrue(state(viewModel).active)
        viewModel.playPause()
        assertTrue(state(viewModel).playing)
    }

    @Test
    fun `next moves to the second Step`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.playPause()
        runCurrent()
        viewModel.next()
        assertEquals(2, state(viewModel).stepNumber)
    }

    @Test
    fun `stop ends the Programme and hands focus back`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.playPause()
        runCurrent()
        viewModel.stop()
        assertFalse(state(viewModel).active)
        assertFalse(focus.held)
    }

    @Test
    fun `closing the screen leaves the Programme playing`() = runTest(dispatcher) {
        val controller = controller()
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(store, factory(controller))[WarmupViewModel::class.java]
        viewModel.playPause()
        runCurrent()
        store.clear()
        runCurrent()
        assertEquals(true, controller.playback.value?.playing)
    }
}
