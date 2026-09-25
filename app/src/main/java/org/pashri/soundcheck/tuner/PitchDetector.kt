package org.pashri.soundcheck.tuner

import kotlin.math.sqrt
import org.pashri.soundcheck.audio.SAMPLE_RATE

/** Samples the detector looks at in one go: 42.7 ms at 48 kHz, 3.5 periods of low E. */
const val WINDOW_SIZE: Int = 2048

/** Fresh samples per estimate: a new estimate every 21.3 ms at 48 kHz. */
const val HOP_SIZE: Int = 1024

/** The lowest pitch reported: below drop-C (C2, 65.4 Hz) with room to spare. */
const val MIN_HZ: Double = 60.0

/** The highest pitch reported: above soprano C6 (1046.5 Hz) with room to spare. */
const val MAX_HZ: Double = 1500.0

/** Frames quieter than this (about -50 dBFS) count as silence. */
const val MIN_RMS: Double = 0.003

/** A pitch whose periodicity (0 to 1) falls below this is treated as noise. */
const val MIN_CLARITY: Double = 0.8

/**
 * A peak this close to the highest one wins if it comes first (a shorter period). Keeps
 * the detector from dropping an octave, while a strong 2nd harmonic, whose peak measures
 * roughly 0.7-0.8 of the true period's peak for a 0.3 : 1 fundamental-to-harmonic ratio,
 * can't pull it an octave up.
 */
const val PEAK_THRESHOLD: Double = 0.9

/**
 * Finds the pitch of one window of mono audio with the McLeod Pitch Method: the normalised
 * square difference function (NSDF), its first key maximum near the highest, and a parabola
 * through that peak for sub-sample precision.
 *
 * Holds its working buffers, so [detect] allocates only its result. Not thread-safe; give
 * each listening loop its own detector.
 *
 * @param sampleRate the rate the audio was captured at.
 */
class PitchDetector(private val sampleRate: Int = SAMPLE_RATE) {
    private val maxLag = (sampleRate / MIN_HZ).toInt()
    private val nsdf = DoubleArray(maxLag + 2)

    /**
     * The pitch of [window], if it has a clear one.
     *
     * @param window [WINDOW_SIZE] samples in the range -1 to 1.
     * @return the frequency in Hz between [MIN_HZ] and [MAX_HZ], or null for silence, noise
     *   or a pitch out of range.
     */
    fun detect(window: FloatArray): Double? {
        require(window.size == WINDOW_SIZE) { "window must hold $WINDOW_SIZE samples" }
        if (rms(window) < MIN_RMS) return null
        fillNsdf(window)
        val peak = choosePeak() ?: return null
        if (nsdf[peak] < MIN_CLARITY) return null
        val hz = sampleRate / refine(peak)
        return hz.takeIf { it in MIN_HZ..MAX_HZ }
    }

    private fun rms(window: FloatArray): Double {
        var sum = 0.0
        for (sample in window) sum += sample * sample
        return sqrt(sum / window.size)
    }

    /** n(tau) = 2*sum x[j]*x[j+tau] / sum (x[j]^2 + x[j+tau]^2), for tau 0 to [maxLag] + 1. */
    private fun fillNsdf(x: FloatArray) {
        var energy = 0.0
        for (sample in x) energy += sample * sample
        var m = 2 * energy
        for (lag in nsdf.indices) {
            if (lag > 0) {
                val leaving = x[x.size - lag]
                val entering = x[lag - 1]
                m -= leaving * leaving + entering * entering
            }
            var acf = 0.0
            for (j in 0 until x.size - lag) acf += x[j] * x[j + lag]
            nsdf[lag] = if (m > 0.0) 2 * acf / m else 0.0
        }
    }

    /**
     * The first key maximum within [PEAK_THRESHOLD] of the highest. A key maximum is the
     * highest point of each positive lobe after the first negative dip; the lobe around
     * lag 0 is skipped because every signal matches itself there.
     */
    private fun choosePeak(): Int? {
        var lag = 1
        while (lag <= maxLag && nsdf[lag] > 0) lag++
        var highest = 0.0
        var chosen: Int? = null
        forEachKeyMaximum(from = lag) { peak -> if (nsdf[peak] > highest) highest = nsdf[peak] }
        forEachKeyMaximum(from = lag) { peak ->
            if (chosen == null && nsdf[peak] >= PEAK_THRESHOLD * highest) chosen = peak
        }
        return chosen
    }

    private inline fun forEachKeyMaximum(from: Int, action: (Int) -> Unit) {
        var lag = from
        while (lag <= maxLag) {
            while (lag <= maxLag && nsdf[lag] <= 0) lag++
            var best = -1
            while (lag <= maxLag && nsdf[lag] > 0) {
                if (best < 0 || nsdf[lag] > nsdf[best]) best = lag
                lag++
            }
            if (best > 0) action(best)
        }
    }

    /** The lag of the parabola's vertex through the peak and its two neighbours. */
    private fun refine(peak: Int): Double {
        val left = nsdf[peak - 1]
        val centre = nsdf[peak]
        val right = nsdf[peak + 1]
        val curvature = left - 2 * centre + right
        if (curvature >= 0.0) return peak.toDouble()
        return peak + 0.5 * (left - right) / curvature
    }
}
