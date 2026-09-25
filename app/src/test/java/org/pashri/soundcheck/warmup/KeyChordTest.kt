package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class KeyChordTest {
    @Test
    fun `each key chord quality has its exact half-steps above the key`() {
        assertEquals(
            mapOf(
                KeyChord.MAJOR to listOf(0, 4, 7),
                KeyChord.MINOR to listOf(0, 3, 7),
                KeyChord.SEVENTH to listOf(0, 4, 7, 10),
                KeyChord.MAJOR_SEVENTH to listOf(0, 4, 7, 11),
                KeyChord.MINOR_SEVENTH to listOf(0, 3, 7, 10),
                KeyChord.DIMINISHED to listOf(0, 3, 6),
                KeyChord.AUGMENTED to listOf(0, 4, 8),
                KeyChord.ROOT_ONLY to listOf(0),
            ),
            KeyChord.entries.associateWith { it.halfSteps },
        )
    }

    @Test
    fun `the key chord labels are the spec's names in the editor's order`() {
        assertEquals(
            listOf("major", "minor", "7", "maj7", "m7", "dim", "aug", "root only"),
            KeyChord.entries.map { it.label },
        )
    }

    @Test
    fun `an E flat major key chord is E flat G and B flat upward from the key`() {
        assertEquals(
            listOf("E♭3", "G3", "B♭3"),
            KeyChord.MAJOR.pitchesOn(Pitch.parse("E♭3")).map { it.name },
        )
    }

    @Test
    fun `root only is the key on its own`() {
        assertEquals(listOf(Pitch.parse("C3")), KeyChord.ROOT_ONLY.pitchesOn(Pitch.parse("C3")))
    }

    @Test
    fun `chord notes use the conventional spellings whatever the key`() {
        assertEquals(
            listOf("A♭3", "C4", "E♭4", "F♯4"),
            KeyChord.SEVENTH.pitchesOn(Pitch.parse("A♭3")).map { it.name },
        )
    }
}
