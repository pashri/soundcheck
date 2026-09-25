package org.pashri.soundcheck.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTest {
    private fun reading(hz: Double): NoteReading = NoteReading.of(midiOf(hz))

    @Test
    fun `440 Hz is A4 exactly in tune`() {
        val a4 = reading(440.0)
        assertEquals("A", a4.name)
        assertEquals(4, a4.octave)
        assertEquals(0.0, a4.cents, 1e-9)
    }

    @Test
    fun `the twelve notes from middle C use the conventional spellings`() {
        val names = (60..71).map { NoteReading(midi = it, cents = 0.0).name }
        val expected =
            listOf("C", "D♭", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")
        assertEquals(expected, names)
    }

    @Test
    fun `no note is ever spelled C sharp, D sharp, G flat, G sharp or A sharp`() {
        val names = (0..127).map { NoteReading(midi = it, cents = 0.0).name }.toSet()
        val forbidden =
            setOf("C♯", "D♯", "G♯", "A♯", "G♭", "C♭", "F♭", "E♯", "B♯")
        val clashes = names.intersect(forbidden)
        assertTrue(clashes.toString(), clashes.isEmpty())
    }

    @Test
    fun `the open guitar strings are E2 A2 D3 G3 B3 E4`() {
        val strings = listOf(82.407, 110.0, 146.832, 195.998, 246.942, 329.628)
        val named = strings.map { reading(it).let { r -> "${r.name}${r.octave}" } }
        assertEquals(listOf("E2", "A2", "D3", "G3", "B3", "E4"), named)
        strings.forEach { assertEquals("$it Hz", 0.0, reading(it).cents, 0.1) }
    }

    @Test
    fun `middle C is C4 and the octave number changes between B and C`() {
        assertEquals(4, reading(261.626).octave)
        assertEquals("C", reading(261.626).name)
        assertEquals(3, NoteReading(midi = 59, cents = 0.0).octave)
        assertEquals("B", NoteReading(midi = 59, cents = 0.0).name)
        assertEquals(6, reading(1046.502).octave)
        assertEquals(-1, NoteReading(midi = 11, cents = 0.0).octave)
        assertEquals(0, NoteReading(midi = 12, cents = 0.0).octave)
    }

    @Test
    fun `cents measure the distance from the nearest note`() {
        assertEquals(19.56, reading(445.0).cents, 0.01)
        assertEquals(-19.78, reading(435.0).cents, 0.01)
        assertEquals("A", reading(435.0).name)
    }

    @Test
    fun `a pitch exactly halfway belongs to the upper note 50 cents flat`() {
        val halfway = NoteReading.of(69.5)
        assertEquals("B♭", halfway.name)
        assertEquals(-50.0, halfway.cents, 1e-9)
        val justBelow = NoteReading.of(69.4999)
        assertEquals("A", justBelow.name)
        assertEquals(50, justBelow.roundedCents)
    }

    @Test
    fun `cents always stay between minus 50 and plus 50`() {
        (0..24_000).forEach { step ->
            val cents = NoteReading.of(40.0 + step / 1000.0).cents
            assertTrue("$step: $cents", cents >= -50.0 && cents < 50.0)
        }
    }

    @Test
    fun `a reading gives back the frequency it came from`() {
        assertEquals(109.7, reading(109.7).hz, 1e-6)
        assertEquals(-4.73, reading(109.7).cents, 0.01)
    }
}
