package org.pashri.soundcheck.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.ui.components.Tab

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

    @Test
    fun `the Warm-up's notification opens the Warm-up and dismissing it stops the Programme`() {
        val spec = notificationSpec(now = now(playing = true), withSession = true)

        assertEquals(Tab.WarmUp, spec.opens)
        assertEquals(PlaybackService.ACTION_STOP, spec.dismissAction)
    }

    @Test
    fun `a playing Metronome's notification offers Pause and Close, and opens the Metronome`() {
        val now = NowPlaying(title = "Metronome", text = "96 bpm", subText = "", playing = true)
        val spec = notificationSpec(now = now, withSession = true, shows = ServiceShows.METRONOME)

        assertEquals(
            listOf(NotificationButton.PAUSE_METRONOME, NotificationButton.CLOSE_METRONOME),
            spec.buttons,
        )
        assertEquals(listOf(0, 1), spec.compactButtons)
        assertFalse(spec.attachesSession)
        assertEquals(Tab.Metronome, spec.opens)
        assertEquals(PlaybackService.ACTION_CLOSE_METRONOME, spec.dismissAction)
    }

    @Test
    fun `a paused Metronome's notification offers Play and Close`() {
        val now = NowPlaying(title = "Metronome", text = "96 bpm", subText = "", playing = false)
        val spec = notificationSpec(now = now, withSession = true, shows = ServiceShows.METRONOME)

        assertEquals(
            listOf(NotificationButton.PLAY_METRONOME, NotificationButton.CLOSE_METRONOME),
            spec.buttons,
        )
        assertEquals(
            PlaybackService.ACTION_TOGGLE_METRONOME,
            NotificationButton.PLAY_METRONOME.action,
        )
        assertEquals(
            PlaybackService.ACTION_TOGGLE_METRONOME,
            NotificationButton.PAUSE_METRONOME.action,
        )
        assertEquals(
            PlaybackService.ACTION_CLOSE_METRONOME,
            NotificationButton.CLOSE_METRONOME.action,
        )
    }
}
