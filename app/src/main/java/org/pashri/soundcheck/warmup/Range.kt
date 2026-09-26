package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.music.Pitch

/**
 * The lowest and highest notes you want a Warm-up to reach. There is one Range for the
 * whole app; each Step may adjust it with a [RangeOffset].
 *
 * @property lowest the lowest note.
 * @property highest the highest note; never below [lowest].
 */
data class Range(val lowest: Pitch, val highest: Pitch) {
    init {
        require(lowest <= highest) { "Range $lowest – $highest is upside down" }
    }

    /** Half-steps from [lowest] to [highest]. */
    val halfSteps: Int
        get() = highest - lowest

    /**
     * Whether [pitch] lies in this Range, edges included.
     *
     * @param pitch the note to check.
     * @return true if [lowest] ≤ [pitch] ≤ [highest].
     */
    operator fun contains(pitch: Pitch): Boolean = pitch >= lowest && pitch <= highest

    /**
     * This Range with a Step's Range Offset applied. An end the offset would push past the
     * piano stops at the piano's last key, A0 or C8.
     *
     * @param offset half-steps added to (or, when negative, taken off) each end.
     * @return the effective Range, or null if the offset closes it.
     */
    fun offsetBy(offset: RangeOffset): Range? {
        val low = maxOf(lowest.midi - offset.bottom, PIANO.lowest.midi)
        val high = minOf(highest.midi + offset.top, PIANO.highest.midi)
        return if (low <= high) Range(lowest = Pitch(low), highest = Pitch(high)) else null
    }

    /** The piano's compass. */
    companion object {
        /** A0 to C8, MIDI 21 to 108: every key of the sampled piano. */
        val PIANO: Range = Range(lowest = Pitch(21), highest = Pitch(108))
    }
}

/**
 * A Step's adjustment to the Range, in half-steps at each end. Positive widens the Range at
 * that end and negative narrows it, so top +2 lets a lip trill go two half-steps higher.
 *
 * @property bottom half-steps added below the Range's lowest note.
 * @property top half-steps added above the Range's highest note.
 */
data class RangeOffset(val bottom: Int = 0, val top: Int = 0) {
    /** Common offsets. */
    companion object {
        /** No change at either end. */
        val NONE: RangeOffset = RangeOffset()
    }
}

/**
 * A suggested starting Range.
 *
 * @property label the name the Settings screen shows.
 * @property range the Range it suggests.
 */
enum class VoiceType(val label: String, val range: Range) {
    /** C4 to A5. */
    SOPRANO(label = "Soprano", range = presetRange(lowest = "C4", highest = "A5")),

    /** F3 to D5. */
    ALTO(label = "Alto", range = presetRange(lowest = "F3", highest = "D5")),

    /** C3 to A4. */
    TENOR(label = "Tenor", range = presetRange(lowest = "C3", highest = "A4")),

    /** E2 to E4. */
    BASS(label = "Bass", range = presetRange(lowest = "E2", highest = "E4")),
}

private fun presetRange(lowest: String, highest: String): Range =
    Range(lowest = Pitch.parse(lowest), highest = Pitch.parse(highest))
