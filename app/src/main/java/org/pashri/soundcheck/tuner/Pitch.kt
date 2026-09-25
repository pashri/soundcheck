package org.pashri.soundcheck.tuner

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Concert pitch: the A above middle C. Fixed; not a setting. */
const val A4_HZ: Double = 440.0

/** The MIDI note number of A4. */
const val A4_MIDI: Int = 69

/** Half-steps in an octave. */
const val SEMITONES_PER_OCTAVE: Int = 12

/** Cents in a half-step. */
const val CENTS_PER_SEMITONE: Double = 100.0

/** The twelve note names from C, spelled D♭, E♭, F♯, A♭ and B♭. */
val NOTE_NAMES: List<String> =
    listOf("C", "D♭", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")

/**
 * The fractional MIDI note number of a frequency.
 *
 * @param hz a frequency above zero.
 * @return 69.0 for A4, 69.5 for a quarter-tone above it.
 */
fun midiOf(hz: Double): Double = A4_MIDI + SEMITONES_PER_OCTAVE * log2(hz / A4_HZ)

/**
 * The frequency of a fractional MIDI note number; the inverse of [midiOf].
 *
 * @param midi a MIDI note number, possibly between notes.
 * @return the frequency in Hz.
 */
fun hzOf(midi: Double): Double = A4_HZ * 2.0.pow((midi - A4_MIDI) / SEMITONES_PER_OCTAVE)

/**
 * The nearest note to a pitch and how far off it is.
 *
 * @property midi the nearest note's MIDI number; 60 is middle C (C4).
 * @property cents how far the pitch is from that note: −50 (flat) up to,
 *           not including, +50.
 */
data class NoteReading(val midi: Int, val cents: Double) {
    /** The note's name, e.g. "B♭". */
    val name: String get() = NOTE_NAMES[midi.mod(SEMITONES_PER_OCTAVE)]

    /** The scientific octave number: C4 is middle C, and B3 is the note
     *  below it.
     */
    val octave: Int get() = midi.floorDiv(SEMITONES_PER_OCTAVE) - 1

    /** The frequency heard, in Hz. */
    val hz: Double get() = hzOf(midi + cents / CENTS_PER_SEMITONE)

    /** [cents] rounded to a whole number, as shown: −50 to +50. */
    val roundedCents: Int get() = cents.roundToInt()

    /** Builds readings. */
    companion object {
        /**
         * The reading for a pitch. A pitch exactly halfway between two
         * notes belongs to the upper one, 50 cents flat.
         *
         * @param midi a fractional MIDI note number, as from [midiOf].
         * @return the nearest note and the offset from it.
         */
        fun of(midi: Double): NoteReading {
            val nearest = midi.roundToInt()
            return NoteReading(
                midi = nearest,
                cents = (midi - nearest) * CENTS_PER_SEMITONE
            )
        }
    }
}
