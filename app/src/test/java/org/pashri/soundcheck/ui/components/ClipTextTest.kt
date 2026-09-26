package org.pashri.soundcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.StarterSounds

class ClipTextTest {
    @Test
    fun `a clip's length is rounded to a tenth of a second`() {
        assertEquals("0.6 s", clipLengthLabel(640))
        assertEquals("0.7 s", clipLengthLabel(650))
        assertEquals("0.1 s", clipLengthLabel(50))
        assertEquals("5.0 s", clipLengthLabel(4_950))
        assertEquals("0.6 seconds", spokenClipLength(640))
    }

    @Test
    fun `a Sound's card says whose voice announces it`() {
        val clip = RecordedClip(name = ClipName("mim.wav"), lengthMs = 640)
        val recorded = StarterSounds.MIM.copy(clip = clip)
        assertEquals("your recording · 0.6 s", announcementDetail(recorded))
        assertEquals("phone voice", announcementDetail(StarterSounds.MIM))
        assertEquals("phone voice", announcementDetail(null))
    }
}
