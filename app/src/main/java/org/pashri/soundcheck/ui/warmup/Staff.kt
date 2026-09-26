package org.pashri.soundcheck.ui.warmup

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.music.HALF_STEPS_PER_OCTAVE
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.NoteLength
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.Range

/**
 * The clef the staff is written in, chosen from the singer's Range by [clefFor].
 *
 * @property bottomLine the note on the staff's lowest line, as sung: E4 for treble, E3 for
 *     treble an octave lower (written like treble, sung an octave down), G2 for bass.
 * @property spoken what TalkBack says for it.
 * @property top the highest step the clef's glyph reaches, the lowest line being step 0.
 * @property bottom the lowest step the glyph reaches, its 8 included.
 */
enum class Clef(val bottomLine: Pitch, val spoken: String, val top: Int, val bottom: Int) {
    /** Treble clef, for a Range whose middle is middle C or above. */
    TREBLE(bottomLine = Pitch(64), spoken = "treble clef", top = 12, bottom = -4),

    /** Treble clef with an 8 below: written an octave above where it is sung, as tenors read. */
    TREBLE_8VB(
        bottomLine = Pitch(52),
        spoken = "treble clef, an octave lower",
        top = 12,
        bottom = -7,
    ),

    /** Bass clef, for a Range whose middle is below G3. */
    BASS(bottomLine = Pitch(43), spoken = "bass clef", top = 9, bottom = 0),
}

/**
 * The clef for a singer's Range, by the note in its middle: treble from middle C (C4) up,
 * treble an octave lower from G3 to just under middle C, and bass below G3. The Voice Types
 * get the clefs their singers read: Soprano and Alto treble, Tenor treble an octave lower,
 * Bass bass.
 *
 * @param range the Range from Settings, without any Step's Range Offset.
 * @return the clef.
 */
fun clefFor(range: Range): Clef {
    val middleTwice = range.lowest.midi + range.highest.midi
    return when {
        middleTwice >= 2 * MIDDLE_C -> Clef.TREBLE
        middleTwice >= 2 * LOW_G -> Clef.TREBLE_8VB
        else -> Clef.BASS
    }
}

/** How a note head is drawn. */
enum class NoteHead {
    /** Filled in: an eighth or a quarter. */
    FILLED,

    /** Open: a half or a whole. */
    HOLLOW,
}

/**
 * One Pattern note placed on the staff.
 *
 * @property step its height in staff steps above the lowest line: each line and each space
 *     is one step, so the lines are steps 0, 2, 4, 6 and 8.
 * @property head filled or open.
 * @property stemTop the step the stem rises to, or null for a whole note, which has none.
 * @property flag whether the stem carries a flag: an eighth not beamed to a neighbour.
 * @property accidental the ♭ or ♯ written before the head, from the note's real pitch.
 * @property ledgers the steps of the short lines the note needs above or below the staff.
 */
data class StaffNote(
    val step: Int,
    val head: NoteHead,
    val stemTop: Int?,
    val flag: Boolean,
    val accidental: Accidental,
    val ledgers: List<Int>,
)

/**
 * Eighths joined by one beam.
 *
 * @property first the first note's index in the Pattern.
 * @property last the last note's index, after [first].
 */
data class StaffBeam(val first: Int, val last: Int)

/**
 * A Pattern on a five-line staff in one key, as the Warm-up design draws it, with a clef and
 * no key signature, so every black-key note carries its accidental. Every stem points up, as
 * in the design.
 *
 * @property clef the clef.
 * @property notes each Pattern note, in order.
 * @property beams the groups of eighths joined by a beam.
 * @property top the highest step anything reaches: stems, beams, heads and the clef.
 * @property bottom the lowest step anything reaches.
 */
data class StaffLayout(
    val clef: Clef,
    val notes: List<StaffNote>,
    val beams: List<StaffBeam>,
    val top: Int,
    val bottom: Int,
)

/**
 * What the playing screen's staff shows.
 *
 * @property layout where every mark goes.
 * @property now the index of the note being sung, drawn in vermilion, or null.
 * @property description what TalkBack reads, e.g. "Pattern on a staff, bass clef: 1 3 5 8".
 */
data class StaffView(val layout: StaffLayout, val now: Int?, val description: String)

/** How far a stem rises above its note head, in steps: three and a half spaces. */
const val STEM_STEPS: Int = 7

/** Steps from the lowest staff line to the highest. */
const val STAFF_SPAN: Int = 8

/** Eighths are beamed within each half bar: two beats, four eighths. */
const val BEAM_EIGHTHS: Int = 4

/** The widest the space between two staff lines gets, as in the design. */
val STAFF_GAP: Dp = 10.dp

/** Each note gets at least this many staff spaces of width, so heads never touch. */
const val GAPS_PER_NOTE: Float = 2.6f

/** The width the clef takes at the start of the staff, in staff spaces. */
const val CLEF_GAPS: Float = 3.5f

/**
 * Lays out [pattern] in the key of [key] on a staff in [clef], each note at its real pitch.
 *
 * @param pattern the Pattern.
 * @param key the Iteration's key (the starting key for the Demo).
 * @param clef the clef, from [clefFor].
 * @return where every head, stem, beam, flag, ledger line and accidental goes.
 * @throws IllegalArgumentException if a note would fall outside MIDI 0–127; a key from the
 *     Step's round trip never does.
 */
fun staffLayout(pattern: Pattern, key: Pitch, clef: Clef): StaffLayout {
    val pitches = pattern.pitchesIn(key)
    val steps = pitches.map { stepOf(pitch = it, clef = clef) }
    val beams = beamsOf(pattern)
    val notes = pattern.notes.indices.map { index ->
        val beam = beams.firstOrNull { index in it.first..it.last }
        staffNote(
            length = pattern.notes[index].length,
            pitch = pitches[index],
            step = steps[index],
            stemFrom = beam?.let { (it.first..it.last).maxOf { at -> steps[at] } },
        )
    }
    val highest = notes.maxOf { it.stemTop ?: (it.step + 1) }
    return StaffLayout(
        clef = clef,
        notes = notes,
        beams = beams,
        top = maxOf(a = maxOf(a = STAFF_SPAN, b = clef.top), b = highest),
        bottom = minOf(a = clef.bottom, b = steps.min() - 1),
    )
}

/**
 * The staff for the playing screen.
 *
 * @param pattern the Step's Pattern.
 * @param key the Iteration's key.
 * @param clef the clef, from [clefFor].
 * @param now the index of the note being sung, or null; an index outside the Pattern (a
 *     moment's lag at a Step change) lights nothing.
 * @return the layout, the lit note and what TalkBack reads.
 */
fun staffView(pattern: Pattern, key: Pitch, clef: Clef, now: Int?): StaffView = StaffView(
    layout = staffLayout(pattern = pattern, key = key, clef = clef),
    now = now?.takeIf { it in pattern.notes.indices },
    description = "Pattern on a staff, ${clef.spoken}: " +
        PatternNotation.degrees(notes = pattern.notes),
)

/**
 * The space between two staff lines for a drawing [width] wide: the design's [STAFF_GAP],
 * or less for a long Pattern, so the clef keeps [CLEF_GAPS] spaces and each note
 * [GAPS_PER_NOTE].
 *
 * @param width the drawing's width.
 * @param notes how many notes the Pattern has; at least 1.
 * @return the gap.
 */
fun staffGap(width: Dp, notes: Int): Dp =
    minOf(a = STAFF_GAP, b = width / (notes * GAPS_PER_NOTE + CLEF_GAPS))

private fun staffNote(length: NoteLength, pitch: Pitch, step: Int, stemFrom: Int?): StaffNote =
    StaffNote(
        step = step,
        head = if (length.eighths >= NoteLength.HALF.eighths) NoteHead.HOLLOW else NoteHead.FILLED,
        stemTop = if (length == NoteLength.WHOLE) null else (stemFrom ?: step) + STEM_STEPS,
        flag = length == NoteLength.EIGHTH && stemFrom == null,
        accidental = ACCIDENTAL_OF[pitch.midi % HALF_STEPS_PER_OCTAVE],
        ledgers = ledgersFor(at = step),
    )

/** How many letters [pitch] is above [clef]'s lowest line. */
private fun stepOf(pitch: Pitch, clef: Clef): Int = letterOf(pitch) - letterOf(clef.bottomLine)

/** A pitch's letter counted up from C in MIDI's lowest octave: seven letters an octave. */
private fun letterOf(pitch: Pitch): Int =
    pitch.midi / HALF_STEPS_PER_OCTAVE * LETTERS_PER_OCTAVE +
        LETTER_OF[pitch.midi % HALF_STEPS_PER_OCTAVE]

/** The ledger lines a note at step [at] passes, from the staff outwards. */
private fun ledgersFor(at: Int): List<Int> = when {
    at < 0 -> (-2 downTo at step 2).toList()
    at > STAFF_SPAN -> (STAFF_SPAN + 2..at step 2).toList()
    else -> emptyList()
}

/**
 * Groups of two or more eighths in a row that share a half bar, counted from the Pattern's
 * start.
 */
private fun beamsOf(pattern: Pattern): List<StaffBeam> {
    val starts = pattern.notes.runningFold(initial = 0) { at, note -> at + note.length.eighths }
    val runs = mutableListOf<MutableList<Int>>()
    pattern.notes.forEachIndexed { index, note ->
        if (note.length != NoteLength.EIGHTH) return@forEachIndexed
        val run = runs.lastOrNull()?.takeIf {
            it.last() == index - 1 &&
                starts[it.last()] / BEAM_EIGHTHS == starts[index] / BEAM_EIGHTHS
        }
        if (run != null) run.add(index) else runs.add(mutableListOf(index))
    }
    return runs.filter { it.size > 1 }.map { StaffBeam(first = it.first(), last = it.last()) }
}

private const val MIDDLE_C = 60
private const val LOW_G = 55
private const val LETTERS_PER_OCTAVE = 7

/**
 * Each pitch class's letter (C = 0 to B = 6) and accidental, in the spellings the app uses
 * for keys: C, D♭, D, E♭, E, F, F♯, G, A♭, A, B♭, B.
 */
private val LETTER_OF = listOf(0, 1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6)

private val ACCIDENTAL_OF = listOf(
    Accidental.NATURAL,
    Accidental.FLAT,
    Accidental.NATURAL,
    Accidental.FLAT,
    Accidental.NATURAL,
    Accidental.NATURAL,
    Accidental.SHARP,
    Accidental.NATURAL,
    Accidental.FLAT,
    Accidental.NATURAL,
    Accidental.FLAT,
    Accidental.NATURAL,
)
