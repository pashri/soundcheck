package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTimelineTest {
    private val tenor = VoiceType.TENOR.range
    private val mim = SoundId("mim")
    private val arpeggio8Hold = Pattern(
        name = "Arpeggio 8-hold",
        notes = PatternNotation.parse("1 3 5 8e 8e 8e 8e 5 3 1h", NoteLength.QUARTER),
        keyChord = KeyChord.MAJOR,
    )
    private val doubleArpeggio = Pattern(
        name = "Double arpeggio",
        notes = PatternNotation.parse("1 3 5 8 10 12 11 9 7 5 4 2 1h", NoteLength.EIGHTH),
        keyChord = KeyChord.MAJOR,
    )

    private fun timeline(
        pattern: Pattern,
        bpm: Int,
        direction: Direction,
        guideMelody: Boolean,
        announcementFrames: Long,
    ): StepTimeline {
        val step = Step(
            pattern = pattern,
            soundId = mim,
            bpm = bpm,
            direction = direction,
            guideMelody = guideMelody,
        )
        return requireNotNull(
            buildStepTimeline(step = step, range = tenor, announcementFrames = announcementFrames),
        )
    }

    /** 120 bpm: 24 000 frames a beat, 12 000 an eighth; 18-eighth Pattern; 19 Iterations. */
    private fun arpeggioAt120(): StepTimeline = timeline(
        pattern = arpeggio8Hold,
        bpm = 120,
        direction = Direction.START_LOW,
        guideMelody = true,
        announcementFrames = 36_000,
    )

    /** 90 bpm: 32 000 frames a beat, 16 000 an eighth; 16-eighth Pattern; 5 Iterations. */
    private fun doubleArpeggioGuideOff(): StepTimeline = timeline(
        pattern = doubleArpeggio,
        bpm = 90,
        direction = Direction.START_HIGH,
        guideMelody = false,
        announcementFrames = 20_000,
    )

    private fun StepTimeline.notes(part: PianoPart): List<PianoNoteEvent> =
        events.filterIsInstance<PianoNoteEvent>().filter { it.part == part }

    @Test
    fun `a step opens with its announcement then a half-second gap before the demo`() {
        val timeline = arpeggioAt120()
        assertEquals(
            AnnouncementEvent(soundId = mim, startFrame = 0, lengthFrames = 36_000),
            timeline.events.first(),
        )
        assertEquals(60_000L, timeline.events[1].startFrame)
    }

    @Test
    fun `the demo plays the pattern once in the starting key with no key chord`() {
        val timeline = arpeggioAt120()
        val demo = timeline.notes(PianoPart.DEMO)
        assertEquals(
            listOf("C3", "E3", "G3", "C4", "C4", "C4", "C4", "G3", "E3", "C3"),
            demo.map { it.pitch.name },
        )
        assertEquals(
            listOf(
                60_000L, 84_000L, 108_000L, 132_000L, 144_000L,
                156_000L, 168_000L, 180_000L, 204_000L, 228_000L,
            ),
            demo.map { it.startFrame },
        )
        assertEquals(276_000L, demo.last().endFrame)
        assertTrue(timeline.notes(PianoPart.KEY_CHORD).all { it.startFrame >= 276_000L })
    }

    @Test
    fun `each iteration rings its key chord for one four-beat bar then plays the pattern`() {
        val timeline = arpeggioAt120()
        val iteration = timeline.iterations[3]
        assertEquals(
            IterationSpan(
                index = 3,
                key = Pitch.parse("E♭3"),
                startFrame = 1_212_000,
                endFrame = 1_524_000,
            ),
            iteration,
        )
        val inside = timeline.events.filterIsInstance<PianoNoteEvent>().filter {
            it.startFrame >= iteration.startFrame && it.startFrame < iteration.endFrame
        }
        val chord = inside.filter { it.part == PianoPart.KEY_CHORD }
        assertEquals(listOf("E♭3", "G3", "B♭3"), chord.map { it.pitch.name })
        assertTrue(chord.all { it.startFrame == 1_212_000L && it.lengthFrames == 96_000L })
        val guide = inside.filter { it.part == PianoPart.GUIDE_MELODY }
        assertEquals(
            listOf("E♭3", "G3", "B♭3", "E♭4", "E♭4", "E♭4", "E♭4", "B♭3", "G3", "E♭3"),
            guide.map { it.pitch.name },
        )
        assertEquals(1_308_000L, guide.first().startFrame)
        assertEquals(1_524_000L, guide.last().endFrame)
    }

    @Test
    fun `the arpeggio 8-hold on a tenor at 120 bpm lasts 6204000 frames in 258 events`() {
        val timeline = arpeggioAt120()
        assertEquals(19, timeline.iterations.size)
        assertEquals(6_204_000L, timeline.lengthFrames)
        assertEquals(258, timeline.events.size)
        assertEquals(listOf(10, 57, 190), PianoPart.entries.map { timeline.notes(it).size })
    }

    @Test
    fun `iterations follow the demo and each other with no gap`() {
        val timeline = arpeggioAt120()
        assertEquals(276_000L, timeline.iterations.first().startFrame)
        assertEquals(timeline.lengthFrames, timeline.iterations.last().endFrame)
        assertEquals((0..18).toList(), timeline.iterations.map { it.index })
    }

    @Test
    fun `events are in time order`() {
        listOf(arpeggioAt120(), doubleArpeggioGuideOff()).forEach { timeline ->
            val starts = timeline.events.map { it.startFrame }
            assertEquals(starts.sorted(), starts)
        }
    }

    @Test
    fun `with the guide melody off only the key chord plays and the pattern's time is kept`() {
        val timeline = doubleArpeggioGuideOff()
        assertEquals(
            listOf("D3", "D♭3", "C3", "D♭3", "D3"),
            timeline.iterations.map { it.key.name },
        )
        assertEquals(
            listOf(300_000L, 684_000L, 1_068_000L, 1_452_000L, 1_836_000L),
            timeline.iterations.map { it.startFrame },
        )
        assertEquals(2_220_000L, timeline.lengthFrames)
        assertEquals(29, timeline.events.size)
        assertTrue(timeline.notes(PianoPart.GUIDE_MELODY).isEmpty())
        assertEquals(15, timeline.notes(PianoPart.KEY_CHORD).size)
        assertTrue(timeline.notes(PianoPart.KEY_CHORD).all { it.lengthFrames == 128_000L })
    }

    @Test
    fun `the demo still plays with the guide melody off`() {
        val demo = doubleArpeggioGuideOff().notes(PianoPart.DEMO)
        assertEquals(13, demo.size)
        assertEquals(44_000L, demo.first().startFrame)
        assertEquals("D3", demo.first().pitch.name)
        assertEquals("A4", demo.maxOf { it.pitch }.name)
        assertEquals(300_000L, demo.last().endFrame)
    }

    @Test
    fun `every sung note of every iteration is inside the Range`() {
        val doubleArpeggioFromLow = timeline(
            pattern = doubleArpeggio,
            bpm = 90,
            direction = Direction.START_LOW,
            guideMelody = true,
            announcementFrames = 20_000,
        )
        listOf(arpeggioAt120(), doubleArpeggioFromLow).forEach { timeline ->
            val sung = timeline.notes(PianoPart.GUIDE_MELODY) + timeline.notes(PianoPart.DEMO)
            assertTrue(sung.all { it.pitch in tenor })
            assertEquals(tenor.lowest, sung.minOf { it.pitch })
            assertEquals(tenor.highest, sung.maxOf { it.pitch })
        }
    }

    @Test
    fun `a fractional tempo ends the Step on the grid without drift`() {
        val timeline = timeline(
            pattern = arpeggio8Hold,
            bpm = 97,
            direction = Direction.START_LOW,
            guideMelody = true,
            announcementFrames = 36_000,
        )
        assertEquals(7_660_825L, timeline.lengthFrames)
        assertEquals(327_216L, timeline.iterations.first().startFrame)
        val guide = timeline.notes(PianoPart.GUIDE_MELODY)
        assertEquals(timeline.lengthFrames, guide.last().endFrame)
    }

    @Test
    fun `a step whose pattern does not fit has no timeline`() {
        val step = Step(
            pattern = doubleArpeggio,
            soundId = mim,
            bpm = 90,
            direction = Direction.START_LOW,
            rangeOffset = RangeOffset(top = -3),
        )
        assertNull(buildStepTimeline(step = step, range = tenor, announcementFrames = 20_000))
    }

    @Test
    fun `a new step plays its guide melody and has no range offset`() {
        val step = Step(
            pattern = arpeggio8Hold,
            soundId = mim,
            bpm = 100,
            direction = Direction.START_LOW,
        )
        assertTrue(step.guideMelody)
        assertEquals(RangeOffset.NONE, step.rangeOffset)
    }

    @Test
    fun `a step's tempo must be one the metronome offers`() {
        listOf(29, 301).forEach { bpm ->
            assertThrows(IllegalArgumentException::class.java) {
                Step(
                    pattern = arpeggio8Hold,
                    soundId = mim,
                    bpm = bpm,
                    direction = Direction.START_LOW,
                )
            }
        }
    }

    @Test
    fun `a step's range offset shapes its round trip`() {
        val step = Step(
            pattern = arpeggio8Hold,
            soundId = mim,
            bpm = 100,
            direction = Direction.START_LOW,
            rangeOffset = RangeOffset(top = 2),
        )
        assertEquals(Pitch.parse("B3"), (step.roundTrip(tenor) as RoundTrip.Fits).turnKey)
    }

    @Test
    fun `a negative announcement length is rejected`() {
        val step = Step(
            pattern = arpeggio8Hold,
            soundId = mim,
            bpm = 100,
            direction = Direction.START_LOW,
        )
        assertThrows(IllegalArgumentException::class.java) {
            buildStepTimeline(step = step, range = tenor, announcementFrames = -1)
        }
    }
}
