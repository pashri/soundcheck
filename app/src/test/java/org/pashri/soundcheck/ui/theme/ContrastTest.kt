package org.pashri.soundcheck.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private val palettes = mapOf("day" to DayColors, "night" to NightColors)

    private fun assertReadable(name: String, text: Color, background: Color) {
        val ratio = contrastRatio(foreground = text, background = background)
        assertTrue("$name is only ${"%.2f".format(ratio)}:1", ratio >= MINIMUM_TEXT_CONTRAST)
    }

    @Test
    fun `black on white is the maximum contrast of 21 to 1`() {
        val ratio = contrastRatio(foreground = Color.Black, background = Color.White)
        assertEquals(21.0, ratio, 0.01)
    }

    @Test
    fun `every text colour is readable on its background in both themes`() {
        palettes.forEach { (theme, c) ->
            assertReadable(name = "$theme ink on paper", text = c.ink, background = c.paper)
            assertReadable(name = "$theme muted on paper", text = c.muted, background = c.paper)
            assertReadable(
                name = "$theme accent text on paper",
                text = c.accentText,
                background = c.paper,
            )
            assertReadable(
                name = "$theme on-accent on accent",
                text = c.onAccent,
                background = c.accent,
            )
            assertReadable(name = "$theme paper on ink", text = c.paper, background = c.ink)
        }
    }

    @Test
    fun `text is readable on raised surfaces such as dialogs and menus`() {
        palettes.forEach { (theme, c) ->
            assertReadable(name = "$theme ink on raised", text = c.ink, background = c.raised)
            assertReadable(name = "$theme muted on raised", text = c.muted, background = c.raised)
            assertReadable(
                name = "$theme accent text on raised",
                text = c.accentText,
                background = c.raised,
            )
            assertReadable(
                name = "$theme a dialog button's accent on raised",
                text = c.accent,
                background = c.raised,
            )
        }
    }

    @Test
    fun `black keys and key edges stand out against white keys in both themes`() {
        palettes.forEach { (theme, c) ->
            val ratio = contrastRatio(foreground = c.blackKey, background = c.key)
            assertTrue("$theme black on white key is ${"%.2f".format(ratio)}:1", ratio >= 3.0)
            val edge = contrastRatio(foreground = c.keyBorder, background = c.key)
            assertTrue("$theme key edge is ${"%.2f".format(edge)}:1", edge >= 3.0)
        }
    }

    @Test
    fun `the key and the top note differ in lightness, and their rings show on both`() {
        palettes.forEach { (theme, c) ->
            val marks = contrastRatio(foreground = c.accent, background = c.topKey)
            assertTrue("$theme key against top note is ${"%.2f".format(marks)}:1", marks >= 1.5)
            val onKey = contrastRatio(foreground = c.keyBorder, background = c.accent)
            assertTrue("$theme ring on the key is ${"%.2f".format(onKey)}:1", onKey >= 3.0)
            val onTop = contrastRatio(foreground = c.keyBorder, background = c.topKey)
            assertTrue("$theme ring on the top note is ${"%.2f".format(onTop)}:1", onTop >= 3.0)
        }
    }

    @Test
    fun `a pressed key differs in lightness from the plain and the tinted keys`() {
        palettes.forEach { (theme, c) ->
            val plain = contrastRatio(foreground = c.keyPressed, background = c.key)
            assertTrue("$theme pressed on plain is ${"%.2f".format(plain)}:1", plain >= 1.3)
            val tint = contrastRatio(foreground = c.keyPressed, background = c.keyTint)
            assertTrue("$theme pressed on tint is ${"%.2f".format(tint)}:1", tint >= 1.3)
            val black = contrastRatio(foreground = c.blackKeyPressed, background = c.blackKey)
            assertTrue("$theme pressed black is ${"%.2f".format(black)}:1", black >= 1.3)
        }
    }

    private companion object {
        const val MINIMUM_TEXT_CONTRAST = 4.5
    }
}
