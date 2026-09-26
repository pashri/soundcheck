package org.pashri.soundcheck.playback

import org.pashri.soundcheck.metronome.MetronomeController
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
