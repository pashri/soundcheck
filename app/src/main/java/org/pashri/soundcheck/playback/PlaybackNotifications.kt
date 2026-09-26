package org.pashri.soundcheck.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import org.pashri.soundcheck.MainActivity
import org.pashri.soundcheck.R
import org.pashri.soundcheck.ui.components.Tab

/** The Warm-up's notification channel, its notification, and its media session's state. */
object PlaybackNotifications {
    /** The notification channel's id. */
    const val CHANNEL_ID: String = "warmup"

    /** The playback notification's id. */
    const val NOTIFICATION_ID: Int = 1

    /** The id a replacement notification takes; see [otherNotificationId]. */
    const val FRESH_NOTIFICATION_ID: Int = 2

    /**
     * Creates the quiet channel the playback notification uses; safe to call again.
     *
     * @param context any context.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Warm-up playback")
            .setDescription("The Programme playing, with pause, next and stop.")
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * The media-style notification for [now], shown on the lock screen with its controls.
     * With a [session] it is the lock screen's media card; without one (while playing over
     * other audio) it keeps the same buttons but leaves the media card to the other app.
     *
     * @param context the playback service.
     * @param session its media session, or null while it has none.
     * @param now what to show.
     * @return the notification.
     */
    fun build(context: Context, session: MediaSessionCompat?, now: NowPlaying): Notification {
        val spec = notificationSpec(now = now, withSession = session != null)
        val style = MediaStyle().setShowActionsInCompactView(*spec.compactButtons.toIntArray())
        if (spec.attachesSession) session?.let { style.setMediaSession(it.sessionToken) }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(now.title)
            .setContentText(now.text)
            .setSubText(now.subText)
            .setContentIntent(openApp(context))
            .setDeleteIntent(
                serviceIntent(context = context, action = PlaybackService.ACTION_STOP),
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(now.playing)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setStyle(style)
        spec.buttons.forEach { builder.addAction(action(context = context, button = it)) }
        return builder.build()
    }

    /**
     * Tells the system what is playing and which controls to offer; Android 13 and later
     * draw the lock-screen controls from this.
     *
     * @param session the media session.
     * @param now what is playing.
     */
    fun updateSession(session: MediaSessionCompat, now: NowPlaying) {
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, now.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, now.text)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, now.subText)
                .build(),
        )
        val state = if (now.playing) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(SESSION_ACTIONS)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .addCustomAction(PlaybackService.ACTION_STOP, "Stop", R.drawable.ic_stop)
                .build(),
        )
    }

    private fun action(context: Context, button: NotificationButton): NotificationCompat.Action =
        NotificationCompat.Action(
            button.icon,
            button.title,
            serviceIntent(context = context, action = button.action),
        )

    private fun serviceIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /**
     * Tapping the notification opens the Warm-up tab, on top of the existing task rather than
     * a second [MainActivity].
     */
    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .putExtra(MainActivity.EXTRA_OPEN_TAB, Tab.WarmUp.route)
            .setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private const val SESSION_ACTIONS: Long =
        PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_STOP
}
