package org.pashri.soundcheck.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PitchDetectorTest {
    private val detector = PitchDetector()

    private fun centsOff(heard: Double?, expected: Double): Double {
        assertNotNull("no pitch found for $expected Hz", heard)
        return (midiOf(checkNotNull(heard)) - midiOf(expected)) * CENTS_PER_SEMITONE
    }

    @Test
    fun `sines across the guitar and voice range are found within a cent`() {
        val pitches = listOf(61.74, 73.42, 82.41, 110.0, 196.0, 261.63, 440.0, 880.0, 1046.5)
        pitches.forEach { hz ->
            assertEquals("$hz Hz", 0.0, centsOff(detector.detect(Signals.sine(hz)), hz), 1.0)
        }
    }

    @Test
    fun `a sawtooth full of harmonics is found at its fundamental`() {
        listOf(82.41, 110.0, 146.83, 329.63, 523.25).forEach { hz ->
            assertEquals("$hz Hz", 0.0, centsOff(detector.detect(Signals.sawtooth(hz)), hz), 2.0)
        }
    }

    @Test
    fun `a strong second harmonic does not pull the reading an octave up`() {
        listOf(82.41, 110.0, 196.0).forEach { hz ->
            val tone = Signals.harmonics(hz, listOf(0.3, 1.0, 0.2))
            assertEquals("$hz Hz", 0.0, centsOff(detector.detect(tone), hz), 2.0)
        }
    }

    @Test
    fun `a tone richer in upper harmonics than its fundamental does not drop an octave`() {
        listOf(261.63, 293.66, 349.23, 392.0).forEach { hz ->
            val tone = Signals.sawtooth(hz)
            assertEquals("$hz Hz", 0.0, centsOff(detector.detect(tone), hz), 2.0)
        }
    }

    @Test
    fun `a quiet tone is still found`() {
        val quiet = Signals.sine(110.0, amplitude = 0.01)
        assertEquals(0.0, centsOff(detector.detect(quiet), 110.0), 1.0)
    }

    @Test
    fun `a tone over steady background noise reads the right pitch on every frame`() {
        listOf(82.41, 196.0, 440.0).forEach { hz ->
            (1..10).forEach { seed ->
                val noisy = Signals.mix(Signals.sine(hz), Signals.noise(0.05, seed))
                assertEquals("$hz Hz, seed $seed", 0.0, centsOff(detector.detect(noisy), hz), 6.0)
            }
        }
    }

    @Test
    fun `silence has no pitch`() {
        assertNull(detector.detect(FloatArray(WINDOW_SIZE)))
    }

    @Test
    fun `a whisper of noise below the silence gate has no pitch`() {
        assertNull(detector.detect(Signals.noise(0.004)))
    }

    @Test
    fun `loud white noise has no pitch`() {
        (1..20).forEach { seed ->
            assertNull("seed $seed", detector.detect(Signals.noise(0.5, seed)))
        }
    }

    @Test
    fun `low rumbling noise has no pitch`() {
        (1..20).forEach { seed ->
            assertNull("seed $seed", detector.detect(Signals.rumble(0.8, seed)))
        }
    }

    @Test
    fun `pitches outside the range are not reported`() {
        assertNull(detector.detect(Signals.sine(40.0)))
        assertNull(detector.detect(Signals.sine(2_500.0)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a window of the wrong size is refused`() {
        detector.detect(FloatArray(100))
    }
}
