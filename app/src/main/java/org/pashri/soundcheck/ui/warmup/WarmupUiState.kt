package org.pashri.soundcheck.ui.warmup

import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.Programme
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.RoundTrip
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.firstStep
import org.pashri.soundcheck.warmup.nextStep

/**
 * The key and the progress through a Step's round trip.
 *
 * @property keyLabel the key with its chord, e.g. "E♭ major".
 * @property progressLabel e.g. "4 / 19 ↑", or "– / 19" before the first Iteration.
 * @property count Iterations in the Step.
 * @property now the Iteration sounding, from 0, or null before the first.
 * @property turnIndex the Iteration at the far end of the Range.
 * @property startLabel where the trip starts and which way, e.g. "C3 ↑".
 * @property turnLabel where it turns, e.g. "TURN AT A3".
 * @property endLabel which way it comes back and where it ends, e.g. "↓ C3".
 * @property arrowUp whether the shown arrow points up, or null before the first Iteration.
 */
data class IterationView(
    val keyLabel: String,
    val progressLabel: String,
    val count: Int,
    val now: Int?,
    val turnIndex: Int,
    val startLabel: String,
    val turnLabel: String,
    val endLabel: String,
    val arrowUp: Boolean?,
)

/**
 * Everything the Warm-up screen shows.
 *
 * @property programmeName the Programme's name, the screen title.
 * @property stepNumber the Step's position, from 1.
 * @property stepCount how many Steps the Programme has.
 * @property soundLabel what to sing, e.g. "mim".
 * @property stepDetail the Pattern and Direction, e.g. "on Arpeggio 8-hold, starting low".
 * @property iterations the key and progress, or null if the Step doesn't fit the Range.
 * @property nextSound the next Step's Sound, or null on the last Step.
 * @property nextDetail the next Step's Pattern, e.g. "on Double arpeggio", or null.
 * @property active whether a Programme is playing or paused.
 * @property playing whether it is playing.
 */
data class WarmupUiState(
    val programmeName: String,
    val stepNumber: Int,
    val stepCount: Int,
    val soundLabel: String,
    val stepDetail: String,
    val iterations: IterationView?,
    val nextSound: String?,
    val nextDetail: String?,
    val active: Boolean,
    val playing: Boolean,
) {
    /** The note in the header, e.g. "STEP 3 / 6". */
    val stepLabel: String
        get() = "STEP $stepNumber / $stepCount"

    /** The big button's label: Start, Pause or Resume. */
    val playLabel: String
        get() = when {
            playing -> "Pause"
            active -> "Resume"
            else -> "Start"
        }
}

/** What the Warm-up screen's controls do. */
interface WarmupActions {
    /** Starts the Programme, or pauses or resumes it. */
    fun playPause()

    /** Goes to the next Step. */
    fun next()

    /** Goes to the previous Step. */
    fun previous()

    /** Stops the Programme. */
    fun stop()
}

/**
 * The screen for [playback], or for [programme] ready to start when nothing is playing.
 *
 * @param playback the Programme playing or paused, or null.
 * @param programme the Programme to offer when nothing is playing; it has at least one Step.
 * @param range the Range to offer it on.
 * @param sounds the Sound library, for labels.
 * @return what to show.
 */
fun warmupUiState(
    playback: Playback?,
    programme: Programme,
    range: Range,
    sounds: List<Sound>,
): WarmupUiState {
    val shown = playback?.programme ?: programme
    val shownRange = playback?.range ?: range
    val index = playback?.stepIndex ?: shown.firstStep(shownRange) ?: 0
    val step = shown.steps[index]
    val next = shown.nextStep(index, shownRange)?.let { shown.steps[it] }
    val trip = step.roundTrip(shownRange) as? RoundTrip.Fits
    return WarmupUiState(
        programmeName = shown.name,
        stepNumber = index + 1,
        stepCount = shown.steps.size,
        soundLabel = labelOf(id = step.soundId, sounds = sounds),
        stepDetail = "on ${step.pattern.name}, ${startingText(step.direction)}",
        iterations = trip?.let {
            iterationView(
                trip = it,
                direction = step.direction,
                chord = step.pattern.keyChord,
                now = playback?.iteration,
            )
        },
        nextSound = next?.let { labelOf(id = it.soundId, sounds = sounds) },
        nextDetail = next?.let { "on ${it.pattern.name}" },
        active = playback != null,
        playing = playback?.playing == true,
    )
}

/**
 * The key and progress for Iteration [now] of a round trip.
 *
 * @param trip the Step's keys.
 * @param direction which end the Step starts from.
 * @param chord the Pattern's Key Chord.
 * @param now the Iteration sounding, from 0, or null before the first.
 * @return the view; before the first Iteration the key is the starting key.
 */
fun iterationView(
    trip: RoundTrip.Fits,
    direction: Direction,
    chord: KeyChord,
    now: Int?,
): IterationView {
    val count = trip.keys.size
    val turn = count / 2
    val outward = if (direction == Direction.START_LOW) UP else DOWN
    val homeward = if (direction == Direction.START_LOW) DOWN else UP
    val arrow = if (now != null && now > turn) homeward else outward
    return IterationView(
        keyLabel = keyLabel(key = trip.keys[now ?: 0], chord = chord),
        progressLabel = if (now == null) "– / $count" else "${now + 1} / $count $arrow",
        count = count,
        now = now,
        turnIndex = turn,
        startLabel = "${trip.startKey.name} $outward",
        turnLabel = "TURN AT ${trip.turnKey.name}",
        endLabel = "$homeward ${trip.startKey.name}",
        arrowUp = now?.let { arrow == UP },
    )
}

/**
 * The Iteration count and direction read aloud, for TalkBack.
 *
 * @param view the key and progress shown.
 * @param active whether a Programme is playing or paused.
 * @return "Iteration 4 of 19, going up" (or "going down"), "Demo" during the Demo, or
 *     "not started" before anything plays.
 */
internal fun spokenProgress(view: IterationView, active: Boolean): String {
    val now = view.now
    return when {
        now != null -> "Iteration ${now + 1} of ${view.count}, going ${directionWord(view)}"
        active -> "Demo"
        else -> "not started"
    }
}

/**
 * The round trip's ends and turn read aloud as one sentence.
 *
 * @param view the key and progress shown.
 * @return e.g. "Starts C3, turns at A3, back to C3".
 */
internal fun spokenTurn(view: IterationView): String {
    val start = view.startLabel.substringBefore(" ")
    val turn = view.turnLabel.removePrefix("TURN AT ")
    val end = view.endLabel.substringAfterLast(" ")
    return "Starts $start, turns at $turn, back to $end"
}

private fun directionWord(view: IterationView): String = if (view.arrowUp == true) "up" else "down"

/**
 * A key and its chord as a singer reads them.
 *
 * @param key the Iteration's key.
 * @param chord the Key Chord's quality.
 * @return "E♭ major", "E♭ minor", "E♭7", "E♭maj7", "E♭m7", "E♭dim", "E♭aug", or "E♭" for a
 *     root only.
 */
fun keyLabel(key: Pitch, chord: KeyChord): String = when (chord) {
    KeyChord.MAJOR, KeyChord.MINOR -> "${key.pitchClassName} ${chord.label}"
    KeyChord.ROOT_ONLY -> key.pitchClassName
    else -> "${key.pitchClassName}${chord.label}"
}

private fun labelOf(id: SoundId, sounds: List<Sound>): String =
    sounds.firstOrNull { it.id == id }?.label ?: id.value

private fun startingText(direction: Direction): String =
    if (direction == Direction.START_LOW) "starting low" else "starting high"

private const val UP = "↑"
private const val DOWN = "↓"
