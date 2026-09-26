package org.pashri.soundcheck.playback

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.pashri.soundcheck.SoundcheckApplication
import org.pashri.soundcheck.di.AppContainer
import org.pashri.soundcheck.metronome.MetronomeStatus
import org.pashri.soundcheck.ui.warmup.warmupUiState
import org.pashri.soundcheck.warmup.Playback

/**
 * Keeps a Programme or the Metronome playing with the screen off: a foreground service
 * started when a Programme loads or the Metronome starts, which stops itself once neither
 * needs it ([serviceShows]). While a Programme is loaded its notification carries the app's
 * media session ([MediaButtonSession]), so the lock screen shows the Programme's controls;
 * while only the Metronome plays or is paused, the notification gives its tempo with Pause
 * (or Play) and Close. The
 * session itself belongs to the app, so the headphone button reaches the Metronome too;
 * with "Play over other audio" on there is none, and the notification keeps its own
 * buttons. Pulling out headphones (or a headset disconnecting) pauses the Programme and the
 * Metronome, so neither switches to the loudspeaker. When the session is made or
 * released, the notification switches tool, or notifications are allowed after the first
 * post, a new notification replaces the old one (see [notificationPost]), so the media card
 * appears from the first Start.
 */
class PlaybackService : Service() {
    private val scope = MainScope()
    private lateinit var container: AppContainer
    private var session: MediaSessionCompat? = null
    private var inForeground = false
    private var notificationId = PlaybackNotifications.NOTIFICATION_ID
    private var permittedAtLastPost: Boolean? = null
    private var shownAtLastPost: ServiceShows? = null
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            onHeadphonesUnplugged(warmup = container.warmup, metronome = container.metronome)
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = (application as SoundcheckApplication).container
        container.mediaButtons.start()
        session = container.mediaButtons.session.value
        PlaybackNotifications.createChannel(this)
        ContextCompat.registerReceiver(
            this,
            noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            combine(
                flow = container.warmup.playback,
                flow2 = settledMetronome(),
            ) { playback, metronome ->
                playback to metronome
            }.collect { (playback, metronome) -> show(playback = playback, metronome = metronome) }
        }
        scope.launch { container.mediaButtons.session.collect(::onSession) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> container.warmup.stop()
            ACTION_CLOSE_METRONOME -> container.metronome.stop()
        }
        val shows = currentShows()
        if (intent?.action in STOPS) {
            // A Stop that recreated the service finds nothing to stop; show() never runs
            // stopSelf() for a service that was never in the foreground.
            if (shows == null) stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_REFRESH && shows == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        showInForeground(
            playback = container.warmup.playback.value,
            metronome = container.metronome.status.value,
        )
        when (intent?.action) {
            ACTION_TOGGLE -> container.warmup.toggle()
            ACTION_NEXT -> container.warmup.next()
            ACTION_TOGGLE_METRONOME -> container.metronome.pauseOrResume()
        }
        if (shows == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(noisy)
        scope.cancel()
        super.onDestroy()
    }

    /**
     * The Metronome's status, with tempo and accent changes settling for a moment while it
     * is held, so dragging the tempo doesn't flood the notification past Android's rate
     * limit. Starting from nothing and ending come through at once.
     */
    @OptIn(FlowPreview::class)
    private fun settledMetronome(): Flow<MetronomeStatus> =
        container.metronome.status.debounce { if (it.held) SETTLE_MS else 0L }

    private fun currentShows(): ServiceShows? = serviceShows(
        programmeLoaded = container.warmup.playback.value != null,
        metronomeHeld = container.metronome.status.value.held,
    )

    private fun show(playback: Playback?, metronome: MetronomeStatus) {
        val shows =
            serviceShows(programmeLoaded = playback != null, metronomeHeld = metronome.held)
        if (shows != null) {
            showInForeground(playback = playback, metronome = metronome)
        } else if (inForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            inForeground = false
            stopSelf()
        }
    }

    /** The app's session was made or released: redraws the notification to carry it. */
    private fun onSession(latest: MediaSessionCompat?) {
        val change = when {
            latest === session -> SessionChange.KEEP
            latest == null -> SessionChange.RELEASE
            else -> SessionChange.CREATE
        }
        session = latest
        if (!repostsAfter(change = change, inForeground = inForeground)) return
        // Only the Warm-up's notification carries the session.
        if (currentShows() != ServiceShows.WARM_UP) return
        showInForeground(
            playback = container.warmup.playback.value,
            metronome = container.metronome.status.value,
            sessionChanged = true,
        )
    }

    /**
     * Puts up the notification [serviceShows] picks; with nothing to show (a start that
     * finds nothing playing, just before stopping) it shows the Warm-up's placeholder.
     */
    private fun showInForeground(
        playback: Playback?,
        metronome: MetronomeStatus,
        sessionChanged: Boolean = false,
    ) {
        val shows =
            serviceShows(programmeLoaded = playback != null, metronomeHeld = metronome.held)
                ?: ServiceShows.WARM_UP
        val now = when (shows) {
            ServiceShows.WARM_UP -> nowPlaying(warmupState(playback))
            ServiceShows.METRONOME -> metronomeNowPlaying(metronome)
        }
        val session = session
        if (shows == ServiceShows.WARM_UP) {
            session?.let { PlaybackNotifications.updateSession(session = it, now = now) }
        }
        val notification = PlaybackNotifications.build(
            context = this,
            session = session,
            now = now,
            shows = shows,
        )
        val switched = shownAtLastPost != null && shownAtLastPost != shows
        post(notification = notification, sessionChanged = sessionChanged || switched)
        shownAtLastPost = shows
    }

    private fun warmupState(playback: Playback?) = playback?.let {
        warmupUiState(
            playback = it,
            programme = it.programme,
            range = it.range,
            sounds = container.library.data.value?.sounds.orEmpty(),
        )
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

    /** The intents the notification's buttons send. */
    companion object {
        /** Pause a playing Metronome, or start a paused one again. */
        const val ACTION_TOGGLE_METRONOME: String =
            "org.pashri.soundcheck.action.TOGGLE_METRONOME"

        /** End the Metronome. */
        const val ACTION_CLOSE_METRONOME: String = "org.pashri.soundcheck.action.CLOSE_METRONOME"

        /** How long tempo and accent changes settle before the notification shows them. */
        private const val SETTLE_MS: Long = 250L

        private val STOPS = setOf(ACTION_STOP, ACTION_CLOSE_METRONOME)

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
    }
}
