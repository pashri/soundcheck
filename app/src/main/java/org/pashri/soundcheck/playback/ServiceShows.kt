package org.pashri.soundcheck.playback

import org.pashri.soundcheck.metronome.MetronomeController
import org.pashri.soundcheck.warmup.WarmupController

/** Which tool the playback service's notification is about. */
enum class ServiceShows {
    /** The loaded Programme, with pause, next and stop. */
    WARM_UP,

    /** The Metronome, with its tempo and a Stop button. */
    METRONOME,
}

/**
 * Whether the playback service is needed, and which notification it shows: a loaded
 * Programme always comes first, even while the Metronome plays over it paused, so the
 * notification never jumps between the two; the Metronome shows when it plays alone.
 *
 * @param programmeLoaded whether a Programme is playing or paused.
 * @param metronomePlaying whether the Metronome is clicking.
 * @return what the notification shows, or null when the service isn't needed.
 */
fun serviceShows(programmeLoaded: Boolean, metronomePlaying: Boolean): ServiceShows? = when {
    programmeLoaded -> ServiceShows.WARM_UP
    metronomePlaying -> ServiceShows.METRONOME
    else -> null
}

/**
 * Headphones were pulled out (or a headset disconnected): pauses the Programme and stops
 * the Metronome, so neither switches to the loudspeaker.
 *
 * @param warmup the Warm-up.
 * @param metronome the Metronome.
 */
fun onHeadphonesUnplugged(warmup: WarmupController, metronome: MetronomeController) {
    warmup.pause()
    metronome.stop()
}
