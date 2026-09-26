package org.pashri.soundcheck.data

import kotlinx.serialization.Serializable
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings

/**
 * Reads and writes the Warm-up settings as JSON.
 *
 * [decode] requires the file's `version` to equal [VERSION] exactly, and unknown keys are
 * not ignored (an added or renamed field fails to parse). Adding a field therefore means a
 * new [VERSION] plus a migration from the previous version's format.
 */
object SettingsCodec : TextCodec<WarmupSettings> {
    /** The format this build writes; a file with any other version is refused. */
    const val VERSION: Int = 1

    override fun encode(value: WarmupSettings): String = DocumentJson.encodeToString(
        serializer = SettingsFile.serializer(),
        value = value.toFile(),
    )

    override fun decode(text: String): WarmupSettings = DocumentJson.decodeFromString(
        deserializer = SettingsFile.serializer(),
        string = text,
    ).readSettings()
}

/**
 * These settings as the record [SettingsCodec] writes.
 *
 * @return the record, at [SettingsCodec.VERSION].
 */
internal fun WarmupSettings.toFile(): SettingsFile = SettingsFile(
    version = SettingsCodec.VERSION,
    voiceType = voiceType.name,
    lowest = range.lowest.midi,
    highest = range.highest.midi,
    playOverOtherAudio = playOverOtherAudio,
)

/**
 * The settings this record holds, checked as [SettingsCodec.decode] checks a whole file.
 *
 * @return the settings.
 * @throws IllegalArgumentException if the version isn't [SettingsCodec.VERSION], the Voice
 *     Type is unknown, or the Range is broken or reaches past the piano.
 */
internal fun SettingsFile.readSettings(): WarmupSettings {
    require(value = version == SettingsCodec.VERSION) {
        "Settings format $version is not ${SettingsCodec.VERSION}"
    }
    return WarmupSettings(
        voiceType = VoiceType.valueOf(voiceType),
        range = Range(lowest = Pitch(lowest), highest = Pitch(highest)),
        playOverOtherAudio = playOverOtherAudio,
    )
}

@Serializable
internal data class SettingsFile(
    val version: Int,
    val voiceType: String,
    val lowest: Int,
    val highest: Int,
    val playOverOtherAudio: Boolean,
)
