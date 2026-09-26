package org.pashri.soundcheck.data

import kotlinx.serialization.Serializable
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.SavedProgramme
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepKey

/**
 * Reads and writes the library as JSON.
 *
 * [decode] reads [VERSION] and every earlier version, and refuses anything newer. Unknown
 * keys are not ignored (an added or renamed field fails to parse), so adding a field means
 * a new [VERSION] plus a migration from the previous version's format. Version 2 added a
 * Sound's optional `clip`; a version 1 file is read as a library with no clips.
 */
object LibraryCodec : TextCodec<Library> {
    /** The format this build writes; a file with a newer version is refused. */
    const val VERSION: Int = 2

    override fun encode(value: Library): String =
        DocumentJson.encodeToString(serializer = LibraryFile.serializer(), value = value.toFile())

    override fun decode(text: String): Library {
        val file = DocumentJson.decodeFromString(
            deserializer = LibraryFile.serializer(),
            string = text,
        )
        return file.readLibrary()
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
internal data class SoundRecord(val id: String, val label: String, val clip: ClipRecord? = null)

@Serializable
internal data class ClipRecord(val file: String, val ms: Long)

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

/** The first format that can hold a Sound's clip. */
private const val FIRST_WITH_CLIPS: Int = 2

/**
 * The library this record holds, checked as [LibraryCodec.decode] checks a whole file.
 *
 * @return the library.
 * @throws IllegalArgumentException if the record's version is newer than this build's, a
 *     version 1 record has a clip, or the library breaks a [Library] rule.
 */
internal fun LibraryFile.readLibrary(): Library {
    require(value = version in 1..LibraryCodec.VERSION) {
        "Library format $version is not 1–${LibraryCodec.VERSION}"
    }
    require(value = version >= FIRST_WITH_CLIPS || sounds.all { it.clip == null }) {
        "Library format $version can't hold clips"
    }
    return toLibrary()
}

/**
 * This library as the record [LibraryCodec] writes.
 *
 * @return the record, at [LibraryCodec.VERSION].
 */
internal fun Library.toFile(): LibraryFile = LibraryFile(
    version = LibraryCodec.VERSION,
    patterns = patterns.map {
        PatternRecord(
            id = it.id.value,
            name = it.name,
            notes = PatternNotation.format(it.notes),
            keyChord = it.keyChord.name,
        )
    },
    sounds = sounds.map { it.toRecord() },
    programmes = programmes.map { programme ->
        ProgrammeRecord(
            id = programme.id.value,
            name = programme.name,
            steps = programme.steps.map { it.toRecord() },
        )
    },
)

private fun Sound.toRecord(): SoundRecord = SoundRecord(
    id = id.value,
    label = label,
    clip = clip?.let { ClipRecord(file = it.name.value, ms = it.lengthMs) },
)

private fun SoundRecord.toSound(): Sound = Sound(
    id = SoundId(id),
    label = label,
    clip = clip?.let { RecordedClip(name = ClipName(it.file), lengthMs = it.ms) },
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
    sounds = sounds.map { it.toSound() },
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
