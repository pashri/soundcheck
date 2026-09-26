package org.pashri.soundcheck.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.warmup.WarmupSettings

class NotificationPostsTest {
    @Test
    fun `a session made or released while showing re-posts the notification`() {
        assertTrue(repostsAfter(change = SessionChange.CREATE, inForeground = true))
        assertTrue(repostsAfter(change = SessionChange.RELEASE, inForeground = true))
    }

    @Test
    fun `no re-post when the session stays or nothing is shown yet`() {
        assertFalse(repostsAfter(change = SessionChange.KEEP, inForeground = true))
        assertFalse(repostsAfter(change = SessionChange.CREATE, inForeground = false))
        assertFalse(repostsAfter(change = SessionChange.RELEASE, inForeground = false))
    }

    @Test
    fun `the session is there before the settings load and stays once they load`() {
        val before = sessionChange(hasSession = false, settings = null)
        val after = sessionChange(hasSession = true, settings = WarmupSettings.DEFAULT)

        assertEquals(SessionChange.CREATE, before)
        assertEquals(SessionChange.KEEP, after)
        assertFalse(repostsAfter(change = after, inForeground = true))
    }

    @Test
    fun `a session change posts a new notification`() {
        assertEquals(
            NotificationPost.FRESH,
            notificationPost(
                sessionChanged = true,
                permittedAtLastPost = true,
                permittedNow = true,
            ),
        )
    }

    @Test
    fun `the first post after notifications are allowed is a new notification`() {
        assertEquals(
            NotificationPost.FRESH,
            notificationPost(
                sessionChanged = false,
                permittedAtLastPost = false,
                permittedNow = true,
            ),
        )
    }

    @Test
    fun `other posts update the notification in place`() {
        val cases = listOf(
            null to true,
            null to false,
            true to true,
            false to false,
            true to false,
        )

        cases.forEach { (before, now) ->
            assertEquals(
                NotificationPost.UPDATE,
                notificationPost(
                    sessionChanged = false,
                    permittedAtLastPost = before,
                    permittedNow = now,
                ),
            )
        }
    }

    @Test
    fun `a new notification swaps between the two ids`() {
        val other = otherNotificationId(PlaybackNotifications.NOTIFICATION_ID)

        assertTrue(other != PlaybackNotifications.NOTIFICATION_ID)
        assertEquals(PlaybackNotifications.NOTIFICATION_ID, otherNotificationId(other))
    }
}
