package org.pashri.soundcheck.ui.sounds

import java.io.File
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeMicInput
import org.pashri.soundcheck.audio.ToolArbiter
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
    private val arbiter = ToolArbiter()

    private fun TestScope.viewModel(
        pickFor: StepRef? = null,
        store: FakeStore<Library> = library,
    ): SoundsViewModel {
        val shared = testAudition(arbiter = arbiter).also { audition = it }
        return SoundsViewModel.Factory(
            pickFor = pickFor,
            library = store,
            newId = { "id-${++ids}" },
            clips = clips,
            audition = shared,
            mic = mic,
            focus = FakeFocusGate(),
            arbiter = arbiter,
            worker = dispatcher,
        ).create(SoundsViewModel::class.java)
    }

    private val mic = FakeMicInput()

    /** [hops] hops of 1 024 frames at [level], the sign alternating frame by frame. */
    private fun hops(hops: Int, level: Float): FloatArray =
        FloatArray(size = hops * 1_024) { if (it % 2 == 0) level else -level }

    /** Queues 10 quiet hops, 15 spoken, 10 quiet: a 420 ms take once trimmed. */
    private fun sayAWord() {
        mic.play(
            signal = hops(hops = 10, level = 0.001f) + hops(hops = 15, level = 0.3f) +
                hops(hops = 10, level = 0.001f),
        )
    }

    /** Holds the record button for [ms] of test time, then lets go and lets the take save. */
    private fun TestScope.hold(viewModel: SoundsViewModel, ms: Long = 800) {
        viewModel.startRecording()
        advanceTimeBy(ms)
        viewModel.stopRecording()
        advanceTimeBy(100)
        runCurrent()
    }

    /** A view model with neh's recorder open and the microphone allowed. */
    private fun TestScope.recordingNeh(): SoundsViewModel = viewModel().also {
        it.onShown(granted = true)
        it.toggleRecorder(StarterSounds.NEH.id)
    }

    private fun nehClip(): RecordedClip? = library.value.sound(StarterSounds.NEH.id)?.clip

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

    @Test
    fun `holding the button records a take and gives the Sound its recording`() =
        runTest(context = dispatcher) {
            val viewModel = recordingNeh()
            sayAWord()
            hold(viewModel)
            assertEquals(420L, nehClip()?.lengthMs)
            val file = File(folder.root, "clips/${nehClip()?.name?.value}")
            assertTrue(file.exists())
            val state = state(viewModel)
            assertEquals("0.4 s", state?.rows?.single { it.id == StarterSounds.NEH.id }?.detail)
            assertNull(state?.panel?.message)
            assertEquals(true, state?.panel?.canUndo)
        }

    @Test
    fun `a take with nothing said leaves the Sound as it was and says why`() =
        runTest(context = dispatcher) {
            val viewModel = recordingNeh()
            hold(viewModel)
            assertNull(nehClip())
            assertEquals(
                "Didn't hear anything. Hold the button while you speak.",
                state(viewModel)?.panel?.message,
            )
        }

    @Test
    fun `a new take can be undone back to the one before`() = runTest(context = dispatcher) {
        val oldName = clips.save(FloatArray(size = 9_600) { 0.2f })
        val old = RecordedClip(name = oldName, lengthMs = 200)
        library.set(library.value.withClip(id = StarterSounds.NEH.id, clip = old))
        val viewModel = recordingNeh()
        sayAWord()
        hold(viewModel)
        assertNotEquals(old, nehClip())
        viewModel.undo(StarterSounds.NEH.id)
        assertEquals(old, nehClip())
        assertEquals(false, state(viewModel)?.panel?.canUndo)
    }

    @Test
    fun `giving up a recording brings back the phone's voice, and can be undone`() =
        runTest(context = dispatcher) {
            recordMim()
            val recorded = library.value.sound(StarterSounds.MIM.id)?.clip
            val viewModel = viewModel()
            viewModel.usePhoneVoice(StarterSounds.MIM.id)
            assertNull(library.value.sound(StarterSounds.MIM.id)?.clip)
            viewModel.undo(StarterSounds.MIM.id)
            assertEquals(recorded, library.value.sound(StarterSounds.MIM.id)?.clip)
        }

    @Test
    fun `opening the recorder before the microphone is allowed asks for it`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            viewModel.onShown(granted = false)
            assertTrue(viewModel.toggleRecorder(StarterSounds.NEH.id))
            assertEquals(PanelMode.ASK, state(viewModel)?.panel?.mode)
            viewModel.onPermissionResult(granted = true, canAskAgain = false)
            assertEquals(PanelMode.READY, state(viewModel)?.panel?.mode)
            assertFalse(viewModel.toggleRecorder(StarterSounds.NEH.id))
            assertNull(state(viewModel)?.panel)
        }

    @Test
    fun `no take starts without the microphone allowed`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.toggleRecorder(StarterSounds.NEH.id)
        hold(viewModel)
        assertEquals(0, mic.timesOpened)
        assertNull(nehClip())
    }

    @Test
    fun `leaving the screen throws a take in progress away`() = runTest(context = dispatcher) {
        val viewModel = recordingNeh()
        sayAWord()
        viewModel.startRecording()
        advanceTimeBy(300)
        viewModel.onHidden()
        advanceTimeBy(100)
        runCurrent()
        assertNull(nehClip())
        assertEquals(0, mic.openNow)
        assertNull(state(viewModel)?.panel?.message)
    }

    @Test
    fun `a take that can't be saved says so and keeps what the Sound had`() =
        runTest(context = dispatcher) {
            folder.newFile("clips")
            val viewModel = recordingNeh()
            sayAWord()
            hold(viewModel)
            assertNull(nehClip())
            assertEquals(
                "Couldn't save the recording. Is the phone's storage full?",
                state(viewModel)?.panel?.message,
            )
        }

    @Test
    fun `starting a take stops a recording that is playing`() =
        runTest(context = dispatcher) {
            recordMim()
            val viewModel = recordingNeh()
            viewModel.play(StarterSounds.MIM.id)
            assertEquals(true, mimRow(state(viewModel))?.playing)
            viewModel.startRecording()
            runCurrent()
            assertEquals(false, audition?.playing?.value)
            assertEquals(false, mimRow(state(viewModel))?.playing)
            viewModel.stopRecording()
        }

    @Test
    fun `deleting the Sound being recorded throws the take away and closes its recorder`() =
        runTest(context = dispatcher) {
            val viewModel = recordingNeh()
            sayAWord()
            viewModel.startRecording()
            advanceTimeBy(300)
            viewModel.delete(StarterSounds.NEH.id)
            advanceTimeBy(100)
            runCurrent()
            assertEquals(0, mic.openNow)
            advanceTimeBy(6_000)
            runCurrent()
            assertNull(state(viewModel)?.panel)
            assertTrue(File(folder.root, "clips").list().isNullOrEmpty())
        }
}
