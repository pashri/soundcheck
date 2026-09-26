package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class DemosTest {
    private val tenor = VoiceType.TENOR.range
    private val steps = StarterProgrammes.WARM_UP.steps

    private fun pattern(notation: String): Pattern =
        StarterPatterns.TRIAD.copy(notes = PatternNotation.parse(notation))

    @Test
    fun `a Step's Demo is its Pattern once in its starting key, from frame 0`() {
        val demo = demoNotes(step = steps[1], range = tenor)
        assertEquals(listOf(48, 52, 55, 52, 48), demo.map { it.pitch.midi })
        assertTrue(demo.all { it.part == PianoPart.DEMO })
        assertEquals(listOf(0L, 32_000L, 64_000L, 96_000L, 128_000L), demo.map { it.startFrame })
        assertEquals(32_000L, demo.first().lengthFrames)
        assertEquals(64_000L, demo.last().lengthFrames)
    }

    @Test
    fun `a Step starting high demos in its top key`() {
        assertEquals(Pitch.parse("D4"), demoNotes(step = steps[3], range = tenor).first().pitch)
    }

    @Test
    fun `a Step that doesn't fit has no Demo`() {
        val narrow = Range(lowest = Pitch.parse("C4"), highest = Pitch.parse("D4"))
        assertTrue(demoNotes(step = steps[1], range = narrow).isEmpty())
    }

    @Test
    fun `a Pattern auditions in the Range's lowest key when it fits, after its Key Chord`() {
        val notes = patternDemoNotes(pattern = StarterPatterns.TRIAD, range = tenor)
        val chord = notes.filter { it.part == PianoPart.KEY_CHORD }
        val demo = notes.filter { it.part == PianoPart.DEMO }
        assertEquals(listOf(48, 52, 55), chord.map { it.pitch.midi })
        assertTrue(chord.all { it.startFrame == 0L })
        val chordEnd = chord.first().endFrame
        assertEquals(listOf(48, 52, 55, 52, 48), demo.map { it.pitch.midi })
        assertEquals(chordEnd, demo.first().startFrame)
        assertEquals(32_000L, demo.first().lengthFrames)
        assertEquals(
            listOf(
                PianoPart.KEY_CHORD, PianoPart.KEY_CHORD, PianoPart.KEY_CHORD,
                PianoPart.DEMO, PianoPart.DEMO, PianoPart.DEMO, PianoPart.DEMO, PianoPart.DEMO,
            ),
            notes.map { it.part },
        )
    }

    @Test
    fun `the Key Chord auditions the right pitches for a non-major quality`() {
        val minorTriad = StarterPatterns.MINOR_FIVE_NOTE_SCALE.copy(keyChord = KeyChord.MINOR)
        val notes = patternDemoNotes(pattern = minorTriad, range = tenor)
        val chord = notes.filter { it.part == PianoPart.KEY_CHORD }
        assertEquals(KeyChord.MINOR.pitchesOn(chord.first().pitch), chord.map { it.pitch })
    }

    @Test
    fun `the Key Chord comes before every event of the Pattern`() {
        val notes = patternDemoNotes(pattern = StarterPatterns.DOUBLE_ARPEGGIO, range = tenor)
        val chordEnd = notes.filter { it.part == PianoPart.KEY_CHORD }.maxOf { it.endFrame }
        val demoStarts = notes.filter { it.part == PianoPart.DEMO }.map { it.startFrame }
        assertTrue(demoStarts.all { it >= chordEnd })
    }

    @Test
    fun `a Pattern too wide for the Range auditions on middle C`() {
        val notes = patternDemoNotes(pattern = pattern("1 ♯13"), range = tenor)
        val demo = notes.filter { it.part == PianoPart.DEMO }
        assertEquals(listOf(60, 82), demo.map { it.pitch.midi })
    }

    @Test
    fun `a Pattern too high for middle C moves down to stay on the piano`() {
        assertEquals(
            Pitch.parse("C3"),
            auditionKey(span = SungSpan(lowest = 0, highest = 60), range = tenor),
        )
    }

    @Test
    fun `a Pattern wider than the keyboard has nothing to audition`() {
        assertNull(auditionKey(span = SungSpan(lowest = 0, highest = 90), range = tenor))
        assertTrue(patternDemoNotes(pattern = pattern("99"), range = tenor).isEmpty())
    }
}
