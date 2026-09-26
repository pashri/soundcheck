package org.pashri.soundcheck.tuner

import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.CENTS_PER_SEMITONE
import org.pashri.soundcheck.music.midiOf

class PitchSmootherTest {
    private val smoother = PitchSmoother()

    private fun feed(hz: Double?, frames: Int): List<Double?> = List(frames) { smoother.next(hz) }

    private fun centsFrom(hz: Double, cents: Double): Double =
        hz * 2.0.pow(cents / 1200)

    @Test
    fun `a steady pitch reads as that pitch`() {
        val readings = feed(110.0, frames = 10)
        assertEquals(midiOf(110.0), checkNotNull(readings.last()), 1e-9)
    }

    @Test
    fun `a new sound shows only once it has lasted three frames`() {
        val readings = feed(110.0, frames = CONFIRM_FRAMES)
        assertEquals(listOf(null, null), readings.take(CONFIRM_FRAMES - 1))
        assertNotNull(readings.last())
    }

    @Test
    fun `jitter of 3 cents either way is calmed to under a cent`() {
        feed(110.0, frames = 5)
        val readings = List(40) { i ->
            smoother.next(centsFrom(110.0, if (i % 2 == 0) 3.0 else -3.0))
        }
        readings.drop(10).forEach { midi ->
            val cents = (checkNotNull(midi) - midiOf(110.0)) * CENTS_PER_SEMITONE
            assertTrue("$cents", abs(cents) < 1.0)
        }
    }

    @Test
    fun `a single frame an octave up is ignored`() {
        feed(110.0, frames = 10)
        val readings = feed(220.0, frames = 1) + feed(110.0, frames = 10)
        readings.forEach { assertEquals(midiOf(110.0), checkNotNull(it), 0.01) }
    }

    @Test
    fun `a new note takes over within three frames without sliding through the notes between`() {
        feed(110.0, frames = 10)
        val readings = feed(146.83, frames = 10).map { checkNotNull(it) }
        assertEquals(midiOf(110.0), readings[0], 0.01)
        assertEquals(midiOf(110.0), readings[1], 0.01)
        assertEquals(midiOf(146.83), readings[2], 0.01)
        readings.forEach { midi ->
            val nearOld = abs(midi - midiOf(110.0)) < 0.01
            val nearNew = abs(midi - midiOf(146.83)) < 0.01
            assertTrue("slid through $midi", nearOld || nearNew)
        }
    }

    @Test
    fun `a gap shorter than the hold keeps the last reading`() {
        feed(110.0, frames = 10)
        val held = feed(null, frames = HOLD_FRAMES)
        held.forEach { assertEquals(midiOf(110.0), checkNotNull(it), 1e-9) }
    }

    @Test
    fun `after the hold the reading clears`() {
        feed(110.0, frames = 10)
        feed(null, frames = HOLD_FRAMES)
        assertNull(smoother.next(null))
    }

    @Test
    fun `a note after silence starts fresh instead of sliding from the old one`() {
        feed(110.0, frames = 10)
        feed(null, frames = HOLD_FRAMES + 1)
        val readings = feed(196.0, frames = CONFIRM_FRAMES)
        assertEquals(listOf(null, null), readings.take(CONFIRM_FRAMES - 1))
        assertEquals(midiOf(196.0), checkNotNull(readings.last()), 1e-9)
    }

    @Test
    fun `reset forgets the reading`() {
        feed(110.0, frames = 10)
        smoother.reset()
        assertNull(smoother.next(196.0))
    }
}
