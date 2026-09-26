package org.pashri.soundcheck.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class PcmTest {
    @Test
    fun `doubling the rate interpolates a frame between each pair`() {
        val out = resample(floatArrayOf(0f, 1f, 0f, -1f), fromRate = 24_000, toRate = 48_000)
        assertArrayEquals(floatArrayOf(0f, 0.5f, 1f, 0.5f, 0f, -0.5f, -1f, -1f), out, 1e-6f)
    }

    @Test
    fun `halving the rate keeps every other frame`() {
        val out = resample(floatArrayOf(0f, 1f, 2f, 3f), fromRate = 48_000, toRate = 24_000)
        assertArrayEquals(floatArrayOf(0f, 2f), out, 1e-6f)
    }

    @Test
    fun `the same rate returns a copy`() {
        val pcm = floatArrayOf(0.1f, 0.2f)
        val out = resample(pcm, fromRate = 48_000, toRate = 48_000)
        assertArrayEquals(pcm, out, 0f)
        assertNotSame(pcm, out)
    }

    @Test
    fun `a second of 22 050 Hz speech is a second at 48 kHz`() {
        assertEquals(48_000, resample(FloatArray(22_050), fromRate = 22_050, toRate = 48_000).size)
    }

    @Test
    fun `silence is trimmed from both ends, keeping a margin`() {
        val pcm = FloatArray(10) { if (it in 4..5) 0.5f else 0.001f }
        val out = trimSilence(pcm, threshold = 0.01f, marginFrames = 1)
        assertArrayEquals(floatArrayOf(0.001f, 0.5f, 0.5f, 0.001f), out, 0f)
    }

    @Test
    fun `the margin never reaches past either end`() {
        val pcm = floatArrayOf(0.5f, 0f, 0f, -0.5f)
        assertArrayEquals(pcm, trimSilence(pcm, threshold = 0.01f, marginFrames = 10), 0f)
    }

    @Test
    fun `all silence trims to nothing`() {
        assertEquals(0, trimSilence(FloatArray(100), threshold = 0.01f, marginFrames = 5).size)
    }

    @Test
    fun `levelling scales the loudest sample to the peak`() {
        val out = normalizePeak(floatArrayOf(0.1f, -0.35f, 0.2f), peak = 0.7f)
        assertArrayEquals(floatArrayOf(0.2f, -0.7f, 0.4f), out, 1e-6f)
    }

    @Test
    fun `levelling silence leaves it silent`() {
        assertArrayEquals(FloatArray(3), normalizePeak(FloatArray(3), peak = 0.7f), 0f)
    }
}
