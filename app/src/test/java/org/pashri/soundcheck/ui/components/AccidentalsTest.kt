package org.pashri.soundcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.warmup.Accidental

class AccidentalsTest {
    @Test
    fun `every accidental is drawn centred in the same box`() {
        Accidental.entries.forEach { accidental ->
            val origin = centredOrigin(accidental = accidental)
            val bounds = accidentalBounds(accidental = accidental)
            val middleX = origin.first + (bounds.left + bounds.right) / 2
            val middleY = origin.second + (bounds.top + bounds.bottom) / 2
            assertEquals(MARK_WIDTH_SPACES / 2, middleX, 0.001f)
            assertEquals(MARK_HEIGHT_SPACES / 2, middleY, 0.001f)
        }
    }

    @Test
    fun `every accidental fits inside the box`() {
        Accidental.entries.forEach { accidental ->
            val origin = centredOrigin(accidental = accidental)
            val bounds = accidentalBounds(accidental = accidental)
            assertTrue(accidental.name, origin.first + bounds.left >= 0f)
            assertTrue(accidental.name, origin.second + bounds.top >= 0f)
            assertTrue(accidental.name, origin.first + bounds.right <= MARK_WIDTH_SPACES)
            assertTrue(accidental.name, origin.second + bounds.bottom <= MARK_HEIGHT_SPACES)
        }
    }

    @Test
    fun `the sharp and the natural are the same height and the flat nearly so`() {
        val heights = Accidental.entries.associateWith { accidental ->
            val bounds = accidentalBounds(accidental = accidental)
            bounds.bottom - bounds.top
        }
        assertEquals(heights.getValue(Accidental.SHARP), heights.getValue(Accidental.NATURAL))
        assertTrue(heights.getValue(Accidental.FLAT) > 0.85f * heights.getValue(Accidental.SHARP))
    }
}
