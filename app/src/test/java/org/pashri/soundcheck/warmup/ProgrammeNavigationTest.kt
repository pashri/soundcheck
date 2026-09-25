package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgrammeNavigationTest {
    private val tenor = VoiceType.TENOR.range
    private val arpeggio8Hold = Pattern(
        name = "Arpeggio 8-hold",
        notes = PatternNotation.parse("1 3 5 8e 8e 8e 8e 5 3 1h", NoteLength.QUARTER),
        keyChord = KeyChord.MAJOR,
    )
    private val doubleArpeggio = Pattern(
        name = "Double arpeggio",
        notes = PatternNotation.parse("1 3 5 8 10 12 11 9 7 5 4 2 1h", NoteLength.EIGHTH),
        keyChord = KeyChord.MAJOR,
    )

    /** Spans 12 half-steps; the Tenor Range has 21. */
    private val fits = Step(
        pattern = arpeggio8Hold,
        soundId = SoundId("mim"),
        bpm = 100,
        direction = Direction.START_LOW,
    )

    /** Needs 19 half-steps; with top −3 the Tenor Range has 18. */
    private val tooWide = Step(
        pattern = doubleArpeggio,
        soundId = SoundId("mah"),
        bpm = 120,
        direction = Direction.START_LOW,
        rangeOffset = RangeOffset(top = -3),
    )

    private fun programme(vararg steps: Step): Programme =
        Programme(name = "Test", steps = steps.toList())

    @Test
    fun `a programme starts at its first step that fits`() {
        assertEquals(0, programme(fits, tooWide).firstStep(tenor))
        assertEquals(1, programme(tooWide, fits, fits).firstStep(tenor))
    }

    @Test
    fun `next skips steps that do not fit`() {
        assertEquals(2, programme(fits, tooWide, fits, fits).nextStep(current = 0, range = tenor))
    }

    @Test
    fun `next after the last playable step ends the programme`() {
        assertNull(programme(fits, tooWide, fits, fits).nextStep(current = 3, range = tenor))
        assertNull(programme(fits, tooWide).nextStep(current = 0, range = tenor))
    }

    @Test
    fun `previous skips back over steps that do not fit`() {
        val steps = programme(fits, tooWide, fits, fits)
        assertEquals(0, steps.previousStep(current = 2, range = tenor))
        assertEquals(2, steps.previousStep(current = 3, range = tenor))
    }

    @Test
    fun `previous on the first playable step restarts it`() {
        assertEquals(0, programme(fits, fits).previousStep(current = 0, range = tenor))
        assertEquals(1, programme(tooWide, fits).previousStep(current = 1, range = tenor))
    }

    @Test
    fun `a programme where no step fits has nothing to play`() {
        assertNull(programme(tooWide, tooWide).firstStep(tenor))
        assertNull(programme().firstStep(tenor))
    }
}
