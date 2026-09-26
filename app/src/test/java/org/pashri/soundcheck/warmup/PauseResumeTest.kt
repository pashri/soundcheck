package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseResumeTest {
    private val arpeggio8Hold = Pattern(
        id = PatternId("arpeggio-8-hold"),
        name = "Arpeggio 8-hold",
        notes = PatternNotation.parse("1 3 5 8e 8e 8e 8e 5 3 1h", NoteLength.QUARTER),
        keyChord = KeyChord.MAJOR,
    )

    /**
     * Tenor, 120 bpm, 36 000-frame Announcement: the Demo is 60 000–276 000 and
     * Iteration i spans 276 000 + 312 000i to 588 000 + 312 000i; the Step ends at 6 204 000.
     */
    private val timeline = requireNotNull(
        buildStepTimeline(
            step = Step(
                pattern = arpeggio8Hold,
                soundId = SoundId("mim"),
                bpm = 120,
                direction = Direction.START_LOW,
            ),
            range = VoiceType.TENOR.range,
            announcementFrames = 36_000,
        ),
    )

    @Test
    fun `pausing mid-iteration resumes at that iteration's key chord`() {
        assertEquals(3, timeline.iterationAt(1_300_000)?.index)
        assertEquals(1_212_000L, timeline.resumeFrame(1_300_000))
    }

    @Test
    fun `the iteration boundary belongs to the iteration that starts there`() {
        assertEquals(1_212_000L, timeline.resumeFrame(1_212_000))
        assertEquals(1_212_000L, timeline.resumeFrame(1_523_999))
        assertEquals(1_524_000L, timeline.resumeFrame(1_524_000))
    }

    @Test
    fun `resuming replays the iteration from its key chord`() {
        val replay = timeline.eventsFrom(timeline.resumeFrame(1_300_000))
        val first = replay.first() as PianoNoteEvent
        assertEquals(PianoPart.KEY_CHORD, first.part)
        assertEquals("E♭3", first.pitch.name)
        assertEquals(1_212_000L, first.startFrame)
        assertEquals(16 * 13, replay.size)
        assertTrue(replay.none { it is PianoNoteEvent && it.part == PianoPart.DEMO })
    }

    @Test
    fun `pausing during the announcement or demo replays the whole step`() {
        listOf(0L, 10_000L, 50_000L, 200_000L, 275_999L).forEach {
            assertNull(timeline.iterationAt(it))
            assertEquals(0L, timeline.resumeFrame(it))
        }
        assertEquals(timeline.events, timeline.eventsFrom(0))
    }

    @Test
    fun `pausing after the last iteration leaves nothing to replay`() {
        assertNull(timeline.iterationAt(6_204_000))
        assertEquals(6_204_000L, timeline.resumeFrame(6_204_000))
        assertEquals(6_204_000L, timeline.resumeFrame(7_000_000))
        assertTrue(timeline.eventsFrom(6_204_000).isEmpty())
    }
}
