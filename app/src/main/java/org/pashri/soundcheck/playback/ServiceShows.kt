package org.pashri.soundcheck.playback

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import org.pashri.soundcheck.metronome.MetronomeController
import org.pashri.soundcheck.metronome.MetronomeStatus
import org.pashri.soundcheck.warmup.WarmupController

/** Which tool the playback service's notification is about. */
enum class ServiceShows {
    /** The loaded Programme, with pause, next and stop. */
    WARM_UP,

    /** The Metronome, with its tempo, Pause or Play, and Close. */
    METRONOME,
}

/**
 * Whether the playback service is needed, and which notification it shows: a loaded
 * Programme always comes first, even while the Metronome plays over it paused, so the
 * notification never jumps between the two; the Metronome shows when it plays or is paused
 * alone.
 *
 * @param programmeLoaded whether a Programme is playing or paused.
 * @param metronomeHeld whether the Metronome is clicking or paused.
 * @return what the notification shows, or null when the service isn't needed.
 */
fun serviceShows(programmeLoaded: Boolean, metronomeHeld: Boolean): ServiceShows? = when {
    programmeLoaded -> ServiceShows.WARM_UP
    metronomeHeld -> ServiceShows.METRONOME
    else -> null
}

/**
 * Headphones were pulled out (or a headset disconnected): pauses the Programme and the
 * Metronome, so neither switches to the loudspeaker; Play or a press resumes either.
 *
 * @param warmup the Warm-up.
 * @param metronome the Metronome.
 */
fun onHeadphonesUnplugged(warmup: WarmupController, metronome: MetronomeController) {
    warmup.pause()
    metronome.pause()
}

/**
 * Pairs each value of [other] with the Metronome's status as it is now, redrawing on
 * Metronome changes once they have settled: while the Metronome is held, a change waits
 * [settleMs] for the next (so dragging the tempo doesn't flood the notification past
 * Android's rate limit); ending comes through at once. The status in each pair is always
 * read fresh, never the settled one, so a pair emitted while a start is still settling can't
 * say the Metronome is stopped.
 *
 * @param other what else the notification follows, such as the Warm-up's playback.
 * @param status the Metronome's status.
 * @param settleMs how long a change to a held Metronome waits.
 * @return each value of [other] with the current status.
 */
@OptIn(FlowPreview::class)
fun <T> withSettledMetronome(
    other: Flow<T>,
    status: StateFlow<MetronomeStatus>,
    settleMs: Long,
): Flow<Pair<T, MetronomeStatus>> = combine(
    flow = other,
    flow2 = status.debounce { if (it.held) settleMs else 0L },
) { value, _ ->
    value to status.value
}
