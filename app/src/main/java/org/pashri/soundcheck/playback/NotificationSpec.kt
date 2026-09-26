package org.pashri.soundcheck.playback

import androidx.annotation.DrawableRes
import org.pashri.soundcheck.R

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
}

/**
 * What the playback notification carries, apart from its text.
 *
 * @property buttons its buttons, in order.
 * @property compactButtons the indices of [buttons] shown when the notification is collapsed.
 * @property attachesSession whether the notification is tied to the media session, which
 *   makes it the lock screen's media card.
 */
data class NotificationSpec(
    val buttons: List<NotificationButton>,
    val compactButtons: List<Int>,
    val attachesSession: Boolean,
)

/**
 * The notification's buttons for [now]; the same with or without a media session, so
 * pause, next and stop still work while playing over other audio.
 *
 * @param now what is playing.
 * @param withSession whether the service holds a media session.
 * @return the notification's buttons and whether it attaches the session.
 */
fun notificationSpec(now: NowPlaying, withSession: Boolean): NotificationSpec {
    val toggle = if (now.playing) NotificationButton.PAUSE else NotificationButton.PLAY
    return NotificationSpec(
        buttons = listOf(toggle, NotificationButton.NEXT, NotificationButton.STOP),
        compactButtons = listOf(0, 1),
        attachesSession = withSession,
    )
}
