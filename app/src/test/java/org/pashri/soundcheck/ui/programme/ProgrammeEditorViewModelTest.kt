package org.pashri.soundcheck.ui.programme

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
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.ui.components.EditorState
import org.pashri.soundcheck.ui.warmup.startProblemMessage
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.newStep
import org.pashri.soundcheck.warmup.testController

@OptIn(ExperimentalCoroutinesApi::class)
class ProgrammeEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val focus = FakeFocusGate()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val settings = FakeStore(WarmupSettings.DEFAULT)
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private var ids = 0

    private fun narrow(lowest: String, highest: String): WarmupSettings = WarmupSettings(
        voiceType = VoiceType.TENOR,
        range = Range(lowest = Pitch.parse(lowest), highest = Pitch.parse(highest)),
        playOverOtherAudio = false,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        controller: WarmupController = testController(focus),
        store: FakeStore<Library> = library,
    ): ProgrammeEditorViewModel = ProgrammeEditorViewModel.Factory(
        programmeId = starter,
        controller = controller,
        library = store,
        settings = settings,
        newId = { "id-${++ids}" },
    ).create(ProgrammeEditorViewModel::class.java)

    private fun TestScope.shown(viewModel: ProgrammeEditorViewModel): ProgrammeEditorUiState {
        runCurrent()
        val state = viewModel.uiState.value
        return (state as EditorState.Ready<ProgrammeEditorUiState>).value
    }

    private fun keys(): List<String> =
        checkNotNull(library.value.programme(starter)).steps.map { it.key.value }

    @Test
    fun `nothing is shown until the library has loaded`() = runTest(dispatcher) {
        val unloaded = FakeStore<Library>(null)
        val viewModel = viewModel(store = unloaded)
        runCurrent()
        assertEquals(EditorState.Loading, viewModel.uiState.value)
        unloaded.set(StarterLibrary.LIBRARY)
        assertEquals("Starter warm-up", shown(viewModel).name)
    }

    @Test
    fun `renaming trims the name`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.rename("  Evening ")
        assertEquals("Evening", shown(viewModel).name)
    }

    @Test
    fun `adding a Step appends the default Step and gives its key`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val key = viewModel.addStep()
        assertEquals(StepKey("id-1"), key)
        assertEquals(
            StarterLibrary.LIBRARY.newStep(StepKey("id-1")),
            library.value.programme(starter)?.steps?.last(),
        )
        assertEquals("7 STEPS", shown(viewModel).stepsLabel)
    }

    @Test
    fun `removing and moving Steps change the list`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.removeStep(StepKey("starter-2"))
        viewModel.moveStep(from = 0, to = 4)
        assertEquals(
            listOf("starter-3", "starter-4", "starter-5", "starter-6", "starter-1"),
            keys(),
        )
    }

    @Test
    fun `deleting the Programme closes the editor`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.delete()
        runCurrent()
        assertEquals(EditorState.Gone, viewModel.uiState.value)
        assertTrue(library.value.programmes.isEmpty())
    }

    @Test
    fun `start plays the Programme on the Range from Settings`() = runTest(dispatcher) {
        val controller = testController(focus)
        val viewModel = viewModel(controller = controller)
        assertTrue(viewModel.start())
        runCurrent()
        assertEquals(true, controller.playback.value?.playing)
        assertNull(shown(viewModel).problem)
    }

    @Test
    fun `a Start where nothing fits explains why`() = runTest(dispatcher) {
        settings.set(narrow(lowest = "C4", highest = "D4"))
        val viewModel = viewModel()
        assertFalse(viewModel.start())
        assertEquals(startProblemMessage(StartOutcome.NOTHING_FITS), shown(viewModel).problem)
    }

    @Test
    fun `narrowing the Range in Settings warns on the Step that no longer fits`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            assertNull(shown(viewModel).rows[2].warning)
            settings.set(narrow(lowest = "C3", highest = "G3"))
            assertEquals(
                "Needs 12 half-steps; this Step's Range has 7. It will be skipped.",
                shown(viewModel).rows[2].warning,
            )
        }
}
