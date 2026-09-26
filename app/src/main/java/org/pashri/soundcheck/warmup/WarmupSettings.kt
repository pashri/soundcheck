package org.pashri.soundcheck.warmup

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
        require(range.lowest in Range.PIANO && range.highest in Range.PIANO) {
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
