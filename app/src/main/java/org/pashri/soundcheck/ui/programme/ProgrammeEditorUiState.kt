package org.pashri.soundcheck.ui.programme

import org.pashri.soundcheck.ui.warmup.startProblemMessage
import org.pashri.soundcheck.ui.warmup.stepsLabel
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.StepKey

/**
 * One Step in the Programme editor.
 *
 * @property key the Step.
 * @property number its position, from 1.
 * @property sound its Sound's label, e.g. "mim".
 * @property meta its summary, e.g. "Arpeggio 8-hold · 100 bpm · from low".
 * @property warning why it will be skipped, or null when it fits.
 */
data class StepRow(
    val key: StepKey,
    val number: Int,
    val sound: String,
    val meta: String,
    val warning: String?,
)

/**
 * Everything the Programme editor shows.
 *
 * @property name the Programme's name.
 * @property stepsLabel e.g. "5 STEPS".
 * @property rows the Steps, in order.
 * @property canStart false with no Steps.
 * @property startLabel e.g. "Start Morning".
 * @property deleteNote what deleting the Programme takes with it.
 * @property otherNames the other Programmes' names, which a rename must avoid.
 * @property problem why the last Start didn't play, or null.
 */
data class ProgrammeEditorUiState(
    val name: String,
    val stepsLabel: String,
    val rows: List<StepRow>,
    val canStart: Boolean,
    val startLabel: String,
    val deleteNote: String,
    val otherNames: List<String>,
    val problem: String?,
)

/**
 * The Programme editor for one Programme.
 *
 * @param library the saved library.
 * @param id the Programme.
 * @param range the Range from Settings, for the "doesn't fit" warnings.
 * @param problem what the last Start reported, or null.
 * @return what to show, or null if the Programme isn't in the library.
 */
fun programmeEditorUiState(
    library: Library,
    id: ProgrammeId,
    range: Range,
    problem: StartOutcome?,
): ProgrammeEditorUiState? {
    val programme = library.programme(id) ?: return null
    return ProgrammeEditorUiState(
        name = programme.name,
        stepsLabel = stepsLabel(programme.steps.size).uppercase(),
        rows = programme.steps.mapIndexed { index, step ->
            stepRow(library = library, step = step, number = index + 1, range = range)
        },
        canStart = programme.steps.isNotEmpty(),
        startLabel = "Start ${programme.name}",
        deleteNote = deleteNote(programme.steps.size),
        otherNames = library.programmes.filter { it.id != id }.map { it.name },
        problem = problem?.let(::startProblemMessage),
    )
}

private fun stepRow(library: Library, step: SavedStep, number: Int, range: Range): StepRow {
    val playable = library.stepToPlay(step)
    return StepRow(
        key = step.key,
        number = number,
        sound = library.sound(step.soundId)?.label ?: step.soundId.value,
        meta = stepMeta(playable),
        warning = fitWarning(playable.roundTrip(range)),
    )
}

private fun deleteNote(steps: Int): String {
    val stays = "Patterns and Sounds stay in the library."
    return when (steps) {
        0 -> stays
        1 -> "Its 1 Step goes with it. $stays"
        else -> "Its $steps Steps go with it. $stays"
    }
}
