package org.pashri.soundcheck.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The microphone, for tools that listen. Separate from [SoundOutput]: it never uses Oboe. */
interface MicInput {
    /**
     * Opens the microphone and starts capturing mono float samples at [SAMPLE_RATE].
     *
     * @return an open session, or null if the microphone can't be opened (no permission,
     *   or the device refused the format).
     */
    fun open(): MicSession?
}

/** One open microphone capture. Use it from one coroutine at a time. */
interface MicSession {
    /**
     * Fills [buffer] with the next samples, waiting until they have been captured.
     *
     * @param buffer where the samples go, in the range −1 to 1.
     * @return how many samples were read; fewer than `buffer.size` means the microphone
     *   has stopped working.
     */
    suspend fun read(buffer: FloatArray): Int

    /** Stops capturing and releases the microphone. Safe to call twice. */
    fun close()
}

/**
 * [MicInput] on Android's [AudioRecord], using the voice-recognition source: the platform
 * requires it to have automatic gain control and noise suppression off, so the tuner hears
 * the instrument as it is.
 */
class AndroidMic : MicInput {
    // RECORD_AUDIO is checked by the Tuner screen before it listens, and a missing grant
    // surfaces here as a SecurityException or an uninitialised recorder.
    @SuppressLint("MissingPermission")
    override fun open(): MicSession? {
        val record = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(FORMAT)
                .setBufferSizeInBytes(bufferBytes())
                .build()
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "The microphone refused the format", e)
            return null
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to use the microphone", e)
            return null
        }
        return start(record)
    }

    private fun start(record: AudioRecord): MicSession? {
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return null
        }
        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "The microphone would not start", e)
            record.release()
            return null
        }
        return AudioRecordSession(record)
    }

    private fun bufferBytes(): Int {
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        return maxOf(minimum, BUFFER_FRAMES * Float.SIZE_BYTES)
    }

    private companion object {
        const val TAG = "AndroidMic"

        /** About 170 ms of audio, so a slow frame never loses samples. */
        const val BUFFER_FRAMES = 8_192

        val FORMAT: AudioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .build()
    }
}

private class AudioRecordSession(private val record: AudioRecord) : MicSession {
    private var closed = false

    override suspend fun read(buffer: FloatArray): Int = withContext(Dispatchers.IO) {
        record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
    }

    override fun close() {
        if (closed) return
        closed = true
        record.stop()
        record.release()
    }
}
