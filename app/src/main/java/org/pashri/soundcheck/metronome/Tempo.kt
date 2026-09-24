package org.pashri.soundcheck.metronome

import org.pashri.soundcheck.audio.SAMPLE_RATE

/** The slowest tempo the Metronome offers. */
const val MIN_BPM: Int = 30

/** The fastest tempo the Metronome offers. */
const val MAX_BPM: Int = 300

/** The tempo a fresh install starts at. */
const val DEFAULT_BPM: Int = 96

/** The accent a fresh install starts with: every fourth beat. */
const val DEFAULT_ACCENT: Int = 4

/**
 * Frames between beats at [bpm], unrounded so long runs don't drift.
 *
 * @param bpm beats per minute.
 * @return frames per beat at [SAMPLE_RATE].
 */
fun framesPerBeat(bpm: Int): Double = SAMPLE_RATE * SECONDS_PER_MINUTE / bpm

private const val SECONDS_PER_MINUTE = 60.0
