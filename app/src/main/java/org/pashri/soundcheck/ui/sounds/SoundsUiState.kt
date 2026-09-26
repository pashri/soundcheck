package org.pashri.soundcheck.ui.sounds

import org.pashri.soundcheck.ui.components.usageLabel
import org.pashri.soundcheck.ui.components.usageText
import org.pashri.soundcheck.ui.step.PHONE_VOICE
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
 * @property detail how its Announcement sounds: "phone voice" until Plan 6 records clips.
 * @property usage e.g. "Used in 2 Steps".
 * @property deleteNote what deleting it takes with it.
 * @property chosen whether it is the Step's Sound, when choosing for a Step.
 */
data class SoundRow(
    val id: SoundId,
    val label: String,
    val detail: String,
    val usage: String,
    val deleteNote: String,
    val chosen: Boolean,
)

/**
 * Everything the Sounds list shows.
 *
 * @property title "Sounds", or "Choose a Sound" when choosing for a Step.
 * @property backLabel where back goes: "Warm-up", or "Step".
 * @property countLabel e.g. "8 SOUNDS".
 * @property rows every Sound, in library order.
 * @property picking whether a tap chooses the Sound for a Step.
 * @property canDelete false with only one Sound.
 * @property labels every label, which a new or renamed label must avoid.
 */
data class SoundsUiState(
    val title: String,
    val backLabel: String,
    val countLabel: String,
    val rows: List<SoundRow>,
    val picking: Boolean,
    val canDelete: Boolean,
    val labels: List<String>,
)

/**
 * The Sounds list.
 *
 * @param library the saved library.
 * @param pickFor the Step to choose a Sound for, or null to browse.
 * @return what to show.
 */
fun soundsUiState(library: Library, pickFor: StepRef?): SoundsUiState {
    val chosen = pickFor?.let { library.programme(it.programmeId)?.step(it.key)?.soundId }
    val count = library.sounds.size
    return SoundsUiState(
        title = if (pickFor != null) "Choose a Sound" else "Sounds",
        backLabel = if (pickFor != null) "Step" else "Warm-up",
        countLabel = if (count == 1) "1 SOUND" else "$count SOUNDS",
        rows = library.sounds.map { soundRow(library = library, sound = it, chosen = chosen) },
        picking = pickFor != null,
        canDelete = count > 1,
        labels = library.sounds.map { it.label },
    )
}

private fun soundRow(library: Library, sound: Sound, chosen: SoundId?): SoundRow {
    val usages = library.soundUsage(sound.id)
    return SoundRow(
        id = sound.id,
        label = sound.label,
        detail = PHONE_VOICE,
        usage = usageLabel(usages),
        deleteNote = usageText(usages),
        chosen = sound.id == chosen,
    )
}
