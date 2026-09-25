package org.pashri.soundcheck.ui.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class TunerScreenTest {
    @Test
    fun `a flat is spoken as the word flat`() {
        assertEquals("B flat 4", spokenNoteName("B♭", octave = 4))
    }

    @Test
    fun `a sharp is spoken as the word sharp`() {
        assertEquals("F sharp 3", spokenNoteName("F♯", octave = 3))
    }

    @Test
    fun `a natural note is spoken as its letter`() {
        assertEquals("A 4", spokenNoteName("A", octave = 4))
    }
}
