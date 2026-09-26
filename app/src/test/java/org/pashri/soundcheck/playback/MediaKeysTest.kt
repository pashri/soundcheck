package org.pashri.soundcheck.playback

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaKeysTest {
    private fun down(keyCode: Int): MediaKeyAction =
        mediaKeyAction(keyCode = keyCode, action = KeyEvent.ACTION_DOWN, repeatCount = 0)

    @Test
    fun `play and pause keys count as presses, like the headset button`() {
        val keys = listOf(
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
        )
        keys.forEach { assertEquals("key $it", MediaKeyAction.PRESS, down(it)) }
    }

    @Test
    fun `skip keys from a car or headset act at once`() {
        assertEquals(MediaKeyAction.NEXT, down(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(MediaKeyAction.NEXT, down(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD))
        assertEquals(MediaKeyAction.PREVIOUS, down(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        assertEquals(MediaKeyAction.PREVIOUS, down(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD))
    }

    @Test
    fun `key-up events and held-button repeats are consumed, not counted`() {
        val hook = KeyEvent.KEYCODE_HEADSETHOOK
        assertEquals(
            MediaKeyAction.CONSUME,
            mediaKeyAction(keyCode = hook, action = KeyEvent.ACTION_UP, repeatCount = 0),
        )
        assertEquals(
            MediaKeyAction.CONSUME,
            mediaKeyAction(keyCode = hook, action = KeyEvent.ACTION_DOWN, repeatCount = 1),
        )
        assertEquals(
            MediaKeyAction.CONSUME,
            mediaKeyAction(
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
                action = KeyEvent.ACTION_UP,
                repeatCount = 0,
            ),
        )
    }

    @Test
    fun `other keys are left to the system`() {
        assertEquals(MediaKeyAction.IGNORE, down(KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(MediaKeyAction.IGNORE, down(KeyEvent.KEYCODE_MEDIA_STOP))
    }
}
