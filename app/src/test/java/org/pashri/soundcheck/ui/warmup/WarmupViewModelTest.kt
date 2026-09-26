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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.renameSound
import org.pashri.soundcheck.warmup.testController

@OptIn(ExperimentalCoroutinesApi::class)
class WarmupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val focus = FakeFocusGate()
    private val library = FakeStore(StarterLibrary.LIBRARY)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun factory(controller: WarmupController): ViewModelProvider.Factory =
        WarmupViewModel.Factory(controller = controller, library = library.data)

    private fun TestScope.playing(): WarmupController = testController(focus).also {
        it.play(StarterProgrammes.WARM_UP, VoiceType.TENOR.range)
        runCurrent()
    }

    private fun viewModel(controller: WarmupController): WarmupViewModel =
        factory(controller).create(WarmupViewModel::class.java)

    private fun TestScope.state(viewModel: WarmupViewModel): WarmupUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `it shows nothing while no Programme is loaded`() = runTest(dispatcher) {
        assertNull(state(viewModel(testController(focus))))
    }

    @Test
    fun `it shows the Programme the controller is playing`() = runTest(dispatcher) {
        val state = state(viewModel(playing()))
        assertEquals("Starter warm-up", state?.programmeName)
        assertEquals("lip trill", state?.soundLabel)
        assertEquals(true, state?.playing)
    }

    @Test
    fun `the play button pauses and resumes the Programme`() = runTest(dispatcher) {
        val viewModel = viewModel(playing())
        viewModel.playPause()
        assertEquals(false, state(viewModel)?.playing)
        assertEquals(true, state(viewModel)?.active)
        viewModel.playPause()
        assertEquals(true, state(viewModel)?.playing)
    }

    @Test
    fun `next moves to the second Step`() = runTest(dispatcher) {
        val viewModel = viewModel(playing())
        viewModel.next()
        assertEquals(2, state(viewModel)?.stepNumber)
    }

    @Test
    fun `stop ends the Programme, hands focus back and shows nothing`() = runTest(dispatcher) {
        val viewModel = viewModel(playing())
        viewModel.stop()
        assertNull(state(viewModel))
        assertFalse(focus.held)
    }

    @Test
    fun `a renamed Sound shows on the playing screen`() = runTest(dispatcher) {
        val viewModel = viewModel(playing())
        library.edit { it.renameSound(id = StarterSounds.LIP_TRILL.id, label = "brr") }
        assertEquals("brr", state(viewModel)?.soundLabel)
    }

    @Test
    fun `closing the screen leaves the Programme playing`() = runTest(dispatcher) {
        val controller = playing()
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(store, factory(controller))[WarmupViewModel::class.java]
        runCurrent()
        assertTrue(viewModel.uiState.value?.playing == true)
        store.clear()
        runCurrent()
        assertEquals(true, controller.playback.value?.playing)
    }
}
