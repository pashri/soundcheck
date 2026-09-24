package org.pashri.soundcheck.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Asks the system for the right to play, so other apps (a podcast) pause meanwhile. */
interface FocusGate {
    /**
     * Requests audio focus.
     *
     * @param onLost called on the main thread if another app takes focus away.
     * @return whether focus was granted.
     */
    fun acquire(onLost: () -> Unit): Boolean

    /** Hands focus back so paused apps can resume. Safe to call when not held. */
    fun release()
}

/**
 * Whether a focus change should stop playback. A call or another media app stops it; a
 * navigation prompt that only asks others to duck does not.
 *
 * @param change an `AudioManager.AUDIOFOCUS_*` value from a focus listener.
 * @return true to stop.
 */
fun focusChangeStopsPlayback(change: Int): Boolean =
    change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT

/**
 * [FocusGate] on Android's audio focus, requested as transient so a paused podcast
 * resumes when Soundcheck stops.
 *
 * @param context any context; only its system services are used.
 */
class AndroidAudioFocus(context: Context) : FocusGate {
    private val audioManager: AudioManager =
        checkNotNull(context.getSystemService(AudioManager::class.java))
    private var request: AudioFocusRequest? = null

    override fun acquire(onLost: () -> Unit): Boolean {
        release()
        val built = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(ATTRIBUTES)
            .setOnAudioFocusChangeListener { if (focusChangeStopsPlayback(it)) onLost() }
            .build()
        request = built
        return audioManager.requestAudioFocus(built) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun release() {
        request?.let { audioManager.abandonAudioFocusRequest(it) }
        request = null
    }

    private companion object {
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}
