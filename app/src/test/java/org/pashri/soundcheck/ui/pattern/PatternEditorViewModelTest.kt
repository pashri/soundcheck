package org.pashri.soundcheck.ui.pattern

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
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.ui.components.EditorState
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.Audition
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.NoteLength
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.PatternNote
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.testAudition

@OptIn(ExperimentalCoroutinesApi::class)
class PatternEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val triad = StarterPatterns.TRIAD.id
    private val starter = StarterProgrammes.SAVED_WARM_UP.id

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        audition: Audition = testAudition(),
        settings: FakeStore<WarmupSettings> = FakeStore(WarmupSettings.DEFAULT),
    ): PatternEditorViewModel = PatternEditorViewModel.Factory(
        patternId = triad,
        library = library,
        settings = settings,
        audition = audition,
    ).create(PatternEditorViewModel::class.java)

    private fun saved(): Pattern = checkNotNull(library.value.pattern(triad))

    private fun degrees(): String = PatternNotation.degrees(saved().notes)

    private fun TestScope.shown(viewModel: PatternEditorViewModel): PatternEditorUiState {
        runCurrent()
        return (viewModel.uiState.value as EditorState.Ready<PatternEditorUiState>).value
    }

    @Test
    fun `a Step follows an edit to its Pattern`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.select(2)
        viewModel.raiseDegree()
        assertEquals("1 3 6 3 1", degrees())
        val hum = library.value.programmeToPlay(starter)?.steps?.get(1)
        assertEquals("1 3 6 3 1", hum?.pattern?.notes?.let(PatternNotation::degrees))
    }

    @Test
    fun `adding a note copies the selected one and selects the copy`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            viewModel.select(1)
            viewModel.addNote()
            assertEquals("1 3 3 5 3 1", degrees())
            assertEquals(2, shown(viewModel).selected)
        }

    @Test
    fun `deleting notes keeps one selected, and the last note stays`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            viewModel.select(4)
            viewModel.deleteNote()
            assertEquals("1 3 5 3", degrees())
            assertEquals(3, shown(viewModel).selected)
            repeat(times = 5) { viewModel.deleteNote() }
            assertEquals("1", degrees())
        }

    @Test
    fun `the accidental, length and Key Chord are saved`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.select(1)
        viewModel.setAccidental(Accidental.FLAT)
        viewModel.setLength(NoteLength.WHOLE)
        viewModel.setKeyChord(KeyChord.MINOR)
        assertEquals(
            PatternNote(degree = 3, length = NoteLength.WHOLE, accidental = Accidental.FLAT),
            saved().notes[1],
        )
        assertEquals(KeyChord.MINOR, saved().keyChord)
    }

    @Test
    fun `renaming trims the name`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.rename("  Big triad ")
        assertEquals("Big triad", saved().name)
    }

    @Test
    fun `deleting the Pattern removes its Steps and closes the editor`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            viewModel.delete()
            runCurrent()
            assertEquals(EditorState.Gone, viewModel.uiState.value)
            assertNull(library.value.pattern(triad))
            assertEquals(5, library.value.programme(starter)?.steps?.size)
        }

    @Test
    fun `playing the Pattern sounds it and a second tap stops it`() =
        runTest(context = dispatcher) {
            val audition = testAudition()
            val viewModel = viewModel(audition = audition)
            viewModel.playPattern()
            runCurrent()
            assertTrue(viewModel.auditioning.value)
            viewModel.playPattern()
            assertFalse(audition.playing.value)
        }

    @Test
    fun `playing the Pattern does nothing until settings have loaded`() =
        runTest(context = dispatcher) {
            val audition = testAudition()
            val viewModel = viewModel(audition = audition, settings = FakeStore(null))
            viewModel.playPattern()
            runCurrent()
            assertFalse(audition.playing.value)
        }
}
