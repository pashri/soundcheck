package org.pashri.soundcheck.playback

import org.pashri.soundcheck.metronome.MetronomeStatus
import org.pashri.soundcheck.ui.warmup.WarmupUiState

/**
 * What the notification and the lock screen say.
 *
 * @property title the Sound, e.g. "mim", or "Metronome".
 * @property text the Step, key and Iteration, e.g. "Step 3 of 6 · E♭ major · 4 / 19 ↑", or
 *   the Metronome's tempo and accent.
 * @property subText the Programme's name, or empty for the Metronome.
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

/**
 * The notification's text while the Metronome plays on its own.
 *
 * @param status the Metronome's tempo and accent.
 * @return e.g. "Metronome", "96 bpm · accent 4" (or "no accent").
 */
fun metronomeNowPlaying(status: MetronomeStatus): NowPlaying {
    val accent = status.accentEvery?.let { "accent $it" } ?: "no accent"
    return NowPlaying(
        title = "Metronome",
        text = "${status.bpm} bpm · $accent",
        subText = "",
        playing = status.running,
    )
}
