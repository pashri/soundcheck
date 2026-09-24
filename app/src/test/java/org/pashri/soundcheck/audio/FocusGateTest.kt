package org.pashri.soundcheck.audio

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGateTest {
    @Test
    fun `a permanent or temporary loss stops playback`() {
        assertTrue(focusChangeStopsPlayback(AudioManager.AUDIOFOCUS_LOSS))
        assertTrue(focusChangeStopsPlayback(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
    }

    @Test
    fun `a request to duck under a navigation prompt does not stop playback`() {
        assertFalse(focusChangeStopsPlayback(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
    }

    @Test
    fun `regaining focus does not stop playback`() {
        assertFalse(focusChangeStopsPlayback(AudioManager.AUDIOFOCUS_GAIN))
    }
}
