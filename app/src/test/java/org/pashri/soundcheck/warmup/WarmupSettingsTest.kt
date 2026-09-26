package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class WarmupSettingsTest {
    private val tenor = WarmupSettings.DEFAULT

    @Test
    fun `picking a Voice Type sets the Range to its preset and keeps the audio setting`() {
        assertEquals(
            WarmupSettings(
                voiceType = VoiceType.SOPRANO,
                range = VoiceType.SOPRANO.range,
                playOverOtherAudio = false,
            ),
            tenor.withVoiceType(VoiceType.SOPRANO),
        )
        val mixing = tenor.copy(playOverOtherAudio = true)
        assertTrue(mixing.withVoiceType(VoiceType.BASS).playOverOtherAudio)
    }

    @Test
    fun `fine-tuning keeps the Voice Type`() {
        val tuned = tenor.withHighest(70)
        assertEquals(VoiceType.TENOR, tuned.voiceType)
        assertEquals(Pitch.parse("B♭4"), tuned.range.highest)
    }

    @Test
    fun `the lowest note stops at A0 and at the highest note`() {
        assertEquals(Pitch.parse("A0"), tenor.withLowest(5).range.lowest)
        assertEquals(Pitch.parse("A4"), tenor.withLowest(80).range.lowest)
    }

    @Test
    fun `the highest note stops at C8 and at the lowest note`() {
        assertEquals(Pitch.parse("C8"), tenor.withHighest(127).range.highest)
        assertEquals(Pitch.parse("C3"), tenor.withHighest(0).range.highest)
    }

    @Test
    fun `a Range reaching past the piano is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            WarmupSettings(
                voiceType = VoiceType.TENOR,
                range = Range(lowest = Pitch(20), highest = Pitch(60)),
                playOverOtherAudio = false,
            )
        }
    }
}
