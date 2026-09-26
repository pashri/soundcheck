package org.pashri.soundcheck.ui.sounds

import java.io.File
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pashri.soundcheck.data.ClipFiles
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.warmup.Audition
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.testAudition
import org.pashri.soundcheck.warmup.withClip

@OptIn(ExperimentalCoroutinesApi::class)
class SoundsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private val mimStep = StepRef(programmeId = starter, key = StepKey("starter-3"))
    private var ids = 0
    private var takes = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @get:Rule
    val folder = TemporaryFolder()

    private val clips: ClipFiles by lazy {
        ClipFiles(
            directory = File(folder.root, "clips"),
            io = dispatcher,
            newName = { "take-${++takes}" },
        )
    }

    private var audition: Audition? = null

    private fun TestScope.viewModel(
        pickFor: StepRef? = null,
        store: FakeStore<Library> = library,
    ): SoundsViewModel {
        val shared = testAudition().also { audition = it }
        return SoundsViewModel.Factory(
            pickFor = pickFor,
            library = store,
            newId = { "id-${++ids}" },
            clips = clips,
            audition = shared,
        ).create(SoundsViewModel::class.java)
    }

    /** Gives mim a recording, saved as a real file. */
    private suspend fun recordMim() {
        val name = clips.save(FloatArray(size = 4_800) { 0.5f })
        val clip = RecordedClip(name = name, lengthMs = 100)
        library.set(library.value.withClip(id = StarterSounds.MIM.id, clip = clip))
    }

    private fun mimRow(state: SoundsUiState?): SoundRow? =
        state?.rows?.single { it.id == StarterSounds.MIM.id }

    private fun TestScope.state(viewModel: SoundsViewModel): SoundsUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `a new Sound is added with its label trimmed and a fresh id`() =
        runTest(context = dispatcher) {
            assertEquals(SoundId("id-1"), viewModel().add(" vroom "))
            assertEquals(Sound(id = SoundId("id-1"), label = "vroom"), library.value.sounds.last())
        }

    @Test
    fun `renaming a Sound keeps its Steps`() = runTest(context = dispatcher) {
        viewModel().rename(id = StarterSounds.MIM.id, label = "mmm")
        assertEquals("mmm", library.value.sound(StarterSounds.MIM.id)?.label)
        assertEquals(
            StarterSounds.MIM.id,
            library.value.programme(starter)?.step(mimStep.key)?.soundId,
        )
    }

    @Test
    fun `choosing for a Step changes its Sound`() = runTest(context = dispatcher) {
        viewModel(pickFor = mimStep).choose(StarterSounds.EE.id)
        assertEquals(
            StarterSounds.EE.id,
            library.value.programme(starter)?.step(mimStep.key)?.soundId,
        )
    }

    @Test
    fun `deleting a Sound removes its Steps`() = runTest(context = dispatcher) {
        viewModel().delete(StarterSounds.HUM.id)
        assertNull(library.value.sound(StarterSounds.HUM.id))
        assertEquals(5, library.value.programme(starter)?.steps?.size)
    }

    @Test
    fun `nothing is shown until the library has loaded`() = runTest(context = dispatcher) {
        assertNull(state(viewModel(store = FakeStore(null))))
    }

    @Test
    fun `play plays a Sound's recording and a second tap stops it`() =
        runTest(context = dispatcher) {
            recordMim()
            val viewModel = viewModel()
            viewModel.play(StarterSounds.MIM.id)
            assertEquals(true, mimRow(state(viewModel))?.playing)
            assertEquals(true, audition?.playing?.value)
            viewModel.play(StarterSounds.MIM.id)
            assertEquals(false, mimRow(state(viewModel))?.playing)
            assertEquals(false, audition?.playing?.value)
        }

    @Test
    fun `a recording whose file has gone says it can't be played`() =
        runTest(context = dispatcher) {
            val gone = RecordedClip(name = ClipName("gone.wav"), lengthMs = 600)
            library.set(library.value.withClip(id = StarterSounds.MIM.id, clip = gone))
            val viewModel = viewModel()
            viewModel.play(StarterSounds.MIM.id)
            assertEquals(
                "Couldn't play that recording. Record it again?",
                state(viewModel)?.notice,
            )
            assertEquals(false, audition?.playing?.value)
        }

    @Test
    fun `leaving the screen stops the recording it was playing`() =
        runTest(context = dispatcher) {
            recordMim()
            val viewModel = viewModel()
            viewModel.play(StarterSounds.MIM.id)
            runCurrent()
            viewModel.onHidden()
            assertEquals(false, audition?.playing?.value)
        }

    @Test
    fun `a Sound with no recording has nothing to play`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.play(StarterSounds.HUM.id)
        runCurrent()
        assertEquals(false, audition?.playing?.value)
        assertNull(state(viewModel)?.notice)
    }
}
