package org.pashri.soundcheck.warmup

/** The longest name or label, in characters, after trimming. */
const val MAX_NAME_LENGTH: Int = 40

/**
 * Why a name or label can't be used.
 *
 * @property message what the naming dialog says.
 */
enum class NameProblem(val message: String) {
    /** Nothing but spaces. */
    BLANK(message = "Give it a name"),

    /** Longer than [MAX_NAME_LENGTH]. */
    TOO_LONG(message = "Keep it to $MAX_NAME_LENGTH characters or fewer"),

    /** Another of the same kind already has it. */
    TAKEN(message = "That name is already used"),
}

/**
 * Checks a Programme name, Pattern name or Sound label.
 *
 * @param name the name as typed; it is judged trimmed.
 * @param taken the names of the others of its kind, not including its own
 *   current name.
 * @return what is wrong with it, or null if it can be used.
 */
fun nameProblem(name: String, taken: Collection<String>): NameProblem? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> NameProblem.BLANK
        trimmed.length > MAX_NAME_LENGTH -> NameProblem.TOO_LONG
        taken.any { it.trim().equals(other = trimmed, ignoreCase = true) } ->
            NameProblem.TAKEN
        else -> null
    }
}

/**
 * A name for something new that none of [taken] has: [base], else "[base] 2",
 * "[base] 3"…
 *
 * @param base the plain name, e.g. "New programme".
 * @param taken the names already used by its kind.
 * @return the first free name, compared ignoring case.
 */
fun uniqueName(base: String, taken: Collection<String>): String =
    generateSequence(seed = 1) { it + 1 }
        .map { if (it == 1) base else "$base $it" }
        .first { candidate ->
            taken.none { it.trim().equals(other = candidate, ignoreCase = true) }
        }
