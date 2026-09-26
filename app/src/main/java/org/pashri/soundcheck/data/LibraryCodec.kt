package org.pashri.soundcheck.data

import kotlinx.serialization.Serializable
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.SavedProgramme
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepKey

/**
 * Reads and writes the library as JSON.
 *
 * [decode] requires the file's `version` to equal [VERSION] exactly, and unknown keys are
 * not ignored (an added or renamed field fails to parse). Adding a field therefore means a
 * new [VERSION] plus a migration from the previous version's format.
 */
object LibraryCodec : TextCodec<Library> {
    /** The format this build writes; a file with any other version is refused. */
    const val VERSION: Int = 1

    override fun encode(value: Library): String =
        DocumentJson.encodeToString(LibraryFile.serializer(), value.toFile())

    override fun decode(text: String): Library {
        val file = DocumentJson.decodeFromString(LibraryFile.serializer(), text)
        require(file.version == VERSION) { "Library format ${file.version} is not $VERSION" }
        return file.toLibrary()
    }
}

@Serializable
internal data class LibraryFile(
    val version: Int,
    val patterns: List<PatternRecord>,
    val sounds: List<SoundRecord>,
    val programmes: List<ProgrammeRecord>,
)

@Serializable
internal data class PatternRecord(
    val id: String,
    val name: String,
    val notes: String,
    val keyChord: String,
)

@Serializable
internal data class SoundRecord(val id: String, val label: String)

@Serializable
internal data class ProgrammeRecord(
    val id: String,
    val name: String,
    val steps: List<StepRecord>,
)

@Serializable
internal data class StepRecord(
    val key: String,
    val pattern: String,
    val sound: String,
    val bpm: Int,
    val direction: String,
    val offsetBottom: Int,
    val offsetTop: Int,
    val guideMelody: Boolean,
)

private fun Library.toFile(): LibraryFile = LibraryFile(
    version = LibraryCodec.VERSION,
    patterns = patterns.map {
        PatternRecord(
            id = it.id.value,
            name = it.name,
            notes = PatternNotation.format(it.notes),
            keyChord = it.keyChord.name,
        )
    },
    sounds = sounds.map { SoundRecord(id = it.id.value, label = it.label) },
    programmes = programmes.map { programme ->
        ProgrammeRecord(
            id = programme.id.value,
            name = programme.name,
            steps = programme.steps.map { it.toRecord() },
        )
    },
)

private fun SavedStep.toRecord(): StepRecord = StepRecord(
    key = key.value,
    pattern = patternId.value,
    sound = soundId.value,
    bpm = bpm,
    direction = direction.name,
    offsetBottom = rangeOffset.bottom,
    offsetTop = rangeOffset.top,
    guideMelody = guideMelody,
)

private fun LibraryFile.toLibrary(): Library = Library(
    patterns = patterns.map {
        Pattern(
            id = PatternId(it.id),
            name = it.name,
            notes = PatternNotation.parse(it.notes),
            keyChord = KeyChord.valueOf(it.keyChord),
        )
    },
    sounds = sounds.map { Sound(id = SoundId(it.id), label = it.label) },
    programmes = programmes.map { programme ->
        SavedProgramme(
            id = ProgrammeId(programme.id),
            name = programme.name,
            steps = programme.steps.map { it.toStep() },
        )
    },
)

private fun StepRecord.toStep(): SavedStep = SavedStep(
    key = StepKey(key),
    patternId = PatternId(pattern),
    soundId = SoundId(sound),
    bpm = bpm,
    direction = Direction.valueOf(direction),
    rangeOffset = RangeOffset(bottom = offsetBottom, top = offsetTop),
    guideMelody = guideMelody,
)
