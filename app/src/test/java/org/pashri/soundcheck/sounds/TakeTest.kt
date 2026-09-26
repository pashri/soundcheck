package org.pashri.soundcheck.sounds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TakeTest {
    /**
     * A recording made of stretches, each a number of frames at a level; the sign alternates
     * frame by frame, as audio does.
     */
    private fun recording(vararg stretches: Pair<Int, Float>): FloatArray =
        stretches.flatMap { (frames, level) -> List(size = frames) { level } }
            .mapIndexed { index, level -> if (index % 2 == 0) level else -level }
            .toFloatArray()

    private fun kept(take: Take): Take.Kept {
        assertTrue("expected a kept take, got $take", take is Take.Kept)
        return take as Take.Kept
    }

    @Test
    fun `the quiet either side of the word is trimmed, keeping 50 ms`() {
        val take = takeOf(
            frames = recording(9_600 to 0.001f, 14_400 to 0.3f, 9_600 to 0.001f),
            cutShort = false,
        )
        val clip = kept(take)
        assertEquals(400L, clip.lengthMs)
        assertEquals(0.7f, clip.pcm.max(), 1e-6f)
        assertFalse(clip.cutShort)
    }

    @Test
    fun `the press and the finger lifting off are not kept`() {
        val take = takeOf(
            frames = recording(
                2_000 to 0.8f,
                10_000 to 0f,
                9_600 to 0.3f,
                6_000 to 0f,
                3_000 to 0.8f,
            ),
            cutShort = false,
        )
        assertEquals(300L, kept(take).lengthMs)
    }

    @Test
    fun `a recording cut short at the limit keeps its end`() {
        val clip = kept(takeOf(frames = recording(4_800 to 0f, 20_000 to 0.3f), cutShort = true))
        assertEquals(466L, clip.lengthMs)
        assertTrue(clip.cutShort)
    }

    @Test
    fun `a quiet voice in a quiet room is still trimmed`() {
        val take = takeOf(
            frames = recording(9_600 to 0.008f, 14_400 to 0.05f, 9_600 to 0.008f),
            cutShort = false,
        )
        assertEquals(400L, kept(take).lengthMs)
    }

    @Test
    fun `nothing loud enough is too quiet`() {
        assertEquals(Take.TooQuiet, takeOf(frames = recording(48_000 to 0.02f), cutShort = false))
    }

    @Test
    fun `a tap too short to hold anything is too quiet`() {
        assertEquals(Take.TooQuiet, takeOf(frames = recording(1_000 to 0.5f), cutShort = false))
    }

    @Test
    fun `a blip is too short to keep`() {
        val take = takeOf(
            frames = recording(9_600 to 0f, 1_200 to 0.3f, 9_600 to 0f),
            cutShort = false,
        )
        assertEquals(Take.TooShort, take)
    }
}
