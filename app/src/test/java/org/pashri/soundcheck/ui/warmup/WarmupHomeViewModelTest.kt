package org.pashri.soundcheck.ui.warmup

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.deleteProgramme
import org.pashri.soundcheck.warmup.testController

@OptIn(ExperimentalCoroutinesApi::class)
class WarmupHomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val focus = FakeFocusGate()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val settings = FakeStore(WarmupSettings.DEFAULT)
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private var ids = 0

    /** C4 to D4: two half-steps, narrower than every starter Pattern. */
    private val narrow = WarmupSettings(
        voiceType = VoiceType.TENOR,
        range = Range(lowest = Pitch.parse("C4"), highest = Pitch.parse("D4")),
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
        library: Store<Library> = this@WarmupHomeViewModelTest.library,
    ): WarmupHomeViewModel = WarmupHomeViewModel.Factory(
        controller = controller,
        library = library,
        settings = settings,
        newId = { "id-${++ids}" },
    ).create(WarmupHomeViewModel::class.java)

    private fun TestScope.state(viewModel: WarmupHomeViewModel): WarmupHomeUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `nothing is shown until the library and the settings have loaded`() =
        runTest(context = dispatcher) {
            val unloaded = FakeStore<Library>(null)
            val viewModel = viewModel(library = unloaded)
            assertNull(state(viewModel))
            unloaded.set(StarterLibrary.LIBRARY)
            assertNotNull(state(viewModel))
        }

    @Test
    fun `start plays the Programme on the Range from Settings`() = runTest(context = dispatcher) {
        settings.set(
            WarmupSettings.DEFAULT.copy(voiceType = VoiceType.BASS, range = VoiceType.BASS.range),
        )
        val controller = testController(focus)
        val viewModel = viewModel(controller = controller)
        assertTrue(viewModel.start(starter))
        runCurrent()
        assertEquals(VoiceType.BASS.range, controller.playback.value?.range)
        assertEquals("Starter warm-up", controller.playback.value?.programme?.name)
    }

    @Test
    fun `a Programme that doesn't fit the Range explains why`() = runTest(context = dispatcher) {
        settings.set(narrow)
        val viewModel = viewModel()
        assertFalse(viewModel.start(starter))
        assertEquals(
            startProblemMessage(StartOutcome.NOTHING_FITS),
            state(viewModel)?.programmes?.single()?.problem,
        )
    }

    @Test
    fun `a later start that plays clears the problem`() = runTest(context = dispatcher) {
        settings.set(narrow)
        val viewModel = viewModel()
        viewModel.start(starter)
        settings.set(WarmupSettings.DEFAULT)
        assertTrue(viewModel.start(starter))
        assertNull(state(viewModel)?.programmes?.single()?.problem)
    }

    @Test
    fun `a new Programme is empty and gets a fresh name`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        assertEquals(ProgrammeId("id-1"), viewModel.newProgramme())
        assertEquals(ProgrammeId("id-2"), viewModel.newProgramme())
        val added = library.value.programmes.drop(1)
        assertEquals(listOf("New programme", "New programme 2"), added.map { it.name })
        assertTrue(added.all { it.steps.isEmpty() })
    }

    @Test
    fun `deleting the playing Programme leaves it playing`() = runTest(context = dispatcher) {
        val controller = testController(focus)
        val viewModel = viewModel(controller = controller)
        viewModel.start(starter)
        runCurrent()
        library.edit { it.deleteProgramme(starter) }
        val shown = state(viewModel)
        assertEquals(true, controller.playback.value?.playing)
        assertEquals("Starter warm-up · Step 1 of 6", shown?.nowPlaying?.title)
        assertTrue(shown?.programmes.orEmpty().isEmpty())
    }

    @Test
    fun `a failed save of either document is shown`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        assertNull(state(viewModel)?.saveProblem)
        settings.saveFailed.value = true
        assertNotNull(state(viewModel)?.saveProblem)
        settings.saveFailed.value = false
        assertNull(state(viewModel)?.saveProblem)
        library.saveFailed.value = true
        assertNotNull(state(viewModel)?.saveProblem)
    }

    @Test
    fun `a library that couldn't be opened is explained instead of a full phone`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            library.saveFailed.value = true
            library.unopened.value = true
            assertEquals(
                "Your saved library couldn't be opened, so changes won't be kept.",
                state(viewModel)?.saveProblem,
            )
        }

    @Test
    fun `a document set aside is explained until it's dismissed`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            assertNull(state(viewModel)?.restoredNotice)
            library.setAside.value = true
            assertEquals(LIBRARY_RESTORED_NOTICE, state(viewModel)?.restoredNotice)
            viewModel.dismissRestoredNotice()
            assertNull(state(viewModel)?.restoredNotice)
        }

    @Test
    fun `the settings being set aside is explained separately from the library`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            settings.setAside.value = true
            assertEquals(SETTINGS_RESTORED_NOTICE, state(viewModel)?.restoredNotice)
        }
}
