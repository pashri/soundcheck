package org.pashri.soundcheck.ui.warmup

import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef

/**
 * The Warm-up tab's screens. Ids are UUIDs or starter slugs, which need no escaping in a
 * route.
 */
object WarmupRoutes {
    /** The route argument holding a Programme's id. */
    const val ARG_PROGRAMME: String = "programme"

    /** The route argument holding a Step's key. */
    const val ARG_STEP: String = "step"

    /** The route argument holding a Pattern's id. */
    const val ARG_PATTERN: String = "pattern"

    /** The Warm-up home: Programmes, the Range and the library. */
    const val HOME: String = "warmup/home"

    /** The Programme playing or paused. */
    const val PLAYING: String = "warmup/playing"

    /** Range, Voice Type and "Play over other audio". */
    const val SETTINGS: String = "warmup/settings"

    /** One Programme's editor. */
    const val PROGRAMME: String = "warmup/programme/{programme}"

    /** One Step's editor. */
    const val STEP: String = "warmup/programme/{programme}/step/{step}"

    /** The Patterns library, optionally choosing a Pattern for one Step. */
    const val PATTERNS: String = "warmup/patterns?programme={programme}&step={step}"

    /** One Pattern's editor. */
    const val PATTERN: String = "warmup/pattern/{pattern}"

    /** The Sounds library, optionally choosing a Sound for one Step. */
    const val SOUNDS: String = "warmup/sounds?programme={programme}&step={step}"

    /**
     * The route to a Programme's editor.
     *
     * @param id the Programme.
     * @return e.g. "warmup/programme/starter-warm-up".
     */
    fun programme(id: ProgrammeId): String = "warmup/programme/${id.value}"

    /**
     * The route to a Step's editor.
     *
     * @param ref the Step.
     * @return e.g. "warmup/programme/starter-warm-up/step/starter-1".
     */
    fun step(ref: StepRef): String =
        "warmup/programme/${ref.programmeId.value}/step/${ref.key.value}"

    /**
     * The route to the Patterns library.
     *
     * @param pickFor the Step to choose a Pattern for, or null to browse.
     * @return e.g. "warmup/patterns" or "warmup/patterns?programme=…&step=…".
     */
    fun patterns(pickFor: StepRef?): String = "warmup/patterns" + pickQuery(pickFor)

    /**
     * The route to a Pattern's editor.
     *
     * @param id the Pattern.
     * @return e.g. "warmup/pattern/triad".
     */
    fun pattern(id: PatternId): String = "warmup/pattern/${id.value}"

    /**
     * The route to the Sounds library.
     *
     * @param pickFor the Step to choose a Sound for, or null to browse.
     * @return e.g. "warmup/sounds" or "warmup/sounds?programme=…&step=…".
     */
    fun sounds(pickFor: StepRef?): String = "warmup/sounds" + pickQuery(pickFor)

    /**
     * The Step a library list is choosing for, from its route arguments.
     *
     * @param programme the [ARG_PROGRAMME] argument, or null.
     * @param step the [ARG_STEP] argument, or null.
     * @return the Step, or null unless both are present.
     */
    fun pickFor(programme: String?, step: String?): StepRef? =
        if (programme != null && step != null) {
            StepRef(programmeId = ProgrammeId(programme), key = StepKey(step))
        } else {
            null
        }

    private fun pickQuery(ref: StepRef?): String =
        ref?.let { "?programme=${it.programmeId.value}&step=${it.key.value}" }.orEmpty()
}
