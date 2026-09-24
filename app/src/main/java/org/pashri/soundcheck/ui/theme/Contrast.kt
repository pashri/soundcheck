package org.pashri.soundcheck.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * WCAG contrast ratio between two colours, from 1:1 (identical) to 21:1 (black on white).
 *
 * @param foreground the text colour.
 * @param background the colour behind it.
 * @return the ratio's left-hand number, e.g. 4.5 for 4.5:1.
 */
fun contrastRatio(foreground: Color, background: Color): Double {
    val a = foreground.luminance() + LUMINANCE_OFFSET
    val b = background.luminance() + LUMINANCE_OFFSET
    return (maxOf(a, b) / minOf(a, b)).toDouble()
}

private const val LUMINANCE_OFFSET = 0.05f
