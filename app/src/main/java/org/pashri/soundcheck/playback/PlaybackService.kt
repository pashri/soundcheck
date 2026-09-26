package org.pashri.soundcheck.playback

import android.app.Notification
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
import androidx.core.app.NotificationManagerCompat
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
import org.pashri.soundcheck.warmup.WarmupSettings

/**
 * Keeps a Programme playing with the screen off: a foreground service with a media session,
 * so the lock screen shows its controls and the headphone button reaches the Warm-up. It is
 * started when a Programme starts and stops itself when the Programme stops or ends. With
 * "Play over other audio" on, there is no session at all (Android 12 and later give the
 * headphone button to the app that last played, even to an inactive session), so the button
 * stays with the other app while the notification keeps its own buttons. Pulling
 * out headphones (or a headset disconnecting) pauses the Programme, so the piano never
 * switches to the loudspeaker. When the session is made or released, or notifications are
 * allowed after the first post, a new notification replaces the old one (see
 * [notificationPost]), so the media card appears from the first Start.
 */
class PlaybackService : Service() {
    private val scope = MainScope()
    private lateinit var container: AppContainer
    private var session: MediaSessionCompat? = null
    private lateinit var presses: PressCounter
    private var inForeground = false
    private var notificationId = PlaybackNotifications.NOTIFICATION_ID
    private var permittedAtLastPost: Boolean? = null
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
        syncSession(container.settings.data.value)
        PlaybackNotifications.createChannel(this)
        ContextCompat.registerReceiver(
            this,
            noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch { container.warmup.playback.collect(::show) }
        scope.launch { container.settings.data.collect(::syncSession) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            container.warmup.stop()
            // A Stop that recreated the service finds nothing to stop; show() never runs
            // stopSelf() for a service that was never in the foreground.
            if (container.warmup.playback.value == null) stopSelf()
            return START_NOT_STICKY
        }
        val playback = container.warmup.playback.value
        if (intent?.action == ACTION_REFRESH && playback == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        showInForeground(playback)
        when (intent?.action) {
            ACTION_TOGGLE -> container.warmup.toggle()
            ACTION_NEXT -> container.warmup.next()
        }
        if (playback == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(noisy)
        scope.cancel()
        releaseSession()
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

    /** Creates or releases the session to match the settings, and redraws the notification. */
    private fun syncSession(settings: WarmupSettings?) {
        val change = sessionChange(hasSession = session != null, settings = settings)
        when (change) {
            SessionChange.CREATE -> session = createSession()
            SessionChange.RELEASE -> releaseSession()
            SessionChange.KEEP -> Unit
        }
        if (!repostsAfter(change = change, inForeground = inForeground)) return
        container.warmup.playback.value?.let {
            showInForeground(playback = it, sessionChanged = true)
        }
    }

    private fun createSession(): MediaSessionCompat =
        MediaSessionCompat(this, SESSION_TAG).apply {
            setCallback(SessionCallback())
            setMediaButtonReceiver(null)
            isActive = true
        }

    private fun releaseSession() {
        session?.run {
            isActive = false
            release()
        }
        session = null
    }

    private fun showInForeground(playback: Playback?, sessionChanged: Boolean = false) {
        val state = playback?.let {
            warmupUiState(
                playback = it,
                programme = it.programme,
                range = it.range,
                sounds = container.library.data.value?.sounds.orEmpty(),
            )
        }
        val now = nowPlaying(state)
        val session = session
        session?.let { PlaybackNotifications.updateSession(session = it, now = now) }
        val notification =
            PlaybackNotifications.build(context = this, session = session, now = now)
        post(notification = notification, sessionChanged = sessionChanged)
    }

    /**
     * Puts [notification] up as the foreground notification. A new one (under the other id,
     * which makes Android drop the old one) goes up when [notificationPost] says so.
     */
    private fun post(notification: Notification, sessionChanged: Boolean) {
        val permittedNow = NotificationManagerCompat.from(this).areNotificationsEnabled()
        val post = notificationPost(
            sessionChanged = sessionChanged,
            permittedAtLastPost = permittedAtLastPost,
            permittedNow = permittedNow,
        )
        val shown = notificationId
        if (post == NotificationPost.FRESH) notificationId = otherNotificationId(shown)
        ServiceCompat.startForeground(
            this,
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (shown != notificationId) NotificationManagerCompat.from(this).cancel(shown)
        permittedAtLastPost = permittedNow
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

        /**
         * Redraw the notification, sent when the notification permission is granted so the
         * media card appears without waiting for the next change.
         */
        const val ACTION_REFRESH: String = "org.pashri.soundcheck.action.REFRESH"

        /** Stop the Programme. */
        const val ACTION_STOP: String = "org.pashri.soundcheck.action.STOP"

        private const val SESSION_TAG = "SoundcheckWarmup"
    }
}
