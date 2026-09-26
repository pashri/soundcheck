package org.pashri.soundcheck.ui.warmup

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.NoteLength
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.VoiceType

class StaffLayoutTest {
    private val c4 = Pitch.parse("C4")

    private fun pattern(notation: String): Pattern = Pattern(
        id = PatternId("test"),
        name = "Test",
        notes = PatternNotation.parse(text = notation, defaultLength = NoteLength.QUARTER),
        keyChord = KeyChord.MAJOR,
    )

    private fun range(lowest: Int, highest: Int): Range =
        Range(lowest = Pitch(lowest), highest = Pitch(highest))

    @Test
    fun `each Voice Type gets the clef its singers read`() {
        assertEquals(Clef.TREBLE, clefFor(VoiceType.SOPRANO.range))
        assertEquals(Clef.TREBLE, clefFor(VoiceType.ALTO.range))
        assertEquals(Clef.TREBLE_8VB, clefFor(VoiceType.TENOR.range))
        assertEquals(Clef.BASS, clefFor(VoiceType.BASS.range))
    }

    @Test
    fun `the clef changes exactly at middle C and at G3`() {
        assertEquals(Clef.TREBLE, clefFor(range(lowest = 60, highest = 60)))
        assertEquals(Clef.TREBLE, clefFor(range(lowest = 55, highest = 65)))
        assertEquals(Clef.TREBLE_8VB, clefFor(range(lowest = 59, highest = 60)))
        assertEquals(Clef.TREBLE_8VB, clefFor(range(lowest = 55, highest = 55)))
        assertEquals(Clef.BASS, clefFor(range(lowest = 54, highest = 55)))
    }

    @Test
    fun `the Arpeggio 8-hold in E flat 3 sits on a bass staff with its flats and ledgers`() {
        val layout = staffLayout(
            pattern = StarterPatterns.ARPEGGIO_8_HOLD,
            key = Pitch.parse("E♭3"),
            clef = Clef.BASS,
        )
        assertEquals(listOf(5, 7, 9, 12, 12, 12, 12, 9, 7, 5), layout.notes.map { it.step })
        assertEquals(
            listOf(1, 8),
            layout.notes.indices.filter { layout.notes[it].accidental == Accidental.NATURAL },
        )
        assertTrue(
            layout.notes.filter { it.accidental != Accidental.NATURAL }
                .all { it.accidental == Accidental.FLAT },
        )
        assertEquals(listOf(10, 12), layout.notes[3].ledgers)
        assertTrue(layout.notes[2].ledgers.isEmpty())
        assertEquals(
            listOf(StaffBeam(first = 0, last = 3), StaffBeam(first = 4, last = 7)),
            layout.beams,
        )
        assertEquals(
            listOf(19, 19, 19, 19, 19, 19, 19, 19, 14, 12),
            layout.notes.map { it.stemTop },
        )
        assertEquals(listOf(8), layout.notes.indices.filter { layout.notes[it].flag })
        assertEquals(19, layout.top)
        assertEquals(0, layout.bottom)
    }

    @Test
    fun `a tenor's staff is written an octave above where it is sung`() {
        val layout = staffLayout(
            pattern = StarterPatterns.ARPEGGIO_8_HOLD,
            key = Pitch.parse("C3"),
            clef = Clef.TREBLE_8VB,
        )
        assertEquals(listOf(-2, 0, 2, 5, 5, 5, 5, 2, 0, -2), layout.notes.map { it.step })
        assertEquals(listOf(-2), layout.notes[0].ledgers)
        assertTrue(layout.notes.all { it.accidental == Accidental.NATURAL })
        assertEquals(12, layout.top)
        assertEquals(-7, layout.bottom)
    }

    @Test
    fun `halves and wholes are open, and a whole has no stem`() {
        val layout = staffLayout(
            pattern = StarterPatterns.SIREN_1_5_1,
            key = c4,
            clef = Clef.TREBLE,
        )
        assertEquals(listOf(-2, 2, -2), layout.notes.map { it.step })
        assertTrue(layout.notes.all { it.head == NoteHead.HOLLOW && !it.flag })
        assertEquals(listOf(5, 9, null), layout.notes.map { it.stemTop })
        assertEquals(listOf(listOf(-2), emptyList(), listOf(-2)), layout.notes.map { it.ledgers })
        assertEquals(12, layout.top)
        assertEquals(-4, layout.bottom)
        assertTrue(layout.beams.isEmpty())
    }

    @Test
    fun `accidentals follow the real pitch, so a major third in D is F sharp`() {
        val layout = staffLayout(
            pattern = StarterPatterns.TRIAD,
            key = Pitch.parse("D4"),
            clef = Clef.TREBLE,
        )
        assertEquals(listOf(-1, 1, 3, 1, -1), layout.notes.map { it.step })
        assertEquals(
            listOf(
                Accidental.NATURAL,
                Accidental.SHARP,
                Accidental.NATURAL,
                Accidental.SHARP,
                Accidental.NATURAL,
            ),
            layout.notes.map { it.accidental },
        )
        assertTrue(layout.notes.all { it.ledgers.isEmpty() })
    }

    @Test
    fun `a flattened degree is a flat on its own letter`() {
        val layout = staffLayout(
            pattern = StarterPatterns.MINOR_FIVE_NOTE_SCALE,
            key = c4,
            clef = Clef.TREBLE,
        )
        assertEquals(listOf(-2, -1, 0, 1, 2, 1, 0, -1, -2), layout.notes.map { it.step })
        assertEquals(
            listOf(2, 6),
            layout.notes.indices.filter { layout.notes[it].accidental == Accidental.FLAT },
        )
    }

    @Test
    fun `notes far from the staff get a ledger line for every line they pass`() {
        val high = staffLayout(pattern = pattern(notation = "1 15"), key = c4, clef = Clef.TREBLE)
        assertEquals(listOf(listOf(-2), listOf(10, 12)), high.notes.map { it.ledgers })
        assertEquals(19, high.top)
        val low = staffLayout(
            pattern = pattern(notation = "1"),
            key = Pitch.parse("E2"),
            clef = Clef.BASS,
        )
        assertEquals(-2, low.notes.single().step)
        assertEquals(listOf(-2), low.notes.single().ledgers)
    }

    @Test
    fun `eighths are beamed within each half bar, and a lone eighth is flagged`() {
        val paired = staffLayout(
            pattern = pattern(notation = "1e 2e 3 4e 5e 6e"),
            key = c4,
            clef = Clef.TREBLE,
        )
        assertEquals(
            listOf(StaffBeam(first = 0, last = 1), StaffBeam(first = 3, last = 5)),
            paired.beams,
        )
        assertTrue(paired.notes.none { it.flag })
        val split = staffLayout(
            pattern = pattern(notation = "1 2e 3e 4e"),
            key = c4,
            clef = Clef.TREBLE,
        )
        assertEquals(listOf(StaffBeam(first = 1, last = 2)), split.beams)
        assertEquals(listOf(3), split.notes.indices.filter { split.notes[it].flag })
        val lone = staffLayout(
            pattern = pattern(notation = "1 2e 3"),
            key = c4,
            clef = Clef.TREBLE,
        )
        assertTrue(lone.beams.isEmpty())
        assertTrue(lone.notes[1].flag)
    }

    @Test
    fun `TalkBack names the clef and reads the degrees, and a note out of range lights nothing`() {
        val view = staffView(
            pattern = StarterPatterns.MINOR_FIVE_NOTE_SCALE,
            key = c4,
            clef = Clef.TREBLE_8VB,
            now = 3,
        )
        assertEquals(
            "Pattern on a staff, treble clef, an octave lower: 1 2 ♭3 4 5 4 ♭3 2 1",
            view.description,
        )
        assertEquals(3, view.now)
        val late = staffView(
            pattern = StarterPatterns.MINOR_FIVE_NOTE_SCALE,
            key = c4,
            clef = Clef.BASS,
            now = 99,
        )
        assertNull(late.now)
        assertTrue(late.description.startsWith("Pattern on a staff, bass clef: "))
    }

    @Test
    fun `a long Pattern shrinks the staff so every note keeps its own room`() {
        assertEquals(STAFF_GAP, staffGap(width = 342.dp, notes = 10))
        assertEquals(3.1814f, staffGap(width = 342.dp, notes = 40).value, 0.001f)
    }
}
