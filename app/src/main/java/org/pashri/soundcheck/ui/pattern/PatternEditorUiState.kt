package org.pashri.soundcheck.ui.pattern

import org.pashri.soundcheck.music.pitchClassNameOf
import org.pashri.soundcheck.ui.components.joinedWithAnd
import org.pashri.soundcheck.ui.components.spokenMusic
import org.pashri.soundcheck.ui.components.usageText
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.MAX_DEGREE
import org.pashri.soundcheck.warmup.NoteLength
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.PatternNote
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.patternUsage
import org.pashri.soundcheck.warmup.voiceTypesFitting

/**
 * One note of the Pattern being edited.
 *
 * @property text its degree, e.g. "8" or "♭3".
 * @property selected whether it is the note the controls change.
 * @property description what TalkBack says, e.g. "Note 5 of 10: 8, eighth".
 */
data class NoteChip(val text: String, val selected: Boolean, val description: String)

/**
 * Everything the Pattern editor shows.
 *
 * @property name the Pattern's name.
 * @property summary e.g. "10 NOTES · SPANS AN OCTAVE".
 * @property fit which Voice Types' presets it fits, e.g. "Fits every Voice Type".
 * @property notes every note, in order.
 * @property selected the selected note's position, from 0, always inside the Pattern.
 * @property noteLabel e.g. "NOTE 5 OF 10".
 * @property degree the selected note's degree, e.g. "♭3".
 * @property degreeName its note in the key of C, e.g. "E♭ in C".
 * @property canLowerDegree false at degree 1.
 * @property canRaiseDegree false at degree 99.
 * @property accidental the selected note's accidental.
 * @property length the selected note's length.
 * @property keyChord the Pattern's Key Chord.
 * @property canDeleteNote false with only one note.
 * @property canDelete false for the library's only Pattern.
 * @property deleteNote what deleting the Pattern takes with it.
 * @property otherNames the other Patterns' names, which a rename must avoid.
 */
data class PatternEditorUiState(
    val name: String,
    val summary: String,
    val fit: String,
    val notes: List<NoteChip>,
    val selected: Int,
    val noteLabel: String,
    val degree: String,
    val degreeName: String,
    val canLowerDegree: Boolean,
    val canRaiseDegree: Boolean,
    val accidental: Accidental,
    val length: NoteLength,
    val keyChord: KeyChord,
    val canDeleteNote: Boolean,
    val canDelete: Boolean,
    val deleteNote: String,
    val otherNames: List<String>,
)

/**
 * The Pattern editor for one Pattern.
 *
 * @param library the saved library.
 * @param id the Pattern.
 * @param selected the note asked for; kept inside the Pattern.
 * @return what to show, or null if the Pattern isn't in the library.
 */
fun patternEditorUiState(library: Library, id: PatternId, selected: Int): PatternEditorUiState? {
    val pattern = library.pattern(id) ?: return null
    val index = selected.coerceIn(0, pattern.notes.lastIndex)
    val note = pattern.notes[index]
    return PatternEditorUiState(
        name = pattern.name,
        summary = "${notesText(pattern.notes.size)} · ${spanText(pattern.span.halfSteps)}",
        fit = fitText(voiceTypesFitting(pattern.span)),
        notes = noteChips(pattern = pattern, selected = index),
        selected = index,
        noteLabel = "NOTE ${index + 1} OF ${pattern.notes.size}",
        degree = degreeText(note),
        degreeName = "${pitchClassNameOf(MIDDLE_C + note.halfSteps)} in C",
        canLowerDegree = note.degree > 1,
        canRaiseDegree = note.degree < MAX_DEGREE,
        accidental = note.accidental,
        length = note.length,
        keyChord = pattern.keyChord,
        canDeleteNote = pattern.notes.size > 1,
        canDelete = library.patterns.size > 1,
        deleteNote = usageText(library.patternUsage(id)),
        otherNames = library.patterns.filter { it.id != id }.map { it.name },
    )
}

/**
 * How far a Pattern reaches, for the editor's header.
 *
 * @param halfSteps its sung span.
 * @return "ONE PITCH", "SPANS 1 HALF-STEP", "SPANS AN OCTAVE", "SPANS TWO OCTAVES" or
 *     "SPANS 7 HALF-STEPS".
 */
fun spanText(halfSteps: Int): String = when (halfSteps) {
    0 -> "ONE PITCH"
    1 -> "SPANS 1 HALF-STEP"
    OCTAVE -> "SPANS AN OCTAVE"
    2 * OCTAVE -> "SPANS TWO OCTAVES"
    else -> "SPANS $halfSteps HALF-STEPS"
}

/**
 * A note count for the editor's header.
 *
 * @param count how many notes.
 * @return "1 NOTE" or "10 NOTES".
 */
fun notesText(count: Int): String = if (count == 1) "1 NOTE" else "$count NOTES"

/**
 * Which Voice Types a Pattern fits, in words.
 *
 * @param voiceTypes the fitting Voice Types, in [VoiceType] order.
 * @return e.g. "Fits every Voice Type", "Fits Bass only", "Fits Alto and Bass" or "Too wide
 *     for every Voice Type".
 */
fun fitText(voiceTypes: List<VoiceType>): String {
    val names = voiceTypes.map { it.label }
    return when (names.size) {
        0 -> "Too wide for every Voice Type"
        1 -> "Fits ${names.single()} only"
        VoiceType.entries.size -> "Fits every Voice Type"
        else -> "Fits ${joinedWithAnd(names)}"
    }
}

/**
 * A length's button label.
 *
 * @param length the length.
 * @return "Eighth", "Quarter", "Half" or "Whole".
 */
fun lengthLabel(length: NoteLength): String =
    lengthName(length).replaceFirstChar { it.uppercase() }

/**
 * An accidental's button label.
 *
 * @param accidental the accidental.
 * @return "♭", "♮" or "♯".
 */
fun accidentalLabel(accidental: Accidental): String = when (accidental) {
    Accidental.FLAT -> "♭"
    Accidental.NATURAL -> "♮"
    Accidental.SHARP -> "♯"
}

/**
 * An accidental as TalkBack says it.
 *
 * @param accidental the accidental.
 * @return "flat", "natural" or "sharp".
 */
fun accidentalSpoken(accidental: Accidental): String = accidental.name.lowercase()

/**
 * A Key Chord's chip label, as the design writes it.
 *
 * @param chord the Key Chord.
 * @return its label, with "root" for root only.
 */
fun chordChipLabel(chord: KeyChord): String =
    if (chord == KeyChord.ROOT_ONLY) "root" else chord.label

/**
 * A Key Chord as TalkBack says it.
 *
 * @param chord the Key Chord.
 * @return e.g. "major seventh" or "root only".
 */
fun chordSpoken(chord: KeyChord): String = when (chord) {
    KeyChord.MAJOR -> "major"
    KeyChord.MINOR -> "minor"
    KeyChord.SEVENTH -> "seventh"
    KeyChord.MAJOR_SEVENTH -> "major seventh"
    KeyChord.MINOR_SEVENTH -> "minor seventh"
    KeyChord.DIMINISHED -> "diminished"
    KeyChord.AUGMENTED -> "augmented"
    KeyChord.ROOT_ONLY -> "root only"
}

private fun noteChips(pattern: Pattern, selected: Int): List<NoteChip> =
    pattern.notes.mapIndexed { index, note ->
        val spoken = "${spokenMusic(degreeText(note))}, ${lengthName(note.length)}"
        NoteChip(
            text = degreeText(note),
            selected = index == selected,
            description = "Note ${index + 1} of ${pattern.notes.size}: $spoken",
        )
    }

private fun degreeText(note: PatternNote): String = PatternNotation.degrees(listOf(note))

private fun lengthName(length: NoteLength): String = length.name.lowercase()

private const val MIDDLE_C = 60
private const val OCTAVE = 12
