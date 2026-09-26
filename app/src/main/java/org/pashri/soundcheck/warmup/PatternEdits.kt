package org.pashri.soundcheck.warmup

/** A new Pattern's notes: up and down the first three notes, ending on a half note. */
private const val NEW_PATTERN_NOTES = "1q 2q 3q 2q 1h"

/**
 * A Pattern to start editing from: "1 2 3 2 1" in quarters with a half note to
 * end, on a major Key Chord.
 *
 * @param id the new Pattern's id.
 * @param name its name.
 * @return the Pattern.
 */
fun newPattern(id: PatternId, name: String): Pattern = Pattern(
    id = id,
    name = name,
    notes = PatternNotation.parse(NEW_PATTERN_NOTES),
    keyChord = KeyChord.MAJOR,
)

/**
 * This Pattern with one note changed.
 *
 * @param index the note's position, from 0.
 * @param change turns the note into its new version.
 * @return the changed Pattern; unchanged if [index] is outside it.
 */
fun Pattern.withNote(index: Int, change: (PatternNote) -> PatternNote): Pattern {
    if (index !in notes.indices) return this
    return copy(
        notes = notes.mapIndexed { i, note -> if (i == index) change(note) else note }
    )
}

/**
 * This Pattern with a copy of one note inserted straight after it.
 *
 * @param index the note's position, from 0.
 * @return the longer Pattern; unchanged if [index] is outside it.
 */
fun Pattern.withNoteCopiedAfter(index: Int): Pattern {
    val note = notes.getOrNull(index) ?: return this
    return copy(notes = notes.take(index + 1) + note + notes.drop(index + 1))
}

/**
 * This Pattern without one note. A Pattern keeps at least one note.
 *
 * @param index the note's position, from 0.
 * @return the shorter Pattern; unchanged if [index] is outside it or it is
 *   the only note.
 */
fun Pattern.withoutNote(index: Int): Pattern {
    if (notes.size <= 1 || index !in notes.indices) return this
    return copy(notes = notes.filterIndexed { i, _ -> i != index })
}

/**
 * This note one scale degree higher.
 *
 * @return the note, or itself at [MAX_DEGREE].
 */
fun PatternNote.raised(): PatternNote = copy(degree = minOf(degree + 1, MAX_DEGREE))

/**
 * This note one scale degree lower.
 *
 * @return the note, or itself at degree 1.
 */
fun PatternNote.lowered(): PatternNote = copy(degree = maxOf(degree - 1, 1))

/**
 * The Voice Types whose preset Range is wide enough for a Pattern with no
 * Range Offset.
 *
 * @param span the Pattern's sung span.
 * @return the fitting Voice Types, in [VoiceType] order.
 */
fun voiceTypesFitting(span: SungSpan): List<VoiceType> = VoiceType.entries.filter {
    val trip = planRoundTrip(
        range = it.range,
        offset = RangeOffset.NONE,
        span = span,
        direction = Direction.START_LOW,
    )
    trip is RoundTrip.Fits
}
