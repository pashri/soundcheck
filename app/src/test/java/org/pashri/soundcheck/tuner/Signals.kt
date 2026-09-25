package org.pashri.soundcheck.tuner

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.pashri.soundcheck.audio.SAMPLE_RATE

/** Synthetic test audio at [SAMPLE_RATE], generated so every test hears the same thing. */
object Signals {
    /**
     * A tone made of harmonics of [hz].
     *
     * @param hz the fundamental.
     * @param amplitudes the amplitude of harmonic 1 (the fundamental), 2, 3 and so on.
     * @param size how many samples.
     * @param start the index of the first sample, so consecutive chunks join up.
     */
    fun harmonics(
        hz: Double,
        amplitudes: List<Double>,
        size: Int = WINDOW_SIZE,
        start: Long = 0,
    ): FloatArray = FloatArray(size) { i ->
        val t = (start + i).toDouble() / SAMPLE_RATE
        amplitudes.withIndex().sumOf { (n, a) -> a * sin(2 * PI * hz * (n + 1) * t) }.toFloat()
    }

    /**
     * A pure sine.
     *
     * @param hz the frequency.
     * @param amplitude the peak level.
     * @param size how many samples.
     * @param start the index of the first sample.
     */
    fun sine(
        hz: Double,
        amplitude: Double = 0.5,
        size: Int = WINDOW_SIZE,
        start: Long = 0,
    ): FloatArray = harmonics(hz, listOf(amplitude), size, start)

    /**
     * A band-limited sawtooth: every harmonic up to 5 kHz at 1/n, like a bowed or bright
     * plucked string.
     *
     * @param hz the fundamental.
     * @param amplitude the fundamental's level.
     */
    fun sawtooth(hz: Double, amplitude: Double = 0.3): FloatArray {
        val count = (5_000 / hz).toInt()
        return harmonics(hz, List(count) { n -> amplitude / (n + 1) })
    }

    /**
     * White noise.
     *
     * @param amplitude the peak level.
     * @param seed fixes the random sequence.
     * @param size how many samples.
     */
    fun noise(amplitude: Double, seed: Int = 1, size: Int = WINDOW_SIZE): FloatArray {
        val random = Random(seed)
        return FloatArray(size) { ((random.nextDouble() * 2 - 1) * amplitude).toFloat() }
    }

    /**
     * Noise through a one-pole low-pass, rumbling like traffic or a fan.
     *
     * @param amplitude the peak level of the noise before filtering.
     * @param seed fixes the random sequence.
     */
    fun rumble(amplitude: Double, seed: Int = 1): FloatArray {
        val white = noise(amplitude, seed)
        var level = 0f
        return FloatArray(white.size) { i ->
            level += RUMBLE_SMOOTHING * (white[i] - level)
            level
        }
    }

    /**
     * Two signals mixed.
     *
     * @param a the first signal.
     * @param b the second, the same length.
     */
    fun mix(a: FloatArray, b: FloatArray): FloatArray = FloatArray(a.size) { a[it] + b[it] }

    private const val RUMBLE_SMOOTHING = 0.05f
}
