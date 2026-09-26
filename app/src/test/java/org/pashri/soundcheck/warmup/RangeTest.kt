package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class RangeTest {
    private val tenor = VoiceType.TENOR.range

    private fun range(lowest: String, highest: String): Range =
        Range(lowest = Pitch.parse(lowest), highest = Pitch.parse(highest))

    @Test
    fun `the voice type presets are the spec's ranges`() {
        assertEquals(
            listOf("Soprano C4 A5", "Alto F3 D5", "Tenor C3 A4", "Bass E2 E4"),
            VoiceType.entries.map { "${it.label} ${it.range.lowest} ${it.range.highest}" },
        )
    }

    @Test
    fun `a positive top offset widens the top of the Range`() {
        assertEquals(range(lowest = "C3", highest = "B4"), tenor.offsetBy(RangeOffset(top = 2)))
    }

    @Test
    fun `a positive bottom offset widens the bottom of the Range`() {
        assertEquals(
            range(lowest = "B♭2", highest = "A4"),
            tenor.offsetBy(RangeOffset(bottom = 2)),
        )
    }

    @Test
    fun `a negative offset narrows that end`() {
        val narrowed = tenor.offsetBy(RangeOffset(top = -3))
        assertEquals(range(lowest = "C3", highest = "F♯4"), narrowed)
        assertEquals(18, narrowed?.halfSteps)
    }

    @Test
    fun `an offset that closes the Range leaves no Range`() {
        assertNull(tenor.offsetBy(RangeOffset(bottom = -11, top = -11)))
        assertEquals(
            range(lowest = "B♭3", highest = "B♭3"),
            tenor.offsetBy(RangeOffset(bottom = -10, top = -11)),
        )
    }

    @Test
    fun `an offset past the piano stops at its last key`() {
        assertEquals(range(lowest = "C3", highest = "C8"), tenor.offsetBy(RangeOffset(top = 100)))
        assertEquals(
            range(lowest = "A0", highest = "E4"),
            VoiceType.BASS.range.offsetBy(RangeOffset(bottom = 30)),
        )
        assertEquals(range(lowest = "A0", highest = "C8"), Range.PIANO)
    }

    @Test
    fun `a Range whose top is below its bottom is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { range(lowest = "D3", highest = "C3") }
    }

    @Test
    fun `the edges are inside the Range and their neighbours are not`() {
        assertTrue(Pitch.parse("C3") in tenor)
        assertTrue(Pitch.parse("A4") in tenor)
        assertFalse(Pitch.parse("B2") in tenor)
        assertFalse(Pitch.parse("B♭4") in tenor)
    }
}
