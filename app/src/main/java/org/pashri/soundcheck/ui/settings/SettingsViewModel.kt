package org.pashri.soundcheck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.pashri.soundcheck.data.Backup
import org.pashri.soundcheck.data.ExportCodec
import org.pashri.soundcheck.data.ExportRead
import org.pashri.soundcheck.data.ImportOutcome
import org.pashri.soundcheck.data.SharedFiles
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.data.importBackup
import org.pashri.soundcheck.data.readExport
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.withHighest
import org.pashri.soundcheck.warmup.withLowest
import org.pashri.soundcheck.warmup.withVoiceType

/**
 * Runs the Settings screen. Every change is saved at once and applies from the next Start
 * after the Programme stops. It also exports the library and settings to a file the person
 * picks, and imports one back after asking.
 *
 * @param settings the saved settings.
 * @param library the saved library, for backups.
 * @param files the files the person picks.
 * @param today the date, for naming a backup.
 * @param clockMs the time, for naming the copies an import keeps.
 */
class SettingsViewModel(
    private val settings: Store<WarmupSettings>,
    private val library: Store<Library>,
    private val files: SharedFiles,
    private val today: () -> LocalDate,
    private val clockMs: () -> Long,
) : ViewModel(), SettingsActions {
    private val backup = MutableStateFlow(BackupView())

    /** The backup waiting for the person to say yes or no. */
    private var chosen: Backup? = null

    /** What the screen shows, or null until the settings have loaded. */
    val uiState: StateFlow<SettingsUiState?> =
        combine(flow = settings.data, flow2 = backup) { saved, view ->
            saved?.let { settingsUiState(settings = it, backup = view) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settings.data.value?.let { settingsUiState(settings = it) },
        )

    override fun selectVoiceType(voiceType: VoiceType) {
        settings.edit { it.withVoiceType(voiceType) }
    }

    override fun lowerLowest() {
        settings.edit { it.withLowest(it.range.lowest.midi - 1) }
    }

    override fun raiseLowest() {
        settings.edit { it.withLowest(it.range.lowest.midi + 1) }
    }

    override fun lowerHighest() {
        settings.edit { it.withHighest(it.range.highest.midi - 1) }
    }

    override fun raiseHighest() {
        settings.edit { it.withHighest(it.range.highest.midi + 1) }
    }

    override fun setPlayOverOtherAudio(on: Boolean) {
        settings.edit { it.copy(playOverOtherAudio = on) }
    }

    /**
     * The name the file picker suggests for a backup made today.
     *
     * @return e.g. "soundcheck-2026-09-26.json".
     */
    fun exportFileName(): String = backupFileName(today())

    override fun exportTo(uri: String) {
        val saved = library.data.value ?: return
        val current = settings.data.value ?: return
        viewModelScope.launch {
            val text = ExportCodec.encode(Backup(library = saved, settings = current))
            val written = files.write(uri = uri, text = text)
            val done = "Saved a backup of ${libraryCounts(saved)}."
            backup.value = BackupView(message = if (written) done else EXPORT_FAILED)
        }
    }

    override fun importFrom(uri: String) {
        chosen = null
        backup.value = BackupView()
        viewModelScope.launch {
            val text = files.read(uri)
            val read = text?.let(::readExport)
            if (read is ExportRead.Valid) {
                chosen = read.backup
                backup.value = BackupView(question = libraryCounts(read.backup.library))
            } else {
                backup.value = BackupView(message = readProblem(read))
            }
        }
    }

    override fun confirmImport() {
        val picked = chosen ?: return
        chosen = null
        backup.value = BackupView()
        viewModelScope.launch {
            val outcome = importBackup(
                backup = picked,
                library = library,
                settings = settings,
                stamp = clockMs(),
            )
            backup.value = BackupView(message = importMessage(outcome = outcome, backup = picked))
        }
    }

    override fun cancelImport() {
        chosen = null
        backup.value = BackupView()
    }

    private fun readProblem(read: ExportRead?): String = when (read) {
        null -> "Couldn't read that file."
        ExportRead.TooNew ->
            "That backup is from a newer Soundcheck. Update the app to import it."
        else -> "That file isn't a Soundcheck backup, so nothing was changed."
    }

    private fun importMessage(outcome: ImportOutcome, backup: Backup): String = when (outcome) {
        ImportOutcome.IMPORTED -> "Imported ${libraryCounts(backup.library)}. Your previous " +
            "library and settings are kept on the phone."
        ImportOutcome.NOTHING_CHANGED ->
            "Couldn't import, so nothing was changed. Is the phone's storage full?"
        ImportOutcome.LIBRARY_ONLY ->
            "Imported the library but not the settings. Is the phone's storage full?"
    }

    /**
     * Builds [SettingsViewModel]s.
     *
     * @param settings the saved settings.
     * @param library the saved library.
     * @param files the files the person picks.
     * @param today the date.
     * @param clockMs the time in ms.
     */
    class Factory(
        private val settings: Store<WarmupSettings>,
        private val library: Store<Library>,
        private val files: SharedFiles,
        private val today: () -> LocalDate,
        private val clockMs: () -> Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
            settings = settings,
            library = library,
            files = files,
            today = today,
            clockMs = clockMs,
        ) as T
    }

    private companion object {
        const val EXPORT_FAILED = "Couldn't save the backup there. Try another place."
    }
}
