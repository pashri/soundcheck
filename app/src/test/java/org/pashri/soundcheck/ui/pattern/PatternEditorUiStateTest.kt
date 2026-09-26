package org.pashri.soundcheck.ui.pattern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.NoteLength
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.savePattern

class PatternEditorUiStateTest {
    private val library = StarterLibrary.LIBRARY

    private fun state(id: PatternId, selected: Int, from: Library = library) =
        checkNotNull(patternEditorUiState(library = from, id = id, selected = selected))

    /** The starter library with the Triad's notes replaced by [notation]. */
    private fun triadAs(notation: String): Library = library.savePattern(
        StarterPatterns.TRIAD.copy(notes = PatternNotation.parse(notation)),
    )

    @Test
    fun `the arpeggio 8-hold shows its notes, span, fit and selected note`() {
        val state = state(id = StarterPatterns.ARPEGGIO_8_HOLD.id, selected = 4)
        assertEquals("Arpeggio 8-hold", state.name)
        assertEquals("10 NOTES · SPANS AN OCTAVE", state.summary)
        assertEquals("Fits every Voice Type", state.fit)
        assertEquals(
            listOf("1", "3", "5", "8", "8", "8", "8", "5", "3", "1"),
            state.notes.map { it.text },
        )
        assertTrue(state.notes[4].selected)
        assertEquals("Note 5 of 10: 8, eighth", state.notes[4].description)
        assertEquals("NOTE 5 OF 10", state.noteLabel)
        assertEquals("8", state.degree)
        assertEquals("C in C", state.degreeName)
        assertEquals(Accidental.NATURAL, state.accidental)
        assertEquals(NoteLength.EIGHTH, state.length)
        assertEquals(KeyChord.MAJOR, state.keyChord)
        assertTrue(state.canDeleteNote && state.canDelete)
        assertEquals("1 Step in Starter warm-up uses it; that Step goes too.", state.deleteNote)
    }

    @Test
    fun `a flattened note is named and spoken with its flat`() {
        val state = state(id = StarterPatterns.MINOR_FIVE_NOTE_SCALE.id, selected = 2)
        assertEquals("♭3", state.degree)
        assertEquals("E♭ in C", state.degreeName)
        assertEquals("Note 3 of 9: flat 3, eighth", state.notes[2].description)
    }

    @Test
    fun `a note below the root is named in C`() {
        val state = state(id = StarterPatterns.TRIAD.id, selected = 0, from = triadAs("♭1 1h"))
        assertEquals("♭1", state.degree)
        assertEquals("B in C", state.degreeName)
    }

    @Test
    fun `a wide Pattern says which Voice Types it fits`() {
        val double = state(id = StarterPatterns.DOUBLE_ARPEGGIO.id, selected = 0)
        assertEquals("13 NOTES · SPANS 19 HALF-STEPS", double.summary)
        assertEquals("Fits every Voice Type", double.fit)
        val bassOnly = state(id = StarterPatterns.TRIAD.id, selected = 0, from = triadAs("1 ♯13"))
        assertEquals("Fits Bass only", bassOnly.fit)
        val none = state(id = StarterPatterns.TRIAD.id, selected = 0, from = triadAs("1 16"))
        assertEquals("Too wide for every Voice Type", none.fit)
        assertEquals("Fits Alto and Bass", fitText(listOf(VoiceType.ALTO, VoiceType.BASS)))
        assertEquals(
            "Fits Soprano, Alto and Bass",
            fitText(listOf(VoiceType.SOPRANO, VoiceType.ALTO, VoiceType.BASS)),
        )
    }

    @Test
    fun `the span and note count are worded for every size`() {
        assertEquals("ONE PITCH", spanText(0))
        assertEquals("SPANS 1 HALF-STEP", spanText(1))
        assertEquals("SPANS 7 HALF-STEPS", spanText(7))
        assertEquals("SPANS AN OCTAVE", spanText(12))
        assertEquals("SPANS TWO OCTAVES", spanText(24))
        assertEquals("1 NOTE", notesText(1))
        assertEquals("5 NOTES", notesText(5))
    }

    @Test
    fun `the degree buttons stop at 1 and 99 and the last note can't be deleted`() {
        val one = state(id = StarterPatterns.TRIAD.id, selected = 0, from = triadAs("1"))
        assertFalse(one.canLowerDegree)
        assertTrue(one.canRaiseDegree)
        assertFalse(one.canDeleteNote)
        val top = state(id = StarterPatterns.TRIAD.id, selected = 0, from = triadAs("99"))
        assertFalse(top.canRaiseDegree)
    }

    @Test
    fun `the selected note is kept inside the Pattern`() {
        val state = state(id = StarterPatterns.TRIAD.id, selected = 50)
        assertEquals(4, state.selected)
        assertEquals("NOTE 5 OF 5", state.noteLabel)
    }

    @Test
    fun `the last Pattern can't be deleted and a missing one shows nothing`() {
        val one = Library(
            patterns = listOf(StarterPatterns.TRIAD),
            sounds = StarterSounds.ALL,
            programmes = emptyList(),
        )
        assertFalse(state(id = StarterPatterns.TRIAD.id, selected = 0, from = one).canDelete)
        assertNull(patternEditorUiState(library = library, id = PatternId("gone"), selected = 0))
    }

    @Test
    fun `the choice labels are the design's`() {
        assertEquals("root", chordChipLabel(KeyChord.ROOT_ONLY))
        assertEquals("maj7", chordChipLabel(KeyChord.MAJOR_SEVENTH))
        assertEquals("minor seventh", chordSpoken(KeyChord.MINOR_SEVENTH))
        assertEquals("♮", accidentalLabel(Accidental.NATURAL))
        assertEquals("sharp", accidentalSpoken(Accidental.SHARP))
        assertEquals("Quarter", lengthLabel(NoteLength.QUARTER))
    }
}
