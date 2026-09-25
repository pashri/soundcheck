package org.pashri.soundcheck.warmup

/** Which end of the Range a Step starts from. */
enum class Direction {
    /** Starts at the lowest key, goes up to the top and comes back down. */
    START_LOW,

    /** Starts at the highest key, goes down to the bottom and comes back up. */
    START_HIGH,
}

/** The keys a Step plays through its Range, or why it can't. */
sealed interface RoundTrip {
    /**
     * The Step fits: one key per Iteration, a half-step apart, out and back.
     *
     * @property keys each Iteration's key in play order; the first and last are the same
     *     and the turn-around key appears once, in the middle.
     */
    data class Fits(val keys: List<Pitch>) : RoundTrip {
        init {
            require(keys.isNotEmpty()) { "A round trip has at least one key" }
        }

        /** The key of the first Iteration, which is also the Demo's key. */
        val startKey: Pitch
            get() = keys.first()

        /** The key at the far end of the Range, where the Step turns back. */
        val turnKey: Pitch
            get() = keys[keys.size / 2]
    }

    /**
     * The Pattern is wider than the Step's Range, so the Step is skipped.
     *
     * @property neededHalfSteps the Pattern's sung span.
     * @property availableHalfSteps the width of the Range with the Range Offset applied;
     *     0 when the offset closes it.
     */
    data class DoesNotFit(val neededHalfSteps: Int, val availableHalfSteps: Int) : RoundTrip
}

/**
 * Plans a Step's round trip: a half-step per Iteration from its starting end to the far end
 * of the Range and back, turning when the Pattern's highest (or lowest) sung note reaches
 * the edge, so no sung note falls outside the Range.
 *
 * @param range the app's Range.
 * @param offset the Step's Range Offset.
 * @param span the Pattern's sung span.
 * @param direction which end the Step starts from.
 * @return the Iteration keys, or [RoundTrip.DoesNotFit].
 */
fun planRoundTrip(
    range: Range,
    offset: RangeOffset,
    span: SungSpan,
    direction: Direction,
): RoundTrip {
    val effective = range.offsetBy(offset)
    val available = effective?.halfSteps ?: 0
    if (effective == null || available < span.halfSteps) {
        return RoundTrip.DoesNotFit(
            neededHalfSteps = span.halfSteps,
            availableHalfSteps = available,
        )
    }
    val lowestKey = effective.lowest.midi - span.lowest
    val highestKey = effective.highest.midi - span.highest
    return RoundTrip.Fits(
        keys = tripKeys(lowestKey = lowestKey, highestKey = highestKey, direction = direction)
            .map(::Pitch),
    )
}

private fun tripKeys(lowestKey: Int, highestKey: Int, direction: Direction): List<Int> {
    val upward = (lowestKey..highestKey).toList()
    val outward = if (direction == Direction.START_LOW) upward else upward.reversed()
    return outward + outward.dropLast(1).reversed()
}
