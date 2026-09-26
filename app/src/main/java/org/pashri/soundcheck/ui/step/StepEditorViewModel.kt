package org.pashri.soundcheck.ui.step

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.ui.components.EditorState
import org.pashri.soundcheck.ui.components.readyOrGone
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.removeStep
import org.pashri.soundcheck.warmup.updateStep
import org.pashri.soundcheck.warmup.withBpm
import org.pashri.soundcheck.warmup.withRangeOffset

/**
 * Runs one Step's editor. Every change is saved at once.
 *
 * @param ref the Step.
 * @param library the saved library.
 * @param settings the saved settings, for the Range.
 */
class StepEditorViewModel(
    private val ref: StepRef,
    private val library: Store<Library>,
    settings: Store<WarmupSettings>,
) : ViewModel(), StepEditorActions {
    /** What the editor shows. */
    val uiState: StateFlow<EditorState<StepEditorUiState>> =
        combine(library.data, settings.data) { saved, chosen ->
            if (saved == null || chosen == null) {
                EditorState.Loading
            } else {
                readyOrGone(stepEditorUiState(library = saved, ref = ref, range = chosen.range))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EditorState.Loading,
        )

    override fun slower() {
        change { it.withBpm(it.bpm - 1) }
    }

    override fun faster() {
        change { it.withBpm(it.bpm + 1) }
    }

    override fun setDirection(direction: Direction) {
        change { it.copy(direction = direction) }
    }

    override fun lowerBottom() {
        change { it.withRangeOffset(bottom = it.rangeOffset.bottom + 1, top = it.rangeOffset.top) }
    }

    override fun raiseBottom() {
        change { it.withRangeOffset(bottom = it.rangeOffset.bottom - 1, top = it.rangeOffset.top) }
    }

    override fun lowerTop() {
        change { it.withRangeOffset(bottom = it.rangeOffset.bottom, top = it.rangeOffset.top - 1) }
    }

    override fun raiseTop() {
        change { it.withRangeOffset(bottom = it.rangeOffset.bottom, top = it.rangeOffset.top + 1) }
    }

    override fun setGuideMelody(on: Boolean) {
        change { it.copy(guideMelody = on) }
    }

    override fun remove() {
        library.edit { it.removeStep(ref) }
    }

    private fun change(edit: (SavedStep) -> SavedStep) {
        library.edit { it.updateStep(ref, edit) }
    }

    /**
     * Builds [StepEditorViewModel]s.
     *
     * @param ref the Step.
     * @param library the saved library.
     * @param settings the saved settings.
     */
    class Factory(
        private val ref: StepRef,
        private val library: Store<Library>,
        private val settings: Store<WarmupSettings>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StepEditorViewModel(ref = ref, library = library, settings = settings) as T
    }
}
