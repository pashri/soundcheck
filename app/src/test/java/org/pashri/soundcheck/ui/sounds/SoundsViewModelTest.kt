package org.pashri.soundcheck.ui.sounds

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef

@OptIn(ExperimentalCoroutinesApi::class)
class SoundsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private val mimStep = StepRef(programmeId = starter, key = StepKey("starter-3"))
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
    ): SoundsViewModel =
        SoundsViewModel.Factory(pickFor = pickFor, library = store, newId = { "id-${++ids}" })
            .create(SoundsViewModel::class.java)

    private fun TestScope.state(viewModel: SoundsViewModel): SoundsUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `a new Sound is added with its label trimmed and a fresh id`() = runTest(dispatcher) {
        assertEquals(SoundId("id-1"), viewModel().add(" vroom "))
        assertEquals(Sound(id = SoundId("id-1"), label = "vroom"), library.value.sounds.last())
    }

    @Test
    fun `renaming a Sound keeps its Steps`() = runTest(dispatcher) {
        viewModel().rename(id = StarterSounds.MIM.id, label = "mmm")
        assertEquals("mmm", library.value.sound(StarterSounds.MIM.id)?.label)
        assertEquals(
            StarterSounds.MIM.id,
            library.value.programme(starter)?.step(mimStep.key)?.soundId,
        )
    }

    @Test
    fun `choosing for a Step changes its Sound`() = runTest(dispatcher) {
        viewModel(pickFor = mimStep).choose(StarterSounds.EE.id)
        assertEquals(
            StarterSounds.EE.id,
            library.value.programme(starter)?.step(mimStep.key)?.soundId,
        )
    }

    @Test
    fun `deleting a Sound removes its Steps`() = runTest(dispatcher) {
        viewModel().delete(StarterSounds.HUM.id)
        assertNull(library.value.sound(StarterSounds.HUM.id))
        assertEquals(5, library.value.programme(starter)?.steps?.size)
    }

    @Test
    fun `nothing is shown until the library has loaded`() = runTest(dispatcher) {
        assertNull(state(viewModel(store = FakeStore(null))))
    }
}
