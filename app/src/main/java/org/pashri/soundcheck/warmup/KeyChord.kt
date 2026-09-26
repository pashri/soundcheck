package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.music.Pitch

/**
 * The chord that rings for one bar before each Iteration, giving the new key and time to
 * breathe. Its quality belongs to the Pattern.
 *
 * @property label the quality's name as the Pattern editor shows it.
 * @property halfSteps each chord note's distance above the key, lowest first.
 */
enum class KeyChord(val label: String, val halfSteps: List<Int>) {
    /** Root, major third, fifth. */
    MAJOR(label = "major", halfSteps = listOf(0, 4, 7)),

    /** Root, minor third, fifth. */
    MINOR(label = "minor", halfSteps = listOf(0, 3, 7)),

    /** Major triad with a minor seventh (dominant seventh). */
    SEVENTH(label = "7", halfSteps = listOf(0, 4, 7, 10)),

    /** Major triad with a major seventh. */
    MAJOR_SEVENTH(label = "maj7", halfSteps = listOf(0, 4, 7, 11)),

    /** Minor triad with a minor seventh. */
    MINOR_SEVENTH(label = "m7", halfSteps = listOf(0, 3, 7, 10)),

    /** Root, minor third, diminished fifth. */
    DIMINISHED(label = "dim", halfSteps = listOf(0, 3, 6)),

    /** Root, major third, augmented fifth. */
    AUGMENTED(label = "aug", halfSteps = listOf(0, 4, 8)),

    /** The key on its own, a single note. */
    ROOT_ONLY(label = "root only", halfSteps = listOf(0)),
    ;

    /**
     * The chord's notes in close position, upward from [key].
     *
     * @param key the Iteration's key, which is also the chord's lowest note.
     * @return the chord's pitches, lowest first.
     */
    fun pitchesOn(key: Pitch): List<Pitch> = halfSteps.map { key + it }
}
