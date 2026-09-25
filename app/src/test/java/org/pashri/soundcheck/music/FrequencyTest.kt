package org.pashri.soundcheck.music

import org.junit.Assert.assertEquals
import org.junit.Test

class FrequencyTest {
    @Test
    fun `an octave up doubles the frequency and an octave down halves it`() {
        assertEquals(2.0, frequencyRatio(12.0), 1e-12)
        assertEquals(0.5, frequencyRatio(-12.0), 1e-12)
        assertEquals(1.0, frequencyRatio(0.0), 0.0)
    }

    @Test
    fun `a half-step up is the twelfth root of two`() {
        assertEquals(1.0594630943592953, frequencyRatio(1.0), 1e-12)
    }

    @Test
    fun `A4 is 440 Hz and middle C is 261_63 Hz`() {
        assertEquals(440.0, hzOf(69.0), 1e-9)
        assertEquals(261.6255653005986, hzOf(60.0), 1e-9)
    }

    @Test
    fun `midiOf undoes hzOf`() {
        assertEquals(60.3, midiOf(hzOf(60.3)), 1e-9)
    }
}
