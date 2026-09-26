package org.pashri.soundcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabTest {
    @Test
    fun `a known route resolves to its Tab`() {
        assertEquals(Tab.WarmUp, tabForRoute("warmup"))
        assertEquals(Tab.Metronome, tabForRoute("metronome"))
        assertEquals(Tab.Tuner, tabForRoute("tuner"))
    }

    @Test
    fun `an unknown or missing route resolves to nothing`() {
        assertNull(tabForRoute("nonsense"))
        assertNull(tabForRoute(null))
    }

    @Test
    fun `a screen inside the Warm-up belongs to the Warm-up tab`() {
        val editor = sequenceOf("warmup/programme/{programme}", "warmup", null)
        assertEquals(Tab.WarmUp, tabFor(editor))
        assertEquals(Tab.Metronome, tabFor(sequenceOf("metronome", null)))
        assertNull(tabFor(emptySequence()))
    }
}
