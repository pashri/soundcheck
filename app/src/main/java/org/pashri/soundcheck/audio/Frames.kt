package org.pashri.soundcheck.audio

/**
 * Converts a duration to a frame count at [SAMPLE_RATE].
 *
 * @param ms the duration in milliseconds.
 * @return the number of frames, rounded down.
 */
fun msToFrames(ms: Long): Long = ms * SAMPLE_RATE / MS_PER_SECOND

/**
 * Converts a frame count at [SAMPLE_RATE] to a duration.
 *
 * @param frames the number of frames.
 * @return the duration in milliseconds, rounded down.
 */
fun framesToMs(frames: Long): Long = frames * MS_PER_SECOND / SAMPLE_RATE

private const val MS_PER_SECOND = 1_000L
