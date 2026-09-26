package org.pashri.soundcheck.ui.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.SungSpan
import org.pashri.soundcheck.warmup.VoiceType

class KeyboardLayoutTest {
    private val tenor = VoiceType.TENOR.range
    private val octave = SungSpan(lowest = 0, highest = 12)

    @Test
    fun `the Tenor Range in E flat reads like the design's keyboard`() {
        val view = keyboardView(range = tenor, key = Pitch.parse("E♭3"), span = octave)
        assertEquals(
            listOf("C3", "D3", "E3", "F3", "G3", "A3", "B3", "C4", "D4", "E4", "F4", "G4", "A4"),
            view.whites.map { it.pitch.name },
        )
        assertEquals(listOf(0, 1, 3, 4, 5, 7, 8, 10, 11), view.blacks.map { it.afterWhite })
        assertEquals(
            (2..8).toList(),
            view.whites.indices.filter { view.whites[it].mark == KeyMark.SUNG },
        )
        assertTrue(view.whites.none { it.mark == KeyMark.ROOT || it.mark == KeyMark.TOP })
        assertEquals(KeyMark.ROOT, view.blacks[1].mark)
        assertEquals(KeyMark.TOP, view.blacks[6].mark)
    }

    @Test
    fun `a Range that ends on black keys widens to the white keys beside them`() {
        val range = Range(lowest = Pitch.parse("E♭3"), highest = Pitch.parse("B♭4"))
        val view = keyboardView(range = range, key = Pitch.parse("E♭3"), span = octave)
        assertEquals("D3", view.whites.first().pitch.name)
        assertEquals("B4", view.whites.last().pitch.name)
        assertEquals("E♭3", view.blacks.first().pitch.name)
        assertEquals(0, view.blacks.first().afterWhite)
    }

    @Test
    fun `a key on a white key is marked on the white key`() {
        val fifth = SungSpan(lowest = 0, highest = 7)
        val view = keyboardView(range = tenor, key = Pitch.parse("C4"), span = fifth)
        val marks = view.whites.associate { it.pitch.name to it.mark }
        assertEquals(KeyMark.ROOT, marks["C4"])
        assertEquals(KeyMark.TOP, marks["G4"])
        assertEquals(
            listOf(KeyMark.SUNG, KeyMark.SUNG, KeyMark.SUNG),
            listOf("D4", "E4", "F4").map { marks[it] },
        )
        assertEquals(KeyMark.SUNG, view.blacks.single { it.pitch.name == "E♭4" }.mark)
        assertEquals(KeyMark.PLAIN, marks["A4"])
    }

    @Test
    fun `a lone note marks only its key`() {
        val view = keyboardView(
            range = tenor,
            key = Pitch.parse("G3"),
            span = SungSpan(lowest = 0, highest = 0),
        )
        val marks = view.whites.map { it.mark } + view.blacks.map { it.mark }
        assertEquals(1, marks.count { it == KeyMark.ROOT })
        assertTrue(marks.none { it == KeyMark.TOP || it == KeyMark.SUNG })
    }

    @Test
    fun `TalkBack hears the ends, the key and the top note`() {
        val view = keyboardView(range = tenor, key = Pitch.parse("E♭3"), span = octave)
        assertEquals("Keyboard C3 to A4, key E♭3, top note E♭4", view.description)
    }
}
