package org.pashri.soundcheck.ui.warmup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.warmup.Programme
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.WarmupController

/**
 * Runs the Warm-up screen. It only shows and steers the [WarmupController]; the Programme
 * keeps playing when the screen goes away.
 *
 * @param controller plays Programmes.
 * @param programme the Programme the Start button plays.
 * @param range the Range it plays through.
 * @param sounds the Sound library, for labels.
 */
class WarmupViewModel(
    private val controller: WarmupController,
    private val programme: Programme,
    private val range: Range,
    sounds: List<Sound>,
) : ViewModel(), WarmupActions {
    /** Everything the screen shows. */
    val uiState: StateFlow<WarmupUiState> = controller.playback
        .map {
            warmupUiState(playback = it, programme = programme, range = range, sounds = sounds)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = warmupUiState(
                playback = controller.playback.value,
                programme = programme,
                range = range,
                sounds = sounds,
            ),
        )

    override fun playPause() {
        val playback = controller.playback.value
        if (playback == null) controller.play(programme, range) else controller.toggle()
    }

    override fun next() {
        controller.next()
    }

    override fun previous() {
        controller.previous()
    }

    override fun stop() {
        controller.stop()
    }

    /**
     * Builds [WarmupViewModel]s.
     *
     * @param controller plays Programmes.
     * @param programme the Programme the Start button plays.
     * @param range the Range it plays through.
     * @param sounds the Sound library.
     */
    class Factory(
        private val controller: WarmupController,
        private val programme: Programme,
        private val range: Range,
        private val sounds: List<Sound>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WarmupViewModel(
            controller = controller,
            programme = programme,
            range = range,
            sounds = sounds,
        ) as T
    }
}
