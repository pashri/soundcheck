package org.pashri.soundcheck.ui.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef

class WarmupRoutesTest {
    private val ref = StepRef(programmeId = ProgrammeId("p1"), key = StepKey("s1"))

    @Test
    fun `each route builder fills in its pattern`() {
        assertEquals("warmup/programme/p1", WarmupRoutes.programme(ProgrammeId("p1")))
        assertEquals("warmup/programme/p1/step/s1", WarmupRoutes.step(ref))
        assertEquals("warmup/pattern/triad", WarmupRoutes.pattern(PatternId("triad")))
    }

    @Test
    fun `the lists carry the Step they choose for, or nothing`() {
        assertEquals("warmup/patterns", WarmupRoutes.patterns(pickFor = null))
        assertEquals("warmup/patterns?programme=p1&step=s1", WarmupRoutes.patterns(pickFor = ref))
        assertEquals("warmup/sounds", WarmupRoutes.sounds(pickFor = null))
        assertEquals("warmup/sounds?programme=p1&step=s1", WarmupRoutes.sounds(pickFor = ref))
    }

    @Test
    fun `a Step to choose for is read back only when both halves are there`() {
        assertEquals(ref, WarmupRoutes.pickFor(programme = "p1", step = "s1"))
        assertNull(WarmupRoutes.pickFor(programme = "p1", step = null))
        assertNull(WarmupRoutes.pickFor(programme = null, step = null))
    }
}
