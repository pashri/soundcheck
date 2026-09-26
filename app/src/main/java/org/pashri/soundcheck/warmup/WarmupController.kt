package org.pashri.soundcheck.warmup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter

/**
 * Runs the Warm-up for the screen, the notification and the headphone button. While a
 * Programme is playing or paused it holds audio focus (so a podcast stays paused) and the
 * one sound-making slot; it hands both back when the Programme stops or ends. While "Play
 * over other audio" is on, [focus] is a `MixingFocusGate`, so "holds audio focus" instead
 * means "is allowed to play": focus is granted at once without being requested from the
 * system, and no podcast is paused. Call from the main thread.
 *
 * @param player plays the Programme.
 * @param focus the Warm-up's own audio focus.
 * @param arbiter keeps one tool sounding at a time.
 * @param scope watches for the Programme ending by itself.
 */
class WarmupController(
    private val player: ProgrammePlayer,
    private val focus: FocusGate,
    private val arbiter: ToolArbiter,
    scope: CoroutineScope,
) {
    /** The Programme playing or paused, or null when stopped. */
    val playback: StateFlow<Playback?> = player.playback

    private var holdsFocus = false
    private var pausedByFocus = false

    init {
        scope.launch { player.playback.collect { if (it == null) handBack() } }
    }

    /**
     * Plays [programme] from its first Step that fits, pausing any other tool.
     *
     * @param programme the Programme.
     * @param range the Range it plays through.
     * @return what happened; anything but [StartOutcome.PLAYING] is something to explain.
     */
    fun play(programme: Programme, range: Range): StartOutcome {
        if (programme.firstStep(range) == null) return StartOutcome.NOTHING_FITS
        if (!takeOver()) return StartOutcome.AUDIO_BUSY
        val started = player.play(programme = programme, range = range)
        if (player.playback.value == null) handBack()
        return if (started) StartOutcome.PLAYING else StartOutcome.OUTPUT_FAILED
    }

    /** Pauses a playing Programme, or resumes a paused one. */
    fun toggle() {
        if (player.playback.value?.playing == true) pause() else resume()
    }

    /** Pauses, keeping audio focus so the podcast stays paused. */
    fun pause() {
        pausedByFocus = false
        player.pause()
    }

    /** Resumes a paused Programme from the paused Iteration's Key Chord. */
    fun resume() {
        if (player.playback.value?.playing != false || !takeOver()) return
        pausedByFocus = false
        player.resume()
    }

    /** Plays the next Step. */
    fun next() {
        if (player.playback.value == null || !takeOver()) return
        pausedByFocus = false
        player.next()
    }

    /** Plays the previous Step. */
    fun previous() {
        if (player.playback.value == null || !takeOver()) return
        pausedByFocus = false
        player.previous()
    }

    /** Stops and hands focus and the slot back. */
    fun stop() {
        player.stop()
        handBack()
    }

    /**
     * Acts on a run of headphone presses.
     *
     * @param count 1 pauses or resumes, 2 goes to the next Step, 3 or more to the previous.
     */
    fun onPresses(count: Int) {
        when {
            count == 1 -> toggle()
            count == 2 -> next()
            count >= 3 -> previous()
        }
    }

    private fun takeOver(): Boolean {
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = ::onEvicted)
        if (!holdsFocus) {
            holdsFocus = focus.acquire(onLost = ::onFocusLost, onRegained = ::onFocusRegained)
        }
        if (!holdsFocus) arbiter.release(Tool.WARM_UP)
        return holdsFocus
    }

    private fun handBack() {
        holdsFocus = false
        pausedByFocus = false
        focus.release()
        arbiter.release(Tool.WARM_UP)
    }

    private fun onEvicted() {
        pausedByFocus = false
        player.pause(fade = false)
    }

    private fun onFocusLost() {
        holdsFocus = false
        if (player.playback.value?.playing != true) return
        pausedByFocus = true
        player.pause()
    }

    private fun onFocusRegained() {
        if (pausedByFocus) resume()
    }
}

/** What pressing Start on a Programme led to. */
enum class StartOutcome {
    /** The Programme is playing. */
    PLAYING,

    /** No Step fits the Range, or there are no Steps, so nothing plays. */
    NOTHING_FITS,

    /** Another app wouldn't give up the audio, so nothing plays. */
    AUDIO_BUSY,

    /** The sound output wouldn't start; the Programme waits, paused, on its first Step. */
    OUTPUT_FAILED,
}

/**
 * Plays a saved Programme, as it is saved now, on the Range from Settings: the one Start
 * behind the Warm-up home and the Programme editor.
 *
 * @param library the saved library, or null before it has loaded.
 * @param settings the saved settings, or null before they have loaded.
 * @param id the Programme.
 * @return what happened, or null (nothing tried) before loading or for a missing Programme.
 */
fun WarmupController.playSaved(
    library: Library?,
    settings: WarmupSettings?,
    id: ProgrammeId,
): StartOutcome? {
    val programme = library?.programmeToPlay(id) ?: return null
    val range = settings?.range ?: return null
    return play(programme = programme, range = range)
}
