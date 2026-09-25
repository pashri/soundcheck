package org.pashri.soundcheck.playback

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.pashri.soundcheck.SoundcheckApplication
import org.pashri.soundcheck.di.AppContainer
import org.pashri.soundcheck.ui.warmup.warmupUiState
import org.pashri.soundcheck.warmup.Playback

/**
 * Keeps a Programme playing with the screen off: a foreground service with a media session,
 * so the lock screen shows its controls and the headphone button reaches the Warm-up. It is
 * started when a Programme starts and stops itself when the Programme stops or ends. Pulling
 * out headphones (or a headset disconnecting) pauses the Programme, so the piano never
 * switches to the loudspeaker.
 */
class PlaybackService : Service() {
    private val scope = MainScope()
    private lateinit var container: AppContainer
    private lateinit var session: MediaSessionCompat
    private lateinit var presses: PressCounter
    private var inForeground = false
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) container.warmup.pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = (application as SoundcheckApplication).container
        presses = PressCounter(
            scope = scope,
            windowMs = PressCounter.WINDOW_MS,
            onPresses = container.warmup::onPresses,
        )
        session = MediaSessionCompat(this, SESSION_TAG).apply {
            setCallback(SessionCallback())
            setMediaButtonReceiver(null)
            isActive = true
        }
        PlaybackNotifications.createChannel(this)
        ContextCompat.registerReceiver(
            this,
            noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch { container.warmup.playback.collect(::show) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val playback = container.warmup.playback.value
        showInForeground(playback)
        when (intent?.action) {
            ACTION_TOGGLE -> container.warmup.toggle()
            ACTION_NEXT -> container.warmup.next()
            ACTION_STOP -> container.warmup.stop()
        }
        if (playback == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(noisy)
        scope.cancel()
        session.isActive = false
        session.release()
        super.onDestroy()
    }

    private fun show(playback: Playback?) {
        if (playback != null) {
            showInForeground(playback)
        } else if (inForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            inForeground = false
            stopSelf()
        }
    }

    private fun showInForeground(playback: Playback?) {
        val state = playback?.let {
            warmupUiState(
                playback = it,
                programme = it.programme,
                range = it.range,
                sounds = container.sounds,
            )
        }
        val now = nowPlaying(state)
        PlaybackNotifications.updateSession(session, now)
        ServiceCompat.startForeground(
            this,
            PlaybackNotifications.NOTIFICATION_ID,
            PlaybackNotifications.build(context = this, session = session, now = now),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        inForeground = true
    }

    /** Headphone, car and lock-screen controls, delivered on the main thread. */
    private inner class SessionCallback : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val event = IntentCompat.getParcelableExtra(
                mediaButtonEvent,
                Intent.EXTRA_KEY_EVENT,
                KeyEvent::class.java,
            ) ?: return false
            val action = mediaKeyAction(
                keyCode = event.keyCode,
                action = event.action,
                repeatCount = event.repeatCount,
            )
            when (action) {
                MediaKeyAction.PRESS -> presses.press()
                MediaKeyAction.NEXT -> container.warmup.next()
                MediaKeyAction.PREVIOUS -> container.warmup.previous()
                MediaKeyAction.CONSUME -> Unit
                MediaKeyAction.IGNORE -> return super.onMediaButtonEvent(mediaButtonEvent)
            }
            return true
        }

        override fun onPlay() {
            container.warmup.resume()
        }

        override fun onPause() {
            container.warmup.pause()
        }

        override fun onSkipToNext() {
            container.warmup.next()
        }

        override fun onSkipToPrevious() {
            container.warmup.previous()
        }

        override fun onStop() {
            container.warmup.stop()
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            if (action == ACTION_STOP) container.warmup.stop()
        }
    }

    /** The intents the notification's buttons send. */
    companion object {
        /** Pause or resume. */
        const val ACTION_TOGGLE: String = "org.pashri.soundcheck.action.TOGGLE"

        /** Go to the next Step. */
        const val ACTION_NEXT: String = "org.pashri.soundcheck.action.NEXT"

        /** Stop the Programme. */
        const val ACTION_STOP: String = "org.pashri.soundcheck.action.STOP"

        private const val SESSION_TAG = "SoundcheckWarmup"
    }
}
