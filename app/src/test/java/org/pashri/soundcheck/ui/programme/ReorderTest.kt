package org.pashri.soundcheck.ui.programme

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    private val even = listOf(100, 100, 100)

    @Test
    fun `a short drag leaves the row where it was`() {
        assertEquals(1, dropIndex(from = 1, dragPx = 40f, heights = even))
        assertEquals(1, dropIndex(from = 1, dragPx = -40f, heights = even))
    }

    @Test
    fun `dragging past half of the next row moves one place`() {
        assertEquals(1, dropIndex(from = 0, dragPx = 60f, heights = even))
        assertEquals(1, dropIndex(from = 2, dragPx = -60f, heights = even))
    }

    @Test
    fun `a long drag stops at either end`() {
        assertEquals(2, dropIndex(from = 0, dragPx = 1_000f, heights = even))
        assertEquals(0, dropIndex(from = 2, dragPx = -1_000f, heights = even))
    }

    @Test
    fun `each row is passed at half its own height`() {
        val uneven = listOf(100, 60, 200)
        assertEquals(1, dropIndex(from = 0, dragPx = 130f, heights = uneven))
        assertEquals(0, dropIndex(from = 2, dragPx = -130f, heights = uneven))
    }
}
