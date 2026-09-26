package org.pashri.soundcheck.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Counts headphone presses that come close together and reports each run once it is over,
 * so a double press is one "2", never a "1" followed by another "1".
 *
 * @param scope runs the wait after each press; call [press] from its thread.
 * @param windowMs the longest gap between presses of one run; a run is reported this long
 *     after its last press.
 * @param onPresses called with the number of presses in the run.
 */
class PressCounter(
    private val scope: CoroutineScope,
    private val windowMs: Long,
    private val onPresses: (Int) -> Unit,
) {
    private var count = 0
    private var pending: Job? = null

    /** Records one press and restarts the wait. */
    fun press() {
        count++
        pending?.cancel()
        pending = scope.launch {
            delay(windowMs)
            val presses = count
            count = 0
            onPresses(presses)
        }
    }

    /** Timing. */
    companion object {
        /** The gap within which presses count together, and the wait before acting. */
        const val WINDOW_MS: Long = 400L
    }
}
