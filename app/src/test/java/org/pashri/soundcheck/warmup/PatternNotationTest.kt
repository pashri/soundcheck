package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PatternNotationTest {
    @Test
    fun `plain degrees take the default length`() {
        assertEquals(
            listOf(1, 3, 5, 8).map { PatternNote(degree = it, length = NoteLength.QUARTER) },
            PatternNotation.parse("1 3 5 8"),
        )
    }

    @Test
    fun `a length letter overrides the default`() {
        val notes = PatternNotation.parse("1 8e 1h 5w", defaultLength = NoteLength.QUARTER)
        assertEquals(
            listOf(NoteLength.QUARTER, NoteLength.EIGHTH, NoteLength.HALF, NoteLength.WHOLE),
            notes.map { it.length },
        )
    }

    @Test
    fun `flats and sharps attach to the degree`() {
        val notes = PatternNotation.parse("♭3 ♯4 5")
        assertEquals(listOf(3, 4, 5), notes.map { it.degree })
        assertEquals(
            listOf(Accidental.FLAT, Accidental.SHARP, Accidental.NATURAL),
            notes.map { it.accidental },
        )
    }

    @Test
    fun `degrees past the octave read as whole numbers`() {
        assertEquals(
            listOf(10, 12, 11),
            PatternNotation.parse("10 12 11").map { it.degree },
        )
    }

    @Test
    fun `extra spaces between notes are ignored`() {
        assertEquals(3, PatternNotation.parse("  1   3\t5 ").size)
    }

    @Test
    fun `formatting then reading gives back the same notes`() {
        val notes = PatternNotation.parse("1 3 ♭5 8e 8e ♯8e 8e 5 3 1h")
        val written = PatternNotation.format(notes)
        assertEquals("1q 3q ♭5q 8e 8e ♯8e 8e 5q 3q 1h", written)
        assertEquals(notes, PatternNotation.parse(written, defaultLength = NoteLength.WHOLE))
    }

    @Test
    fun `the degrees line shows accidentals but not lengths`() {
        assertEquals(
            "1 2 ♭3 4 5",
            PatternNotation.degrees(PatternNotation.parse("1 2 ♭3 4 5h")),
        )
    }

    @Test
    fun `a malformed note is rejected`() {
        listOf("", "   ", "0", "x", "3z", "b3", "3♭", "1,3", "100").forEach {
            assertThrows(IllegalArgumentException::class.java) { PatternNotation.parse(it) }
        }
    }
}
