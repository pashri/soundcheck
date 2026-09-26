package org.pashri.soundcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenTest {
    @Test
    fun `flats, sharps and naturals are read as words`() {
        assertEquals("B flat 2", spokenMusic("B♭2"))
        assertEquals("E flat in C", spokenMusic("E♭ in C"))
        assertEquals("F sharp 4", spokenMusic("F♯4"))
        assertEquals("natural", spokenMusic("♮"))
        assertEquals("E flat major", spokenMusic("E♭ major"))
        assertEquals("F sharp 7", spokenMusic("F♯7"))
    }

    @Test
    fun `minus signs and arrows are read as words`() {
        assertEquals("top minus 3", spokenMusic("top −3"))
        assertEquals("Start low up", spokenMusic("Start low ↑"))
        assertEquals("Start high down", spokenMusic("Start high ↓"))
    }

    @Test
    fun `plain text is left alone`() {
        assertEquals("Tenor · C3 – A4", spokenMusic("Tenor · C3 – A4"))
    }

    @Test
    fun `a merged row is read as its label and then its music text as words`() {
        assertEquals("Lowest, B flat 2", spokenRow(parts = listOf("Lowest", "B♭2")))
        assertEquals(
            "YOUR RANGE, Tenor · C3 – A4, top minus 3",
            spokenRow(parts = listOf("YOUR RANGE", "Tenor · C3 – A4", "top −3")),
        )
    }

    @Test
    fun `a merged row leaves out a missing detail`() {
        assertEquals("Tempo, 90 bpm", spokenRow(parts = listOf("Tempo", "90 bpm", null)))
    }
}
