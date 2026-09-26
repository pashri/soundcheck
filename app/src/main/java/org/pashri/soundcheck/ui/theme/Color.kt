package org.pashri.soundcheck.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Manuscript palette.
 *
 * @property paper page background.
 * @property ink primary text and strokes.
 * @property muted secondary text, and the borders of text fields.
 * @property rule divider lines.
 * @property faint decorative strokes and unselected outlines; never used for text.
 * @property accent vermilion fills and highlights.
 * @property onAccent text drawn on an [accent] fill.
 * @property accentText vermilion used as text on [paper].
 * @property raised what dialogs, menus and text fields sit on, a step off the page.
 * @property key a white key on the playing screen's keyboard.
 * @property keyTint a white key between the lowest and highest notes being sung.
 * @property keyBorder the line between white keys.
 * @property blackKey a black key.
 * @property topKey the key of the Pattern's highest sung note.
 * @property keyPressed a white key being sung, drawn pressed down: darker than both [key] and
 *     [keyTint] by at least 1.3:1, so it reads without relying on hue.
 * @property blackKeyPressed a black key being sung, a little lighter than [blackKey].
 * @property accentPressed the Iteration's key while it is being sung: a darker [accent], at
 *     least 1.3:1 off it.
 * @property topKeyPressed the top note's key while it is being sung: a darker [topKey], at
 *     least 1.3:1 off it by day and 1.8:1 at night, where the pale rose needs more.
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
    val raised: Color,
    val key: Color,
    val keyTint: Color,
    val keyBorder: Color,
    val blackKey: Color,
    val topKey: Color,
    val keyPressed: Color,
    val blackKeyPressed: Color,
    val accentPressed: Color,
    val topKeyPressed: Color,
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
    raised = Color(0xFFFFFDF8),
    key = Color(0xFFFFFDF8),
    keyTint = Color(0xFFEADCCB),
    keyBorder = Color(0xFF1D1B18),
    blackKey = Color(0xFF1D1B18),
    topKey = Color(0xFFD98A78),
    keyPressed = Color(0xFFCFBEA9),
    blackKeyPressed = Color(0xFF4A433B),
    accentPressed = Color(0xFF8F2716),
    topKeyPressed = Color(0xFFB06A5A),
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
    raised = Color(0xFF2A2622),
    key = Color(0xFFBFB5A4),
    keyTint = Color(0xFFA08E7C),
    keyBorder = Color(0xFF1C1A17),
    blackKey = Color(0xFF0F0E0C),
    topKey = Color(0xFFFAC3B3),
    keyPressed = Color(0xFF8A7A69),
    blackKeyPressed = Color(0xFF4A433B),
    accentPressed = Color(0xFFB8513A),
    topKeyPressed = Color(0xFFC27A6A),
)
