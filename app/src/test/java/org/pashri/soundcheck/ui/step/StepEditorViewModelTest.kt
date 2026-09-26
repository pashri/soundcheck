package org.pashri.soundcheck.ui.step

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
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.ui.components.EditorState
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.updateStep

@OptIn(ExperimentalCoroutinesApi::class)
class StepEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val settings = FakeStore(WarmupSettings.DEFAULT)
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private val ref = StepRef(programmeId = starter, key = StepKey("starter-1"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): StepEditorViewModel =
        StepEditorViewModel.Factory(ref = ref, library = library, settings = settings)
            .create(StepEditorViewModel::class.java)

    private fun saved(): SavedStep = checkNotNull(library.value.programme(starter)?.step(ref.key))

    private fun TestScope.shown(viewModel: StepEditorViewModel): StepEditorUiState {
        runCurrent()
        return (viewModel.uiState.value as EditorState.Ready<StepEditorUiState>).value
    }

    @Test
    fun `faster and slower move the tempo a beat per minute and stop at the limits`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.faster()
            assertEquals(91, saved().bpm)
            library.edit { it.updateStep(ref) { step -> step.copy(bpm = 30) } }
            viewModel.slower()
            assertEquals(30, saved().bpm)
        }

    @Test
    fun `lowering the bottom widens the Range downwards`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.lowerBottom()
        assertEquals(RangeOffset(bottom = 1, top = 2), saved().rangeOffset)
        val state = shown(viewModel)
        assertEquals("+1", state.bottom)
        assertEquals("B2 – B4 · 35 Iterations", state.tripLabel)
    }

    @Test
    fun `the top can be raised and lowered and the bottom raised`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.raiseTop()
        assertEquals(3, saved().rangeOffset.top)
        viewModel.lowerTop()
        viewModel.lowerTop()
        viewModel.raiseBottom()
        assertEquals(RangeOffset(bottom = -1, top = 1), saved().rangeOffset)
    }

    @Test
    fun `the Direction and Guide Melody are saved`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setDirection(Direction.START_HIGH)
        viewModel.setGuideMelody(false)
        assertEquals(Direction.START_HIGH, saved().direction)
        assertFalse(saved().guideMelody)
        assertEquals(Direction.START_HIGH, shown(viewModel).direction)
    }

    @Test
    fun `an edit shows in the Programme that plays next`() = runTest(dispatcher) {
        viewModel().faster()
        assertEquals(91, library.value.programmeToPlay(starter)?.steps?.first()?.bpm)
    }

    @Test
    fun `removing the Step closes the editor`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.remove()
        runCurrent()
        assertEquals(EditorState.Gone, viewModel.uiState.value)
        assertEquals(5, library.value.programme(starter)?.steps?.size)
    }
}
