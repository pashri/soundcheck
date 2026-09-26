package org.pashri.soundcheck.ui.sounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.pashri.soundcheck.data.ClipFiles
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.warmup.Audition
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.addSound
import org.pashri.soundcheck.warmup.deleteSound
import org.pashri.soundcheck.warmup.renameSound
import org.pashri.soundcheck.warmup.updateStep

/**
 * Runs the Sounds list, for browsing or for choosing one Step's Sound, and plays a Sound's
 * recording through the app's [Audition].
 *
 * @param pickFor the Step to choose a Sound for, or null to browse.
 * @param library the saved library.
 * @param newId makes an id for a new Sound.
 * @param clips the recorded clips' files.
 * @param audition plays a recording.
 */
class SoundsViewModel(
    private val pickFor: StepRef?,
    private val library: Store<Library>,
    private val newId: () -> String,
    private val clips: ClipFiles,
    private val audition: Audition,
) : ViewModel() {
    /** The Sound whose recording this screen last started, until the audition ends. */
    private val previewing = MutableStateFlow<SoundId?>(null)
    private val notice = MutableStateFlow<String?>(null)

    /** What the list shows, or null until the library has loaded. */
    val uiState: StateFlow<SoundsUiState?> =
        combine(
            flow = library.data,
            flow2 = previewing,
            flow3 = audition.playing,
            flow4 = notice,
        ) { saved, id, on, problem ->
            saved?.let {
                soundsUiState(
                    library = it,
                    pickFor = pickFor,
                    playing = id.takeIf { on },
                    notice = problem,
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
     * start of the app, when files nothing uses are swept away.
     *
     * @param id the Sound.
     */
    fun delete(id: SoundId) {
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

    /** The screen went away: stops a recording this screen was playing. */
    fun onHidden() {
        if (previewing.value != null && audition.playing.value) audition.stop()
        previewing.value = null
    }

    override fun onCleared() {
        onHidden()
    }

    /**
     * Builds [SoundsViewModel]s.
     *
     * @param pickFor the Step to choose for, or null.
     * @param library the saved library.
     * @param newId makes an id for a new Sound.
     * @param clips the recorded clips' files.
     * @param audition plays a recording.
     */
    class Factory(
        private val pickFor: StepRef?,
        private val library: Store<Library>,
        private val newId: () -> String,
        private val clips: ClipFiles,
        private val audition: Audition,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SoundsViewModel(
            pickFor = pickFor,
            library = library,
            newId = newId,
            clips = clips,
            audition = audition,
        ) as T
    }

    private companion object {
        const val CANT_PLAY = "Couldn't play that recording. Record it again?"
    }
}
