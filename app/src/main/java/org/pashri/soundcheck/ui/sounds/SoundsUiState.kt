package org.pashri.soundcheck.ui.sounds

import org.pashri.soundcheck.ui.components.PHONE_VOICE
import org.pashri.soundcheck.ui.components.clipLengthLabel
import org.pashri.soundcheck.ui.components.spokenClipLength
import org.pashri.soundcheck.ui.components.usageLabel
import org.pashri.soundcheck.ui.components.usageText
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.soundUsage

/**
 * One Sound in the Sounds list.
 *
 * @property id the Sound.
 * @property label e.g. "lip trill".
 * @property detail how its Announcement sounds: its recording's length, e.g. "0.6 s", or
 *     "phone voice".
 * @property spokenDetail [detail] as TalkBack says it, e.g. "your recording, 0.6 seconds".
 * @property recorded whether it has a recording to play.
 * @property playing whether its recording is playing now.
 * @property usage e.g. "Used in 2 Steps".
 * @property deleteNote what deleting it takes with it.
 * @property chosen whether it is the Step's Sound, when choosing for a Step.
 */
data class SoundRow(
    val id: SoundId,
    val label: String,
    val detail: String,
    val spokenDetail: String,
    val recorded: Boolean,
    val playing: Boolean,
    val usage: String,
    val deleteNote: String,
    val chosen: Boolean,
)

/**
 * Everything the Sounds list shows.
 *
 * @property title "Sounds", or "Choose a Sound" when choosing for a Step.
 * @property backLabel where back goes: "Warm-up", or "Step".
 * @property countLabel e.g. "3 OF 8 RECORDED".
 * @property rows every Sound, in library order.
 * @property picking whether a tap chooses the Sound for a Step.
 * @property canDelete false with only one Sound.
 * @property labels every label, which a new or renamed label must avoid.
 * @property notice a problem to show under the header, e.g. a recording that won't play.
 */
data class SoundsUiState(
    val title: String,
    val backLabel: String,
    val countLabel: String,
    val rows: List<SoundRow>,
    val picking: Boolean,
    val canDelete: Boolean,
    val labels: List<String>,
    val notice: String?,
)

/**
 * The Sounds list.
 *
 * @param library the saved library.
 * @param pickFor the Step to choose a Sound for, or null to browse.
 * @param playing the Sound whose recording is playing, or null.
 * @param notice a problem to show, or null.
 * @return what to show.
 */
fun soundsUiState(
    library: Library,
    pickFor: StepRef?,
    playing: SoundId? = null,
    notice: String? = null,
): SoundsUiState {
    val chosen = pickFor?.let { library.programme(it.programmeId)?.step(it.key)?.soundId }
    val recorded = library.sounds.count { it.clip != null }
    return SoundsUiState(
        title = if (pickFor != null) "Choose a Sound" else "Sounds",
        backLabel = if (pickFor != null) "Step" else "Warm-up",
        countLabel = "$recorded OF ${library.sounds.size} RECORDED",
        rows = library.sounds.map { sound ->
            soundRow(library = library, sound = sound, chosen = chosen, playing = playing)
        },
        picking = pickFor != null,
        canDelete = library.sounds.size > 1,
        labels = library.sounds.map { it.label },
        notice = notice,
    )
}

private fun soundRow(
    library: Library,
    sound: Sound,
    chosen: SoundId?,
    playing: SoundId?,
): SoundRow {
    val usages = library.soundUsage(sound.id)
    val clip = sound.clip
    return SoundRow(
        id = sound.id,
        label = sound.label,
        detail = clip?.let { clipLengthLabel(it.lengthMs) } ?: PHONE_VOICE,
        spokenDetail = clip?.let { "your recording, ${spokenClipLength(it.lengthMs)}" }
            ?: PHONE_VOICE,
        recorded = clip != null,
        playing = clip != null && sound.id == playing,
        usage = usageLabel(usages),
        deleteNote = usageText(usages),
        chosen = sound.id == chosen,
    )
}
