package org.pashri.soundcheck.warmup

/**
 * The lowest and highest notes a Pattern sings, in half-steps from its root.
 *
 * @property lowest the lowest note; negative when a note sits below the root (♭1).
 * @property highest the highest note.
 */
data class SungSpan(val lowest: Int, val highest: Int) {
    /** Half-steps from the lowest sung note to the highest. */
    val halfSteps: Int
        get() = highest - lowest
}

/**
 * A reusable sequence of notes, each a scale degree relative to a root plus a length, with
 * the Key Chord that comes before each Iteration. It has no Sound, tempo or key of its own.
 *
 * @property name what the Pattern is called, e.g. "Arpeggio 8-hold".
 * @property notes the notes in order; never empty.
 * @property keyChord the chord that rings before each Iteration.
 */
data class Pattern(
    val name: String,
    val notes: List<PatternNote>,
    val keyChord: KeyChord,
) {
    init {
        require(notes.isNotEmpty()) { "Pattern \"$name\" has no notes" }
    }

    /** The lowest and highest sung notes relative to the root. */
    val span: SungSpan
        get() = SungSpan(
            lowest = notes.minOf { it.halfSteps },
            highest = notes.maxOf { it.halfSteps },
        )

    /** How long the Pattern lasts, in eighth notes. */
    val lengthInEighths: Int
        get() = notes.sumOf { it.length.eighths }

    /**
     * The Pattern's notes as pitches in the key of [key].
     *
     * @param key the root the degrees count from.
     * @return one pitch per note, in order.
     */
    fun pitchesIn(key: Pitch): List<Pitch> = notes.map { key + it.halfSteps }
}
