package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM

/**
 * Identifies a Sound in the library.
 *
 * @property value a stable key, e.g. "lip-trill".
 */
@JvmInline
value class SoundId(val value: String)

/**
 * The name of a recorded clip's file in the app's clips folder, e.g. "3f2a….wav". Only
 * letters, digits and dashes before ".wav", so a name read from a file can never point
 * outside that folder.
 *
 * @property value the file name.
 * @throws IllegalArgumentException if [value] is not such a name.
 */
@JvmInline
value class ClipName(val value: String) {
    init {
        require(value = CLIP_NAME.matches(value)) { "\"$value\" is not a clip file name" }
    }

    /** Finds clip names in text that may not be readable any other way. */
    companion object {
        /**
         * Every clip name that appears anywhere in [text], whatever surrounds it.
         *
         * @param text any text, e.g. a library file that can't be decoded.
         * @return the clip names found.
         */
        fun findIn(text: String): Set<ClipName> =
            CLIP_NAME.findAll(input = text).map { ClipName(value = it.value) }.toSet()
    }
}

private val CLIP_NAME = Regex("[A-Za-z0-9-]{1,64}\\.wav")

/**
 * A Sound's recorded clip: you saying it, with the silence trimmed from both ends.
 *
 * @property name its file in the clips folder.
 * @property lengthMs how long it plays, in milliseconds.
 * @throws IllegalArgumentException if [lengthMs] is not positive.
 */
data class RecordedClip(val name: ClipName, val lengthMs: Long) {
    init {
        require(value = lengthMs > 0) { "A clip of $lengthMs ms is empty" }
    }
}

/**
 * What you sing a Step on: a syllable such as "mim" or a technique such as a lip trill.
 *
 * @property id the Sound's identifier.
 * @property label what the Sound is called, and what the phone's voice reads aloud when it
 *     has no clip.
 * @property clip your recording of it, or null for the phone's voice.
 */
data class Sound(val id: SoundId, val label: String, val clip: RecordedClip? = null)

/**
 * One entry in a Programme.
 *
 * @property pattern the notes to sing.
 * @property soundId the Sound to sing them on.
 * @property bpm the tempo, from the Metronome's slowest to its fastest.
 * @property direction which end of the Range the Step starts from.
 * @property rangeOffset the Step's adjustment to the Range.
 * @property guideMelody whether the piano plays the Pattern with you.
 * @property soundLabel [soundId]'s label when the Programme was prepared to play. Kept as a
 *     fallback for the Announcement and the screen if the Sound is later renamed away or
 *     deleted mid-Programme.
 */
data class Step(
    val pattern: Pattern,
    val soundId: SoundId,
    val bpm: Int,
    val direction: Direction,
    val rangeOffset: RangeOffset = RangeOffset.NONE,
    val guideMelody: Boolean = true,
    val soundLabel: String = soundId.value,
) {
    init {
        require(value = bpm in MIN_BPM..MAX_BPM) { "Tempo $bpm is outside $MIN_BPM–$MAX_BPM bpm" }
    }

    /**
     * This Step's keys through [range], with its Range Offset applied.
     *
     * @param range the app's Range.
     * @return the Iteration keys, or [RoundTrip.DoesNotFit] if the Step will be skipped.
     */
    fun roundTrip(range: Range): RoundTrip = planRoundTrip(
        range = range,
        offset = rangeOffset,
        span = pattern.span,
        direction = direction,
    )
}

/**
 * An ordered list of Steps that plays from start to finish without touching the phone.
 *
 * @property name what the Programme is called.
 * @property steps the Steps in play order.
 */
data class Programme(val name: String, val steps: List<Step>)
