package org.pashri.soundcheck.ui.step

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.updateStep
import org.pashri.soundcheck.warmup.withBpm
import org.pashri.soundcheck.warmup.withRangeOffset

class StepEditorUiStateTest {
    private val starter = StarterProgrammes.SAVED_WARM_UP.id

    private fun ref(number: Int): StepRef =
        StepRef(programmeId = starter, key = StepKey("starter-$number"))

    private fun state(
        number: Int,
        change: (SavedStep) -> SavedStep = { it },
    ): StepEditorUiState? {
        val library: Library = StarterLibrary.LIBRARY.updateStep(ref(number), change)
        return stepEditorUiState(
            library = library,
            ref = ref(number),
            range = VoiceType.TENOR.range,
        )
    }

    @Test
    fun `the first starter Step shows its Sound, Pattern, tempo and Range Offset`() {
        val state = checkNotNull(state(number = 1))
        assertEquals("lip trill", state.title)
        assertEquals("STEP 1 OF 6", state.stepLabel)
        assertEquals("Starter warm-up", state.programmeName)
        assertEquals("5-note scale", state.patternName)
        assertEquals("1 2 3 4 5 4 3 2 1 · major", state.patternDetail)
        assertEquals("lip trill", state.soundLabel)
        assertEquals("phone voice", state.soundDetail)
        assertEquals("90 bpm", state.bpm)
        assertEquals(Direction.START_LOW, state.direction)
        assertEquals("0", state.bottom)
        assertEquals("+2", state.top)
        assertEquals("C3 – B4 · 33 Iterations", state.tripLabel)
        assertNull(state.warning)
        assertTrue(state.guideMelody)
    }

    @Test
    fun `the double arpeggio climbs to G5 on a tenor`() {
        val state = checkNotNull(state(number = 5))
        assertEquals("1 3 5 8 10 12 11 9 7 5 4 2 1 · major", state.patternDetail)
        assertEquals("+10", state.top)
        assertEquals("C3 – G5 · 25 Iterations", state.tripLabel)
    }

    @Test
    fun `a Range Offset that leaves too little room warns with the numbers`() {
        val state = checkNotNull(state(number = 5) { it.withRangeOffset(bottom = 0, top = -3) })
        assertEquals("−3", state.top)
        assertEquals("C3 – F♯4", state.tripLabel)
        assertEquals(
            "Needs 19 half-steps; this Step's Range has 18. It will be skipped.",
            state.warning,
        )
    }

    @Test
    fun `an offset that closes the Range says so`() {
        val state = checkNotNull(
            state(number = 5) { it.withRangeOffset(bottom = -11, top = -11) },
        )
        assertEquals("No Range left", state.tripLabel)
        assertEquals(
            "Needs 19 half-steps; this Step's Range has 0. It will be skipped.",
            state.warning,
        )
    }

    @Test
    fun `the Range Offset buttons stop at 24 half-steps`() {
        val state = checkNotNull(state(number = 1) { it.withRangeOffset(bottom = 24, top = -24) })
        assertFalse(state.canLowerBottom)
        assertTrue(state.canRaiseBottom)
        assertFalse(state.canLowerTop)
        assertTrue(state.canRaiseTop)
    }

    @Test
    fun `the Range Offset buttons stop at the piano's edge`() {
        val state = checkNotNull(
            stepEditorUiState(
                library = StarterLibrary.LIBRARY,
                ref = ref(2),
                range = Range.PIANO,
            ),
        )
        assertFalse(state.canLowerBottom)
        assertTrue(state.canRaiseBottom)
        assertTrue(state.canLowerTop)
        assertFalse(state.canRaiseTop)
    }

    @Test
    fun `the tempo buttons stop at 30 and 300 bpm`() {
        val slowest = checkNotNull(state(number = 1) { it.withBpm(30) })
        assertFalse(slowest.canSlower)
        assertTrue(slowest.canFaster)
        val fastest = checkNotNull(state(number = 1) { it.withBpm(300) })
        assertTrue(fastest.canSlower)
        assertFalse(fastest.canFaster)
    }

    @Test
    fun `a Step that isn't in the library shows nothing`() {
        val library = StarterLibrary.LIBRARY
        val range = VoiceType.TENOR.range
        val missingStep = StepRef(programmeId = starter, key = StepKey("nope"))
        val missingProgramme =
            StepRef(programmeId = ProgrammeId("nope"), key = StepKey("starter-1"))
        assertNull(stepEditorUiState(library = library, ref = missingStep, range = range))
        assertNull(stepEditorUiState(library = library, ref = missingProgramme, range = range))
    }
}
