package org.pashri.soundcheck.warmup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.piano.Piano

/**
 * Plays a few piano notes from an editor (a Step's Demo, or a Pattern once), or a Sound's
 * recorded clip from the Sounds list. It is a tool of its own ([Tool.AUDITION]), so
 * starting it pauses a playing Programme (and stops the Metronome, the Tuner or a
 * recording), and any tool starting after it stops it. It asks for transient audio focus
 * while it sounds (skipped with "Play over other audio" on, through the focus gate it is
 * given) and hands focus, the slot and the output back when it ends, when it is stopped, or
 * when focus is lost. It belongs to the app, like the Warm-up. Call from the main thread.
 *
 * @param output where the sounds play.
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

    /** The clip in [SampleIds.PREVIEW], so the same clip isn't loaded twice in a row. */
    private var previewLoaded: ClipName? = null

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
        if (notes.isEmpty() || !begin()) return false
        job = scope.launch { run(notes) }
        return true
    }

    /**
     * Plays a Sound's recorded clip once, from a moment after the tap, stopping any audition
     * already sounding and pausing a playing Programme. The engine keeps every sample it is
     * given until the app closes, so a clip played again straight after is not loaded again.
     *
     * @param name the clip's file name, which says whether it is loaded already.
     * @param pcm the clip, mono at the engine's rate.
     * @return false, having changed nothing else, if [pcm] is empty; false after handing
     *     everything back if focus is refused or the output won't start.
     */
    fun playClip(name: ClipName, pcm: FloatArray): Boolean {
        stop()
        if (pcm.isEmpty() || !begin()) return false
        job = scope.launch { runClip(name = name, pcm = pcm) }
        return true
    }

    /** Stops at once and hands focus, the slot and the output back. Safe when silent. */
    fun stop() {
        if (!_playing.value) return
        halt()
        arbiter.release(Tool.AUDITION)
    }

    /**
     * Takes the slot, focus and the output.
     *
     * @return false, having handed them back, if focus is refused or the output won't start.
     */
    private fun begin(): Boolean {
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
        return true
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

    private suspend fun runClip(name: ClipName, pcm: FloatArray) {
        if (previewLoaded != name) {
            if (!output.loadSample(id = SampleIds.PREVIEW, pcm = pcm)) {
                job = null
                stop()
                return
            }
            previewLoaded = name
        }
        val origin = output.framePosition() + msToFrames(ProgrammePlayer.START_MARGIN_MS)
        output.schedule(
            id = SampleIds.PREVIEW,
            frame = origin,
            gain = ProgrammePlayer.ANNOUNCEMENT_GAIN,
        )
        val end = origin + pcm.size + msToFrames(RELEASE_MS)
        while (output.framePosition() < end && !output.hasFailed()) {
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
