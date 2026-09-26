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
import org.pashri.soundcheck.playback.HeadphoneButton

/**
 * Runs the Metronome screen. While the screen shows, one press of the headphone button
 * starts or stops the Metronome.
 *
 * @param output where clicks play.
 * @param focus audio focus, taken while clicking.
 * @param clockMs a monotonic clock for tap tempo.
 * @param arbiter keeps one tool sounding at a time; starting takes the slot, and another
 *     tool taking it stops the Metronome.
 * @param headphones the headphone button, offered the Metronome while its screen shows.
 */
class MetronomeViewModel(
    output: SoundOutput,
    private val focus: FocusGate,
    clockMs: () -> Long,
    private val arbiter: ToolArbiter,
    private val headphones: HeadphoneButton,
) : ViewModel(), MetronomeActions {
    private val metronome = Metronome(output = output, scope = viewModelScope)
    private val tapTempo = TapTempo(clockMs)
    private val settings = MutableStateFlow(MetronomeUiState())

    /** What a headphone press does: the same as the screen's start/stop button. */
    private val headphoneToggle: () -> Unit = { toggle() }

    /** Everything the screen shows. */
    val uiState: StateFlow<MetronomeUiState> = combine(
        flow = settings,
        flow2 = metronome.beat,
    ) { state, beat ->
        state.copy(beatInBar = beat?.positionInBar, beatIndex = beat?.index)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MetronomeUiState(),
    )

    override fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(minimumValue = MIN_BPM, maximumValue = MAX_BPM)
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

    /** The screen is showing: the headphone button can start and stop the Metronome. */
    fun onShown() {
        headphones.offerMetronome(headphoneToggle)
    }

    /** The screen has gone: stops clicking and takes the headphone button back. */
    fun onHidden() {
        stop()
        headphones.withdrawMetronome(headphoneToggle)
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
        onHidden()
    }

    private fun start() {
        arbiter.claim(tool = Tool.METRONOME, onEvicted = ::stop)
        if (!focus.acquire(onLost = ::stop)) {
            arbiter.release(Tool.METRONOME)
            return
        }
        val current = settings.value
        if (!metronome.start(bpm = current.bpm, accentEvery = current.accentEvery)) {
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
     * @param headphones the headphone button.
     */
    class Factory(
        private val output: SoundOutput,
        private val focus: FocusGate,
        private val clockMs: () -> Long,
        private val arbiter: ToolArbiter,
        private val headphones: HeadphoneButton,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MetronomeViewModel(
            output = output,
            focus = focus,
            clockMs = clockMs,
            arbiter = arbiter,
            headphones = headphones,
        ) as T
    }
}
