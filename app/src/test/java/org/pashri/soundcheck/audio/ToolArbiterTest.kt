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
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = { evictions++ })
        arbiter.claim(tool = Tool.TUNER, onEvicted = {})
        arbiter.claim(tool = Tool.METRONOME, onEvicted = {})
        assertEquals(1, evictions)
        assertEquals(Tool.METRONOME, arbiter.current)
    }

    @Test
    fun `a tool claiming again does not evict itself`() {
        var evicted = false
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = { evicted = true })
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = {})
        assertFalse(evicted)
    }

    @Test
    fun `only the tool holding the slot can free it`() {
        arbiter.claim(tool = Tool.TUNER, onEvicted = {})
        arbiter.release(Tool.WARM_UP)
        assertEquals(Tool.TUNER, arbiter.current)
        arbiter.release(Tool.TUNER)
        assertNull(arbiter.current)
    }

    @Test
    fun `an evicted tool that frees the slot as it stops leaves the new holder in place`() {
        arbiter.claim(tool = Tool.METRONOME, onEvicted = { arbiter.release(Tool.METRONOME) })
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = {})
        assertEquals(Tool.WARM_UP, arbiter.current)
    }

    @Test
    fun `the last tool to start is remembered after it stops`() {
        assertNull(arbiter.last)
        arbiter.claim(tool = Tool.WARM_UP, onEvicted = {})
        arbiter.claim(tool = Tool.METRONOME, onEvicted = {})
        arbiter.release(Tool.METRONOME)
        assertNull(arbiter.current)
        assertEquals(Tool.METRONOME, arbiter.last)
    }
}
