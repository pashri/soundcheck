package org.pashri.soundcheck.music

import kotlin.math.log2
import kotlin.math.pow

/** Concert pitch: the A above middle C. Fixed; not a setting. */
const val A4_HZ: Double = 440.0

/** The MIDI note number of A4. */
const val A4_MIDI: Int = 69

/** Cents in a half-step. */
const val CENTS_PER_SEMITONE: Double = 100.0

/**
 * How many times higher a note [halfSteps] up sounds. It is also the speed to play a
 * recording at to move it that far.
 *
 * @param halfSteps the interval, fractional for cents; negative for down.
 * @return 2.0 for +12, 0.5 for −12.
 */
fun frequencyRatio(halfSteps: Double): Double = 2.0.pow(halfSteps / HALF_STEPS_PER_OCTAVE)

/**
 * The fractional MIDI note number of a frequency.
 *
 * @param hz a frequency above zero.
 * @return 69.0 for A4, 69.5 for a quarter-tone above it.
 */
fun midiOf(hz: Double): Double = A4_MIDI + HALF_STEPS_PER_OCTAVE * log2(hz / A4_HZ)

/**
 * The frequency of a fractional MIDI note number; the inverse of [midiOf].
 *
 * @param midi a MIDI note number, possibly between notes.
 * @return the frequency in Hz.
 */
fun hzOf(midi: Double): Double = A4_HZ * frequencyRatio(midi - A4_MIDI)
