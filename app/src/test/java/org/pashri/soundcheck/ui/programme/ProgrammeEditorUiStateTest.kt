package org.pashri.soundcheck.ui.programme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.ui.warmup.startProblemMessage
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.addProgramme
import org.pashri.soundcheck.warmup.updateStep
import org.pashri.soundcheck.warmup.withRangeOffset

class ProgrammeEditorUiStateTest {
    private val starter = StarterProgrammes.SAVED_WARM_UP.id
    private val morning = ProgrammeId("morning")
    private val tenor = VoiceType.TENOR.range

    private fun state(
        library: Library = StarterLibrary.LIBRARY,
        id: ProgrammeId = starter,
        problem: StartOutcome? = null,
    ): ProgrammeEditorUiState? =
        programmeEditorUiState(library = library, id = id, range = tenor, problem = problem)

    @Test
    fun `the starter Programme lists its six Steps with their Sounds and summaries`() {
        val state = checkNotNull(state())
        assertEquals("Starter warm-up", state.name)
        assertEquals("6 STEPS", state.stepsLabel)
        assertEquals(
            StepRow(
                key = StepKey("starter-1"),
                number = 1,
                sound = "lip trill",
                meta = "5-note scale · 90 bpm · from low · top +2",
                warning = null,
            ),
            state.rows.first(),
        )
        assertEquals(
            listOf("lip trill", "hum", "mim", "oo", "neh", "mah"),
            state.rows.map { it.sound },
        )
        assertEquals("Start Starter warm-up", state.startLabel)
        assertTrue(state.canStart)
        assertEquals(
            "Its 6 Steps go with it. Patterns and Sounds stay in the library.",
            state.deleteNote,
        )
    }

    @Test
    fun `a Step that doesn't fit its Range warns with the numbers`() {
        val ref = StepRef(programmeId = starter, key = StepKey("starter-5"))
        val narrowed = StarterLibrary.LIBRARY.updateStep(ref = ref) {
            it.withRangeOffset(bottom = 0, top = -3)
        }
        val rows = checkNotNull(state(library = narrowed)).rows
        assertEquals(
            "Needs 19 half-steps; this Step's Range has 18. It will be skipped.",
            rows[4].warning,
        )
        assertTrue(rows.filterIndexed { index, _ -> index != 4 }.all { it.warning == null })
    }

    @Test
    fun `an empty Programme can't start`() {
        val library = StarterLibrary.LIBRARY.addProgramme(id = morning, name = "Morning")
        val state = checkNotNull(state(library = library, id = morning))
        assertEquals("NO STEPS YET", state.stepsLabel)
        assertTrue(state.rows.isEmpty())
        assertFalse(state.canStart)
        assertEquals("Patterns and Sounds stay in the library.", state.deleteNote)
    }

    @Test
    fun `a Programme that isn't in the library shows nothing`() {
        assertNull(state(id = ProgrammeId("nowhere")))
    }

    @Test
    fun `renaming checks against the other Programmes' names only`() {
        assertEquals(emptyList<String>(), checkNotNull(state()).otherNames)
        val library = StarterLibrary.LIBRARY.addProgramme(id = morning, name = "Morning")
        assertEquals(listOf("Morning"), checkNotNull(state(library = library)).otherNames)
    }

    @Test
    fun `a start problem is explained`() {
        assertNull(checkNotNull(state()).problem)
        assertEquals(
            startProblemMessage(StartOutcome.NOTHING_FITS),
            checkNotNull(state(problem = StartOutcome.NOTHING_FITS)).problem,
        )
    }
}
