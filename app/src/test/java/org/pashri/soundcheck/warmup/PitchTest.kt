package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTest {
    @Test
    fun `middle C is MIDI 60 and named C4`() {
        assertEquals("C4", Pitch(60).name)
        assertEquals(4, Pitch(60).octave)
    }

    @Test
    fun `the twelve pitch classes use the conventional spellings`() {
        assertEquals(
            listOf("C", "D♭", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B"),
            (60..71).map { Pitch(it).pitchClassName },
        )
    }

    @Test
    fun `the octave number goes up at C`() {
        assertEquals("B3", Pitch(59).name)
        assertEquals("A4", Pitch(69).name)
        assertEquals("C-1", Pitch(0).name)
        assertEquals("G9", Pitch(127).name)
    }

    @Test
    fun `every name parses back to the same pitch`() {
        (0..127).forEach { assertEquals(Pitch(it), Pitch.parse(Pitch(it).name)) }
    }

    @Test
    fun `the voice type edges parse to their MIDI numbers`() {
        assertEquals(
            listOf(40, 48, 53, 60, 64, 69, 74, 81),
            listOf("E2", "C3", "F3", "C4", "E4", "A4", "D5", "A5").map { Pitch.parse(it).midi },
        )
    }

    @Test
    fun `an unconventional or malformed name is rejected`() {
        listOf("C♯4", "G♭3", "H4", "C10", "c4", "", "E♭").forEach {
            assertThrows(IllegalArgumentException::class.java) { Pitch.parse(it) }
        }
    }

    @Test
    fun `moving by half-steps adds to the MIDI number`() {
        assertEquals(Pitch(51), Pitch(48) + 3)
        assertEquals(Pitch(45), Pitch(48) + -3)
        assertEquals(3, Pitch(51) - Pitch(48))
    }

    @Test
    fun `a pitch outside MIDI is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Pitch(128) }
        assertThrows(IllegalArgumentException::class.java) { Pitch(-1) }
        assertThrows(IllegalArgumentException::class.java) { Pitch(127) + 1 }
    }

    @Test
    fun `pitches order from low to high`() {
        assertTrue(Pitch(40) < Pitch(41))
        assertEquals(Pitch(40), listOf(Pitch(64), Pitch(40), Pitch(52)).min())
    }
}
