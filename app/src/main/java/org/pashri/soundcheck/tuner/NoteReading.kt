package org.pashri.soundcheck.tuner

import kotlin.math.roundToInt
import org.pashri.soundcheck.music.CENTS_PER_SEMITONE
import org.pashri.soundcheck.music.HALF_STEPS_PER_OCTAVE
import org.pashri.soundcheck.music.hzOf
import org.pashri.soundcheck.music.pitchClassNameOf

/**
 * The nearest note to a pitch and how far off it is.
 *
 * @property midi the nearest note's MIDI number; 60 is middle C (C4).
 * @property cents how far the pitch is from that note: −50 (flat) up to,
 *           not including, +50.
 */
data class NoteReading(val midi: Int, val cents: Double) {
    /** The note's name, e.g. "B♭", spelled as the Warm-up spells it. */
    val name: String get() = pitchClassNameOf(midi)

    /** The scientific octave number: C4 is middle C, and B3 is the note
     *  below it.
     */
    val octave: Int get() = midi.floorDiv(HALF_STEPS_PER_OCTAVE) - 1

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
         * @param midi a fractional MIDI note number, as from
         *     [org.pashri.soundcheck.music.midiOf].
         * @return the nearest note and the offset from it.
         */
        fun of(midi: Double): NoteReading {
            val nearest = midi.roundToInt()
            return NoteReading(
                midi = nearest,
                cents = (midi - nearest) * CENTS_PER_SEMITONE,
            )
        }
    }
}
