package org.pashri.soundcheck.ui.metronome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM
import org.pashri.soundcheck.metronome.Metronome
import org.pashri.soundcheck.metronome.TapTempo

/**
 * Runs the Metronome screen.
 *
 * @param output where clicks play.
 * @param focus audio focus, taken while clicking.
 * @param clockMs a monotonic clock for tap tempo.
 * @param arbiter keeps one tool sounding at a time; starting takes the slot, and another
 *     tool taking it stops the Metronome.
 */
class MetronomeViewModel(
    output: SoundOutput,
    private val focus: FocusGate,
    clockMs: () -> Long,
    private val arbiter: ToolArbiter,
) : ViewModel(), MetronomeActions {
    private val metronome = Metronome(output, viewModelScope)
    private val tapTempo = TapTempo(clockMs)
    private val settings = MutableStateFlow(MetronomeUiState())

    /** Everything the screen shows. */
    val uiState: StateFlow<MetronomeUiState> =
        combine(settings, metronome.beat) { state, beat ->
            state.copy(beatInBar = beat?.positionInBar, beatIndex = beat?.index)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MetronomeUiState())

    override fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(MIN_BPM, MAX_BPM)
        if (clamped == settings.value.bpm) return
        settings.update { it.copy(bpm = clamped) }
        if (settings.value.running) metronome.setTempo(clamped)
    }

    override fun faster() {
        setBpm(settings.value.bpm + 1)
    }

    override fun slower() {
        setBpm(settings.value.bpm - 1)
    }

    override fun setAccent(every: Int?) {
        settings.update { it.copy(accentEvery = every) }
        if (settings.value.running) metronome.setAccent(every)
    }

    override fun tap() {
        tapTempo.tap()?.let(::setBpm)
    }

    override fun toggle() {
        if (settings.value.running) stop() else start()
    }

    /**
     * Stops clicking, hands audio focus back and frees the tool slot. Safe to call when
     * already stopped.
     */
    fun stop() {
        metronome.stop()
        focus.release()
        arbiter.release(Tool.METRONOME)
        settings.update { it.copy(running = false) }
    }

    override fun onCleared() {
        stop()
    }

    private fun start() {
        arbiter.claim(Tool.METRONOME, onEvicted = ::stop)
        if (!focus.acquire(onLost = ::stop)) {
            arbiter.release(Tool.METRONOME)
            return
        }
        val current = settings.value
        if (!metronome.start(current.bpm, current.accentEvery)) {
            focus.release()
            arbiter.release(Tool.METRONOME)
            return
        }
        settings.update { it.copy(running = true) }
    }

    /**
     * Builds [MetronomeViewModel]s.
     *
     * @param output where clicks play.
     * @param focus audio focus.
     * @param clockMs a monotonic clock for tap tempo.
     * @param arbiter keeps one tool sounding at a time.
     */
    class Factory(
        private val output: SoundOutput,
        private val focus: FocusGate,
        private val clockMs: () -> Long,
        private val arbiter: ToolArbiter,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MetronomeViewModel(output, focus, clockMs = clockMs, arbiter = arbiter) as T
    }
}
