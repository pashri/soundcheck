package org.pashri.soundcheck.metronome

import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.msToFrames

/**
 * Keeps a [SoundOutput] supplied with clicks a little ahead of time.
 *
 * Clicks are scheduled on exact frames, so their timing doesn't depend on when this loop
 * wakes; the loop only has to stay [LOOKAHEAD_MS] ahead. Call every method from the thread
 * [scope] runs on.
 *
 * @param output where the clicks play.
 * @param scope runs the scheduling loop; cancelling it stops the loop.
 */
class Metronome(private val output: SoundOutput, private val scope: CoroutineScope) {
    private val _beat = MutableStateFlow<Beat?>(null)

    /** The beat sounding now, or null when stopped or before the first click. */
    val beat: StateFlow<Beat?> = _beat.asStateFlow()

    private var job: Job? = null
    private var grid: BeatGrid? = null
    private var accentEvery: Int? = null
    private var nextIndex = 0L
    private var barOrigin = 0L
    private val pending = ArrayDeque<Beat>()

    init {
        output.loadSample(SampleIds.METRONOME_CLICK, ClickSynth.click(CLICK_HZ))
        output.loadSample(SampleIds.METRONOME_ACCENT, ClickSynth.click(ACCENT_HZ))
    }

    /**
     * Starts clicking, replacing any beat already running.
     *
     * @param bpm the tempo.
     * @param accentEvery beats per bar, or null for no accent.
     * @return whether the output started.
     */
    fun start(bpm: Int, accentEvery: Int?): Boolean {
        stop()
        if (!output.start()) return false
        this.accentEvery = accentEvery
        val first = output.framePosition() + msToFrames(START_MARGIN_MS)
        grid = BeatGrid(anchorFrame = first, anchorIndex = 0, framesPerBeat = framesPerBeat(bpm))
        job = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
        return true
    }

    /**
     * Changes the tempo from the next beat, keeping the beat sounding now.
     *
     * @param bpm the new tempo.
     */
    fun setTempo(bpm: Int) {
        val current = grid ?: return
        val now = output.framePosition()
        val last = cancelPending(now)
        val perBeat = framesPerBeat(bpm)
        grid = if (last == null) current.copy(framesPerBeat = perBeat) else BeatGrid(
            anchorFrame = maxOf(
                last.frame + perBeat.roundToLong(),
                now + msToFrames(RESCHEDULE_MARGIN_MS),
            ),
            anchorIndex = last.index + 1,
            framesPerBeat = perBeat,
        )
        tick()
    }

    /**
     * Changes the accent; the next beat starts a new bar.
     *
     * @param accentEvery beats per bar, or null for no accent.
     */
    fun setAccent(accentEvery: Int?) {
        if (grid != null) cancelPending(output.framePosition())
        this.accentEvery = accentEvery
        barOrigin = nextIndex
        tick()
    }

    /** Stops clicking at once. */
    fun stop() {
        job?.cancel()
        job = null
        if (grid != null) {
            output.silence()
            output.stop()
        }
        grid = null
        pending.clear()
        nextIndex = 0
        barOrigin = 0
        _beat.value = null
    }

    private fun tick() {
        val current = grid ?: return
        val now = output.framePosition()
        val horizon = now + msToFrames(LOOKAHEAD_MS)
        while (current.frameOf(nextIndex) < horizon) scheduleBeat(current.frameOf(nextIndex))
        publishCurrent(now)
    }

    private fun scheduleBeat(frame: Long) {
        val beat = beatAt(nextIndex, frame)
        val id = if (beat.accented) SampleIds.METRONOME_ACCENT else SampleIds.METRONOME_CLICK
        output.schedule(id, frame, if (beat.accented) ACCENT_GAIN else CLICK_GAIN)
        pending.addLast(beat)
        nextIndex++
    }

    private fun beatAt(index: Long, frame: Long): Beat {
        val every = accentEvery
        val position = if (every == null) 0L else Math.floorMod(index - barOrigin, every.toLong())
        return Beat(
            index = index,
            frame = frame,
            accented = every != null && position == 0L,
            positionInBar = position.toInt(),
        )
    }

    private fun publishCurrent(now: Long) {
        while (pending.size > 1 && pending[1].frame <= now) pending.removeFirst()
        _beat.value = pending.firstOrNull()?.takeIf { it.frame <= now }
    }

    /**
     * Cancels beats that haven't sounded; returns the last one that has, or is committed to.
     *
     * A beat inside [RESCHEDULE_MARGIN_MS] of [now] is kept rather than cancelled: the
     * native mixer drains its commands per audio block and can already be part-way into
     * starting that beat's voice by the time [SoundOutput.cancelFrom] takes effect, so
     * treating it as still cancellable would let it be rescheduled and played twice. This
     * margin assumes the engine's audio block is no longer than [RESCHEDULE_MARGIN_MS] (960
     * frames at 48 kHz); a longer block could let a cancelled beat start anyway.
     */
    private fun cancelPending(now: Long): Beat? {
        val commit = now + msToFrames(RESCHEDULE_MARGIN_MS)
        output.cancelFrom(commit)
        pending.removeAll { it.frame >= commit }
        val last = pending.lastOrNull()
        nextIndex = (last?.index ?: -1) + 1
        return last
    }

    /** Timing and sound constants. */
    companion object {
        /** How far ahead clicks are scheduled. */
        const val LOOKAHEAD_MS: Long = 150L

        /** Gap between pressing Start and the first click. */
        const val START_MARGIN_MS: Long = 100L

        /** Shortest gap before a click rescheduled by a tempo change. */
        const val RESCHEDULE_MARGIN_MS: Long = 20L

        /** How often the loop tops up the schedule. */
        const val TICK_MS: Long = 25L

        /** Loudness of an ordinary click. */
        const val CLICK_GAIN: Float = 0.7f

        /** Loudness of an accented click. */
        const val ACCENT_GAIN: Float = 1f

        /** Pitch of an ordinary click. */
        const val CLICK_HZ: Double = 1_000.0

        /** Pitch of an accented click. */
        const val ACCENT_HZ: Double = 1_600.0
    }
}
