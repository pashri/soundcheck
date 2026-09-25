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
 * one sound-making slot; it hands both back when the Programme stops or ends. Call from the
 * main thread.
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
     * Plays [programme] from the start, pausing any other tool.
     *
     * @param programme the Programme.
     * @param range the Range it plays through.
     */
    fun play(programme: Programme, range: Range) {
        if (!takeOver()) return
        player.play(programme, range)
        if (player.playback.value == null) handBack()
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
        arbiter.claim(Tool.WARM_UP, onEvicted = ::onEvicted)
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
