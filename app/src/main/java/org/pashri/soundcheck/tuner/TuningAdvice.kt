package org.pashri.soundcheck.tuner

import kotlin.math.abs

/** The words under the Tuner's needle. */
object TuningAdvice {
    /** Within this many cents either way counts as in tune. */
    const val IN_TUNE_CENTS: Int = 2

    /** Up to this many cents off is "a touch" off. */
    const val NEARLY_CENTS: Int = 10

    /** Up to this many cents off is plainly off; beyond it is "very" off. */
    const val OFF_CENTS: Int = 25

    /**
     * The big reading.
     *
     * @param cents the rounded offset from the note, −50 to +50.
     * @return e.g. "4 cents flat", "1 cent sharp", or "spot on" at 0.
     */
    fun reading(cents: Int): String {
        if (cents == 0) return "spot on"
        val size = abs(cents)
        val unit = if (size == 1) "cent" else "cents"
        return "$size $unit ${if (cents < 0) "flat" else "sharp"}"
    }

    /**
     * The plain-language line under the reading.
     *
     * @param cents the rounded offset from the note, −50 to +50.
     * @return e.g. "a touch low, nearly there".
     */
    fun advice(cents: Int): String {
        val size = abs(cents)
        val (flat, sharp) = when {
            size <= IN_TUNE_CENTS -> return "in tune, hold it there"
            size <= NEARLY_CENTS ->
                "a touch low, nearly there" to "a touch high, nearly there"
            size <= OFF_CENTS -> "low, bring it up" to "high, bring it down"
            else -> "very low, bring it up" to "very high, bring it down"
        }
        return if (cents < 0) flat else sharp
    }
}
