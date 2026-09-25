package org.pashri.soundcheck.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleIdsTest {
    @Test
    fun `Announcements and piano samples have their own slots after the Metronome's`() {
        assertEquals(SampleId(16), SampleIds.announcement(0))
        assertEquals(SampleId(47), SampleIds.announcement(SampleIds.ANNOUNCEMENT_SLOTS - 1))
        assertEquals(SampleId(64), SampleIds.piano(0))
        assertEquals(SampleId(95), SampleIds.piano(SampleIds.PIANO_SLOTS - 1))
    }

    @Test
    fun `no two tools share a slot and every slot fits the engine's 256`() {
        val metronome = listOf(SampleIds.METRONOME_CLICK, SampleIds.METRONOME_ACCENT)
        val announcements = (0 until SampleIds.ANNOUNCEMENT_SLOTS).map(SampleIds::announcement)
        val piano = (0 until SampleIds.PIANO_SLOTS).map(SampleIds::piano)
        val all = metronome + announcements + piano
        assertEquals(all.size, all.toSet().size)
        assertTrue(all.all { it.value in 0 until 256 })
    }

    @Test
    fun `an index past the end of a range is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            SampleIds.announcement(SampleIds.ANNOUNCEMENT_SLOTS)
        }
        assertThrows(IllegalArgumentException::class.java) { SampleIds.piano(-1) }
    }
}
