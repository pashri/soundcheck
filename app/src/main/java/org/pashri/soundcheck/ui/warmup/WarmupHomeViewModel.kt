package org.pashri.soundcheck.ui.warmup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.addProgramme
import org.pashri.soundcheck.warmup.playSaved
import org.pashri.soundcheck.warmup.uniqueName

/**
 * Runs the Warm-up home: the Programmes, the Range and the library, and Start.
 *
 * @param controller plays Programmes.
 * @param library the saved library.
 * @param settings the saved settings.
 * @param newId makes an id for a new Programme.
 */
class WarmupHomeViewModel(
    private val controller: WarmupController,
    private val library: Store<Library>,
    private val settings: Store<WarmupSettings>,
    private val newId: () -> String,
) : ViewModel() {
    private val problem = MutableStateFlow<StartProblem?>(null)
    private val restoredDismissed = MutableStateFlow(false)

    /** Whether the last save failed, and what to say if a saved document was restored. */
    private val documentNotices = combine(
        library.saveFailed,
        settings.saveFailed,
        library.setAside,
        settings.setAside,
        restoredDismissed,
    ) { librarySaveFailed, settingsSaveFailed, librarySetAside, settingsSetAside, dismissed ->
        val notice = when {
            dismissed -> null
            librarySetAside -> LIBRARY_RESTORED_NOTICE
            settingsSetAside -> SETTINGS_RESTORED_NOTICE
            else -> null
        }
        DocumentNotices(saveFailed = librarySaveFailed || settingsSaveFailed, restored = notice)
    }

    /** What the home shows, or null until the library and the settings have loaded. */
    val uiState: StateFlow<WarmupHomeUiState?> = combine(
        library.data,
        settings.data,
        controller.playback,
        problem,
        documentNotices,
    ) { saved, chosen, playback, startProblem, notices ->
        if (saved == null || chosen == null) {
            null
        } else {
            warmupHomeUiState(
                library = saved,
                settings = chosen,
                playback = playback,
                problem = startProblem,
                saveFailed = notices.saveFailed,
                restoredNotice = notices.restored,
            )
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    /**
     * Plays a Programme, as it is saved now, on the Range from Settings.
     *
     * @param id the Programme.
     * @return true when it is playing, so the screen can open the playing screen.
     */
    fun start(id: ProgrammeId): Boolean {
        val outcome = controller.playSaved(
            library = library.data.value,
            settings = settings.data.value,
            id = id,
        ) ?: return false
        problem.value = StartProblem(programmeId = id, outcome = outcome)
            .takeIf { outcome != StartOutcome.PLAYING }
        return outcome == StartOutcome.PLAYING
    }

    /**
     * Adds an empty Programme called "New programme" (or "New programme 2"…).
     *
     * @return its id, or null before the library has loaded.
     */
    fun newProgramme(): ProgrammeId? {
        val saved = library.data.value ?: return null
        val id = ProgrammeId(newId())
        val name = uniqueName(base = NEW_PROGRAMME_NAME, taken = saved.programmes.map { it.name })
        library.edit { it.addProgramme(id = id, name = name) }
        return id
    }

    /** Dismisses the notice that a saved document was set aside and replaced. */
    fun dismissRestoredNotice() {
        restoredDismissed.value = true
    }

    /**
     * Builds [WarmupHomeViewModel]s.
     *
     * @param controller plays Programmes.
     * @param library the saved library.
     * @param settings the saved settings.
     * @param newId makes an id for a new Programme.
     */
    class Factory(
        private val controller: WarmupController,
        private val library: Store<Library>,
        private val settings: Store<WarmupSettings>,
        private val newId: () -> String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WarmupHomeViewModel(
            controller = controller,
            library = library,
            settings = settings,
            newId = newId,
        ) as T
    }
}

/**
 * Whether the last save of either saved document failed, and what to say if one was set
 * aside and replaced.
 *
 * @property saveFailed whether the last save of either document failed.
 * @property restored what to say, or null.
 */
private data class DocumentNotices(val saveFailed: Boolean, val restored: String?)
