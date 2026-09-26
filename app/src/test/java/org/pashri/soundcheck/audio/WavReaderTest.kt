package org.pashri.soundcheck.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WavReaderTest {
    private fun ascii(text: String): ByteArray = text.toByteArray(Charsets.US_ASCII)

    private fun le(size: Int): ByteBuffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    private fun chunk(id: String, body: ByteArray): ByteArray =
        le(8 + body.size + body.size % 2).put(ascii(id)).putInt(body.size).put(body).array()

    private fun format(channels: Int, rate: Int, bits: Int = 16, encoding: Int = 1): ByteArray {
        val blockAlign = channels * bits / 8
        return le(16).putShort(encoding.toShort()).putShort(channels.toShort()).putInt(rate)
            .putInt(rate * blockAlign).putShort(blockAlign.toShort()).putShort(bits.toShort())
            .array()
    }

    private fun pcm(vararg samples: Int): ByteArray =
        le(samples.size * 2).apply { samples.forEach { putShort(it.toShort()) } }.array()

    private fun wav(vararg chunks: ByteArray): ByteArray {
        val body = chunks.fold(ByteArray(0)) { all, next -> all + next }
        return le(12 + body.size).put(ascii("RIFF")).putInt(4 + body.size).put(ascii("WAVE"))
            .put(body).array()
    }

    @Test
    fun `mono 16-bit samples read as fractions of full scale`() {
        val bytes = wav(
            chunk("fmt ", format(channels = 1, rate = 48_000)),
            chunk("data", pcm(0, 16_384, -32_768, 32_767)),
        )
        val audio = WavReader.read(bytes)
        assertEquals(48_000, audio.sampleRate)
        assertArrayEquals(floatArrayOf(0f, 0.5f, -1f, 32_767f / 32_768f), audio.frames, 0f)
    }

    @Test
    fun `stereo is averaged into one channel`() {
        val bytes = wav(
            chunk("fmt ", format(channels = 2, rate = 24_000)),
            chunk("data", pcm(16_384, 0, -8_192, -8_192)),
        )
        val audio = WavReader.read(bytes)
        assertEquals(24_000, audio.sampleRate)
        assertArrayEquals(floatArrayOf(0.25f, -0.25f), audio.frames, 0f)
    }

    @Test
    fun `chunks before the data are skipped, an odd-sized one with its pad byte`() {
        val bytes = wav(
            chunk("fmt ", format(channels = 1, rate = 48_000)),
            chunk("LIST", ascii("INFOabc")),
            chunk("data", pcm(8_192)),
        )
        assertArrayEquals(floatArrayOf(0.25f), WavReader.read(bytes).frames, 0f)
    }

    @Test
    fun `a data size written as unknown reads to the end of the file`() {
        val data = le(8 + 4).put(ascii("data")).putInt(-1).put(pcm(8_192, 8_192)).array()
        val bytes = wav(chunk("fmt ", format(channels = 1, rate = 22_050)), data)
        assertArrayEquals(floatArrayOf(0.25f, 0.25f), WavReader.read(bytes).frames, 0f)
    }

    @Test
    fun `anything but 16-bit PCM WAV is refused rather than read as silence`() {
        val brokenChunk = le(8).put(ascii("junk")).putInt(-1).array()
        val cases = listOf(
            ByteArray(4),
            ascii("RIFX") + ByteArray(40),
            wav(
                chunk("fmt ", format(channels = 1, rate = 48_000, bits = 8)),
                chunk("data", pcm(0)),
            ),
            wav(
                chunk("fmt ", format(channels = 1, rate = 48_000, bits = 32, encoding = 3)),
                chunk("data", pcm(0, 0)),
            ),
            wav(chunk("fmt ", format(channels = 1, rate = 48_000))),
            wav(chunk("data", pcm(1)), chunk("fmt ", format(channels = 1, rate = 48_000))),
            wav(chunk("fmt ", format(channels = 1, rate = 48_000)), brokenChunk),
        )
        cases.forEachIndexed { index, bytes ->
            assertThrows("case $index", IllegalArgumentException::class.java) {
                WavReader.read(bytes)
            }
        }
    }
}
