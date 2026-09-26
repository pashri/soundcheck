package org.pashri.soundcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import org.pashri.soundcheck.warmup.Usage

class LibraryTextTest {
    @Test
    fun `what a delete takes with it is said in full`() {
        assertEquals("No Step uses it.", usageText(emptyList()))
        assertEquals(
            "1 Step in Starter warm-up uses it; that Step goes too.",
            usageText(listOf(Usage(programmeName = "Starter warm-up", steps = 1))),
        )
        assertEquals(
            "3 Steps in Starter warm-up and Morning use it; they go too.",
            usageText(
                listOf(
                    Usage(programmeName = "Starter warm-up", steps = 1),
                    Usage(programmeName = "Morning", steps = 2),
                ),
            ),
        )
        assertEquals(
            "3 Steps in A, B and C use it; they go too.",
            usageText(listOf("A", "B", "C").map { Usage(programmeName = it, steps = 1) }),
        )
    }

    @Test
    fun `names are joined as a sentence joins them`() {
        assertEquals("", joinedWithAnd(emptyList()))
        assertEquals("A", joinedWithAnd(listOf("A")))
        assertEquals("A and B", joinedWithAnd(listOf("A", "B")))
        assertEquals("A, B and C", joinedWithAnd(listOf("A", "B", "C")))
    }

    @Test
    fun `a library row says how many Steps use it`() {
        assertEquals("Not in any Step", usageLabel(emptyList()))
        assertEquals("Used in 1 Step", usageLabel(listOf(Usage(programmeName = "A", steps = 1))))
        assertEquals(
            "Used in 3 Steps",
            usageLabel(
                listOf(
                    Usage(programmeName = "A", steps = 1),
                    Usage(programmeName = "B", steps = 2),
                ),
            ),
        )
    }
}
