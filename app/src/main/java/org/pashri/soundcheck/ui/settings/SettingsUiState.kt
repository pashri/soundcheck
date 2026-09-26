package org.pashri.soundcheck.ui.settings

import java.time.LocalDate
import org.pashri.soundcheck.warmup.Library
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
 * @property importQuestion the chosen backup's contents, e.g. "4 programmes · 10 patterns
 *     · 9 sounds", while asking whether to import it; null when not asking.
 * @property backupMessage how the last export or import went, or null.
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
    val importQuestion: String?,
    val backupMessage: String?,
)

/**
 * Where an export or import has got to, as the Settings view model holds it.
 *
 * @property question the chosen backup's contents while asking whether to import it.
 * @property message how the last export or import went.
 */
data class BackupView(val question: String? = null, val message: String? = null)

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

    /**
     * Writes a backup of the library and settings to a file the person picked.
     *
     * @param uri the file's address, from the system's file picker.
     */
    fun exportTo(uri: String)

    /**
     * Reads a backup the person picked and, if it can be imported, asks first.
     *
     * @param uri the file's address, from the system's file picker.
     */
    fun importFrom(uri: String)

    /** Yes: replaces the library and settings with the chosen backup. */
    fun confirmImport()

    /** No: forgets the chosen backup. */
    fun cancelImport()
}

/**
 * The Settings screen for [settings].
 *
 * @param settings the saved settings.
 * @param backup where an export or import has got to.
 * @return what to show.
 */
fun settingsUiState(settings: WarmupSettings, backup: BackupView = BackupView()): SettingsUiState {
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
        importQuestion = backup.question,
        backupMessage = backup.message,
    )
}

/**
 * What a library holds, for a backup's messages.
 *
 * @param library the library.
 * @return e.g. "4 programmes · 10 patterns · 9 sounds".
 */
fun libraryCounts(library: Library): String = listOf(
    counted(count = library.programmes.size, noun = "programme"),
    counted(count = library.patterns.size, noun = "pattern"),
    counted(count = library.sounds.size, noun = "sound"),
).joinToString(separator = " · ")

/**
 * The name the file picker suggests for a backup.
 *
 * @param date today.
 * @return e.g. "soundcheck-2026-09-26.json".
 */
fun backupFileName(date: LocalDate): String = "soundcheck-$date.json"

private fun counted(count: Int, noun: String): String =
    if (count == 1) "1 $noun" else "$count ${noun}s"

/** What the BACKUP section says a backup holds. */
const val BACKUP_NOTE: String =
    "Your Programmes, Patterns, Sounds and settings, in one file you can keep on Drive or " +
        "in Downloads. Recordings stay on the phone."

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
