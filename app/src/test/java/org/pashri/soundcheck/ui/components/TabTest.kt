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
}
