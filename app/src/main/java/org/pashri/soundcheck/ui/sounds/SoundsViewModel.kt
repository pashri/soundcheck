package org.pashri.soundcheck.ui.sounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.addSound
import org.pashri.soundcheck.warmup.deleteSound
import org.pashri.soundcheck.warmup.renameSound
import org.pashri.soundcheck.warmup.updateStep

/**
 * Runs the Sounds list, for browsing or for choosing one Step's Sound.
 *
 * @param pickFor the Step to choose a Sound for, or null to browse.
 * @param library the saved library.
 * @param newId makes an id for a new Sound.
 */
class SoundsViewModel(
    private val pickFor: StepRef?,
    private val library: Store<Library>,
    private val newId: () -> String,
) : ViewModel() {
    /** What the list shows, or null until the library has loaded. */
    val uiState: StateFlow<SoundsUiState?> = library.data
        .map { saved -> saved?.let { soundsUiState(library = it, pickFor = pickFor) } }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    /**
     * Makes a Sound the chosen Step's Sound; does nothing when browsing.
     *
     * @param id the Sound.
     */
    fun choose(id: SoundId) {
        val step = pickFor ?: return
        library.edit { it.updateStep(step) { saved -> saved.copy(soundId = id) } }
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
     * Renames a Sound; its Steps keep it.
     *
     * @param id the Sound.
     * @param label the new label; the dialog has already checked it.
     */
    fun rename(id: SoundId, label: String) {
        library.edit { it.renameSound(id = id, label = label) }
    }

    /**
     * Deletes a Sound and the Steps that use it.
     *
     * @param id the Sound.
     */
    fun delete(id: SoundId) {
        library.edit { it.deleteSound(id) }
    }

    /**
     * Builds [SoundsViewModel]s.
     *
     * @param pickFor the Step to choose for, or null.
     * @param library the saved library.
     * @param newId makes an id for a new Sound.
     */
    class Factory(
        private val pickFor: StepRef?,
        private val library: Store<Library>,
        private val newId: () -> String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SoundsViewModel(pickFor = pickFor, library = library, newId = newId) as T
    }
}
