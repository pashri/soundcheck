package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PatternNoteTest {
    @Test
    fun `degrees one to eight follow the major scale`() {
        assertEquals(listOf(0, 2, 4, 5, 7, 9, 11, 12), (1..8).map { majorScaleHalfSteps(it) })
    }

    @Test
    fun `degrees continue past the octave`() {
        assertEquals(listOf(14, 16, 17, 19, 24), listOf(9, 10, 11, 12, 15).map {
            majorScaleHalfSteps(it)
        })
    }

    @Test
    fun `a flat lowers and a sharp raises the note by a half-step`() {
        val flatThree = PatternNote(
            degree = 3,
            length = NoteLength.EIGHTH,
            accidental = Accidental.FLAT,
        )
        val sharpFour = PatternNote(
            degree = 4,
            length = NoteLength.EIGHTH,
            accidental = Accidental.SHARP,
        )
        assertEquals(3, flatThree.halfSteps)
        assertEquals(6, sharpFour.halfSteps)
        assertEquals(7, PatternNote(degree = 5, length = NoteLength.HALF).halfSteps)
    }

    @Test
    fun `note lengths count in eighths with a quarter as one beat`() {
        assertEquals(listOf(1, 2, 4, 8), NoteLength.entries.map { it.eighths })
    }

    @Test
    fun `degree zero is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            PatternNote(degree = 0, length = NoteLength.QUARTER)
        }
        assertThrows(IllegalArgumentException::class.java) { majorScaleHalfSteps(0) }
    }
}
