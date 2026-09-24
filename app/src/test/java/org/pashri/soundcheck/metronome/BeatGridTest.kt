package org.pashri.soundcheck.metronome

import kotlin.math.roundToLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatGridTest {
    @Test
    fun `120 bpm is 24000 frames per beat`() {
        assertEquals(24_000.0, framesPerBeat(120), 0.0)
    }

    @Test
    fun `beats count from the anchor`() {
        val grid = BeatGrid(
            anchorFrame = 1_000,
            anchorIndex = 5,
            framesPerBeat = 24_000.0,
        )
        assertEquals(1_000L, grid.frameOf(5))
        assertEquals(25_000L, grid.frameOf(6))
    }

    @Test
    fun `a fractional tempo does not drift over a thousand beats`() {
        val perBeat = framesPerBeat(97)
        val grid = BeatGrid(
            anchorFrame = 0,
            anchorIndex = 0,
            framesPerBeat = perBeat,
        )
        assertEquals((1_000 * perBeat).roundToLong(), grid.frameOf(1_000))
        val gaps = (0L until 1_000L)
            .map { grid.frameOf(it + 1) - grid.frameOf(it) }
            .toSet()
        assertTrue("gaps were $gaps", gaps.all { it == 29_690L || it == 29_691L })
    }
}
