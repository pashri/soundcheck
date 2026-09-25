package org.pashri.soundcheck.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Turns text into speech the engine can play. */
interface SpeechSynth {
    /**
     * Speaks [text] into memory rather than out loud.
     *
     * @param text what to say, e.g. "lip trill".
     * @return mono PCM at [SAMPLE_RATE], or null if the phone has no working voice.
     */
    suspend fun speak(text: String): FloatArray?
}

/**
 * [SpeechSynth] on Android's text-to-speech, in the phone's default voice and language.
 * Speech is written to a file rather than spoken aloud, so it can play through the engine on
 * an exact frame, and its length is known before a Step is laid out.
 *
 * @param context any context; only the application context is kept.
 */
class AndroidSpeech(context: Context) : SpeechSynth {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var engine: TextToSpeech? = null

    override suspend fun speak(text: String): FloatArray? = mutex.withLock {
        val tts = engine() ?: return@withLock null
        val file = File(appContext.cacheDir, SPEECH_FILE)
        if (!synthesize(tts = tts, text = text, file = file)) return@withLock null
        withContext(Dispatchers.IO) { decode(file) }
    }

    private suspend fun engine(): TextToSpeech? {
        engine?.let { return it }
        val ready = CompletableDeferred<Boolean>()
        val created = withContext(Dispatchers.Main) {
            TextToSpeech(appContext) { status -> ready.complete(status == TextToSpeech.SUCCESS) }
        }
        if (!ready.await()) {
            Log.w(TAG, "No text-to-speech voice is available")
            created.shutdown()
            return null
        }
        engine = created
        return created
    }

    // Two callbacks can race for the same utterance (onDone vs. onError, or the queue-failure
    // path below), so an AtomicBoolean guard makes sure only the first ever resumes.
    private suspend fun synthesize(tts: TextToSpeech, text: String, file: File): Boolean =
        suspendCancellableCoroutine { continuation ->
            val id = UUID.randomUUID().toString()
            val resumed = AtomicBoolean(false)
            tts.setOnUtteranceProgressListener(
                Listener(id) { done ->
                    if (resumed.compareAndSet(false, true)) continuation.resume(done)
                },
            )
            val queued = tts.synthesizeToFile(text, Bundle(), file, id)
            if (queued != TextToSpeech.SUCCESS && resumed.compareAndSet(false, true)) {
                continuation.resume(false)
            }
        }

    private fun decode(file: File): FloatArray? =
        try {
            val audio = WavReader.read(file.readBytes())
            resample(audio.frames, fromRate = audio.sampleRate, toRate = SAMPLE_RATE)
        } catch (e: IOException) {
            Log.w(TAG, "Could not read the synthesised speech", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "The synthesised speech is not 16-bit WAV", e)
            null
        }

    /** Reports when utterance [id] has been written, or failed. */
    private class Listener(
        private val id: String,
        private val finished: (Boolean) -> Unit,
    ) : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            if (utteranceId == id) finished(true)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == id) finished(false)
        }
    }

    private companion object {
        const val TAG = "AndroidSpeech"
        const val SPEECH_FILE = "announcement.wav"
    }
}
