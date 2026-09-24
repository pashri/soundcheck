package org.pashri.soundcheck.metronome

import kotlin.math.roundToLong

/**
 * One metronome beat.
 *
 * @property index beats since the Metronome started.
 * @property frame when the click sounds.
 * @property accented whether it gets the accented click.
 * @property positionInBar 0 for the first beat of a bar; always 0 with the accent off.
 */
data class Beat(
    val index: Long,
    val frame: Long,
    val accented: Boolean,
    val positionInBar: Int,
)

/**
 * Where every beat falls, measured from one anchor beat.
 *
 * Each frame is computed from the anchor rather than by adding to the previous beat, so
 * rounding never accumulates.
 *
 * @property anchorFrame the frame of beat [anchorIndex].
 * @property anchorIndex the beat the grid is anchored on.
 * @property framesPerBeat the unrounded spacing.
 */
data class BeatGrid(
    val anchorFrame: Long,
    val anchorIndex: Long,
    val framesPerBeat: Double,
) {
    /**
     * The frame beat [index] sounds on.
     *
     * @param index a beat index at or after [anchorIndex].
     * @return its frame.
     */
    fun frameOf(index: Long): Long =
        anchorFrame + ((index - anchorIndex) * framesPerBeat).roundToLong()
}
