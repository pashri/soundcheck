package org.pashri.soundcheck.playback

/** How the playback service puts its notification up. */
enum class NotificationPost {
    /** Change the notification in place. */
    UPDATE,

    /**
     * Put up a new notification in place of the old one. The system decides once, when a
     * notification first appears, whether it is the lock screen's media card, so a change of
     * session or of the notification permission needs a new one.
     */
    FRESH,
}

/**
 * Whether a media session made or released needs the notification put up again straight
 * away, with or without the session.
 *
 * @param change what happened to the session.
 * @param inForeground whether the notification is showing; if not, the first post will
 *   carry the session as it is.
 * @return true when the notification must be re-posted now.
 */
fun repostsAfter(change: SessionChange, inForeground: Boolean): Boolean =
    inForeground && change != SessionChange.KEEP

/**
 * Whether the next post updates the notification or replaces it. A notification put up
 * without the notification permission (while Android asks for it, on the first Start) is
 * not read as a media card once the permission is granted, so the first post after that is
 * a new notification, as is one after a session change.
 *
 * @param sessionChanged whether the media session was just made or released.
 * @param permittedAtLastPost whether notifications were allowed at the last post, or null
 *   before the first.
 * @param permittedNow whether notifications are allowed now.
 * @return how to post.
 */
fun notificationPost(
    sessionChanged: Boolean,
    permittedAtLastPost: Boolean?,
    permittedNow: Boolean,
): NotificationPost {
    val justPermitted = permittedAtLastPost == false && permittedNow
    return if (sessionChanged || justPermitted) NotificationPost.FRESH else NotificationPost.UPDATE
}

/**
 * The id a new notification takes, so it replaces the one showing rather than updating it.
 *
 * @param current the id showing now.
 * @return the other of the two playback notification ids.
 */
fun otherNotificationId(current: Int): Int =
    if (current == PlaybackNotifications.NOTIFICATION_ID) {
        PlaybackNotifications.FRESH_NOTIFICATION_ID
    } else {
        PlaybackNotifications.NOTIFICATION_ID
    }
