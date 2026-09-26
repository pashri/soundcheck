package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM

/** How far the Step editor moves either end of a Range Offset, in half-steps: two octaves. */
const val MAX_RANGE_OFFSET: Int = 24

/**
 * This Step at a new tempo, kept within 30–300 bpm.
 *
 * @param bpm the tempo asked for.
 * @return the Step at [bpm], or at the nearest limit.
 */
fun SavedStep.withBpm(bpm: Int): SavedStep =
    copy(bpm = bpm.coerceIn(minimumValue = MIN_BPM, maximumValue = MAX_BPM))

/**
 * This Step with a new Range Offset, each end kept within ±[MAX_RANGE_OFFSET].
 *
 * @param bottom half-steps added below the Range.
 * @param top half-steps added above the Range.
 * @return the Step with the offset, each end at most [MAX_RANGE_OFFSET]
 *   either way.
 */
fun SavedStep.withRangeOffset(bottom: Int, top: Int): SavedStep = copy(
    rangeOffset = RangeOffset(
        bottom = bottom.coerceIn(
            minimumValue = -MAX_RANGE_OFFSET,
            maximumValue = MAX_RANGE_OFFSET,
        ),
        top = top.coerceIn(
            minimumValue = -MAX_RANGE_OFFSET,
            maximumValue = MAX_RANGE_OFFSET,
        ),
    ),
)
