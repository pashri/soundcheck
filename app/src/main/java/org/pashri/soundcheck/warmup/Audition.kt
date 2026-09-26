package org.pashri.soundcheck.warmup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.piano.Piano

/**
 * Plays a few piano notes from an editor: a Step's Demo, or a Pattern once. It is a tool of
 * its own ([Tool.AUDITION]), so starting it pauses a playing Programme (and stops the
 * Metronome or the Tuner), and any tool starting after it stops it. It asks for transient
 * audio focus while it sounds (skipped with "Play over other audio" on, through the focus
 * gate it is given) and hands focus, the slot and the output back when it ends, when it is
 * stopped, or when focus is lost. It belongs to the app, like the Warm-up. Call from the
 * main thread.
 *
 * @param output where the notes play.
 * @param piano the piano they play on.
 * @param focus the audition's own audio focus.
 * @param arbiter keeps one tool sounding at a time.
 * @param scope runs the scheduling loop.
 */
class Audition(
    private val output: SoundOutput,
    private val piano: Piano,
    private val focus: FocusGate,
    private val arbiter: ToolArbiter,
    private val scope: CoroutineScope,
) {
    private val _playing = MutableStateFlow(false)

    /** Whether an audition is sounding. */
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private var job: Job? = null

    /**
     * Plays [notes] once, from a moment after the tap, stopping any audition already
     * sounding and pausing a playing Programme.
     *
     * @param notes the notes, ordered by start frame, with frames counted from the first.
     * @return false, having changed nothing else, if there is nothing to play; false after
     *     handing everything back if focus is refused or the output won't start.
     */
    fun play(notes: List<PianoNoteEvent>): Boolean {
        stop()
        if (notes.isEmpty()) return false
        // Claims the slot before asking for focus, so a playing Programme is already paused
        // (not just about to be) when it receives the transient loss; otherwise it would see
        // itself still holding the slot and resume once the Demo's focus request lands.
        arbiter.claim(tool = Tool.AUDITION, onEvicted = ::halt)
        if (!focus.acquire(onLost = ::stop) || !output.start()) {
            focus.release()
            arbiter.release(Tool.AUDITION)
            return false
        }
        _playing.value = true
        job = scope.launch { run(notes) }
        return true
    }

    /** Stops at once and hands focus, the slot and the output back. Safe when silent. */
    fun stop() {
        if (!_playing.value) return
        halt()
        arbiter.release(Tool.AUDITION)
    }

    /** Silences and lets go of the output and focus; the slot is already someone else's. */
    private fun halt() {
        job?.cancel()
        job = null
        _playing.value = false
        output.silence()
        output.stop()
        focus.release()
    }

    private suspend fun run(notes: List<PianoNoteEvent>) {
        piano.load()
        val origin = output.framePosition() + msToFrames(ProgrammePlayer.START_MARGIN_MS)
        val end = origin + notes.maxOf { it.endFrame } + msToFrames(RELEASE_MS)
        var next = 0
        while (output.framePosition() < end && !output.hasFailed()) {
            val horizon = output.framePosition() + msToFrames(ProgrammePlayer.LOOKAHEAD_MS)
            while (next < notes.size && origin + notes[next].startFrame < horizon) {
                output.schedulePianoNote(piano = piano, event = notes[next], origin = origin)
                next++
            }
            delay(ProgrammePlayer.TICK_MS)
        }
        job = null
        stop()
    }

    /** Timing. */
    companion object {
        /** How long after the last note's end the output is let go: its fade and a little. */
        const val RELEASE_MS: Long = 200L
    }
}
