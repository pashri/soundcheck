package org.pashri.soundcheck.ui.components

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class FitTest {
    @Test
    fun `options that fit side by side stay in one row`() {
        assertEquals(4, fittingColumns(count = 4, widest = 50f, available = 400f))
        assertEquals(3, fittingColumns(count = 3, widest = 100f, available = 300f))
    }

    @Test
    fun `four options too wide for one row fall into two rows of two`() {
        assertEquals(2, fittingColumns(count = 4, widest = 120f, available = 400f))
        assertEquals(2, fittingColumns(count = 4, widest = 190f, available = 400f))
    }

    @Test
    fun `options too wide for two columns stack one per row`() {
        assertEquals(1, fittingColumns(count = 4, widest = 250f, available = 400f))
        assertEquals(1, fittingColumns(count = 2, widest = 500f, available = 400f))
    }

    @Test
    fun `the gap between columns counts against their width`() {
        assertEquals(2, fittingColumns(count = 2, widest = 195f, available = 400f))
        assertEquals(
            1,
            fittingColumns(count = 2, widest = 195f, available = 400f, spacing = 20f),
        )
    }

    @Test
    fun `rows are balanced rather than leaving one option alone`() {
        assertEquals(3, fittingColumns(count = 5, widest = 90f, available = 400f))
        assertEquals(3, fittingColumns(count = 6, widest = 120f, available = 400f))
    }

    @Test
    fun `a label is split into the words a line may break between`() {
        assertEquals(listOf("Tap", "tempo"), words(text = "Tap tempo"))
        assertEquals(listOf("Patterns", "9"), words(text = " Patterns  9 "))
        assertEquals(emptyList<String>(), words(text = " "))
    }

    @Test
    fun `the header arrangement asks the flow row for a minimum gap`() {
        assertEquals(12.dp, SpaceBetweenAtLeast(minimum = 12.dp).spacing)
    }

    @Test
    fun `the header arrangement pushes the note to the far end`() {
        val positions = IntArray(size = 2)
        with(receiver = SpaceBetweenAtLeast(minimum = 12.dp)) {
            Density(density = 1f).arrange(
                totalSize = 100,
                sizes = intArrayOf(10, 20),
                layoutDirection = LayoutDirection.Ltr,
                outPositions = positions,
            )
        }
        assertArrayEquals(intArrayOf(0, 80), positions)
    }

    @Test
    fun `a title alone on its line starts at the edge`() {
        val positions = IntArray(size = 1)
        with(receiver = SpaceBetweenAtLeast(minimum = 12.dp)) {
            Density(density = 1f).arrange(
                totalSize = 100,
                sizes = intArrayOf(60),
                layoutDirection = LayoutDirection.Ltr,
                outPositions = positions,
            )
        }
        assertArrayEquals(intArrayOf(0), positions)
    }
}
