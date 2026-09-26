package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM

/**
 * Identifies a saved Programme.
 *
 * @property value a stable key, e.g. "starter-warm-up".
 */
@JvmInline
value class ProgrammeId(val value: String)

/**
 * Identifies a Step inside its Programme; it stays with the Step when Steps are reordered.
 *
 * @property value a stable key, e.g. "starter-1".
 */
@JvmInline
value class StepKey(val value: String)

/**
 * Points at one Step of one saved Programme.
 *
 * @property programmeId the Programme.
 * @property key the Step.
 */
data class StepRef(val programmeId: ProgrammeId, val key: StepKey)

/**
 * A Step as the library saves it. It names its Pattern and Sound by id, so every Step that
 * uses a Pattern follows an edit to it.
 *
 * @property key the Step's identifier within its Programme.
 * @property patternId the Pattern to sing.
 * @property soundId the Sound to sing it on.
 * @property bpm the tempo, 30 to 300.
 * @property direction which end of the Range the Step starts from.
 * @property rangeOffset the Step's adjustment to the Range.
 * @property guideMelody whether the piano plays the Pattern with you.
 * @throws IllegalArgumentException if [bpm] is outside 30–300.
 */
data class SavedStep(
    val key: StepKey,
    val patternId: PatternId,
    val soundId: SoundId,
    val bpm: Int,
    val direction: Direction,
    val rangeOffset: RangeOffset = RangeOffset.NONE,
    val guideMelody: Boolean = true,
) {
    init {
        require(value = bpm in MIN_BPM..MAX_BPM) { "Tempo $bpm is outside $MIN_BPM–$MAX_BPM bpm" }
    }

    /**
     * This Step ready to play.
     *
     * @param pattern the Pattern [patternId] names.
     * @param soundLabel [soundId]'s label now, kept as its fallback if the Sound is later
     *     renamed away or deleted while this Step plays.
     * @return the playable Step.
     */
    fun toStep(pattern: Pattern, soundLabel: String): Step = Step(
        pattern = pattern,
        soundId = soundId,
        bpm = bpm,
        direction = direction,
        rangeOffset = rangeOffset,
        guideMelody = guideMelody,
        soundLabel = soundLabel,
    )
}

/**
 * A Programme as the library saves it.
 *
 * @property id the Programme's identifier.
 * @property name what it is called, e.g. "Morning".
 * @property steps the Steps in play order; no two share a key.
 * @throws IllegalArgumentException if two Steps share a key.
 */
data class SavedProgramme(val id: ProgrammeId, val name: String, val steps: List<SavedStep>) {
    init {
        require(value = steps.map { it.key }.toSet().size == steps.size) {
            "Programme \"$name\" has two Steps with one key"
        }
    }

    /**
     * The Step called [key].
     *
     * @param key the Step's key.
     * @return the Step, or null if the Programme has none by that key.
     */
    fun step(key: StepKey): SavedStep? = steps.firstOrNull { it.key == key }
}

/**
 * Everything the Warm-up saves on the phone: the Patterns, the Sounds and the Programmes.
 * Every Step names a Pattern and a Sound that are in the library.
 *
 * @property patterns the Patterns, in library order.
 * @property sounds the Sounds, in library order.
 * @property programmes the Programmes, in the order the Warm-up home lists them.
 * @throws IllegalArgumentException if two Patterns, Sounds or Programmes share an id, or a
 *     Step names a Pattern or Sound that isn't in the library.
 */
data class Library(
    val patterns: List<Pattern>,
    val sounds: List<Sound>,
    val programmes: List<SavedProgramme>,
) {
    init {
        requireDistinct(ids = patterns.map { it.id }, kind = "Patterns")
        requireDistinct(ids = sounds.map { it.id }, kind = "Sounds")
        requireDistinct(ids = programmes.map { it.id }, kind = "Programmes")
        val patternIds = patterns.map { it.id }.toSet()
        val soundIds = sounds.map { it.id }.toSet()
        programmes.flatMap { it.steps }.forEach { step ->
            require(value = step.patternId in patternIds) { "A Step names a missing Pattern" }
            require(value = step.soundId in soundIds) { "A Step names a missing Sound" }
        }
    }

    /**
     * The Pattern called [id].
     *
     * @param id the Pattern's id.
     * @return the Pattern, or null if there is none.
     */
    fun pattern(id: PatternId): Pattern? = patterns.firstOrNull { it.id == id }

    /**
     * The Sound called [id].
     *
     * @param id the Sound's id.
     * @return the Sound, or null if there is none.
     */
    fun sound(id: SoundId): Sound? = sounds.firstOrNull { it.id == id }

    /**
     * The saved Programme called [id].
     *
     * @param id the Programme's id.
     * @return the Programme, or null if there is none.
     */
    fun programme(id: ProgrammeId): SavedProgramme? = programmes.firstOrNull { it.id == id }

    /**
     * [step] ready to play, with its Pattern looked up.
     *
     * @param step a Step of one of this library's Programmes.
     * @return the playable Step.
     * @throws NoSuchElementException if [step] names a Pattern this library lacks.
     */
    fun stepToPlay(step: SavedStep): Step =
        step.toStep(
            pattern = patterns.first { it.id == step.patternId },
            soundLabel = sound(step.soundId)?.label ?: step.soundId.value,
        )

    /**
     * The Programme called [id], ready to play: a snapshot of its Steps and their Patterns
     * as they are now.
     *
     * @param id the Programme's id.
     * @return the playable Programme, or null if there is none.
     */
    fun programmeToPlay(id: ProgrammeId): Programme? = programme(id)?.let { saved ->
        Programme(name = saved.name, steps = saved.steps.map(::stepToPlay))
    }
}

private fun <T> requireDistinct(ids: List<T>, kind: String) {
    require(value = ids.toSet().size == ids.size) { "Two $kind share an id" }
}
