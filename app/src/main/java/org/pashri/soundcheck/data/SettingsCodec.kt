package org.pashri.soundcheck.data

import kotlinx.serialization.Serializable
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings

/** Reads and writes the Warm-up settings as JSON. */
object SettingsCodec : TextCodec<WarmupSettings> {
    /** The format this build writes; a file with any other version is refused. */
    const val VERSION: Int = 1

    override fun encode(value: WarmupSettings): String = DocumentJson.encodeToString(
        SettingsFile.serializer(),
        SettingsFile(
            version = VERSION,
            voiceType = value.voiceType.name,
            lowest = value.range.lowest.midi,
            highest = value.range.highest.midi,
            playOverOtherAudio = value.playOverOtherAudio,
        ),
    )

    override fun decode(text: String): WarmupSettings {
        val file = DocumentJson.decodeFromString(SettingsFile.serializer(), text)
        require(file.version == VERSION) { "Settings format ${file.version} is not $VERSION" }
        return WarmupSettings(
            voiceType = VoiceType.valueOf(file.voiceType),
            range = Range(lowest = Pitch(file.lowest), highest = Pitch(file.highest)),
            playOverOtherAudio = file.playOverOtherAudio,
        )
    }
}

@Serializable
internal data class SettingsFile(
    val version: Int,
    val voiceType: String,
    val lowest: Int,
    val highest: Int,
    val playOverOtherAudio: Boolean,
)
