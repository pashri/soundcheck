package org.pashri.soundcheck.music

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteNamesTest {
    @Test
    fun `pitch class names wrap by octaves for any MIDI number`() {
        assertEquals("C", pitchClassNameOf(60))
        assertEquals("E♭", pitchClassNameOf(63))
        assertEquals("B", pitchClassNameOf(-1))
        assertEquals("A♭", pitchClassNameOf(140))
    }

    @Test
    fun `a Pitch uses the same names`() {
        (0..127).forEach { assertEquals(pitchClassNameOf(it), Pitch(it).pitchClassName) }
    }
}
