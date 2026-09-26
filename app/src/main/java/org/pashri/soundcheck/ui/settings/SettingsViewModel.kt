package org.pashri.soundcheck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.withHighest
import org.pashri.soundcheck.warmup.withLowest
import org.pashri.soundcheck.warmup.withVoiceType

/**
 * Runs the Settings screen. Every change is saved at once and applies from the next Start.
 *
 * @param settings the saved settings.
 */
class SettingsViewModel(private val settings: Store<WarmupSettings>) :
    ViewModel(), SettingsActions {
    /** What the screen shows, or null until the settings have loaded. */
    val uiState: StateFlow<SettingsUiState?> = settings.data
        .map { it?.let(::settingsUiState) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settings.data.value?.let(::settingsUiState),
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
     * Builds [SettingsViewModel]s.
     *
     * @param settings the saved settings.
     */
    class Factory(private val settings: Store<WarmupSettings>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settings = settings) as T
    }
}
