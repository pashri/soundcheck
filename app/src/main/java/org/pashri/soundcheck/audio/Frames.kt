package org.pashri.soundcheck.audio

/**
 * Converts a duration to a frame count at [SAMPLE_RATE].
 *
 * @param ms the duration in milliseconds.
 * @return the number of frames, rounded down.
 */
fun msToFrames(ms: Long): Long = ms * SAMPLE_RATE / MS_PER_SECOND

private const val MS_PER_SECOND = 1_000L
