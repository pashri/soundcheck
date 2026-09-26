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

/**
 * The Metronome's tempo and accent, and whether it is clicking or paused.
 *
 * @property bpm the tempo.
 * @property accentEvery beats per bar, or null with the accent off.
 * @property running whether it is clicking.
 * @property paused whether the headset or the notification paused it: silent, but still
 *     holding audio focus, the tool slot, the headphone button and its notification.
 */
data class MetronomeStatus(
    val bpm: Int = DEFAULT_BPM,
    val accentEvery: Int? = DEFAULT_ACCENT,
    val running: Boolean = false,
    val paused: Boolean = false,
) {
    /** Whether it is clicking or paused, so it keeps the service and the button. */
    val held: Boolean get() = running || paused
}

/**
 * Where the Metronome offers itself to the headphone button; the playback package's
 * `HeadphoneButton` is the real one.
 */
interface HeadphoneOffer {
    /**
     * The Metronome takes presses now.
     *
     * @param toggle what one press does.
     */
    fun offerMetronome(toggle: () -> Unit)

    /**
     * The Metronome no longer takes presses.
     *
     * @param toggle the function [offerMetronome] was given; any other is ignored.
     */
    fun withdrawMetronome(toggle: () -> Unit)
}

/**
 * Whether the headphone button reaches the Metronome: while its screen shows, and while it
 * plays or is paused, even with the screen off or another tab open.
 *
 * @param shown whether the Metronome's screen is showing.
 * @param held whether it is clicking or paused ([MetronomeStatus.held]).
 * @return true when one press should start, pause or stop it.
 */
fun offersHeadphones(shown: Boolean, held: Boolean): Boolean = shown || held

/**
 * Runs the Metronome for the whole app, so it keeps clicking with the screen off, in the
 * background or on another tab (the playback service keeps the app alive meanwhile).
 *
 * Off screen, a headphone press or the notification's Pause pauses it, like a paused
 * Programme: silent, but keeping audio focus, the tool slot, the button and the
 * notification, so another press or Play starts it again. It ends (stopped, nothing held)
 * when Stop is pressed on screen (a press while the screen shows does the same), Close is
 * pressed in the notification, another tool starts, or audio focus is lost for good.
 * Unplugging headphones pauses it. Call from the main thread.
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
    private val headphones: HeadphoneOffer,
    scope: CoroutineScope,
) {
    private val metronome = Metronome(output = output, scope = scope)
    private val _status = MutableStateFlow(MetronomeStatus())
    private var shown = false

    /** What a headphone press does: see [onPress]. */
    private val headphoneToggle: () -> Unit = { onPress() }

    /** The tempo, the accent and whether it is clicking or paused. */
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

    /** The screen's button: starts (or resumes) the Metronome, or ends a running one. */
    fun toggle() {
        if (_status.value.running) stop() else start()
    }

    /** The notification's Pause and Play: nothing happens unless it is running or paused. */
    fun pauseOrResume() {
        when {
            _status.value.running -> pause()
            _status.value.paused -> start()
        }
    }

    /**
     * Pauses a running Metronome: silent, keeping audio focus, the tool slot, the headphone
     * button and the notification. Does nothing unless it is running.
     */
    fun pause() {
        if (!_status.value.running) return
        metronome.stop()
        _status.update { it.copy(running = false, paused = true) }
    }

    /**
     * Ends the Metronome: stops clicking, hands audio focus back, frees the tool slot and,
     * off screen, the headphone button. Safe to call when already stopped.
     */
    fun stop() {
        metronome.stop()
        focus.release()
        arbiter.release(Tool.METRONOME)
        _status.update { it.copy(running = false, paused = false) }
        syncHeadphones()
    }

    /** The Metronome's screen is showing: the headphone button can start and stop it. */
    fun show() {
        shown = true
        syncHeadphones()
    }

    /**
     * The Metronome's screen has gone (another tab, the screen off, the app in the
     * background). It keeps clicking, and keeps the headphone button until it ends.
     */
    fun hide() {
        shown = false
        syncHeadphones()
    }

    /**
     * One headphone press: starts a stopped or paused Metronome; a running one ends while its
     * screen shows (like the Stop button) and pauses off screen, so the next press restarts it.
     */
    private fun onPress() {
        when {
            !_status.value.running -> start()
            shown -> stop()
            else -> pause()
        }
    }

    /** Starts clicking; a paused Metronome still holds focus, so it doesn't ask again. */
    private fun start() {
        arbiter.claim(tool = Tool.METRONOME, onEvicted = ::stop)
        if (!_status.value.paused && !focus.acquire(onLost = ::stop)) {
            arbiter.release(Tool.METRONOME)
            return
        }
        val current = _status.value
        if (!metronome.start(bpm = current.bpm, accentEvery = current.accentEvery)) {
            stop()
            return
        }
        _status.update { it.copy(running = true, paused = false) }
        syncHeadphones()
    }

    private fun syncHeadphones() {
        if (offersHeadphones(shown = shown, held = _status.value.held)) {
            headphones.offerMetronome(headphoneToggle)
        } else {
            headphones.withdrawMetronome(headphoneToggle)
        }
    }
}
