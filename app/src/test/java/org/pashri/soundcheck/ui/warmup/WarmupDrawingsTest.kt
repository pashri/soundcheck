package org.pashri.soundcheck.ui.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.ui.theme.DayColors
import org.pashri.soundcheck.ui.theme.NightColors
import org.pashri.soundcheck.ui.theme.contrastRatio

class WarmupDrawingsTest {
    private val palettes = mapOf("day" to DayColors, "night" to NightColors)

    @Test
    fun `a pressed key and a pressed top note take their own pressed colours`() {
        palettes.values.forEach { c ->
            listOf(false, true).forEach { black ->
                assertEquals(
                    c.accentPressed,
                    keyFill(mark = KeyMark.ROOT, pressed = true, black = black, colors = c),
                )
                assertEquals(
                    c.topKeyPressed,
                    keyFill(mark = KeyMark.TOP, pressed = true, black = black, colors = c),
                )
                assertEquals(
                    c.accent,
                    keyFill(mark = KeyMark.ROOT, pressed = false, black = black, colors = c),
                )
                assertEquals(
                    c.topKey,
                    keyFill(mark = KeyMark.TOP, pressed = false, black = black, colors = c),
                )
            }
        }
    }

    @Test
    fun `other pressed keys darken white and lighten black`() {
        palettes.values.forEach { c ->
            val sung = keyFill(mark = KeyMark.SUNG, pressed = true, black = false, colors = c)
            val plain = keyFill(mark = KeyMark.PLAIN, pressed = true, black = false, colors = c)
            val black = keyFill(mark = KeyMark.SUNG, pressed = true, black = true, colors = c)
            assertEquals(c.keyPressed, sung)
            assertEquals(c.keyPressed, plain)
            assertEquals(c.blackKeyPressed, black)
            assertEquals(
                c.keyTint,
                keyFill(mark = KeyMark.SUNG, pressed = false, black = false, colors = c),
            )
        }
    }

    @Test
    fun `the ring on a marked key shows at 3 to 1, pressed or not`() {
        palettes.forEach { (theme, c) ->
            listOf(c.accent, c.topKey, c.accentPressed, c.topKeyPressed).forEach { fill ->
                val ratio = contrastRatio(
                    foreground = markRing(fill = fill, colors = c),
                    background = fill,
                )
                assertTrue("$theme ring on $fill is ${"%.2f".format(ratio)}:1", ratio >= 3.0)
            }
        }
    }
}
