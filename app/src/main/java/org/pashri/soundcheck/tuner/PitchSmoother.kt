package org.pashri.soundcheck.tuner

import kotlin.math.abs

/** Recent estimates the median is taken over. */
const val MEDIAN_FRAMES: Int = 5

/** Estimates a new sound needs before it shows: 64 ms of hops, so a noisy attack is skipped. */
const val CONFIRM_FRAMES: Int = 3

/** Silent frames the last reading is held for before it clears: about 750 ms of hops. */
const val HOLD_FRAMES: Int = 35

/** How far each estimate pulls the needle towards the median, 0 to 1. */
const val SMOOTHING: Double = 0.3

/** A median this many half-steps from the reading is a new note: jump, don't glide. */
const val NOTE_CHANGE_SEMITONES: Double = 0.5

/**
 * Turns per-frame pitch estimates into a steady reading.
 *
 * The median of the last [MEDIAN_FRAMES] estimates throws out single wrong frames (an
 * octave slip, a pluck's attack), then exponential smoothing calms the needle. A jump of
 * more than [NOTE_CHANGE_SEMITONES] is taken at once so a new note doesn't slide in. A new
 * sound shows after [CONFIRM_FRAMES] estimates; after silence the reading holds for
 * [HOLD_FRAMES] frames and then clears. Allocates nothing per frame except its result.
 */
class PitchSmoother {
    private val history = DoubleArray(MEDIAN_FRAMES)
    private val scratch = DoubleArray(MEDIAN_FRAMES)
    private var count = 0
    private var next = 0
    private var misses = 0
    private var reading: Double? = null

    /**
     * Takes the next frame's estimate.
     *
     * @param hz the detected pitch, or null for a frame with no clear pitch.
     * @return the reading as a fractional MIDI note number, or null when nothing shows.
     */
    fun next(hz: Double?): Double? {
        if (hz == null) return afterSilentFrame()
        misses = 0
        remember(midiOf(hz))
        val current = reading
        if (current == null && count < CONFIRM_FRAMES) return null
        val median = median()
        val updated = if (current == null || abs(median - current) > NOTE_CHANGE_SEMITONES) {
            median
        } else {
            current + SMOOTHING * (median - current)
        }
        reading = updated
        return updated
    }

    /** Forgets every estimate, as if listening had just started. */
    fun reset() {
        count = 0
        next = 0
        misses = 0
        reading = null
    }

    private fun afterSilentFrame(): Double? {
        misses++
        if (misses > HOLD_FRAMES) reset()
        return reading
    }

    private fun remember(midi: Double) {
        history[next] = midi
        next = (next + 1) % MEDIAN_FRAMES
        if (count < MEDIAN_FRAMES) count++
    }

    private fun median(): Double {
        history.copyInto(scratch, endIndex = count)
        scratch.sort(fromIndex = 0, toIndex = count)
        return scratch[count / 2]
    }
}
