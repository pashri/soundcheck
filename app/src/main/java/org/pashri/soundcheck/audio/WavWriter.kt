package org.pashri.soundcheck.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Writes 16-bit PCM WAV files: the Sounds' recorded clips. */
object WavWriter {
    /**
     * Encodes mono audio as a 16-bit PCM WAV file that [WavReader] reads back.
     *
     * @param frames samples in the range −1 to 1; anything beyond is held at full scale.
     * @param sampleRate frames per second.
     * @return the file's bytes: a 44-byte header, then two bytes a frame.
     */
    fun write(frames: FloatArray, sampleRate: Int): ByteArray {
        val dataBytes = frames.size * BYTES_PER_SAMPLE
        val buffer = ByteBuffer.allocate(HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(ascii("RIFF")).putInt(HEADER_BYTES - CHUNK_HEADER_BYTES + dataBytes)
        buffer.put(ascii("WAVE")).put(ascii("fmt ")).putInt(FORMAT_BYTES)
        buffer.putShort(PCM).putShort(MONO).putInt(sampleRate)
        buffer.putInt(sampleRate * BYTES_PER_SAMPLE).putShort(BLOCK_ALIGN).putShort(BITS)
        buffer.put(ascii("data")).putInt(dataBytes)
        frames.forEach { buffer.putShort(sampleOf(it)) }
        return buffer.array()
    }

    private fun sampleOf(value: Float): Short =
        (value.coerceIn(minimumValue = -1f, maximumValue = 1f) * MAX_SAMPLE).roundToInt().toShort()

    private fun ascii(text: String): ByteArray = text.toByteArray(Charsets.US_ASCII)

    private const val HEADER_BYTES = 44
    private const val CHUNK_HEADER_BYTES = 8
    private const val FORMAT_BYTES = 16
    private const val BYTES_PER_SAMPLE = 2
    private const val PCM: Short = 1
    private const val MONO: Short = 1
    private const val BLOCK_ALIGN: Short = 2
    private const val BITS: Short = 16
    private const val MAX_SAMPLE = 32_767f
}
