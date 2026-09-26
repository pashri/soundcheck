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
    fun `a Pattern auditions in the Range's lowest key when it fits`() {
        val notes = patternDemoNotes(pattern = StarterPatterns.TRIAD, range = tenor)
        assertEquals(listOf(48, 52, 55, 52, 48), notes.map { it.pitch.midi })
        assertEquals(0L, notes.first().startFrame)
        assertEquals(32_000L, notes.first().lengthFrames)
    }

    @Test
    fun `a Pattern too wide for the Range auditions on middle C`() {
        val notes = patternDemoNotes(pattern = pattern("1 ♯13"), range = tenor)
        assertEquals(listOf(60, 82), notes.map { it.pitch.midi })
    }

    @Test
    fun `a Pattern too high for middle C moves down to stay on the piano`() {
        assertEquals(Pitch.parse("C3"), auditionKey(SungSpan(lowest = 0, highest = 60), tenor))
    }

    @Test
    fun `a Pattern wider than the keyboard has nothing to audition`() {
        assertNull(auditionKey(SungSpan(lowest = 0, highest = 90), tenor))
        assertTrue(patternDemoNotes(pattern = pattern("99"), range = tenor).isEmpty())
    }
}
