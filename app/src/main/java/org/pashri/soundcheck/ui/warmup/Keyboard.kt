package org.pashri.soundcheck.ui.warmup

import org.pashri.soundcheck.music.HALF_STEPS_PER_OCTAVE
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.SungSpan

/** What a key on the playing screen's keyboard shows. */
enum class KeyMark {
    /** Nothing: outside the notes this Iteration sings. */
    PLAIN,

    /** Between the lowest and highest sung notes. */
    SUNG,

    /** The Iteration's key, in vermilion. */
    ROOT,

    /** The Pattern's highest sung note in this key. */
    TOP,
}

/**
 * A white key.
 *
 * @property pitch the key's note.
 * @property mark what it shows.
 */
data class WhiteKey(val pitch: Pitch, val mark: KeyMark)

/**
 * A black key, drawn over the line between two white keys.
 *
 * @property pitch the key's note.
 * @property afterWhite the index of the white key to its left.
 * @property mark what it shows.
 */
data class BlackKey(val pitch: Pitch, val afterWhite: Int, val mark: KeyMark)

/**
 * The keyboard under the key on the playing screen.
 *
 * @property whites the white keys, low to high.
 * @property blacks the black keys, low to high.
 * @property description what TalkBack reads, e.g. "Keyboard C3 to A4, key E♭3, top note
 *     E♭4". It doesn't name [pressed], so TalkBack doesn't speak up on every note.
 * @property notes each Pattern note's pitch in the Iteration's key, in order.
 * @property pressed the key of the note being sung, drawn pressed down, or null.
 */
data class KeyboardView(
    val whites: List<WhiteKey>,
    val blacks: List<BlackKey>,
    val description: String,
    val notes: List<Pitch> = emptyList(),
    val pressed: Pitch? = null,
)

/**
 * This keyboard with the key of Pattern note [note] pressed. The keys, the notes and the
 * description stay the same instances, so a note change is cheap.
 *
 * @param note the index of the Pattern note being sung, or null; an index outside the
 *     Pattern (a moment's lag at a Step change) presses nothing.
 * @return the keyboard with [note]'s key pressed.
 */
fun KeyboardView.pressing(note: Int?): KeyboardView =
    copy(pressed = note?.let { notes.getOrNull(it) })

/**
 * A keyboard over [range], starting and ending on white keys, with the Iteration's key, its
 * top sung note and the keys between marked.
 *
 * @param range the Range the Step sings in, with its Range Offset applied.
 * @param key the Iteration's key.
 * @param span the Pattern's sung span.
 * @param topName the top note's name as the staff spells it (e.g. "C♯4" in F♯), so what
 *     TalkBack reads agrees with the staff; null names it as the app names keys ("D♭4").
 * @param notes each Pattern note's pitch in [key] (from [Pattern.pitchesIn], as the staff
 *     places them), for [pressing].
 * @return the keys and what TalkBack reads, with nothing pressed.
 */
fun keyboardView(
    range: Range,
    key: Pitch,
    span: SungSpan,
    topName: String? = null,
    notes: List<Pitch> = emptyList(),
): KeyboardView {
    val low = if (isBlackKey(range.lowest)) Pitch(range.lowest.midi - 1) else range.lowest
    val high = if (isBlackKey(range.highest)) Pitch(range.highest.midi + 1) else range.highest
    val top = key + span.highest
    val marks = KeyMarks(root = key, top = top, sung = (key + span.lowest)..top)
    val pitches = (low.midi..high.midi).map(::Pitch)
    val whites = pitches.filterNot(::isBlackKey).map { WhiteKey(pitch = it, mark = marks.of(it)) }
    val blacks = pitches.filter(::isBlackKey).map { black ->
        BlackKey(
            pitch = black,
            afterWhite = whites.indexOfLast { it.pitch < black },
            mark = marks.of(black),
        )
    }
    val ends = "Keyboard ${low.name} to ${high.name}"
    return KeyboardView(
        whites = whites,
        blacks = blacks,
        description = "$ends, key ${key.name}, top note ${topName ?: top.name}",
        notes = notes,
    )
}

/**
 * Whether a note is a black key on the piano.
 *
 * @param pitch the note.
 * @return true for D♭, E♭, F♯, A♭ and B♭ in any octave.
 */
fun isBlackKey(pitch: Pitch): Boolean = pitch.midi % HALF_STEPS_PER_OCTAVE in BLACK_PITCH_CLASSES

/** Decides each key's mark; the key wins over the top note for a one-note Pattern. */
private class KeyMarks(val root: Pitch, val top: Pitch, val sung: ClosedRange<Pitch>) {
    fun of(pitch: Pitch): KeyMark = when {
        pitch == root -> KeyMark.ROOT
        pitch == top -> KeyMark.TOP
        pitch in sung -> KeyMark.SUNG
        else -> KeyMark.PLAIN
    }
}

private val BLACK_PITCH_CLASSES = setOf(1, 3, 6, 8, 10)
