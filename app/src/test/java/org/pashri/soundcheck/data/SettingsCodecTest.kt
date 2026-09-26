package org.pashri.soundcheck.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings

class SettingsCodecTest {
    private val pinned =
        """{"version":1,"voiceType":"BASS","lowest":38,"highest":64,"playOverOtherAudio":true}"""

    private fun refuses(text: String) {
        assertThrows(IllegalArgumentException::class.java) { SettingsCodec.decode(text) }
    }

    @Test
    fun `the default settings are Tenor, C3 to A4, not playing over other audio`() {
        val default = WarmupSettings.DEFAULT
        assertEquals(VoiceType.TENOR, default.voiceType)
        assertEquals(VoiceType.TENOR.range, default.range)
        assertEquals(false, default.playOverOtherAudio)
        assertEquals(default, SettingsCodec.decode(SettingsCodec.encode(default)))
    }

    @Test
    fun `the file format is pinned`() {
        val expected = WarmupSettings(
            voiceType = VoiceType.BASS,
            range = Range(lowest = Pitch(38), highest = Pitch(64)),
            playOverOtherAudio = true,
        )
        assertEquals(expected, SettingsCodec.decode(pinned))
        assertEquals(
            Json.parseToJsonElement(pinned),
            Json.parseToJsonElement(SettingsCodec.encode(expected)),
        )
    }

    @Test
    fun `a Range beyond the piano is refused`() {
        refuses(pinned.replace("\"lowest\":38", "\"lowest\":20"))
        refuses(pinned.replace("\"highest\":64", "\"highest\":109"))
    }

    @Test
    fun `an upside-down Range is refused`() {
        refuses(pinned.replace("\"lowest\":38", "\"lowest\":65"))
    }

    @Test
    fun `an unknown Voice Type is refused`() {
        refuses(pinned.replace("BASS", "BARITONE"))
    }

    @Test
    fun `a file from another version is refused`() {
        refuses(pinned.replace("\"version\":1", "\"version\":2"))
    }
}
