package org.pashri.soundcheck.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixingFocusGateTest {
    private val focus = FakeFocusGate()
    private var mixing = false
    private val gate = MixingFocusGate(focus = focus, mixing = { mixing })

    @Test
    fun `without mixing it asks for focus as before`() {
        assertTrue(gate.acquire(onLost = {}))
        assertTrue(focus.held)
        assertEquals(1, focus.acquireCount)
    }

    @Test
    fun `while mixing it plays without asking for focus`() {
        mixing = true
        assertTrue(gate.acquire(onLost = {}))
        assertFalse(focus.held)
        assertEquals(0, focus.acquireCount)
    }

    @Test
    fun `refused focus is still refused without mixing`() {
        focus.grant = false
        assertFalse(gate.acquire(onLost = {}))
    }

    @Test
    fun `switching mixing on hands back focus held from before`() {
        gate.acquire(onLost = {})
        mixing = true
        gate.acquire(onLost = {})
        assertFalse(focus.held)
    }

    @Test
    fun `release hands focus back`() {
        gate.acquire(onLost = {})
        gate.release()
        assertFalse(focus.held)
    }
}
