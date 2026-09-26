package org.pashri.soundcheck.playback

import androidx.annotation.DrawableRes
import org.pashri.soundcheck.R
import org.pashri.soundcheck.ui.components.Tab

/**
 * A button on the playback notification.
 *
 * @property icon its icon.
 * @property title its label, also what TalkBack reads.
 * @property action the intent action it sends to [PlaybackService].
 */
enum class NotificationButton(
    @DrawableRes val icon: Int,
    val title: String,
    val action: String,
) {
    /** Pause the Programme. */
    PAUSE(icon = R.drawable.ic_pause, title = "Pause", action = PlaybackService.ACTION_TOGGLE),

    /** Resume the Programme. */
    PLAY(icon = R.drawable.ic_play, title = "Play", action = PlaybackService.ACTION_TOGGLE),

    /** Go to the next Step. */
    NEXT(icon = R.drawable.ic_next, title = "Next", action = PlaybackService.ACTION_NEXT),

    /** Stop the Programme. */
    STOP(icon = R.drawable.ic_stop, title = "Stop", action = PlaybackService.ACTION_STOP),

    /** Stop the Metronome. */
    STOP_METRONOME(
        icon = R.drawable.ic_stop,
        title = "Stop",
        action = PlaybackService.ACTION_STOP_METRONOME,
    ),
}

/**
 * What the playback notification carries, apart from its text.
 *
 * @property buttons its buttons, in order.
 * @property compactButtons the indices of [buttons] shown when the notification is collapsed.
 * @property attachesSession whether the notification is tied to the media session, which
 *   makes it the lock screen's media card.
 * @property opens the tab a tap on the notification opens.
 * @property dismissAction the intent action sent to [PlaybackService] when it is swiped away
 *   (only possible while paused).
 */
data class NotificationSpec(
    val buttons: List<NotificationButton>,
    val compactButtons: List<Int>,
    val attachesSession: Boolean,
    val opens: Tab,
    val dismissAction: String,
)

/**
 * The notification's buttons for [now]. The Warm-up's are the same with or without a media
 * session, so pause, next and stop still work while playing over other audio. The
 * Metronome's notification only has Stop and never carries the session: the session's
 * lock-screen play, pause and skip controls belong to the Warm-up.
 *
 * @param now what is playing.
 * @param withSession whether the service holds a media session.
 * @param shows which tool the notification is about.
 * @return the notification's buttons, whether it attaches the session and where it leads.
 */
fun notificationSpec(
    now: NowPlaying,
    withSession: Boolean,
    shows: ServiceShows = ServiceShows.WARM_UP,
): NotificationSpec {
    if (shows == ServiceShows.METRONOME) {
        return NotificationSpec(
            buttons = listOf(NotificationButton.STOP_METRONOME),
            compactButtons = listOf(0),
            attachesSession = false,
            opens = Tab.Metronome,
            dismissAction = PlaybackService.ACTION_STOP_METRONOME,
        )
    }
    val toggle = if (now.playing) NotificationButton.PAUSE else NotificationButton.PLAY
    return NotificationSpec(
        buttons = listOf(toggle, NotificationButton.NEXT, NotificationButton.STOP),
        compactButtons = listOf(0, 1),
        attachesSession = withSession,
        opens = Tab.WarmUp,
        dismissAction = PlaybackService.ACTION_STOP,
    )
}
