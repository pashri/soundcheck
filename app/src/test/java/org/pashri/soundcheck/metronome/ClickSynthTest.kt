package org.pashri.soundcheck.metronome

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickSynthTest {
    private val click = ClickSynth.click(frequencyHz = 1_000.0)

    @Test
    fun `a click lasts thirty milliseconds at 48 kHz`() {
        assertEquals(1_440, click.size)
    }

    @Test
    fun `a click starts from silence so it does not pop`() {
        assertEquals(0f, click.first(), 0f)
    }

    @Test
    fun `a click never exceeds its peak level`() {
        assertTrue(click.all { abs(it) <= ClickSynth.PEAK })
    }

    @Test
    fun `a click has died away by its last sample`() {
        assertTrue(abs(click.last()) < 0.01f)
    }

    @Test
    fun `clicks at different pitches differ`() {
        assertFalse(click.contentEquals(ClickSynth.click(frequencyHz = 1_600.0)))
    }
}
