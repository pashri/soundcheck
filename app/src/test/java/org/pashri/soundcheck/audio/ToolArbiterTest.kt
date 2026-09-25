package org.pashri.soundcheck.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ToolArbiterTest {
    private val arbiter = ToolArbiter()

    @Test
    fun `claiming the slot evicts the tool that held it, once`() {
        var evictions = 0
        arbiter.claim(Tool.WARM_UP, onEvicted = { evictions++ })
        arbiter.claim(Tool.TUNER, onEvicted = {})
        arbiter.claim(Tool.METRONOME, onEvicted = {})
        assertEquals(1, evictions)
        assertEquals(Tool.METRONOME, arbiter.current)
    }

    @Test
    fun `a tool claiming again does not evict itself`() {
        var evicted = false
        arbiter.claim(Tool.WARM_UP, onEvicted = { evicted = true })
        arbiter.claim(Tool.WARM_UP, onEvicted = {})
        assertFalse(evicted)
    }

    @Test
    fun `only the tool holding the slot can free it`() {
        arbiter.claim(Tool.TUNER, onEvicted = {})
        arbiter.release(Tool.WARM_UP)
        assertEquals(Tool.TUNER, arbiter.current)
        arbiter.release(Tool.TUNER)
        assertNull(arbiter.current)
    }

    @Test
    fun `an evicted tool that frees the slot as it stops leaves the new holder in place`() {
        arbiter.claim(Tool.METRONOME, onEvicted = { arbiter.release(Tool.METRONOME) })
        arbiter.claim(Tool.WARM_UP, onEvicted = {})
        assertEquals(Tool.WARM_UP, arbiter.current)
    }
}
