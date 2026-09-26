package org.pashri.soundcheck.ui.components

import org.pashri.soundcheck.warmup.Usage

/**
 * What deleting a Pattern or a Sound takes with it, for the confirmation.
 *
 * @param usages where it is used.
 * @return e.g. "3 Steps in Starter warm-up and Morning use it; they go too.", or
 *     "No Step uses it."
 */
fun usageText(usages: List<Usage>): String {
    val steps = usages.sumOf { it.steps }
    if (steps == 0) return "No Step uses it."
    val where = joinedWithAnd(usages.map { it.programmeName })
    return if (steps == 1) {
        "1 Step in $where uses it; that Step goes too."
    } else {
        "$steps Steps in $where use it; they go too."
    }
}

/**
 * How much a Pattern or a Sound is used, for its row in a library list.
 *
 * @param usages where it is used.
 * @return "Not in any Step", "Used in 1 Step" or "Used in 3 Steps".
 */
fun usageLabel(usages: List<Usage>): String = when (val steps = usages.sumOf { it.steps }) {
    0 -> "Not in any Step"
    1 -> "Used in 1 Step"
    else -> "Used in $steps Steps"
}

/**
 * Names joined as a sentence joins them; every "A, B and C" in the app comes from here.
 *
 * @param names the names, in order.
 * @return "", "A", "A and B" or "A, B and C".
 */
fun joinedWithAnd(names: List<String>): String {
    if (names.size <= 1) return names.joinToString()
    return "${names.dropLast(1).joinToString()} and ${names.last()}"
}
