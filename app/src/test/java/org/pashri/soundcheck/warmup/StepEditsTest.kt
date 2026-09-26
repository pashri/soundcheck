package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Test

class StepEditsTest {
    private val step = StarterProgrammes.SAVED_WARM_UP.steps.first()

    @Test
    fun `the tempo stays between 30 and 300 bpm`() {
        assertEquals(30, step.withBpm(29).bpm)
        assertEquals(300, step.withBpm(301).bpm)
        assertEquals(120, step.withBpm(120).bpm)
    }

    @Test
    fun `each end of a Range Offset stays within 24 half-steps`() {
        assertEquals(
            RangeOffset(bottom = 24, top = -24),
            step.withRangeOffset(bottom = 25, top = -30).rangeOffset,
        )
        assertEquals(
            RangeOffset(bottom = -3, top = 2),
            step.withRangeOffset(bottom = -3, top = 2).rangeOffset,
        )
    }
}
