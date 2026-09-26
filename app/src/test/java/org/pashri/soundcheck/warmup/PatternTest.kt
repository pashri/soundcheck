package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class PatternTest {
    private fun note(
        degree: Int,
        length: NoteLength = NoteLength.EIGHTH,
        accidental: Accidental = Accidental.NATURAL,
    ): PatternNote = PatternNote(degree = degree, length = length, accidental = accidental)

    private fun pattern(notes: List<PatternNote>): Pattern =
        Pattern(id = PatternId("test"), name = "Test", notes = notes, keyChord = KeyChord.MAJOR)

    @Test
    fun `the double arpeggio spans 19 half-steps from its root`() {
        val degrees = listOf(1, 3, 5, 8, 10, 12, 11, 9, 7, 5, 4, 2, 1)
        val span = pattern(degrees.map { note(it) }).span
        assertEquals(SungSpan(lowest = 0, highest = 19), span)
        assertEquals(19, span.halfSteps)
    }

    @Test
    fun `a flattened root reaches below the root`() {
        val span = pattern(listOf(note(1, accidental = Accidental.FLAT), note(5))).span
        assertEquals(SungSpan(lowest = -1, highest = 7), span)
        assertEquals(8, span.halfSteps)
    }

    @Test
    fun `a pattern's length is the sum of its notes in eighths`() {
        val q = NoteLength.QUARTER
        val e = NoteLength.EIGHTH
        val notes = listOf(
            note(1, q), note(3, q), note(5, q),
            note(8, e), note(8, e), note(8, e), note(8, e),
            note(5, q), note(3, q), note(1, NoteLength.HALF),
        )
        assertEquals(18, pattern(notes).lengthInEighths)
    }

    @Test
    fun `a pattern sounds in the key it is given`() {
        val arpeggio = pattern(listOf(1, 3, 5, 8).map { note(it) })
        assertEquals(
            listOf("E♭3", "G3", "B♭3", "E♭4"),
            arpeggio.pitchesIn(Pitch.parse("E♭3")).map { it.name },
        )
    }

    @Test
    fun `a pattern with no notes is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { pattern(emptyList()) }
    }
}
