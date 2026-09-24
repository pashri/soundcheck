package org.pashri.soundcheck.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private val palettes = mapOf("day" to DayColors, "night" to NightColors)

    private fun assertReadable(name: String, text: Color, background: Color) {
        val ratio = contrastRatio(text, background)
        assertTrue("$name is only ${"%.2f".format(ratio)}:1", ratio >= MINIMUM_TEXT_CONTRAST)
    }

    @Test
    fun `black on white is the maximum contrast of 21 to 1`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
    }

    @Test
    fun `every text colour is readable on its background in both themes`() {
        palettes.forEach { (theme, c) ->
            assertReadable("$theme ink on paper", c.ink, c.paper)
            assertReadable("$theme muted on paper", c.muted, c.paper)
            assertReadable("$theme accent text on paper", c.accentText, c.paper)
            assertReadable("$theme on-accent on accent", c.onAccent, c.accent)
            assertReadable("$theme paper on ink", c.paper, c.ink)
        }
    }

    private companion object {
        const val MINIMUM_TEXT_CONTRAST = 4.5
    }
}
