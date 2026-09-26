package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.music.Pitch

/** The tempo a Pattern is auditioned at: the tempo a new Step gets. */
const val AUDITION_BPM: Int = NEW_STEP_BPM

/**
 * A Step's Demo on its own: the Pattern once in the Step's starting key, as the Programme
 * plays it after the Announcement, with frames counted from its first note. It is cut from
 * the Step's own timeline, so it can never differ from what the Programme plays.
 *
 * @param step the Step.
 * @param range the app's Range, before the Step's Range Offset.
 * @return the Demo's notes in order, or none if the Step doesn't fit.
 */
fun demoNotes(step: Step, range: Range): List<PianoNoteEvent> {
    val timeline = buildStepTimeline(step = step, range = range, announcementFrames = 0L)
        ?: return emptyList()
    val demoStart = msToFrames(ANNOUNCEMENT_GAP_MS)
    return timeline.events
        .filterIsInstance<PianoNoteEvent>()
        .filter { it.part == PianoPart.DEMO }
        .map { it.copy(startFrame = it.startFrame - demoStart) }
}

/**
 * The key a Pattern is auditioned in: the lowest key of [range] when it fits there, so it
 * sits in your voice; otherwise as near middle C as the piano's keys allow.
 *
 * @param span the Pattern's sung span.
 * @param range the app's Range.
 * @return the key, or null if no key keeps every note on the piano.
 */
fun auditionKey(span: SungSpan, range: Range): Pitch? {
    val trip = planRoundTrip(
        range = range,
        offset = RangeOffset.NONE,
        span = span,
        direction = Direction.START_LOW,
    )
    if (trip is RoundTrip.Fits) return trip.startKey
    val lowest = maxOf(Range.PIANO.lowest.midi - span.lowest, Pitch.MIDI_NOTES.first)
    val highest = minOf(Range.PIANO.highest.midi - span.highest, Pitch.MIDI_NOTES.last)
    return if (lowest <= highest) {
        Pitch(MIDDLE_C.coerceIn(minimumValue = lowest, maximumValue = highest))
    } else {
        null
    }
}

/**
 * A Pattern played once in its [auditionKey] at [AUDITION_BPM], built as the Demo of a
 * Step whose Range is exactly as wide as the Pattern, so it shares the Programme's timing.
 *
 * @param pattern the Pattern.
 * @param range the app's Range.
 * @return the notes in order, from frame 0, or none if no key keeps it on the piano.
 */
fun patternDemoNotes(pattern: Pattern, range: Range): List<PianoNoteEvent> {
    val span = pattern.span
    val key = auditionKey(span = span, range = range) ?: return emptyList()
    val step = Step(
        pattern = pattern,
        soundId = AUDITION_SOUND,
        bpm = AUDITION_BPM,
        direction = Direction.START_LOW,
    )
    val exact = Range(lowest = key + span.lowest, highest = key + span.highest)
    return demoNotes(step = step, range = exact)
}

/** A Sound no library holds; an audition has no Announcement. */
private val AUDITION_SOUND = SoundId("audition")

private const val MIDDLE_C = 60
