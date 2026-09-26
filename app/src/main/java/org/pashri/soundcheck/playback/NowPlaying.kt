package org.pashri.soundcheck.playback

import org.pashri.soundcheck.ui.warmup.WarmupUiState

/**
 * What the notification and the lock screen say.
 *
 * @property title the Sound, e.g. "mim".
 * @property text the Step, key and Iteration, e.g. "Step 3 of 6 · E♭ major · 4 / 19 ↑".
 * @property subText the Programme's name.
 * @property playing whether to offer pause (true) or play (false).
 */
data class NowPlaying(
    val title: String,
    val text: String,
    val subText: String,
    val playing: Boolean,
)

/**
 * The notification's text for the Warm-up screen's state.
 *
 * @param state what the Warm-up screen would show, or null when nothing is loaded yet.
 * @return the notification's text; the Iteration is left out before the first one.
 */
fun nowPlaying(state: WarmupUiState?): NowPlaying {
    if (state == null) {
        return NowPlaying(title = "Warm-up", text = "", subText = "", playing = false)
    }
    val iterations = state.iterations
    val parts = listOfNotNull(
        "Step ${state.stepNumber} of ${state.stepCount}",
        iterations?.keyLabel,
        iterations?.takeIf { it.now != null }?.progressLabel,
    )
    return NowPlaying(
        title = state.soundLabel,
        text = parts.joinToString(separator = " · "),
        subText = state.programmeName,
        playing = state.playing,
    )
}
