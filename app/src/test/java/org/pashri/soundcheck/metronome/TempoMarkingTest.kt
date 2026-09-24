package org.pashri.soundcheck.metronome

import org.junit.Assert.assertEquals
import org.junit.Test

class TempoMarkingTest {
    @Test
    fun `96 bpm is Andante`() {
        assertEquals("Andante", TempoMarking.forBpm(96))
    }

    @Test
    fun `each marking starts at its lower bound`() {
        val expected = mapOf(
            30 to "Grave",
            39 to "Grave",
            40 to "Largo",
            60 to "Larghetto",
            66 to "Adagio",
            75 to "Adagio",
            76 to "Andante",
            108 to "Moderato",
            120 to "Allegro",
            168 to "Presto",
            200 to "Prestissimo",
            300 to "Prestissimo",
        )
        expected.forEach { (bpm, marking) ->
            assertEquals("$bpm", marking, TempoMarking.forBpm(bpm))
        }
    }
}
