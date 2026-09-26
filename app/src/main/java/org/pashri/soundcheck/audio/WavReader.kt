package org.pashri.soundcheck.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decoded audio.
 *
 * @property sampleRate frames per second.
 * @property frames one value per frame in the range −1 to 1, channels averaged into one.
 */
class DecodedAudio(val sampleRate: Int, val frames: FloatArray)

/** Reads 16-bit PCM WAV files: the bundled piano samples and the phone's synthesised voice. */
object WavReader {
    /**
     * Decodes a whole WAV file. A data size written as unknown or too large (as streaming
     * writers leave it) reads to the end of the file.
     *
     * @param bytes the file's contents.
     * @return the audio, mixed down to one channel.
     * @throws IllegalArgumentException if [bytes] is not a 16-bit PCM WAV file.
     */
    fun read(bytes: ByteArray): DecodedAudio {
        require(bytes.size >= RIFF_HEADER_BYTES) { "Too short to be a WAV file" }
        require(tag(bytes, 0) == "RIFF" && tag(bytes, WAVE_AT) == "WAVE") { "Not a WAV file" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var format: Format? = null
        var at = RIFF_HEADER_BYTES
        while (at + CHUNK_HEADER_BYTES <= bytes.size) {
            val id = tag(bytes, at)
            val size = buffer.getInt(at + TAG_BYTES)
            val body = at + CHUNK_HEADER_BYTES
            if (id == "data") {
                val known = requireNotNull(format) { "WAV data comes before its format" }
                return decode(buffer = buffer, format = known, at = body, declared = size)
            }
            require(size in 0..bytes.size - body) { "WAV chunk \"$id\" has a broken size" }
            if (id == "fmt ") format = readFormat(buffer = buffer, at = body, size = size)
            at = body + size + size % 2
        }
        throw IllegalArgumentException("WAV file has no data")
    }

    private fun readFormat(buffer: ByteBuffer, at: Int, size: Int): Format {
        require(size >= FORMAT_BYTES) { "WAV format chunk is too short" }
        val encoding = buffer.getShort(at).toInt()
        val channels = buffer.getShort(at + 2).toInt()
        val rate = buffer.getInt(at + 4)
        val bits = buffer.getShort(at + 14).toInt()
        require(encoding == PCM && bits == BITS) { "Not 16-bit PCM: format $encoding, $bits bits" }
        require(channels >= 1 && rate > 0) { "Broken WAV format: $channels channels, $rate Hz" }
        return Format(channels = channels, sampleRate = rate)
    }

    private fun decode(buffer: ByteBuffer, format: Format, at: Int, declared: Int): DecodedAudio {
        val available = buffer.limit() - at
        val size = if (declared in 0..available) declared else available
        val frameBytes = format.channels * BYTES_PER_SAMPLE
        val scale = format.channels * FULL_SCALE
        val frames = FloatArray(size / frameBytes) { frame ->
            val start = at + frame * frameBytes
            var sum = 0
            for (channel in 0 until format.channels) {
                sum += buffer.getShort(start + channel * BYTES_PER_SAMPLE)
            }
            sum / scale
        }
        return DecodedAudio(sampleRate = format.sampleRate, frames = frames)
    }

    private fun tag(bytes: ByteArray, at: Int): String =
        String(bytes, at, TAG_BYTES, Charsets.US_ASCII)

    private class Format(val channels: Int, val sampleRate: Int)

    private const val TAG_BYTES = 4
    private const val WAVE_AT = 8
    private const val RIFF_HEADER_BYTES = 12
    private const val CHUNK_HEADER_BYTES = 8
    private const val FORMAT_BYTES = 16
    private const val PCM = 1
    private const val BITS = 16
    private const val BYTES_PER_SAMPLE = 2
    private const val FULL_SCALE = 32_768f
}
