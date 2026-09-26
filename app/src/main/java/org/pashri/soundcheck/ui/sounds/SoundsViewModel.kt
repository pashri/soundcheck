package org.pashri.soundcheck.ui.sounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.data.ClipFiles
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.sounds.Recorder
import org.pashri.soundcheck.sounds.Take
import org.pashri.soundcheck.ui.tuner.MicAccess
import org.pashri.soundcheck.ui.tuner.micAccessAfterRequest
import org.pashri.soundcheck.ui.tuner.micAccessOnShown
import org.pashri.soundcheck.warmup.Audition
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.addSound
import org.pashri.soundcheck.warmup.deleteSound
import org.pashri.soundcheck.warmup.renameSound
import org.pashri.soundcheck.warmup.updateStep
import org.pashri.soundcheck.warmup.withClip

/**
 * Runs the Sounds list, for browsing or for choosing one Step's Sound; plays a Sound's
 * recording through the app's [Audition]; and records a new one while the record button is
 * held. A new take is saved as a new file before the library switches to it, and the file
 * it replaces stays on the phone until the next start of the app, so the last change of
 * each Sound's recording can be undone for as long as this screen is open.
 *
 * @param pickFor the Step to choose a Sound for, or null to browse.
 * @param library the saved library.
 * @param newId makes an id for a new Sound.
 * @param clips the recorded clips' files.
 * @param audition plays a recording.
 * @param mic the microphone, shared with the Tuner.
 * @param focus the recorder's own audio focus.
 * @param arbiter keeps one tool sounding or listening at a time.
 * @param worker where the recorder opens the microphone and trims a take, off the main
 *     thread.
 */
class SoundsViewModel(
    private val pickFor: StepRef?,
    private val library: Store<Library>,
    private val newId: () -> String,
    private val clips: ClipFiles,
    private val audition: Audition,
    mic: MicInput,
    focus: FocusGate,
    arbiter: ToolArbiter,
    worker: CoroutineDispatcher,
) : ViewModel() {
    private val recorder = Recorder(
        mic = mic,
        focus = focus,
        arbiter = arbiter,
        scope = viewModelScope,
        worker = worker,
    )

    /** The Sound whose recording this screen last started, until the audition ends. */
    private val previewing = MutableStateFlow<SoundId?>(null)
    private val notice = MutableStateFlow<String?>(null)
    private val view = MutableStateFlow(RecordingView())

    /** Each Sound's recording before its last change, for [undo]; null means none. */
    private val previous = mutableMapOf<SoundId, RecordedClip?>()

    /** The Sound whose recording is playing, or null. */
    private val playing =
        combine(flow = previewing, flow2 = audition.playing) { id, on -> id.takeIf { on } }

    /** What the list shows, or null until the library has loaded. */
    val uiState: StateFlow<SoundsUiState?> = combine(
        flow = library.data,
        flow2 = playing,
        flow3 = notice,
        flow4 = view,
        flow5 = combine(flow = recorder.recording, flow2 = recorder.levels) { on, levels ->
            on to levels
        },
    ) { saved, sounding, problem, recording, (on, levels) ->
        saved?.let {
            soundsUiState(
                library = it,
                pickFor = pickFor,
                playing = sounding,
                notice = problem,
                recording = recording.copy(recording = on, levels = levels),
            )
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    /**
     * Makes a Sound the chosen Step's Sound; does nothing when browsing.
     *
     * @param id the Sound.
     */
    fun choose(id: SoundId) {
        val step = pickFor ?: return
        library.edit { it.updateStep(ref = step) { saved -> saved.copy(soundId = id) } }
    }

    /**
     * Adds a Sound.
     *
     * @param label its label; the dialog has already checked it.
     * @return its id, or null before the library has loaded.
     */
    fun add(label: String): SoundId? {
        if (library.data.value == null) return null
        val id = SoundId(newId())
        library.edit { it.addSound(Sound(id = id, label = label.trim())) }
        return id
    }

    /**
     * Renames a Sound; its Steps and its recording stay with it.
     *
     * @param id the Sound.
     * @param label the new label; the dialog has already checked it.
     */
    fun rename(id: SoundId, label: String) {
        library.edit { it.renameSound(id = id, label = label) }
    }

    /**
     * Deletes a Sound and the Steps that use it. Its recording's file stays until the next
     * start of the app, when files nothing uses are swept away. If its recorder is open, it
     * closes, throwing away a take in progress.
     *
     * @param id the Sound.
     */
    fun delete(id: SoundId) {
        if (view.value.open == id) {
            recorder.cancel()
            view.update { it.copy(open = null, message = null) }
        }
        library.edit { it.deleteSound(id) }
    }

    /**
     * Plays a Sound's recording, or stops it if it is the one playing.
     *
     * @param id the Sound.
     */
    fun play(id: SoundId) {
        if (previewing.value == id && audition.playing.value) {
            audition.stop()
            return
        }
        val clip = library.data.value?.sound(id)?.clip ?: return
        notice.value = null
        viewModelScope.launch {
            val pcm = clips.load(clip.name)
            if (pcm == null) {
                notice.value = CANT_PLAY
            } else if (audition.playClip(name = clip.name, pcm = pcm)) {
                previewing.value = id
            }
        }
    }

    /**
     * The screen came into view: notes whether the microphone is allowed now.
     *
     * @param granted whether Soundcheck holds the microphone permission.
     */
    fun onShown(granted: Boolean) {
        view.update { it.copy(access = micAccessOnShown(previous = it.access, granted = granted)) }
    }

    /**
     * Android's permission dialog answered.
     *
     * @param granted whether the user allowed the microphone.
     * @param canAskAgain whether Android would show the dialog again.
     */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        view.update { current ->
            val access = micAccessAfterRequest(
                previous = current.access,
                granted = granted,
                canAskAgain = canAskAgain,
            )
            current.copy(access = access)
        }
    }

    /**
     * Opens a Sound's recorder, or closes it if it is open. Opening another Sound's recorder
     * closes the one that was open, throwing away a take in progress.
     *
     * @param id the Sound.
     * @return true if the caller should show Android's permission dialog now: the recorder
     *     opened and Soundcheck hasn't been allowed or refused the microphone yet.
     */
    fun toggleRecorder(id: SoundId): Boolean {
        recorder.cancel()
        val current = view.value
        val opening = current.open != id
        view.value = current.copy(open = id.takeIf { opening }, message = null)
        return opening && current.access == MicAccess.Unknown
    }

    /** The record button went down: starts a take for the open Sound. */
    fun startRecording() {
        val current = view.value
        val id = current.open ?: return
        if (current.access != MicAccess.Granted) return
        previewing.value = null
        view.value = current.copy(message = null)
        recorder.start { take -> keep(id = id, take = take) }
    }

    /** The record button came up: ends the take and keeps it if it is worth keeping. */
    fun stopRecording() {
        recorder.stop()
    }

    /**
     * Takes back a Sound's last change of recording: a new take, or giving it up for the
     * phone's voice.
     *
     * @param id the Sound.
     */
    fun undo(id: SoundId) {
        if (id !in previous) return
        val restored = previous.remove(id)
        library.edit { it.withClip(id = id, clip = restored) }
        view.update { it.copy(undoable = it.undoable - id, message = null) }
    }

    /**
     * Gives up a Sound's recording, so the phone's voice reads its label again.
     *
     * @param id the Sound.
     */
    fun usePhoneVoice(id: SoundId) {
        replaceClip(id = id, clip = null)
        view.update { it.copy(message = null) }
    }

    /** The screen went away: throws away a take in progress and stops a recording playing. */
    fun onHidden() {
        recorder.cancel()
        if (previewing.value != null && audition.playing.value) audition.stop()
        previewing.value = null
    }

    override fun onCleared() {
        onHidden()
    }

    private fun keep(id: SoundId, take: Take) {
        if (take is Take.Kept) {
            viewModelScope.launch { save(id = id, take = take) }
        } else {
            view.update { it.copy(message = messageFor(take)) }
        }
    }

    private suspend fun save(id: SoundId, take: Take.Kept) {
        val name = try {
            clips.save(take.pcm)
        } catch (_: IOException) {
            view.update { it.copy(message = SAVE_FAILED) }
            return
        }
        replaceClip(id = id, clip = RecordedClip(name = name, lengthMs = take.lengthMs))
        view.update { it.copy(message = if (take.cutShort) CUT_SHORT else null) }
    }

    /**
     * Switches a Sound to [clip], remembering what it had for [undo]. A Sound deleted while
     * its take was being saved is left alone; the new file is swept away at the next start.
     */
    private fun replaceClip(id: SoundId, clip: RecordedClip?) {
        val sound = library.data.value?.sound(id) ?: return
        previous[id] = sound.clip
        library.edit { it.withClip(id = id, clip = clip) }
        view.update { it.copy(undoable = it.undoable + id) }
    }

    private fun messageFor(take: Take): String? = when (take) {
        is Take.Kept -> null
        Take.TooQuiet -> "Didn't hear anything. Hold the button while you speak."
        Take.TooShort -> "That was too short. Hold the button while you speak."
        Take.MicUnavailable -> "The microphone isn't available. Is another app using it?"
        Take.Interrupted -> "Recording stopped."
    }

    /**
     * Builds [SoundsViewModel]s.
     *
     * @param pickFor the Step to choose for, or null.
     * @param library the saved library.
     * @param newId makes an id for a new Sound.
     * @param clips the recorded clips' files.
     * @param audition plays a recording.
     * @param mic the microphone, shared with the Tuner.
     * @param focus the recorder's own audio focus.
     * @param arbiter keeps one tool sounding or listening at a time.
     * @param worker where the recorder opens the microphone and trims a take.
     */
    class Factory(
        private val pickFor: StepRef?,
        private val library: Store<Library>,
        private val newId: () -> String,
        private val clips: ClipFiles,
        private val audition: Audition,
        private val mic: MicInput,
        private val focus: FocusGate,
        private val arbiter: ToolArbiter,
        private val worker: CoroutineDispatcher,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SoundsViewModel(
            pickFor = pickFor,
            library = library,
            newId = newId,
            clips = clips,
            audition = audition,
            mic = mic,
            focus = focus,
            arbiter = arbiter,
            worker = worker,
        ) as T
    }

    private companion object {
        const val CANT_PLAY = "Couldn't play that recording. Record it again?"
        const val SAVE_FAILED = "Couldn't save the recording. Is the phone's storage full?"
        const val CUT_SHORT = "That's the longest a recording can be: kept the first 5 seconds."
    }
}
