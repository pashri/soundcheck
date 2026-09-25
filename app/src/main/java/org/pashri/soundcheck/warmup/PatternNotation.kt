package org.pashri.soundcheck.warmup

/**
 * Writes and reads Pattern notes as text such as "1 3 5 8e 8e ♭3h".
 *
 * Notes are separated by spaces. Each is an optional ♭ or ♯, a scale degree from 1 to 99,
 * and an optional length letter: e (eighth), q (quarter), h (half) or w (whole).
 */
object PatternNotation {
    /**
     * Reads notes from [text].
     *
     * @param text the notes, e.g. "1 3 5 8e 8e 8e 8e 5 3 1h".
     * @param defaultLength the length of a note written without a letter.
     * @return the notes in order.
     * @throws IllegalArgumentException if [text] is blank or any note is malformed.
     */
    fun parse(text: String, defaultLength: NoteLength = NoteLength.QUARTER): List<PatternNote> =
        text.trim().split(WHITESPACE).map { parseNote(token = it, defaultLength = defaultLength) }

    /**
     * Writes [notes] with every length spelled out, so [parse] reads them back exactly.
     *
     * @param notes the notes to write.
     * @return text such as "1q 3q ♭3h".
     */
    fun format(notes: List<PatternNote>): String =
        notes.joinToString(separator = " ") { "${degreeText(it)}${it.length.code}" }

    /**
     * Writes only the degrees and accidentals of [notes], for display.
     *
     * @param notes the notes to write.
     * @return text such as "1 2 ♭3 4 5".
     */
    fun degrees(notes: List<PatternNote>): String =
        notes.joinToString(separator = " ") { degreeText(it) }

    private fun degreeText(note: PatternNote): String = "${note.accidental.symbol}${note.degree}"

    private fun parseNote(token: String, defaultLength: NoteLength): PatternNote {
        val match = requireNotNull(NOTE.matchEntire(token)) { "Not a Pattern note: \"$token\"" }
        val (accidental, degree, length) = match.destructured
        return PatternNote(
            degree = degree.toInt(),
            length = lengthFor(code = length, defaultLength = defaultLength),
            accidental = Accidental.entries.first { it.symbol == accidental },
        )
    }

    private fun lengthFor(code: String, defaultLength: NoteLength): NoteLength =
        NoteLength.entries.firstOrNull { it.code.toString() == code } ?: defaultLength

    private val WHITESPACE = Regex("\\s+")

    // Degrees 1–99, matching [MAX_DEGREE] in PatternNote.kt.
    private val NOTE = Regex("([♭♯]?)([1-9][0-9]?)([eqhw]?)")
}
