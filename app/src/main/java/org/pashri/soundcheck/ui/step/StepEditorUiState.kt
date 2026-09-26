package org.pashri.soundcheck.ui.step

import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM
import org.pashri.soundcheck.ui.programme.fitWarning
import org.pashri.soundcheck.ui.programme.signed
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.MAX_RANGE_OFFSET
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.RoundTrip
import org.pashri.soundcheck.warmup.SavedProgramme
import org.pashri.soundcheck.warmup.StepRef

/** What a Sound's card says until Plan 6 records clips: the phone's voice reads it. */
const val PHONE_VOICE: String = "phone voice"

/**
 * Everything the Step editor shows.
 *
 * @property title the Step's Sound, the screen title.
 * @property stepLabel e.g. "STEP 2 OF 5".
 * @property programmeName where back goes.
 * @property patternName e.g. "Arpeggio 8-hold".
 * @property patternDetail its degrees and Key Chord, e.g. "1 3 5 8 8 8 8 5 3 1 · major".
 * @property soundLabel e.g. "mim".
 * @property soundDetail how the Announcement sounds, e.g. "phone voice".
 * @property bpm e.g. "100 bpm".
 * @property canSlower false at 30 bpm.
 * @property canFaster false at 300 bpm.
 * @property direction which end the Step starts from.
 * @property bottom the Range Offset's bottom, e.g. "0" or "+2".
 * @property top the Range Offset's top, e.g. "−3".
 * @property canLowerBottom false at bottom +24, or when the bottom is already at A0.
 * @property canRaiseBottom false at bottom −24.
 * @property canLowerTop false at top −24.
 * @property canRaiseTop false at top +24, or when the top is already at C8.
 * @property tripLabel the effective Range and the Iteration count, e.g.
 *     "C3 – A4 · 19 Iterations"; the Range alone when the Step doesn't fit.
 * @property warning why the Step will be skipped, or null when it fits.
 * @property guideMelody whether the piano plays the Pattern with you.
 * @property canHearDemo whether the Step fits, so it has a Demo to hear.
 */
data class StepEditorUiState(
    val title: String,
    val stepLabel: String,
    val programmeName: String,
    val patternName: String,
    val patternDetail: String,
    val soundLabel: String,
    val soundDetail: String,
    val bpm: String,
    val canSlower: Boolean,
    val canFaster: Boolean,
    val direction: Direction,
    val bottom: String,
    val top: String,
    val canLowerBottom: Boolean,
    val canRaiseBottom: Boolean,
    val canLowerTop: Boolean,
    val canRaiseTop: Boolean,
    val tripLabel: String,
    val warning: String?,
    val guideMelody: Boolean,
    val canHearDemo: Boolean,
)

/** What the Step editor's controls do. */
interface StepEditorActions {
    /** One bpm slower. */
    fun slower()

    /** One bpm faster. */
    fun faster()

    /**
     * Picks the end the Step starts from.
     *
     * @param direction the Direction.
     */
    fun setDirection(direction: Direction)

    /** Adds a half-step below the Range. */
    fun lowerBottom()

    /** Takes a half-step off the bottom of the Range. */
    fun raiseBottom()

    /** Takes a half-step off the top of the Range. */
    fun lowerTop()

    /** Adds a half-step above the Range. */
    fun raiseTop()

    /**
     * Turns the Guide Melody on or off.
     *
     * @param on true for the piano to play the Pattern with you.
     */
    fun setGuideMelody(on: Boolean)

    /** Removes the Step from its Programme; the editor then closes. */
    fun remove()

    /** Plays the Step's Demo, or stops it if it is sounding. */
    fun hearDemo()

    /** Stops the Demo, as the screen goes away. */
    fun stopAudition()
}

/**
 * The Step editor for one Step.
 *
 * @param library the saved library.
 * @param ref the Step.
 * @param range the Range from Settings.
 * @return what to show, or null if the Step or its Programme isn't in the library.
 */
fun stepEditorUiState(library: Library, ref: StepRef, range: Range): StepEditorUiState? {
    val programme = library.programme(ref.programmeId) ?: return null
    val index = programme.steps.indexOfFirst { it.key == ref.key }
    if (index < 0) return null
    return build(library = library, programme = programme, index = index, range = range)
}

private fun build(
    library: Library,
    programme: SavedProgramme,
    index: Int,
    range: Range,
): StepEditorUiState {
    val saved = programme.steps[index]
    val step = library.stepToPlay(saved)
    val offset = saved.rangeOffset
    val trip = step.roundTrip(range)
    val sound = library.sound(saved.soundId)?.label ?: saved.soundId.value
    return StepEditorUiState(
        title = sound,
        stepLabel = "STEP ${index + 1} OF ${programme.steps.size}",
        programmeName = programme.name,
        patternName = step.pattern.name,
        patternDetail = "${PatternNotation.degrees(step.pattern.notes)} · " +
            step.pattern.keyChord.label,
        soundLabel = sound,
        soundDetail = PHONE_VOICE,
        bpm = "${saved.bpm} bpm",
        canSlower = saved.bpm > MIN_BPM,
        canFaster = saved.bpm < MAX_BPM,
        direction = saved.direction,
        bottom = signed(offset.bottom),
        top = signed(offset.top),
        canLowerBottom = offset.bottom < MAX_RANGE_OFFSET &&
            range.lowest.midi - offset.bottom > Range.PIANO.lowest.midi,
        canRaiseBottom = offset.bottom > -MAX_RANGE_OFFSET,
        canLowerTop = offset.top > -MAX_RANGE_OFFSET,
        canRaiseTop = offset.top < MAX_RANGE_OFFSET &&
            range.highest.midi + offset.top < Range.PIANO.highest.midi,
        tripLabel = tripLabel(range = range, offset = offset, trip = trip),
        warning = fitWarning(trip),
        guideMelody = saved.guideMelody,
        canHearDemo = trip is RoundTrip.Fits,
    )
}

private fun tripLabel(range: Range, offset: RangeOffset, trip: RoundTrip): String {
    val effective = range.offsetBy(offset) ?: return "No Range left"
    val notes = "${effective.lowest.name} – ${effective.highest.name}"
    return if (trip is RoundTrip.Fits) "$notes · ${trip.keys.size} Iterations" else notes
}
