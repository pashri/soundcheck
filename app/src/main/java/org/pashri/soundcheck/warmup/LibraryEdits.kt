package org.pashri.soundcheck.warmup

/**
 * Where a Pattern or a Sound is used.
 *
 * @property programmeName the Programme's name.
 * @property steps how many of its Steps use it.
 */
data class Usage(val programmeName: String, val steps: Int)

/** A new Step's tempo. */
const val NEW_STEP_BPM: Int = 90

/**
 * Adds an empty Programme at the end.
 *
 * @param id the new Programme's id; not already in the library.
 * @param name its name, trimmed.
 * @return the library with the Programme added.
 */
fun Library.addProgramme(id: ProgrammeId, name: String): Library =
    copy(
        programmes = programmes +
            SavedProgramme(id = id, name = name.trim(), steps = emptyList()),
    )

/**
 * Renames a Programme.
 *
 * @param id the Programme.
 * @param name its new name, trimmed.
 * @return the library with the Programme renamed.
 */
fun Library.renameProgramme(id: ProgrammeId, name: String): Library =
    mapProgramme(id = id) { it.copy(name = name.trim()) }

/**
 * Deletes a Programme and its Steps; Patterns and Sounds stay.
 *
 * @param id the Programme.
 * @return the library without it.
 */
fun Library.deleteProgramme(id: ProgrammeId): Library =
    copy(programmes = programmes.filterNot { it.id == id })

/**
 * A Step ready to add: the first Pattern and the first Sound at [NEW_STEP_BPM],
 * starting low, with no Range Offset and Guide Melody on.
 *
 * @param key the new Step's key.
 * @return the Step, or null if the library has no Patterns or no Sounds.
 */
fun Library.newStep(key: StepKey): SavedStep? {
    val pattern = patterns.firstOrNull() ?: return null
    val sound = sounds.firstOrNull() ?: return null
    return SavedStep(
        key = key,
        patternId = pattern.id,
        soundId = sound.id,
        bpm = NEW_STEP_BPM,
        direction = Direction.START_LOW,
    )
}

/**
 * Adds [step] at the end of a Programme.
 *
 * @param programmeId the Programme.
 * @param step the Step; its key is new to the Programme.
 * @return the library with the Step added.
 */
fun Library.addStep(programmeId: ProgrammeId, step: SavedStep): Library =
    mapProgramme(id = programmeId) {
        it.copy(steps = it.steps + step)
    }

/**
 * Changes one Step.
 *
 * @param ref the Step.
 * @param change turns the Step into its new version, keeping its key.
 * @return the library with the Step changed.
 */
fun Library.updateStep(ref: StepRef, change: (SavedStep) -> SavedStep): Library =
    mapProgramme(id = ref.programmeId) { programme ->
        programme.copy(
            steps = programme.steps.map {
                if (it.key == ref.key) change(it) else it
            },
        )
    }

/**
 * Removes one Step.
 *
 * @param ref the Step.
 * @return the library without it.
 */
fun Library.removeStep(ref: StepRef): Library =
    mapProgramme(id = ref.programmeId) { programme ->
        programme.copy(steps = programme.steps.filterNot { it.key == ref.key })
    }

/**
 * Moves one Step to a new position in its Programme.
 *
 * @param programmeId the Programme.
 * @param from the Step's position now, from 0.
 * @param to its new position, from 0.
 * @return the reordered library; unchanged if either position is outside the
 *   Programme.
 */
fun Library.moveStep(programmeId: ProgrammeId, from: Int, to: Int): Library =
    mapProgramme(id = programmeId) {
        it.copy(steps = it.steps.moved(from = from, to = to))
    }

/**
 * Adds a Pattern at the end of the library.
 *
 * @param pattern the Pattern; its id is new to the library.
 * @return the library with it added.
 */
fun Library.addPattern(pattern: Pattern): Library = copy(patterns = patterns + pattern)

/**
 * Replaces the Pattern with [pattern]'s id; every Step using it follows.
 *
 * @param pattern the new version.
 * @return the library with the Pattern replaced.
 */
fun Library.savePattern(pattern: Pattern): Library =
    copy(patterns = patterns.map { if (it.id == pattern.id) pattern else it })

/**
 * Deletes a Pattern and every Step that uses it. The last Pattern stays, so a
 * new Step always has one.
 *
 * @param id the Pattern.
 * @return the library without it and its Steps, or unchanged if it is the
 *   only Pattern.
 */
fun Library.deletePattern(id: PatternId): Library {
    if (patterns.size <= 1) return this
    return withoutSteps { it.patternId == id }.copy(
        patterns = patterns.filterNot { it.id == id },
    )
}

/**
 * Adds a Sound at the end of the library.
 *
 * @param sound the Sound; its id is new to the library.
 * @return the library with it added.
 */
fun Library.addSound(sound: Sound): Library = copy(sounds = sounds + sound)

/**
 * Renames a Sound; its Steps keep it, since they name it by id.
 *
 * @param id the Sound.
 * @param label its new label, trimmed.
 * @return the library with the Sound renamed.
 */
fun Library.renameSound(id: SoundId, label: String): Library =
    copy(
        sounds = sounds.map {
            if (it.id == id) it.copy(label = label.trim()) else it
        },
    )

/**
 * Gives a Sound a recorded clip, replaces its clip, or takes it away so the phone's voice
 * reads its label again. The clip files themselves are never touched here.
 *
 * @param id the Sound.
 * @param clip its new clip, or null for the phone's voice.
 * @return the library with the Sound's clip changed.
 */
fun Library.withClip(id: SoundId, clip: RecordedClip?): Library =
    copy(sounds = sounds.map { if (it.id == id) it.copy(clip = clip) else it })

/**
 * Every clip file the library's Sounds use.
 *
 * @return their names.
 */
fun Library.clipNames(): Set<ClipName> = sounds.mapNotNull { it.clip?.name }.toSet()

/**
 * Deletes a Sound and every Step that uses it. The last Sound stays, so a
 * new Step always has one.
 *
 * @param id the Sound.
 * @return the library without it and its Steps, or unchanged if it is the
 *   only Sound.
 */
fun Library.deleteSound(id: SoundId): Library {
    if (sounds.size <= 1) return this
    return withoutSteps { it.soundId == id }.copy(
        sounds = sounds.filterNot { it.id == id },
    )
}

/**
 * The Programmes whose Steps use a Pattern.
 *
 * @param id the Pattern.
 * @return one entry per Programme that uses it, in library order.
 */
fun Library.patternUsage(id: PatternId): List<Usage> = usage { it.patternId == id }

/**
 * The Programmes whose Steps use a Sound.
 *
 * @param id the Sound.
 * @return one entry per Programme that uses it, in library order.
 */
fun Library.soundUsage(id: SoundId): List<Usage> = usage { it.soundId == id }

/**
 * This list with the item at [from] moved to [to].
 *
 * @param from the item's position now.
 * @param to its new position.
 * @return the reordered list; unchanged if either position is out of range.
 */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val items = toMutableList()
    items.add(index = to, element = items.removeAt(from))
    return items
}

private fun Library.usage(uses: (SavedStep) -> Boolean): List<Usage> =
    programmes.mapNotNull { programme ->
        val count = programme.steps.count(uses)
        if (count > 0) Usage(programmeName = programme.name, steps = count) else null
    }

private fun Library.withoutSteps(drop: (SavedStep) -> Boolean): Library =
    copy(
        programmes = programmes.map {
            it.copy(steps = it.steps.filterNot(drop))
        },
    )

private fun Library.mapProgramme(
    id: ProgrammeId,
    change: (SavedProgramme) -> SavedProgramme,
): Library = copy(programmes = programmes.map { if (it.id == id) change(it) else it })
