package org.pashri.soundcheck.metronome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.playback.HeadphoneButton

/**
 * The Metronome's tempo and accent, and whether it is clicking.
 *
 * @property bpm the tempo.
 * @property accentEvery beats per bar, or null with the accent off.
 * @property running whether it is clicking.
 */
data class MetronomeStatus(
    val bpm: Int = DEFAULT_BPM,
    val accentEvery: Int? = DEFAULT_ACCENT,
    val running: Boolean = false,
)

/**
 * Whether the headphone button reaches the Metronome: while its screen shows, and while it
 * plays even with the screen off or another tab open.
 *
 * @param shown whether the Metronome's screen is showing.
 * @param running whether it is clicking.
 * @return true when one press should start or stop it.
 */
fun offersHeadphones(shown: Boolean, running: Boolean): Boolean = shown || running

/**
 * Runs the Metronome for the whole app, so it keeps clicking with the screen off, in the
 * background or on another tab (the playback service keeps the app alive meanwhile). It
 * stops when Stop is pressed, another tool starts, audio focus is lost for good, or
 * headphones are unplugged. Call from the main thread.
 *
 * @param output where clicks play.
 * @param focus audio focus, taken while clicking.
 * @param arbiter keeps one tool sounding at a time; starting takes the slot, and another
 *     tool taking it stops the Metronome.
 * @param headphones the headphone button, offered the Metronome as [offersHeadphones] says.
 * @param scope runs the clicks; it must outlive every screen.
 */
class MetronomeController(
    output: SoundOutput,
    private val focus: FocusGate,
    private val arbiter: ToolArbiter,
    private val headphones: HeadphoneButton,
    scope: CoroutineScope,
) {
    private val metronome = Metronome(output = output, scope = scope)
    private val _status = MutableStateFlow(MetronomeStatus())
    private var shown = false

    /** What a headphone press does: the same as the screen's start/stop button. */
    private val headphoneToggle: () -> Unit = { toggle() }

    /** The tempo, the accent and whether it is clicking. */
    val status: StateFlow<MetronomeStatus> = _status.asStateFlow()

    /** The beat sounding now, or null when stopped or before the first click. */
    val beat: StateFlow<Beat?> = metronome.beat

    /**
     * Sets the tempo, clamped to the allowed range; a running Metronome keeps its beat.
     *
     * @param bpm the new tempo.
     */
    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(minimumValue = MIN_BPM, maximumValue = MAX_BPM)
        if (clamped == _status.value.bpm) return
        _status.update { it.copy(bpm = clamped) }
        if (_status.value.running) metronome.setTempo(clamped)
    }

    /**
     * Sets the accent.
     *
     * @param every beats per bar, or null for none.
     */
    fun setAccent(every: Int?) {
        _status.update { it.copy(accentEvery = every) }
        if (_status.value.running) metronome.setAccent(every)
    }

    /** Starts the Metronome if stopped, stops it if running. */
    fun toggle() {
        if (_status.value.running) stop() else start()
    }

    /**
     * Stops clicking, hands audio focus back and frees the tool slot. Safe to call when
     * already stopped.
     */
    fun stop() {
        metronome.stop()
        focus.release()
        arbiter.release(Tool.METRONOME)
        _status.update { it.copy(running = false) }
        syncHeadphones()
    }

    /** The Metronome's screen is showing: the headphone button can start and stop it. */
    fun show() {
        shown = true
        syncHeadphones()
    }

    /**
     * The Metronome's screen has gone (another tab, the screen off, the app in the
     * background). It keeps clicking, and keeps the headphone button until it stops.
     */
    fun hide() {
        shown = false
        syncHeadphones()
    }

    private fun start() {
        arbiter.claim(tool = Tool.METRONOME, onEvicted = ::stop)
        if (!focus.acquire(onLost = ::stop)) {
            arbiter.release(Tool.METRONOME)
            return
        }
        val current = _status.value
        if (!metronome.start(bpm = current.bpm, accentEvery = current.accentEvery)) {
            focus.release()
            arbiter.release(Tool.METRONOME)
            return
        }
        _status.update { it.copy(running = true) }
        syncHeadphones()
    }

    private fun syncHeadphones() {
        if (offersHeadphones(shown = shown, running = _status.value.running)) {
            headphones.offerMetronome(headphoneToggle)
        } else {
            headphones.withdrawMetronome(headphoneToggle)
        }
    }
}
