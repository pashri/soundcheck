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
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef

@OptIn(ExperimentalCoroutinesApi::class)
class PatternListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private val hum = StepRef(programmeId = starter, key = StepKey("starter-2"))
    private var ids = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        pickFor: StepRef? = null,
        store: FakeStore<Library> = library,
    ): PatternListViewModel =
        PatternListViewModel.Factory(pickFor = pickFor, library = store, newId = { "id-${++ids}" })
            .create(PatternListViewModel::class.java)

    private fun TestScope.state(viewModel: PatternListViewModel): PatternListUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `the library lists every Pattern with its notes, chord and use`() = runTest(dispatcher) {
        val state = checkNotNull(state(viewModel()))
        assertEquals("Patterns", state.title)
        assertEquals("Warm-up", state.backLabel)
        assertEquals("8 PATTERNS", state.countLabel)
        assertFalse(state.picking)
        assertEquals(
            PatternRow(
                id = StarterPatterns.TRIAD.id,
                name = "Triad",
                detail = "1 3 5 3 1 · major",
                usage = "Used in 1 Step",
                chosen = false,
            ),
            state.rows[4],
        )
        assertEquals("1 2 ♭3 4 5 4 ♭3 2 1 · minor", state.rows[5].detail)
        assertEquals("Not in any Step", state.rows[5].usage)
    }

    @Test
    fun `choosing for a Step marks its Pattern and changes it`() = runTest(dispatcher) {
        val viewModel = viewModel(pickFor = hum)
        val before = checkNotNull(state(viewModel))
        assertEquals("Choose a Pattern", before.title)
        assertEquals("Step", before.backLabel)
        assertTrue(before.rows[4].chosen)
        viewModel.choose(StarterPatterns.NINE_NOTE_SCALE.id)
        assertEquals(
            StarterPatterns.NINE_NOTE_SCALE.id,
            library.value.programme(starter)?.step(hum.key)?.patternId,
        )
        assertTrue(checkNotNull(state(viewModel)).rows[6].chosen)
    }

    @Test
    fun `a new Pattern gets a fresh name and comes last`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(PatternId("id-1"), viewModel.addPattern())
        assertEquals(PatternId("id-2"), viewModel.addPattern())
        assertEquals(
            listOf("New pattern", "New pattern 2"),
            library.value.patterns.takeLast(2).map { it.name },
        )
    }

    @Test
    fun `nothing is shown until the library has loaded`() = runTest(dispatcher) {
        assertNull(state(viewModel(store = FakeStore(null))))
    }
}
