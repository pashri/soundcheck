package org.pashri.soundcheck.ui.metronome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.metronome.Beat
import org.pashri.soundcheck.metronome.MetronomeController
import org.pashri.soundcheck.metronome.MetronomeStatus
import org.pashri.soundcheck.metronome.TapTempo

/**
 * Runs the Metronome screen over the app's [MetronomeController]. While the screen shows,
 * one press of the headphone button starts or stops the Metronome; leaving the screen never
 * stops it.
 *
 * @param metronome the app's Metronome, which outlives this screen.
 * @param clockMs a monotonic clock for tap tempo.
 */
class MetronomeViewModel(
    private val metronome: MetronomeController,
    clockMs: () -> Long,
) : ViewModel(), MetronomeActions {
    private val tapTempo = TapTempo(clockMs)

    /** Everything the screen shows. */
    val uiState: StateFlow<MetronomeUiState> = combine(
        flow = metronome.status,
        flow2 = metronome.beat,
        transform = ::uiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = uiState(status = metronome.status.value, beat = metronome.beat.value),
    )

    override fun setBpm(bpm: Int) {
        metronome.setBpm(bpm)
    }

    override fun faster() {
        metronome.setBpm(metronome.status.value.bpm + 1)
    }

    override fun slower() {
        metronome.setBpm(metronome.status.value.bpm - 1)
    }

    override fun setAccent(every: Int?) {
        metronome.setAccent(every)
    }

    override fun tap() {
        tapTempo.tap()?.let(::setBpm)
    }

    override fun toggle() {
        metronome.toggle()
    }

    /** The screen is showing: the headphone button can start and stop the Metronome. */
    fun onShown() {
        metronome.show()
    }

    /**
     * The screen has gone (another tab, the screen off, the app in the background): the
     * Metronome keeps clicking or stays paused, and a stopped one gives the headphone
     * button back.
     */
    fun onHidden() {
        metronome.hide()
    }

    override fun stop() {
        metronome.stop()
    }

    override fun onCleared() {
        onHidden()
    }

    /**
     * Builds [MetronomeViewModel]s.
     *
     * @param metronome the app's Metronome.
     * @param clockMs a monotonic clock for tap tempo.
     */
    class Factory(
        private val metronome: MetronomeController,
        private val clockMs: () -> Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MetronomeViewModel(metronome = metronome, clockMs = clockMs) as T
    }
}

private fun uiState(status: MetronomeStatus, beat: Beat?): MetronomeUiState = MetronomeUiState(
    bpm = status.bpm,
    accentEvery = status.accentEvery,
    running = status.running,
    paused = status.paused,
    beatInBar = beat?.positionInBar,
    beatIndex = beat?.index,
)
