package org.pashri.soundcheck.ui.tuner

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
import org.pashri.soundcheck.audio.FakeMicInput
import org.pashri.soundcheck.audio.FakeMicInput.Companion.HOP_MS
import org.pashri.soundcheck.tuner.HOP_SIZE
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.Signals

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mic = FakeMicInput()
    private val focus = FakeFocusGate()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TunerViewModel(mic, focus, worker = dispatcher)

    private fun TestScope.state(viewModel: TunerViewModel): TunerUiState {
        runCurrent()
        return viewModel.uiState.value
    }

    private fun TestScope.hops(count: Int) {
        advanceTimeBy(count * HOP_MS)
        runCurrent()
    }

    @Test
    fun `before the permission is known it offers to allow the microphone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = false)
            assertEquals(TunerMode.AskPermission, state(viewModel).mode)
            assertEquals(0, mic.timesOpened)
        }

    @Test
    fun `it asks on open only once`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = false)
        assertTrue(viewModel.shouldAskOnOpen())
        assertFalse(viewModel.shouldAskOnOpen())
        viewModel.stop()
        viewModel.onShown(granted = false)
        assertFalse(viewModel.shouldAskOnOpen())
    }

    @Test
    fun `it does not ask on open when the permission is already granted`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            assertFalse(viewModel.shouldAskOnOpen())
            viewModel.stop()
        }

    @Test
    fun `with the permission granted it listens while shown and hears a note`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            mic.play(Signals.sine(110.0, size = 12 * HOP_SIZE))
            hops(12)
            val state = state(viewModel)
            assertEquals(TunerMode.Listening, state.mode)
            assertEquals("A", state.note?.name)
            assertEquals(1, mic.openNow)
            viewModel.stop()
        }

    @Test
    fun `allowing the microphone in the dialog starts listening`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = false)
        viewModel.onPermissionResult(granted = true, canAskAgain = false)
        hops(1)
        assertEquals(TunerMode.Listening, state(viewModel).mode)
        assertEquals(MicStatus.Listening, state(viewModel).mic)
        viewModel.stop()
    }

    @Test
    fun `a first refusal offers the dialog again and never opens the microphone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = false)
            viewModel.onPermissionResult(granted = false, canAskAgain = true)
            hops(5)
            assertEquals(TunerMode.AskPermission, state(viewModel).mode)
            assertEquals(0, mic.timesOpened)
        }

    @Test
    fun `a refusal Android will not ask again sends the user to settings`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = false)
            viewModel.onPermissionResult(granted = false, canAskAgain = false)
            assertEquals(TunerMode.OpenSettings, state(viewModel).mode)
            viewModel.stop()
            viewModel.onShown(granted = false)
            assertEquals(TunerMode.OpenSettings, state(viewModel).mode)
            assertEquals(0, mic.timesOpened)
        }

    @Test
    fun `coming back from settings with the microphone allowed starts listening`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = false)
            viewModel.onPermissionResult(granted = false, canAskAgain = false)
            viewModel.stop()
            viewModel.onShown(granted = true)
            hops(1)
            assertEquals(TunerMode.Listening, state(viewModel).mode)
            assertEquals(1, mic.openNow)
            viewModel.stop()
        }

    @Test
    fun `hiding the screen releases the microphone and showing it again reopens it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            hops(2)
            viewModel.stop()
            hops(1)
            assertEquals(0, mic.openNow)
            assertEquals(MicStatus.Off, state(viewModel).mic)
            viewModel.onShown(granted = true)
            hops(1)
            assertEquals(2, mic.timesOpened)
            assertEquals(1, mic.openNow)
            viewModel.stop()
        }

    @Test
    fun `a permission answer that arrives while hidden does not open the microphone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = false)
            viewModel.stop()
            viewModel.onPermissionResult(granted = true, canAskAgain = false)
            hops(2)
            assertEquals(0, mic.timesOpened)
        }

    @Test
    fun `being shown again while listening keeps the one microphone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            hops(2)
            viewModel.onShown(granted = true)
            hops(2)
            assertEquals(1, mic.timesOpened)
            viewModel.stop()
        }

    @Test
    fun `focus is acquired once per listening run, not on every onShown`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            hops(2)
            viewModel.onShown(granted = true)
            hops(2)
            assertEquals(1, focus.acquireCount)
            viewModel.stop()
        }

    @Test
    fun `an unavailable microphone offers to try again and trying again listens`() =
        runTest(dispatcher) {
            mic.available = false
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            assertEquals(TunerMode.MicUnavailable, state(viewModel).mode)
            mic.available = true
            viewModel.retry()
            hops(1)
            assertEquals(TunerMode.Listening, state(viewModel).mode)
            viewModel.stop()
        }

    @Test
    fun `retrying with the microphone still unavailable does not hold focus`() =
        runTest(dispatcher) {
            mic.available = false
            val viewModel = viewModel()
            viewModel.onShown(granted = true)
            runCurrent()
            viewModel.retry()
            runCurrent()
            assertEquals(TunerMode.MicUnavailable, state(viewModel).mode)
            assertFalse(focus.held)
        }

    @Test
    fun `the answer to a permission request maps to access`() {
        assertEquals(MicAccess.Granted, micAccessAfterRequest(granted = true, canAskAgain = true))
        assertEquals(MicAccess.Granted, micAccessAfterRequest(granted = true, canAskAgain = false))
        assertEquals(MicAccess.Denied, micAccessAfterRequest(granted = false, canAskAgain = true))
        val blocked = micAccessAfterRequest(granted = false, canAskAgain = false)
        assertEquals(MicAccess.Blocked, blocked)
    }

    @Test
    fun `nothing is heard before the screen is shown`() = runTest(dispatcher) {
        val viewModel = viewModel()
        hops(5)
        assertNull(state(viewModel).note)
        assertEquals(0, mic.timesOpened)
    }

    @Test
    fun `listening holds audio focus so a podcast pauses`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        hops(1)
        assertTrue(focus.held)
        assertEquals(1, mic.openNow)
        viewModel.stop()
    }

    @Test
    fun `focus is never taken while the microphone is not allowed`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = false)
        runCurrent()
        assertFalse(focus.held)
        viewModel.onPermissionResult(granted = false, canAskAgain = true)
        runCurrent()
        assertFalse(focus.held)
        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        runCurrent()
        assertFalse(focus.held)
    }

    @Test
    fun `hiding the screen hands focus back`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        hops(1)
        viewModel.stop()
        runCurrent()
        assertFalse(focus.held)
    }

    @Test
    fun `losing the permission stops listening and hands focus back`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        hops(1)
        viewModel.onShown(granted = false)
        hops(1)
        val held = focus.held
        val openNow = mic.openNow
        viewModel.stop()
        assertFalse(held)
        assertEquals(0, openNow)
    }

    @Test
    fun `a microphone that will not open hands focus back`() = runTest(dispatcher) {
        mic.available = false
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        runCurrent()
        assertEquals(TunerMode.MicUnavailable, state(viewModel).mode)
        assertFalse(focus.held)
    }

    @Test
    fun `a microphone that dies mid-tune hands focus back`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        hops(2)
        mic.breakMic()
        hops(1)
        assertEquals(TunerMode.MicUnavailable, state(viewModel).mode)
        assertFalse(focus.held)
    }

    @Test
    fun `trying again after a failure takes focus again`() = runTest(dispatcher) {
        mic.available = false
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        runCurrent()
        mic.available = true
        viewModel.retry()
        hops(1)
        assertTrue(focus.held)
        viewModel.stop()
    }

    @Test
    fun `closing the view model hands focus back and releases the microphone`() =
        runTest(dispatcher) {
            val store = ViewModelStore()
            val factory = TunerViewModel.Factory(mic, focus, dispatcher)
            val viewModel = ViewModelProvider(store, factory)[TunerViewModel::class.java]
            viewModel.onShown(granted = true)
            hops(1)
            store.clear()
            hops(1)
            assertFalse(focus.held)
            assertEquals(0, mic.openNow)
        }

    @Test
    fun `losing focus to another app keeps the tuner listening`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        hops(1)
        focus.loseFocus()
        mic.play(Signals.sine(110.0, size = 12 * HOP_SIZE))
        hops(12)
        assertEquals(MicStatus.Listening, state(viewModel).mic)
        assertEquals("A", state(viewModel).note?.name)
        assertEquals(1, mic.openNow)
        viewModel.stop()
    }

    @Test
    fun `refused focus does not stop the tuner tuning`() = runTest(dispatcher) {
        focus.grant = false
        val viewModel = viewModel()
        viewModel.onShown(granted = true)
        mic.play(Signals.sine(110.0, size = 12 * HOP_SIZE))
        hops(12)
        assertEquals(TunerMode.Listening, state(viewModel).mode)
        assertEquals("A", state(viewModel).note?.name)
        viewModel.stop()
    }
}
