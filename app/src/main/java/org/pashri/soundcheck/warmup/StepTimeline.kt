package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.metronome.BeatGrid
import org.pashri.soundcheck.metronome.framesPerBeat
import org.pashri.soundcheck.music.Pitch

/** Silence between the Announcement and the Demo, so the voice stands apart from the piano. */
const val ANNOUNCEMENT_GAP_MS: Long = 500L

/** Beats the Key Chord rings before each Iteration: one bar of four. */
const val KEY_CHORD_BEATS: Int = 4

/** What a piano note is for. */
enum class PianoPart {
    /** The Pattern played once in the starting key, after the Announcement. */
    DEMO,

    /** A note of the chord that rings for one bar before each Iteration. */
    KEY_CHORD,

    /** A note of the Pattern played along with you during an Iteration. */
    GUIDE_MELODY,
}

/** Something that sounds during a Step, timed in frames at the engine's sample rate. */
sealed interface TimelineEvent {
    /** When it starts, in frames from the start of the Step. */
    val startFrame: Long

    /** How long it lasts, in frames. */
    val lengthFrames: Long

    /** The first frame after it ends. */
    val endFrame: Long
        get() = startFrame + lengthFrames
}

/**
 * The next Step's Sound: its clip, or the phone's voice reading its label.
 *
 * @property soundId which Sound to announce.
 * @property startFrame when it starts, in frames from the start of the Step.
 * @property lengthFrames how long the clip or speech lasts, as the caller measured it.
 */
data class AnnouncementEvent(
    val soundId: SoundId,
    override val startFrame: Long,
    override val lengthFrames: Long,
) : TimelineEvent

/**
 * One piano note.
 *
 * @property pitch the key to play.
 * @property part what the note is for.
 * @property startFrame when it starts, in frames from the start of the Step.
 * @property lengthFrames how long it is held, in frames.
 */
data class PianoNoteEvent(
    val pitch: Pitch,
    val part: PianoPart,
    override val startFrame: Long,
    override val lengthFrames: Long,
) : TimelineEvent

/**
 * Where one Iteration sits in its Step: its Key Chord's first frame to the end of its
 * Pattern.
 *
 * @property index the Iteration's position, from 0.
 * @property key the Iteration's key.
 * @property startFrame the Key Chord's first frame.
 * @property endFrame the first frame after the Pattern's time ends.
 */
data class IterationSpan(
    val index: Int,
    val key: Pitch,
    val startFrame: Long,
    val endFrame: Long,
)

/**
 * Everything one Step plays, in order, with frames counted from the Step's start.
 *
 * @property events the Announcement, then the Demo's notes, then each Iteration's Key Chord
 *     and Guide Melody notes, ordered by start frame.
 * @property iterations each Iteration's position, in play order.
 * @property lengthFrames the Step's total length; the end of the last Iteration.
 */
data class StepTimeline(
    val events: List<TimelineEvent>,
    val iterations: List<IterationSpan>,
    val lengthFrames: Long,
)

/**
 * Lays out a Step: Announcement, a [ANNOUNCEMENT_GAP_MS] gap, the Demo, then one Iteration
 * per round-trip key, each a [KEY_CHORD_BEATS]-beat Key Chord followed by the Pattern.
 *
 * @param step the Step to lay out.
 * @param range the app's Range, before the Step's Range Offset.
 * @param announcementFrames how long the Announcement lasts.
 * @return the timeline, or null if the Pattern doesn't fit and the Step is skipped.
 * @throws IllegalArgumentException if [announcementFrames] is negative.
 */
fun buildStepTimeline(step: Step, range: Range, announcementFrames: Long): StepTimeline? {
    require(announcementFrames >= 0) { "Announcement length $announcementFrames < 0" }
    val trip = step.roundTrip(range) as? RoundTrip.Fits ?: return null
    val demoStart = announcementFrames + msToFrames(ANNOUNCEMENT_GAP_MS)
    val builder = TimelineBuilder(step = step, demoStartFrame = demoStart)
    return builder.timeline(trip = trip, announcementFrames = announcementFrames)
}

private const val EIGHTHS_PER_BEAT = 2

private const val KEY_CHORD_EIGHTHS = KEY_CHORD_BEATS * EIGHTHS_PER_BEAT

/**
 * Places a Step's piano notes. Every frame comes from one grid with a slot per eighth note,
 * anchored at the Demo's first note, so rounding never accumulates across the Step.
 */
private class TimelineBuilder(private val step: Step, demoStartFrame: Long) {
    private val grid = BeatGrid(
        anchorFrame = demoStartFrame,
        anchorIndex = 0,
        framesPerBeat = framesPerBeat(step.bpm) / EIGHTHS_PER_BEAT,
    )
    private val patternEighths = step.pattern.lengthInEighths
    private val iterationEighths = KEY_CHORD_EIGHTHS + patternEighths

    /**
     * Builds the full [StepTimeline] for [trip]: the Announcement, the Demo, then one
     * Iteration per round-trip key.
     *
     * @param trip the Step's Pattern once it fits the Range.
     * @param announcementFrames how long the Announcement lasts.
     */
    fun timeline(trip: RoundTrip.Fits, announcementFrames: Long): StepTimeline {
        val announcement = AnnouncementEvent(
            soundId = step.soundId,
            startFrame = 0L,
            lengthFrames = announcementFrames,
        )
        val iterations = trip.keys.mapIndexed(::iterationSpan)
        val notes = demo(trip.startKey) + trip.keys.flatMapIndexed(::iterationNotes)
        return StepTimeline(
            events = listOf<TimelineEvent>(announcement) + notes,
            iterations = iterations,
            lengthFrames = iterations.last().endFrame,
        )
    }

    private fun demo(key: Pitch): List<PianoNoteEvent> =
        melody(key = key, fromEighth = 0, part = PianoPart.DEMO)

    private fun iterationSpan(index: Int, key: Pitch): IterationSpan {
        val start = iterationStartEighth(index)
        return IterationSpan(
            index = index,
            key = key,
            startFrame = frameAt(start),
            endFrame = frameAt(start + iterationEighths),
        )
    }

    private fun iterationNotes(index: Int, key: Pitch): List<PianoNoteEvent> {
        val start = iterationStartEighth(index)
        val chord = chordNotes(key = key, fromEighth = start)
        val melodyStart = start + KEY_CHORD_EIGHTHS
        val guide = if (step.guideMelody) {
            melody(key = key, fromEighth = melodyStart, part = PianoPart.GUIDE_MELODY)
        } else {
            emptyList()
        }
        return chord + guide
    }

    private fun chordNotes(key: Pitch, fromEighth: Int): List<PianoNoteEvent> =
        step.pattern.keyChord.pitchesOn(key).map {
            note(
                pitch = it,
                fromEighth = fromEighth,
                eighths = KEY_CHORD_EIGHTHS,
                part = PianoPart.KEY_CHORD,
            )
        }

    private fun iterationStartEighth(index: Int): Int = patternEighths + index * iterationEighths

    private fun melody(key: Pitch, fromEighth: Int, part: PianoPart): List<PianoNoteEvent> {
        val notes = step.pattern.notes
        val pitches = step.pattern.pitchesIn(key)
        val starts = notes.runningFold(fromEighth) { at, note -> at + note.length.eighths }
        return pitches.zip(notes.zip(starts)) { pitch, (patternNote, start) ->
            note(
                pitch = pitch,
                fromEighth = start,
                eighths = patternNote.length.eighths,
                part = part,
            )
        }
    }

    private fun note(
        pitch: Pitch,
        fromEighth: Int,
        eighths: Int,
        part: PianoPart,
    ): PianoNoteEvent {
        val start = frameAt(fromEighth)
        return PianoNoteEvent(
            pitch = pitch,
            part = part,
            startFrame = start,
            lengthFrames = frameAt(fromEighth + eighths) - start,
        )
    }

    private fun frameAt(eighth: Int): Long = grid.frameOf(eighth.toLong())
}
