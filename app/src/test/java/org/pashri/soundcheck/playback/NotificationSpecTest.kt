package org.pashri.soundcheck.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSpecTest {
    private fun now(playing: Boolean): NowPlaying =
        NowPlaying(title = "mim", text = "Step 1 of 6", subText = "Warm-up", playing = playing)

    @Test
    fun `without a session the notification keeps pause, next and stop`() {
        val spec = notificationSpec(now = now(playing = true), withSession = false)

        assertEquals(
            listOf(NotificationButton.PAUSE, NotificationButton.NEXT, NotificationButton.STOP),
            spec.buttons,
        )
        assertEquals(listOf(0, 1), spec.compactButtons)
        assertFalse(spec.attachesSession)
    }

    @Test
    fun `a paused Programme offers play instead of pause`() {
        val spec = notificationSpec(now = now(playing = false), withSession = false)

        assertEquals(NotificationButton.PLAY, spec.buttons.first())
    }

    @Test
    fun `with a session the notification is the media card it always was`() {
        val spec = notificationSpec(now = now(playing = true), withSession = true)

        assertEquals(
            listOf(NotificationButton.PAUSE, NotificationButton.NEXT, NotificationButton.STOP),
            spec.buttons,
        )
        assertEquals(listOf(0, 1), spec.compactButtons)
        assertTrue(spec.attachesSession)
    }

    @Test
    fun `every button sends its intent to the playback service`() {
        assertEquals(PlaybackService.ACTION_TOGGLE, NotificationButton.PAUSE.action)
        assertEquals(PlaybackService.ACTION_TOGGLE, NotificationButton.PLAY.action)
        assertEquals(PlaybackService.ACTION_NEXT, NotificationButton.NEXT.action)
        assertEquals(PlaybackService.ACTION_STOP, NotificationButton.STOP.action)
    }
}
