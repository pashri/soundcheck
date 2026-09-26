package org.pashri.soundcheck.ui.pattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.addPattern
import org.pashri.soundcheck.warmup.newPattern
import org.pashri.soundcheck.warmup.uniqueName
import org.pashri.soundcheck.warmup.updateStep

/**
 * Runs the Patterns list, for browsing or for choosing one Step's Pattern.
 *
 * @param pickFor the Step to choose a Pattern for, or null to browse.
 * @param library the saved library.
 * @param newId makes an id for a new Pattern.
 */
class PatternListViewModel(
    private val pickFor: StepRef?,
    private val library: Store<Library>,
    private val newId: () -> String,
) : ViewModel() {
    /** What the list shows, or null until the library has loaded. */
    val uiState: StateFlow<PatternListUiState?> = library.data
        .map { saved -> saved?.let { patternListUiState(library = it, pickFor = pickFor) } }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    /**
     * Makes a Pattern the chosen Step's Pattern; does nothing when browsing.
     *
     * @param id the Pattern.
     */
    fun choose(id: PatternId) {
        val step = pickFor ?: return
        library.edit { it.updateStep(ref = step) { saved -> saved.copy(patternId = id) } }
    }

    /**
     * Adds a Pattern called "New pattern" (or "New pattern 2"…).
     *
     * @return its id, or null before the library has loaded.
     */
    fun addPattern(): PatternId? {
        val saved = library.data.value ?: return null
        val id = PatternId(newId())
        val name = uniqueName(base = NEW_PATTERN_NAME, taken = saved.patterns.map { it.name })
        library.edit { it.addPattern(newPattern(id = id, name = name)) }
        return id
    }

    /**
     * Builds [PatternListViewModel]s.
     *
     * @param pickFor the Step to choose for, or null.
     * @param library the saved library.
     * @param newId makes an id for a new Pattern.
     */
    class Factory(
        private val pickFor: StepRef?,
        private val library: Store<Library>,
        private val newId: () -> String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PatternListViewModel(pickFor = pickFor, library = library, newId = newId) as T
    }
}
