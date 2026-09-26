package org.pashri.soundcheck.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings

class SettingsUiStateTest {
    private fun on(lowest: String, highest: String): SettingsUiState = settingsUiState(
        WarmupSettings(
            voiceType = VoiceType.TENOR,
            range = Range(lowest = Pitch.parse(lowest), highest = Pitch.parse(highest)),
            playOverOtherAudio = false,
        ),
    )

    @Test
    fun `the Tenor preset shows C3 to A4 with every button on`() {
        val state = settingsUiState(WarmupSettings.DEFAULT)
        assertEquals(VoiceType.TENOR, state.voiceType)
        assertEquals("C3", state.lowest)
        assertEquals("A4", state.highest)
        assertTrue(state.canLowerLowest && state.canRaiseLowest)
        assertTrue(state.canLowerHighest && state.canRaiseHighest)
    }

    @Test
    fun `the buttons at an edge are off`() {
        val bottom = on(lowest = "A0", highest = "A0")
        assertFalse(bottom.canLowerLowest)
        assertFalse(bottom.canRaiseLowest)
        assertFalse(bottom.canLowerHighest)
        assertTrue(bottom.canRaiseHighest)
        assertFalse(on(lowest = "C4", highest = "C8").canRaiseHighest)
    }

    @Test
    fun `flats are shown in the note names`() {
        val state = on(lowest = "B♭2", highest = "E♭4")
        assertEquals("B♭2", state.lowest)
        assertEquals("E♭4", state.highest)
    }

    @Test
    fun `the audio note explains what the switch does`() {
        assertEquals(
            "Off: your podcast pauses, and the headphone button controls Soundcheck. " +
                "The headphone button switches straight away; the rest applies from the next " +
                "Start.",
            settingsUiState(WarmupSettings.DEFAULT).audioNote,
        )
        val mixing = settingsUiState(WarmupSettings.DEFAULT.copy(playOverOtherAudio = true))
        assertTrue(mixing.playOverOtherAudio)
        assertEquals(
            "On: your podcast keeps playing under Soundcheck, the headphone button stays with " +
                "it, and a phone call won't pause the Warm-up. The headphone button switches " +
                "straight away; the rest applies from the next Start.",
            mixing.audioNote,
        )
    }

    @Test
    fun `the Range says when a change takes effect`() {
        assertEquals("Applies from the next Start.", RANGE_NOTE)
    }
}
