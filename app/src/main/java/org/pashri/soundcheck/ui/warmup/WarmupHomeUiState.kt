package org.pashri.soundcheck.ui.warmup

import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.SavedProgramme
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.WarmupSettings

/** What a new Programme is called, before "New programme 2" and so on. */
const val NEW_PROGRAMME_NAME: String = "New programme"

/**
 * One Programme on the Warm-up home.
 *
 * @property id the Programme.
 * @property name its name, e.g. "Morning".
 * @property sounds its Sounds in Step order, each once, e.g. "lip trill, mim"; empty with no
 *     Steps.
 * @property stepsLabel e.g. "5 steps", "1 step" or "No steps yet".
 * @property canStart false with no Steps.
 * @property problem why the last Start of this Programme didn't play, or null.
 */
data class ProgrammeCard(
    val id: ProgrammeId,
    val name: String,
    val sounds: String,
    val stepsLabel: String,
    val canStart: Boolean,
    val problem: String?,
)

/**
 * The card for the Programme playing or paused.
 *
 * @property label "NOW PLAYING" or "PAUSED".
 * @property title e.g. "Morning · Step 2 of 5".
 */
data class NowPlayingCard(val label: String, val title: String)

/**
 * Everything the Warm-up home shows.
 *
 * @property rangeLabel e.g. "Tenor · C3 – A4".
 * @property nowPlaying the loaded Programme, or null.
 * @property programmes every Programme, in library order.
 * @property patternCount how many Patterns the library holds.
 * @property soundCount how many Sounds it holds.
 * @property saveProblem what to say when the last change couldn't be saved, or null.
 * @property restoredNotice what to say when a saved document couldn't be read and the
 *     starter kit or the defaults were loaded in its place, or null.
 */
data class WarmupHomeUiState(
    val rangeLabel: String,
    val nowPlaying: NowPlayingCard?,
    val programmes: List<ProgrammeCard>,
    val patternCount: Int,
    val soundCount: Int,
    val saveProblem: String?,
    val restoredNotice: String? = null,
)

/**
 * A Start that didn't play.
 *
 * @property programmeId the Programme it was for.
 * @property outcome what happened instead.
 */
data class StartProblem(val programmeId: ProgrammeId, val outcome: StartOutcome)

/**
 * The Warm-up home for the library, the settings and what is playing.
 *
 * @param library the saved library.
 * @param settings the saved settings.
 * @param playback the loaded Programme, or null.
 * @param problem the last Start that didn't play, or null.
 * @param saveFailed whether the last change couldn't be saved.
 * @param restoredNotice what to say when a saved document was set aside and replaced, or
 *     null.
 * @return what to show.
 */
fun warmupHomeUiState(
    library: Library,
    settings: WarmupSettings,
    playback: Playback?,
    problem: StartProblem?,
    saveFailed: Boolean,
    restoredNotice: String? = null,
): WarmupHomeUiState = WarmupHomeUiState(
    rangeLabel = rangeLabel(settings),
    nowPlaying = playback?.let(::nowPlayingCard),
    programmes = library.programmes.map { programme ->
        programmeCard(
            programme = programme,
            library = library,
            problem = problem?.takeIf { it.programmeId == programme.id }?.outcome,
        )
    },
    patternCount = library.patterns.size,
    soundCount = library.sounds.size,
    saveProblem = if (saveFailed) SAVE_PROBLEM else null,
    restoredNotice = restoredNotice,
)

/**
 * The Range as the home and Settings name it.
 *
 * @param settings the saved settings.
 * @return e.g. "Tenor · C3 – A4".
 */
fun rangeLabel(settings: WarmupSettings): String =
    "${settings.voiceType.label} · ${settings.range.lowest.name} – ${settings.range.highest.name}"

/**
 * Why a Start didn't play, in words.
 *
 * @param outcome what [org.pashri.soundcheck.warmup.WarmupController.play] reported.
 * @return the explanation, or null for [StartOutcome.PLAYING].
 */
fun startProblemMessage(outcome: StartOutcome): String? = when (outcome) {
    StartOutcome.PLAYING -> null
    StartOutcome.NOTHING_FITS -> "No Step fits your Range, so there's nothing to play."
    StartOutcome.AUDIO_BUSY -> "Another app is holding on to the sound. Try again in a moment."
    StartOutcome.OUTPUT_FAILED ->
        "The sound wouldn't start. Open it from Paused and press Resume to try again."
}

/**
 * A Step count in words.
 *
 * @param count how many Steps.
 * @return "No steps yet", "1 step" or "5 steps".
 */
fun stepsLabel(count: Int): String = when (count) {
    0 -> "No steps yet"
    1 -> "1 step"
    else -> "$count steps"
}

/**
 * What to say when the saved library couldn't be read and the starter kit was loaded in its
 * place.
 */
const val LIBRARY_RESTORED_NOTICE: String =
    "Your saved library couldn't be read. It was kept as a backup and the starter kit was " +
        "loaded."

/**
 * What to say when the saved settings couldn't be read and the defaults were loaded in their
 * place.
 */
const val SETTINGS_RESTORED_NOTICE: String =
    "Your saved settings couldn't be read. They were kept as a backup and the defaults were " +
        "loaded."

private fun programmeCard(
    programme: SavedProgramme,
    library: Library,
    problem: StartOutcome?,
): ProgrammeCard = ProgrammeCard(
    id = programme.id,
    name = programme.name,
    sounds = programme.steps
        .map { library.sound(it.soundId)?.label ?: it.soundId.value }
        .distinct()
        .joinToString(separator = ", "),
    stepsLabel = stepsLabel(programme.steps.size),
    canStart = programme.steps.isNotEmpty(),
    problem = problem?.let(::startProblemMessage),
)

private fun nowPlayingCard(playback: Playback): NowPlayingCard = NowPlayingCard(
    label = if (playback.playing) "NOW PLAYING" else "PAUSED",
    title = "${playback.programme.name} · Step ${playback.stepIndex + 1} of " +
        "${playback.programme.steps.size}",
)

private const val SAVE_PROBLEM = "Couldn't save your last change. Is the phone's storage full?"
