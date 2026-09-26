package org.pashri.soundcheck.ui.settings

import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings

/**
 * Everything the Settings screen shows.
 *
 * @property voiceType the Voice Type picked.
 * @property lowest the lowest note, e.g. "C3".
 * @property highest the highest note, e.g. "A4".
 * @property canLowerLowest false at A0.
 * @property canRaiseLowest false when the lowest note meets the highest.
 * @property canLowerHighest false when the highest note meets the lowest.
 * @property canRaiseHighest false at C8.
 * @property playOverOtherAudio whether the switch is on.
 * @property audioNote what the switch does in its current position.
 */
data class SettingsUiState(
    val voiceType: VoiceType,
    val lowest: String,
    val highest: String,
    val canLowerLowest: Boolean,
    val canRaiseLowest: Boolean,
    val canLowerHighest: Boolean,
    val canRaiseHighest: Boolean,
    val playOverOtherAudio: Boolean,
    val audioNote: String,
)

/** What the Settings screen's controls do. */
interface SettingsActions {
    /**
     * Picks a Voice Type, setting the Range to its preset.
     *
     * @param voiceType the Voice Type.
     */
    fun selectVoiceType(voiceType: VoiceType)

    /** Moves the lowest note down a half-step. */
    fun lowerLowest()

    /** Moves the lowest note up a half-step. */
    fun raiseLowest()

    /** Moves the highest note down a half-step. */
    fun lowerHighest()

    /** Moves the highest note up a half-step. */
    fun raiseHighest()

    /**
     * Turns "Play over other audio" on or off.
     *
     * @param on true to mix with other apps' audio.
     */
    fun setPlayOverOtherAudio(on: Boolean)
}

/**
 * The Settings screen for [settings].
 *
 * @param settings the saved settings.
 * @return what to show.
 */
fun settingsUiState(settings: WarmupSettings): SettingsUiState {
    val range = settings.range
    return SettingsUiState(
        voiceType = settings.voiceType,
        lowest = range.lowest.name,
        highest = range.highest.name,
        canLowerLowest = range.lowest > Range.PIANO.lowest,
        canRaiseLowest = range.lowest < range.highest,
        canLowerHighest = range.highest > range.lowest,
        canRaiseHighest = range.highest < Range.PIANO.highest,
        playOverOtherAudio = settings.playOverOtherAudio,
        audioNote = if (settings.playOverOtherAudio) MIXING_NOTE else PAUSING_NOTE,
    )
}

/** A Programme already playing keeps the settings it started with. */
private const val NEXT_START = "Applies from the next Start."

/** The Range section's supporting text: a Programme already playing keeps its Range. */
const val RANGE_NOTE: String = NEXT_START

/** The headphone button follows the setting at once; focus and mixing wait for a Start. */
private const val AUDIO_TIMING =
    "The headphone button switches straight away; the rest applies from the next Start."

private const val PAUSING_NOTE =
    "Off: your podcast pauses, and the headphone button controls Soundcheck. $AUDIO_TIMING"

private const val MIXING_NOTE =
    "On: your podcast keeps playing under Soundcheck, the headphone button stays with it, " +
        "and a phone call won't pause the Warm-up. $AUDIO_TIMING"
