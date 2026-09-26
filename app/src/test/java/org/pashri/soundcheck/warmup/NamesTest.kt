package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NamesTest {
    @Test
    fun `a blank name is refused`() {
        assertEquals(NameProblem.BLANK, nameProblem(name = "", taken = emptyList()))
        assertEquals(NameProblem.BLANK, nameProblem(name = "   ", taken = emptyList()))
    }

    @Test
    fun `a name over 40 characters is refused, counting after trimming`() {
        assertEquals(NameProblem.TOO_LONG, nameProblem(name = "x".repeat(41), taken = emptyList()))
        assertNull(nameProblem(name = "x".repeat(40), taken = emptyList()))
        assertNull(nameProblem(name = "  ${"x".repeat(40)} ", taken = emptyList()))
    }

    @Test
    fun `a name already used, ignoring case and spaces, is refused`() {
        assertEquals(
            NameProblem.TAKEN,
            nameProblem(name = " starter WARM-UP ", taken = listOf("Starter warm-up")),
        )
    }

    @Test
    fun `a fresh name is fine`() {
        assertNull(nameProblem(name = "Morning", taken = listOf("Starter warm-up")))
    }

    @Test
    fun `a unique name counts up from 2`() {
        assertEquals("New programme", uniqueName(base = "New programme", taken = emptyList()))
        assertEquals(
            "New programme 2",
            uniqueName(base = "New programme", taken = listOf("New programme")),
        )
        assertEquals(
            "New programme 3",
            uniqueName(base = "New programme", taken = listOf("new programme", "New programme 2")),
        )
    }
}
