package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Test

class PatternEditsTest {
    private val triad = StarterPatterns.TRIAD

    private fun degrees(pattern: Pattern): String = PatternNotation.degrees(pattern.notes)

    @Test
    fun `a new Pattern goes 1 2 3 2 1 on a major chord`() {
        val fresh = newPattern(id = PatternId("p"), name = "New pattern")
        assertEquals(PatternId("p"), fresh.id)
        assertEquals("New pattern", fresh.name)
        assertEquals("1q 2q 3q 2q 1h", PatternNotation.format(fresh.notes))
        assertEquals(KeyChord.MAJOR, fresh.keyChord)
    }

    @Test
    fun `changing a note changes only that note`() {
        val sharp = triad.withNote(2) { it.copy(accidental = Accidental.SHARP) }
        assertEquals("1 3 ♯5 3 1", degrees(sharp))
        assertEquals(triad.notes[2].length, sharp.notes[2].length)
    }

    @Test
    fun `a note outside the Pattern changes nothing`() {
        assertEquals(triad, triad.withNote(9) { it.copy(degree = 2) })
        assertEquals(triad, triad.withNoteCopiedAfter(-1))
        assertEquals(triad, triad.withoutNote(5))
    }

    @Test
    fun `copying a note puts the copy straight after it`() {
        assertEquals("1 3 5 5 3 1", degrees(triad.withNoteCopiedAfter(2)))
        assertEquals("1 3 5 3 1 1", degrees(triad.withNoteCopiedAfter(4)))
    }

    @Test
    fun `deleting a note closes the gap`() {
        assertEquals("3 5 3 1", degrees(triad.withoutNote(0)))
    }

    @Test
    fun `the last note can't be deleted`() {
        val single = triad.copy(notes = triad.notes.take(1))
        assertEquals(single, single.withoutNote(0))
    }

    @Test
    fun `a degree stops at 1 and at 99`() {
        assertEquals(1, PatternNote(degree = 1, length = NoteLength.QUARTER).lowered().degree)
        assertEquals(99, PatternNote(degree = 99, length = NoteLength.QUARTER).raised().degree)
        assertEquals(9, PatternNote(degree = 8, length = NoteLength.QUARTER).raised().degree)
        assertEquals(7, PatternNote(degree = 8, length = NoteLength.QUARTER).lowered().degree)
    }

    @Test
    fun `a Pattern fits the Voice Types whose Range is wide enough`() {
        assertEquals(VoiceType.entries, voiceTypesFitting(triad.span))
        assertEquals(VoiceType.entries, voiceTypesFitting(SungSpan(lowest = 0, highest = 21)))
        assertEquals(listOf(VoiceType.BASS), voiceTypesFitting(SungSpan(lowest = 0, highest = 22)))
        assertEquals(emptyList<VoiceType>(), voiceTypesFitting(SungSpan(lowest = 0, highest = 25)))
    }
}
