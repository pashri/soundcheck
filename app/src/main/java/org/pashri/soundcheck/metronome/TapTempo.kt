package org.pashri.soundcheck.metronome

import kotlin.math.roundToInt

/**
 * Works out a tempo from the rhythm of taps.
 *
 * @param clockMs the current time in milliseconds, from a monotonic clock.
 */
class TapTempo(private val clockMs: () -> Long) {
    private val taps = ArrayDeque<Long>()

    /**
     * Records a tap.
     *
     * @return the tempo of the recent taps, clamped to [MIN_BPM]..[MAX_BPM], or null until
     *   there are two taps.
     */
    fun tap(): Int? {
        val now = clockMs()
        if (taps.isNotEmpty() && now - taps.last() > RESET_AFTER_MS) {
            taps.clear()
        }
        taps.addLast(now)
        if (taps.size > MAX_TAPS) taps.removeFirst()
        if (taps.size < 2) return null
        val averageMs = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
        return (MS_PER_MINUTE / averageMs).roundToInt()
            .coerceIn(MIN_BPM, MAX_BPM)
    }

    private companion object {
        const val RESET_AFTER_MS = 2_500L
        const val MAX_TAPS = 5
        const val MS_PER_MINUTE = 60_000.0
    }
}
