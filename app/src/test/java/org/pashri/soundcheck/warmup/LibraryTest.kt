package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private val library = StarterLibrary.LIBRARY
    private val starter = StarterProgrammes.SAVED_WARM_UP
    private val firstStep = starter.steps.first()

    @Test
    fun `the saved starter Programme plays the same Steps as the built-in one`() {
        assertEquals(StarterProgrammes.WARM_UP, library.programmeToPlay(starter.id))
    }

    @Test
    fun `a Step follows an edit to its Pattern`() {
        val wider = StarterPatterns.TRIAD.copy(notes = PatternNotation.parse("1 3 5 8 5 3 1h"))
        val edited = library.copy(
            patterns = library.patterns.map { if (it.id == wider.id) wider else it },
        )
        val hum = requireNotNull(edited.programmeToPlay(starter.id)).steps[1]
        assertEquals(wider, hum.pattern)
        assertEquals(12, hum.pattern.span.halfSteps)
    }

    @Test
    fun `a Step to play carries its Sound's label from the library`() {
        val hmm = StarterSounds.HUM.copy(label = "hmm")
        val renamed = library.copy(
            sounds = library.sounds.map { if (it.id == hmm.id) hmm else it },
        )
        val steps = requireNotNull(renamed.programmeToPlay(starter.id)).steps
        val hums = steps.filter { it.soundId == StarterSounds.HUM.id }
        assertTrue(hums.isNotEmpty())
        assertTrue(hums.all { it.soundLabel == "hmm" })
    }

    @Test
    fun `an unknown Programme has nothing to play`() {
        assertNull(library.programmeToPlay(ProgrammeId("nowhere")))
    }

    @Test
    fun `a Step is found by its key`() {
        assertEquals(firstStep, starter.step(StepKey("starter-1")))
        assertNull(starter.step(StepKey("starter-99")))
    }

    @Test
    fun `a Step naming a Pattern the library lacks is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            library.copy(patterns = library.patterns - StarterPatterns.TRIAD)
        }
    }

    @Test
    fun `a Step naming a Sound the library lacks is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            library.copy(sounds = library.sounds - StarterSounds.HUM)
        }
    }

    @Test
    fun `two Patterns sharing an id are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            library.copy(patterns = library.patterns + StarterPatterns.TRIAD.copy(name = "Again"))
        }
    }

    @Test
    fun `two Programmes sharing an id are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            library.copy(programmes = listOf(starter, starter.copy(name = "Copy")))
        }
    }

    @Test
    fun `two Steps sharing a key in one Programme are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            starter.copy(steps = listOf(firstStep, firstStep))
        }
    }

    @Test
    fun `a saved Step outside 30 to 300 bpm is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { firstStep.copy(bpm = 29) }
        assertThrows(IllegalArgumentException::class.java) { firstStep.copy(bpm = 301) }
    }

    @Test
    fun `a clip name is letters, digits and dashes ending in wav`() {
        assertEquals("a-1.wav", ClipName("a-1.wav").value)
        listOf("../library.json", "a/b.wav", "clip.mp3", ".wav", "").forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { ClipName(name) }
        }
    }

    @Test
    fun `an empty recorded clip is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecordedClip(name = ClipName("a.wav"), lengthMs = 0)
        }
    }
}
