package org.pashri.soundcheck.audio

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.pashri.soundcheck.tuner.HOP_SIZE
import org.pashri.soundcheck.tuner.PitchDetector
import org.pashri.soundcheck.tuner.WINDOW_SIZE

class DemoToneMicTest {
    private val real = FakeMicInput()
    private val mic = DemoToneMic(real = real)

    private suspend fun readWindow(session: MicSession?): FloatArray {
        val first = FloatArray(HOP_SIZE)
        val second = FloatArray(HOP_SIZE)
        checkNotNull(session).read(buffer = first)
        session.read(buffer = second)
        return first + second
    }

    @Test
    fun `the demo tone is heard at its frequency across consecutive reads`() = runTest {
        mic.toneHz = A4_PLUS_8_CENTS
        val window = readWindow(session = mic.open())
        assertEquals(WINDOW_SIZE, window.size)
        val heard = PitchDetector().detect(window)
        assertNotNull(heard)
        assertEquals(A4_PLUS_8_CENTS, checkNotNull(heard), 0.1)
    }

    @Test
    fun `the demo tone never opens the real microphone`() {
        mic.toneHz = A4_PLUS_8_CENTS
        mic.open()
        assertEquals(0, real.timesOpened)
    }

    @Test
    fun `without a demo tone the real microphone is opened`() {
        mic.open()
        assertEquals(1, real.timesOpened)
    }

    private companion object {
        const val A4_PLUS_8_CENTS = 442.04
    }
}
