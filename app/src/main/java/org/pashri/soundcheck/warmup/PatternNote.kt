package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.music.HALF_STEPS_PER_OCTAVE

/**
 * Raises or lowers a Pattern note from the major scale.
 *
 * @property halfSteps how far the note moves: −1, 0 or +1.
 * @property symbol how the accidental is written before the degree; empty for none.
 */
enum class Accidental(val halfSteps: Int, val symbol: String) {
    /** A half-step lower, written ♭. */
    FLAT(halfSteps = -1, symbol = "♭"),

    /** As the major scale has it. */
    NATURAL(halfSteps = 0, symbol = ""),

    /** A half-step higher, written ♯. */
    SHARP(halfSteps = 1, symbol = "♯"),
}

/**
 * How long a Pattern note lasts. A quarter is one beat at the Step's tempo.
 *
 * @property eighths the length in eighth notes.
 * @property code the letter that marks this length in written Patterns, e.g. 'h' in "1h".
 */
enum class NoteLength(val eighths: Int, val code: Char) {
    /** Half a beat. */
    EIGHTH(eighths = 1, code = 'e'),

    /** One beat. */
    QUARTER(eighths = 2, code = 'q'),

    /** Two beats. */
    HALF(eighths = 4, code = 'h'),

    /** Four beats. */
    WHOLE(eighths = 8, code = 'w'),
}

/** The highest scale degree [PatternNote] accepts; kept in step with [PatternNotation]'s regex. */
const val MAX_DEGREE: Int = 99

/**
 * One note of a Pattern.
 *
 * @property degree the scale degree, 1 for the root; 8 is the octave, 9 the 2 above it.
 * @property length how long the note lasts.
 * @property accidental a raise or lowering from the major scale.
 */
data class PatternNote(
    val degree: Int,
    val length: NoteLength,
    val accidental: Accidental = Accidental.NATURAL,
) {
    init {
        require(degree in 1..MAX_DEGREE) {
            "Scale degrees must be between 1 and $MAX_DEGREE, not $degree"
        }
    }

    /** Half-steps from the root to this note. */
    val halfSteps: Int
        get() = majorScaleHalfSteps(degree) + accidental.halfSteps
}

/**
 * Half-steps from the root to [degree] of the major scale, continuing past the octave.
 *
 * @param degree a scale degree, 1 or more.
 * @return 0 for 1, 12 for 8, 19 for 12.
 * @throws IllegalArgumentException if [degree] is below 1.
 */
fun majorScaleHalfSteps(degree: Int): Int {
    require(degree >= 1) { "Scale degrees start at 1, not $degree" }
    val index = degree - 1
    val octaves = index / DEGREES_PER_OCTAVE
    return octaves * HALF_STEPS_PER_OCTAVE + MAJOR_SCALE[index % DEGREES_PER_OCTAVE]
}

private const val DEGREES_PER_OCTAVE = 7

private val MAJOR_SCALE = listOf(0, 2, 4, 5, 7, 9, 11)
