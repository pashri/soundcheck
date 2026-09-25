package org.pashri.soundcheck.warmup

/**
 * The Step a Programme starts on: its first Step that fits.
 *
 * @param range the app's Range.
 * @return that Step's index, or null if no Step fits.
 */
fun Programme.firstStep(range: Range): Int? = firstPlayableFrom(index = 0, range = range)

/**
 * The Step after [current], skipping Steps that don't fit.
 *
 * @param current the index of the Step playing now.
 * @param range the app's Range.
 * @return the next playable Step's index, or null when the Programme is over.
 */
fun Programme.nextStep(current: Int, range: Range): Int? =
    firstPlayableFrom(index = current + 1, range = range)

/**
 * The Step before [current], skipping Steps that don't fit.
 *
 * @param current the index of the Step playing now.
 * @param range the app's Range.
 * @return the previous playable Step's index, or [current] when there is none, which
 *     restarts the Step playing now.
 */
fun Programme.previousStep(current: Int, range: Range): Int =
    (current - 1 downTo 0).firstOrNull { fits(index = it, range = range) } ?: current

private fun Programme.firstPlayableFrom(index: Int, range: Range): Int? =
    (index until steps.size).firstOrNull { fits(index = it, range = range) }

private fun Programme.fits(index: Int, range: Range): Boolean =
    steps[index].roundTrip(range) is RoundTrip.Fits
