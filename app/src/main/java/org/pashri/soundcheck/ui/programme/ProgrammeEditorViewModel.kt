package org.pashri.soundcheck.ui.programme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.ui.components.EditorState
import org.pashri.soundcheck.ui.components.readyOrGone
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.addStep
import org.pashri.soundcheck.warmup.deleteProgramme
import org.pashri.soundcheck.warmup.moveStep
import org.pashri.soundcheck.warmup.newStep
import org.pashri.soundcheck.warmup.playSaved
import org.pashri.soundcheck.warmup.removeStep
import org.pashri.soundcheck.warmup.renameProgramme

/**
 * Runs one Programme's editor. Every change is saved at once.
 *
 * @param programmeId the Programme.
 * @param controller plays Programmes.
 * @param library the saved library.
 * @param settings the saved settings, for the Range.
 * @param newId makes a key for a new Step.
 */
class ProgrammeEditorViewModel(
    private val programmeId: ProgrammeId,
    private val controller: WarmupController,
    private val library: Store<Library>,
    private val settings: Store<WarmupSettings>,
    private val newId: () -> String,
) : ViewModel() {
    private val problem = MutableStateFlow<StartOutcome?>(null)

    /** What the editor shows. */
    val uiState: StateFlow<EditorState<ProgrammeEditorUiState>> =
        combine(
            flow = library.data,
            flow2 = settings.data,
            flow3 = problem,
        ) { saved, chosen, outcome ->
            if (saved == null || chosen == null) {
                EditorState.Loading
            } else {
                readyOrGone(
                    programmeEditorUiState(
                        library = saved,
                        id = programmeId,
                        range = chosen.range,
                        problem = outcome,
                    ),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EditorState.Loading,
        )

    /**
     * Renames the Programme.
     *
     * @param name the new name; the dialog has already checked it.
     */
    fun rename(name: String) {
        library.edit { it.renameProgramme(id = programmeId, name = name) }
    }

    /** Deletes the Programme; the editor then closes. */
    fun delete() {
        library.edit { it.deleteProgramme(programmeId) }
    }

    /**
     * Adds a new Step at the end.
     *
     * @return its key, or null before the library has loaded.
     */
    fun addStep(): StepKey? {
        val step = library.data.value?.newStep(StepKey(newId())) ?: return null
        library.edit { it.addStep(programmeId = programmeId, step = step) }
        return step.key
    }

    /**
     * Removes a Step.
     *
     * @param key the Step.
     */
    fun removeStep(key: StepKey) {
        library.edit { it.removeStep(StepRef(programmeId = programmeId, key = key)) }
    }

    /**
     * Moves a Step.
     *
     * @param from its position now, from 0.
     * @param to its new position, from 0.
     */
    fun moveStep(from: Int, to: Int) {
        library.edit { it.moveStep(programmeId = programmeId, from = from, to = to) }
    }

    /**
     * Plays the Programme, as it is saved now, on the Range from Settings.
     *
     * @return true when it is playing, so the screen can open the playing screen.
     */
    fun start(): Boolean {
        val outcome = controller.playSaved(
            library = library.data.value,
            settings = settings.data.value,
            id = programmeId,
        ) ?: return false
        problem.value = outcome.takeIf { it != StartOutcome.PLAYING }
        return outcome == StartOutcome.PLAYING
    }

    /**
     * Builds [ProgrammeEditorViewModel]s.
     *
     * @param programmeId the Programme.
     * @param controller plays Programmes.
     * @param library the saved library.
     * @param settings the saved settings.
     * @param newId makes a key for a new Step.
     */
    class Factory(
        private val programmeId: ProgrammeId,
        private val controller: WarmupController,
        private val library: Store<Library>,
        private val settings: Store<WarmupSettings>,
        private val newId: () -> String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgrammeEditorViewModel(
            programmeId = programmeId,
            controller = controller,
            library = library,
            settings = settings,
            newId = newId,
        ) as T
    }
}
