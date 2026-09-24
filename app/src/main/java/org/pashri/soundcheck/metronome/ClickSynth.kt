package org.pashri.soundcheck.metronome

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import org.pashri.soundcheck.audio.SAMPLE_RATE
import org.pashri.soundcheck.audio.msToFrames

/** Synthesises metronome clicks: a short sine burst that dies away fast. */
object ClickSynth {
    /** How long a click lasts. */
    const val LENGTH_MS: Long = 30L

    /** The loudest a click gets, leaving headroom for other sounds. */
    const val PEAK: Float = 0.8f

    private const val DECAY_SECONDS = 0.006

    /**
     * A click at [frequencyHz].
     *
     * @param frequencyHz the click's pitch; higher sounds brighter.
     * @return mono PCM at [SAMPLE_RATE].
     */
    fun click(frequencyHz: Double): FloatArray =
        FloatArray(msToFrames(LENGTH_MS).toInt()) { i ->
            val seconds = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-seconds / DECAY_SECONDS)
            (sin(2 * PI * frequencyHz * seconds) * envelope * PEAK)
                .toFloat()
        }
}
