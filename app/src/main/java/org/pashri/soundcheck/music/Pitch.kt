package org.pashri.soundcheck.music

/** Half-steps in one octave. */
const val HALF_STEPS_PER_OCTAVE: Int = 12

/**
 * The conventional name of a note's pitch class, the same in every key: C, D♭, D, E♭, E,
 * F, F♯, G, A♭, A, B♭ or B.
 *
 * @param midi any MIDI note number; numbers outside 0–127 wrap by octaves.
 * @return the name without its octave, e.g. "E♭".
 */
fun pitchClassNameOf(midi: Int): String = PITCH_CLASS_NAMES[midi.mod(HALF_STEPS_PER_OCTAVE)]

/**
 * A piano key, identified by its MIDI note number; middle C is C4, MIDI 60.
 *
 * Names use the conventional spellings C, D♭, D, E♭, E, F, F♯, G, A♭, A, B♭ and B in
 * every key, with scientific octave numbers that go up at each C.
 *
 * @property midi the MIDI note number, 0 (C-1) to 127 (G9).
 */
@JvmInline
value class Pitch(val midi: Int) : Comparable<Pitch> {
    init {
        require(midi in MIDI_NOTES) { "MIDI note $midi is outside $MIDI_NOTES" }
    }

    /** The note name without its octave, e.g. "E♭". */
    val pitchClassName: String
        get() = pitchClassNameOf(midi)

    /** The scientific octave: 4 from middle C up to the B above it. */
    val octave: Int
        get() = midi / HALF_STEPS_PER_OCTAVE - 1

    /** The note name with its octave, e.g. "E♭3". */
    val name: String
        get() = "$pitchClassName$octave"

    /**
     * The pitch [halfSteps] higher, or lower when negative.
     *
     * @param halfSteps how far to move.
     * @return the moved pitch.
     * @throws IllegalArgumentException if the result is outside MIDI 0–127.
     */
    operator fun plus(halfSteps: Int): Pitch = Pitch(midi + halfSteps)

    /**
     * How many half-steps this pitch is above [other].
     *
     * @param other the pitch to measure from.
     * @return the distance, negative when this pitch is lower.
     */
    operator fun minus(other: Pitch): Int = midi - other.midi

    /**
     * Orders pitches from low to high.
     *
     * @param other the pitch to compare with.
     * @return negative, zero or positive as this pitch is lower, equal or higher.
     */
    override fun compareTo(other: Pitch): Int = midi.compareTo(other.midi)

    /**
     * The note name with its octave, so test failures and logs read "E♭3".
     *
     * @return [name].
     */
    override fun toString(): String = name

    /** Limits and parsing for [Pitch]. */
    companion object {
        /** Every MIDI note number a [Pitch] can have. */
        val MIDI_NOTES: IntRange = 0..127

        /**
         * Reads a name such as "C4", "E♭3" or "F♯4".
         *
         * @param text a conventional pitch-class name followed by an octave from -1 to 9.
         * @return the pitch.
         * @throws IllegalArgumentException if [text] is malformed, spelled
         *     unconventionally (C♯, G♭) or outside MIDI 0–127.
         */
        fun parse(text: String): Pitch {
            val match = requireNotNull(NAME_PATTERN.matchEntire(text)) {
                "Not a note name: \"$text\""
            }
            val (letter, octave) = match.destructured
            val pitchClass = PITCH_CLASS_NAMES.indexOf(letter)
            require(pitchClass >= 0) { "\"$letter\" is not a conventional spelling" }
            return Pitch((octave.toInt() + 1) * HALF_STEPS_PER_OCTAVE + pitchClass)
        }
    }
}

private val PITCH_CLASS_NAMES =
    listOf("C", "D♭", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")

private val NAME_PATTERN = Regex("([A-G][♭♯]?)(-1|[0-9])")
