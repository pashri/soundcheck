package org.pashri.soundcheck.ui.programme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.RoundTrip
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StepKey

class StepTextTest {
    private val steps = StarterProgrammes.WARM_UP.steps

    @Test
    fun `a Step's summary names its Pattern, tempo, Direction and any Range Offset`() {
        assertEquals("5-note scale · 90 bpm · from low · top +2", stepMeta(steps[0]))
        assertEquals("1-5-1 siren · 80 bpm · from high", stepMeta(steps[3]))
        val both = steps[1].copy(rangeOffset = RangeOffset(bottom = -1, top = -3))
        assertEquals("Triad · 90 bpm · from low · bottom −1 · top −3", stepMeta(both))
        assertNull(offsetText(RangeOffset.NONE))
    }

    @Test
    fun `a signed offset uses a real minus sign`() {
        assertEquals("+2", signed(2))
        assertEquals("−3", signed(-3))
        assertEquals("0", signed(0))
    }

    @Test
    fun `a Step that doesn't fit says how many half-steps it needs and has`() {
        assertEquals(
            "Needs 19 half-steps; this Step's Range has 18. It will be skipped.",
            fitWarning(RoundTrip.DoesNotFit(neededHalfSteps = 19, availableHalfSteps = 18)),
        )
        assertNull(fitWarning(RoundTrip.Fits(keys = listOf(Pitch(60)))))
    }

    @Test
    fun `a Pattern that fits the Range but not the keyboard says so`() {
        assertEquals(
            "Its notes reach past the piano's keys here. It will be skipped.",
            fitWarning(RoundTrip.DoesNotFit(neededHalfSteps = 0, availableHalfSteps = 21)),
        )
    }

    @Test
    fun `a Step with no Range left says it will be skipped without counting half-steps`() {
        assertEquals(
            "No Range left for this Step. It will be skipped.",
            fitWarning(RoundTrip.DoesNotFit(neededHalfSteps = 1, availableHalfSteps = 0)),
        )
        assertEquals(
            "No Range left for this Step. It will be skipped.",
            fitWarning(RoundTrip.DoesNotFit(neededHalfSteps = 0, availableHalfSteps = 0)),
        )
    }

    @Test
    fun `a Step row is read as one stop with its number, Sound, summary and warning`() {
        val row = StepRow(
            key = StepKey(value = "s1"),
            number = 2,
            sound = "mim",
            meta = "Triad · 90 bpm · from low · top −3",
            warning = "No Range left for this Step. It will be skipped.",
        )
        assertEquals(
            "Step 2, mim, Triad · 90 bpm · from low · top minus 3, " +
                "No Range left for this Step. It will be skipped.",
            spokenStep(row = row),
        )
        val plain = row.copy(meta = "Triad", warning = null)
        assertEquals("Step 2, mim, Triad", spokenStep(row = plain))
    }
}
