package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.music.Pitch

/**
 * The Warm-up's settings.
 *
 * @property voiceType the Voice Type last picked; its name stays when the Range is fine-tuned.
 * @property range the one Range every Programme plays through, inside the piano's compass.
 * @property playOverOtherAudio true to mix with other apps' audio instead of pausing them,
 *     leaving the headphone button with the other app.
 * @throws IllegalArgumentException if [range] reaches past A0 or C8.
 */
data class WarmupSettings(
    val voiceType: VoiceType,
    val range: Range,
    val playOverOtherAudio: Boolean,
) {
    init {
        require(value = range.lowest in Range.PIANO && range.highest in Range.PIANO) {
            "Range ${range.lowest} – ${range.highest} reaches past the piano"
        }
    }

    /** The settings on a fresh install. */
    companion object {
        /** Tenor, C3 to A4, pausing other audio. */
        val DEFAULT: WarmupSettings = WarmupSettings(
            voiceType = VoiceType.TENOR,
            range = VoiceType.TENOR.range,
            playOverOtherAudio = false,
        )
    }
}

/**
 * These settings with a Voice Type picked: its preset becomes the Range.
 *
 * @param voiceType the Voice Type.
 * @return the new settings; "Play over other audio" is kept.
 */
fun WarmupSettings.withVoiceType(voiceType: VoiceType): WarmupSettings =
    copy(voiceType = voiceType, range = voiceType.range)

/**
 * These settings with a new lowest note, kept between A0 and the highest note. The Voice
 * Type's name stays.
 *
 * @param midi the lowest note asked for, as a MIDI number.
 * @return the new settings.
 */
fun WarmupSettings.withLowest(midi: Int): WarmupSettings {
    val lowest = midi.coerceIn(
        minimumValue = Range.PIANO.lowest.midi,
        maximumValue = range.highest.midi,
    )
    return copy(range = Range(lowest = Pitch(lowest), highest = range.highest))
}

/**
 * These settings with a new highest note, kept between the lowest note and C8. The Voice
 * Type's name stays.
 *
 * @param midi the highest note asked for, as a MIDI number.
 * @return the new settings.
 */
fun WarmupSettings.withHighest(midi: Int): WarmupSettings {
    val highest = midi.coerceIn(
        minimumValue = range.lowest.midi,
        maximumValue = Range.PIANO.highest.midi,
    )
    return copy(range = Range(lowest = range.lowest, highest = Pitch(highest)))
}
