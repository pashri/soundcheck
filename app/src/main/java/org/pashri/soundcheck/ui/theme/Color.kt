package org.pashri.soundcheck.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Manuscript palette.
 *
 * @property paper page background.
 * @property ink primary text and strokes.
 * @property muted secondary text.
 * @property rule divider lines.
 * @property faint decorative strokes and unselected outlines; never used for text.
 * @property accent vermilion fills and highlights.
 * @property onAccent text drawn on an [accent] fill.
 * @property accentText vermilion used as text on [paper].
 */
@Immutable
data class ManuscriptColors(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val rule: Color,
    val faint: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
)

/** Manuscript by day: warm paper, ink and vermilion. */
val DayColors: ManuscriptColors = ManuscriptColors(
    paper = Color(0xFFF4EEE3),
    ink = Color(0xFF1D1B18),
    muted = Color(0xFF5F584D),
    rule = Color(0xFFD6CCBA),
    faint = Color(0xFF8F8676),
    accent = Color(0xFFC23B22),
    onAccent = Color(0xFFFFF8F0),
    accentText = Color(0xFFA8321C),
)

/** Manuscript at night: dark ink ground, cream text and a lighter vermilion. */
val NightColors: ManuscriptColors = ManuscriptColors(
    paper = Color(0xFF1C1A17),
    ink = Color(0xFFEFE6D6),
    muted = Color(0xFFABA290),
    rule = Color(0xFF3A362F),
    faint = Color(0xFF6B6455),
    accent = Color(0xFFF0785D),
    onAccent = Color(0xFF1C1A17),
    accentText = Color(0xFFF0785D),
)
