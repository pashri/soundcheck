package org.pashri.soundcheck.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WavWriterTest {
    @Test
    fun `a clip reads back through the WAV reader at its rate`() {
        val frames = floatArrayOf(0f, 0.5f, -0.5f, 0.25f, -1f)
        val audio = WavReader.read(WavWriter.write(frames = frames, sampleRate = 48_000))
        assertEquals(48_000, audio.sampleRate)
        assertArrayEquals(frames, audio.frames, 1e-4f)
    }

    @Test
    fun `samples past full scale are held at full scale rather than wrapping`() {
        val bytes = WavWriter.write(frames = floatArrayOf(1.5f, -2f), sampleRate = 8_000)
        val audio = WavReader.read(bytes)
        assertArrayEquals(floatArrayOf(1f, -1f), audio.frames, 1e-4f)
    }

    @Test
    fun `the file is a 44-byte header and two bytes a frame`() {
        val bytes = WavWriter.write(frames = FloatArray(size = 480), sampleRate = 48_000)
        assertEquals(44 + 2 * 480, bytes.size)
    }
}
