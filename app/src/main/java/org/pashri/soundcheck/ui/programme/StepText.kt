package org.pashri.soundcheck.ui.programme

import org.pashri.soundcheck.ui.components.spokenRow
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.RoundTrip
import org.pashri.soundcheck.warmup.Step

/**
 * A Step's one-line summary, as the Programme editor lists it.
 *
 * @param step the Step, with its Pattern.
 * @return e.g. "5-note scale · 90 bpm · from low · top +2".
 */
fun stepMeta(step: Step): String = listOfNotNull(
    step.pattern.name,
    "${step.bpm} bpm",
    if (step.direction == Direction.START_LOW) "from low" else "from high",
    offsetText(step.rangeOffset),
).joinToString(separator = " · ")

/**
 * A Range Offset in words, leaving out an end that is 0.
 *
 * @param offset the offset.
 * @return e.g. "top +2" or "bottom −1 · top −3", or null for no offset.
 */
fun offsetText(offset: RangeOffset): String? {
    val parts = listOfNotNull(
        offset.bottom.takeIf { it != 0 }?.let { "bottom ${signed(it)}" },
        offset.top.takeIf { it != 0 }?.let { "top ${signed(it)}" },
    )
    return parts.joinToString(separator = " · ").ifEmpty { null }
}

/**
 * A number of half-steps with its sign, using a real minus sign.
 *
 * @param halfSteps the number.
 * @return "+2", "−3" or "0".
 */
fun signed(halfSteps: Int): String = when {
    halfSteps > 0 -> "+$halfSteps"
    halfSteps < 0 -> "−${-halfSteps}"
    else -> "0"
}

/**
 * The warning under a Step whose Pattern doesn't fit its Range.
 *
 * @param trip the Step's round trip.
 * @return e.g. "Needs 19 half-steps; this Step's Range has 18. It will be skipped.", a
 *     note that the notes reach past the piano's keys when the Range is wide enough but no
 *     key is left (degrees far above the root), or null when it fits.
 */
fun fitWarning(trip: RoundTrip): String? {
    val tooWide = trip as? RoundTrip.DoesNotFit ?: return null
    val available = tooWide.availableHalfSteps
    if (available > 0 && tooWide.neededHalfSteps <= available) return PAST_THE_KEYS
    val needed = halfStepsText(tooWide.neededHalfSteps)
    return "Needs $needed; this Step's Range has ${tooWide.availableHalfSteps}. " +
        "It will be skipped."
}

/**
 * What TalkBack says for a Step's row in the Programme editor, as one stop.
 *
 * @param row the Step's row.
 * @return e.g. "Step 2, mim, Triad · 90 bpm · from low, " followed by any fit warning.
 */
fun spokenStep(row: StepRow): String =
    spokenRow(parts = listOf("Step ${row.number}", row.sound, row.meta, row.warning))

private fun halfStepsText(count: Int): String =
    if (count == 1) "1 half-step" else "$count half-steps"

/** When the Range is wide enough but every key would sing past A0 or C8 (Task 1). */
private const val PAST_THE_KEYS = "Its notes reach past the piano's keys here. It will be skipped."
