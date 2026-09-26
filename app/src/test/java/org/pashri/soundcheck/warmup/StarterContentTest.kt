package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class StarterContentTest {
    private val programme = StarterProgrammes.WARM_UP

    @Test
    fun `there are eight starter patterns with distinct names`() {
        assertEquals(8, StarterPatterns.ALL.size)
        assertEquals(8, StarterPatterns.ALL.map { it.name }.toSet().size)
    }

    @Test
    fun `the two patterns the spec names have its exact degrees`() {
        assertEquals("Arpeggio 8-hold", StarterPatterns.ARPEGGIO_8_HOLD.name)
        assertEquals(
            "1 3 5 8 8 8 8 5 3 1",
            PatternNotation.degrees(StarterPatterns.ARPEGGIO_8_HOLD.notes),
        )
        assertEquals("Double arpeggio", StarterPatterns.DOUBLE_ARPEGGIO.name)
        assertEquals(
            "1 3 5 8 10 12 11 9 7 5 4 2 1",
            PatternNotation.degrees(StarterPatterns.DOUBLE_ARPEGGIO.notes),
        )
    }

    @Test
    fun `the starter patterns have the documented spans lengths and key chords`() {
        val all = StarterPatterns.ALL
        assertEquals(
            listOf(
                "5-note scale", "Arpeggio 8-hold", "Double arpeggio", "1-5-1 siren",
                "Triad", "Minor 5-note scale", "9-note scale", "Dominant arpeggio",
            ),
            all.map { it.name },
        )
        assertEquals(listOf(7, 12, 19, 7, 7, 7, 14, 12), all.map { it.span.halfSteps })
        assertEquals(listOf(12, 18, 16, 16, 12, 12, 20, 12), all.map { it.lengthInEighths })
        assertEquals(
            listOf(
                KeyChord.MAJOR, KeyChord.MAJOR, KeyChord.MAJOR, KeyChord.ROOT_ONLY,
                KeyChord.MAJOR, KeyChord.MINOR, KeyChord.MAJOR, KeyChord.SEVENTH,
            ),
            all.map { it.keyChord },
        )
    }

    @Test
    fun `every starter pattern fits every voice type from either end`() {
        StarterPatterns.ALL.forEach { pattern ->
            VoiceType.entries.forEach { voice ->
                Direction.entries.forEach { direction ->
                    val trip = planRoundTrip(
                        range = voice.range,
                        offset = RangeOffset.NONE,
                        span = pattern.span,
                        direction = direction,
                    )
                    assertTrue("${pattern.name} ${voice.label}: $trip", trip is RoundTrip.Fits)
                }
            }
        }
    }

    @Test
    fun `the eight starter sounds are the spec's`() {
        assertEquals(
            listOf("lip trill", "mim", "neh", "mah", "ng", "oo", "ee", "hum"),
            StarterSounds.ALL.map { it.label },
        )
        assertEquals(8, StarterSounds.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun `the starter programme is six steps on the mockup's sounds`() {
        assertEquals("Starter warm-up", programme.name)
        assertEquals(
            listOf("lip trill", "hum", "mim", "oo", "neh", "mah"),
            programme.steps.map { step ->
                StarterSounds.ALL.first { it.id == step.soundId }.label
            },
        )
        assertEquals(RangeOffset(top = 2), programme.steps.first().rangeOffset)
    }

    @Test
    fun `the starter programme uses only starter patterns and sounds`() {
        val soundIds = StarterSounds.ALL.map { it.id }
        assertTrue(programme.steps.all { it.pattern in StarterPatterns.ALL })
        assertTrue(programme.steps.all { it.soundId in soundIds })
    }

    @Test
    fun `every starter step keeps every sung note inside every voice type's range`() {
        VoiceType.entries.forEach { voice ->
            programme.steps.forEach { step ->
                val label = "${voice.label} ${step.pattern.name}"
                val effective = requireNotNull(voice.range.offsetBy(step.rangeOffset))
                val timeline = buildStepTimeline(
                    step = step,
                    range = voice.range,
                    announcementFrames = 24_000,
                )
                val sung = requireNotNull(value = timeline) { label }.events
                    .filterIsInstance<PianoNoteEvent>()
                    .filter { it.part != PianoPart.KEY_CHORD }
                assertTrue(label, sung.isNotEmpty())
                assertTrue(label, sung.all { it.pitch in effective })
            }
        }
    }

    @Test
    fun `the double arpeggio climbs a whole octave from the bottom on a tenor`() {
        val step = programme.steps[4]
        assertEquals(StarterPatterns.DOUBLE_ARPEGGIO, step.pattern)
        assertEquals(Direction.START_LOW, step.direction)
        assertEquals(RangeOffset(top = 10), step.rangeOffset)

        val trip = step.roundTrip(VoiceType.TENOR.range)
        val fits = trip as RoundTrip.Fits
        assertEquals(
            Pitch.parse("C3"),
            fits.startKey,
        )
        assertEquals(
            Pitch.parse("C4"),
            fits.turnKey,
        )
        assertEquals(25, fits.keys.size)
    }

    @Test
    fun `the starter patterns have distinct ids`() {
        assertEquals(
            listOf(
                "five-note-scale", "arpeggio-8-hold", "double-arpeggio", "siren-1-5-1",
                "triad", "minor-five-note-scale", "nine-note-scale", "dominant-arpeggio",
            ),
            StarterPatterns.ALL.map { it.id.value },
        )
    }

    @Test
    fun `the starter library holds the eight Patterns, the eight Sounds and the Programme`() {
        val library = StarterLibrary.LIBRARY
        assertEquals(StarterPatterns.ALL, library.patterns)
        assertEquals(StarterSounds.ALL, library.sounds)
        assertEquals(listOf(StarterProgrammes.SAVED_WARM_UP), library.programmes)
        assertEquals(
            (1..6).map { "starter-$it" },
            StarterProgrammes.SAVED_WARM_UP.steps.map { it.key.value },
        )
    }
}
