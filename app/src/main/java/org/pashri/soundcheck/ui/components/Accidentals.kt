package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.warmup.Accidental

/**
 * A ♭, ♮ or ♯ drawn from the staff's strokes rather than a font, so all three are the same
 * size whatever font the phone falls back to. It is sized in dp, like an icon, and says
 * nothing to TalkBack: the control it sits in speaks the word.
 *
 * @param accidental which one.
 * @param color its ink.
 * @param modifier modifier for the drawing.
 * @param space the staff space it is drawn at; the mark is 1.4 spaces wide and 2.7 high.
 */
@Composable
fun AccidentalMark(
    accidental: Accidental,
    color: Color,
    modifier: Modifier = Modifier,
    space: Dp = MARK_SPACE,
) {
    Canvas(
        modifier = modifier.size(
            width = space * MARK_WIDTH_SPACES,
            height = space * MARK_HEIGHT_SPACES,
        ),
    ) {
        val gap = space.toPx()
        val (x, y) = centredOrigin(accidental = accidental)
        drawAccidentalGlyph(
            accidental = accidental,
            origin = Offset(x = x * gap, y = y * gap),
            gap = gap,
            color = color,
        )
    }
}

/**
 * Draws [accidental] with its left edge at [origin] and its note's centre line through it,
 * [gap] pixels to a staff space.
 *
 * @param accidental which one.
 * @param origin the glyph's left edge on its note's centre line.
 * @param gap pixels per staff space.
 * @param color its ink.
 */
internal fun DrawScope.drawAccidentalGlyph(
    accidental: Accidental,
    origin: Offset,
    gap: Float,
    color: Color,
) {
    val glyph = glyphOf(accidental = accidental)
    glyph.lines.forEach { line ->
        drawLine(
            color = color,
            start = origin + Offset(x = gap * line.x1, y = gap * line.y1),
            end = origin + Offset(x = gap * line.x2, y = gap * line.y2),
            strokeWidth = gap * line.width,
        )
    }
    glyph.bowlStart?.let { start ->
        drawPath(
            path = spacePath(origin = origin, gap = gap, start = start, curves = glyph.bowl),
            color = color,
            style = Stroke(width = gap * FLAT_BOWL_STROKE, cap = StrokeCap.Round),
        )
    }
}

/**
 * How wide [accidental] is set on the staff, in staff spaces.
 *
 * @param accidental which one.
 * @return its width in staff spaces.
 */
internal fun accidentalWidth(accidental: Accidental): Float =
    glyphOf(accidental = accidental).width

/**
 * A path in staff spaces from [origin]: from [start] through cubic curves, six numbers each.
 * The clefs and the ♭'s bowl are drawn with it.
 *
 * @param origin where the path's (0, 0) sits, in pixels.
 * @param gap pixels per staff space.
 * @param start the first point, in staff spaces.
 * @param curves the curves' control and end points, six numbers each, in staff spaces.
 * @return the path in pixels.
 */
internal fun spacePath(
    origin: Offset,
    gap: Float,
    start: Pair<Float, Float>,
    curves: List<Float>,
): Path = Path().apply {
    moveTo(x = origin.x + gap * start.first, y = origin.y + gap * start.second)
    curves.chunked(size = 6).forEach { c ->
        cubicTo(
            x1 = origin.x + gap * c[0],
            y1 = origin.y + gap * c[1],
            x2 = origin.x + gap * c[2],
            y2 = origin.y + gap * c[3],
            x3 = origin.x + gap * c[4],
            y3 = origin.y + gap * c[5],
        )
    }
}

/**
 * The box an accidental's ink stays inside, in staff spaces from its origin, counting each
 * stroke's width and the ♭ bowl's control points.
 *
 * @property left the leftmost ink.
 * @property top the highest ink; y grows downwards.
 * @property right the rightmost ink.
 * @property bottom the lowest ink.
 */
internal data class GlyphBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Where [accidental]'s ink reaches, in staff spaces from its origin.
 *
 * @param accidental which one.
 * @return its bounds.
 */
internal fun accidentalBounds(accidental: Accidental): GlyphBounds {
    val glyph = glyphOf(accidental = accidental)
    val lineXs = glyph.lines.flatMap { spread(values = listOf(it.x1, it.x2), by = it.width) }
    val lineYs = glyph.lines.flatMap { spread(values = listOf(it.y1, it.y2), by = it.width) }
    val bowl = glyph.bowlStart?.let { listOf(it.first, it.second) + glyph.bowl }.orEmpty()
    val bowlXs = bowl.filterIndexed { index, _ -> index % 2 == 0 }
    val bowlYs = bowl.filterIndexed { index, _ -> index % 2 == 1 }
    val xs = lineXs + spread(values = bowlXs, by = FLAT_BOWL_STROKE)
    val ys = lineYs + spread(values = bowlYs, by = FLAT_BOWL_STROKE)
    return GlyphBounds(left = xs.min(), top = ys.min(), right = xs.max(), bottom = ys.max())
}

/** Each of [values] moved half a stroke of width [by] either way. */
private fun spread(values: List<Float>, by: Float): List<Float> =
    values.flatMap { listOf(it - by / 2, it + by / 2) }

/**
 * The origin, in staff spaces from the mark's top left, that centres [accidental]'s ink in
 * an [AccidentalMark].
 *
 * @param accidental which one.
 * @return the origin's x and y in staff spaces.
 */
internal fun centredOrigin(accidental: Accidental): Pair<Float, Float> {
    val bounds = accidentalBounds(accidental = accidental)
    val x = MARK_WIDTH_SPACES / 2 - (bounds.left + bounds.right) / 2
    val y = MARK_HEIGHT_SPACES / 2 - (bounds.top + bounds.bottom) / 2
    return x to y
}

/** An [AccidentalMark]'s size in staff spaces: room for the widest ♯ and the tallest ♭. */
internal const val MARK_WIDTH_SPACES = 1.4f
internal const val MARK_HEIGHT_SPACES = 2.7f

/** A button's accidental is drawn at a 10 dp staff space: a ♯ about 26 dp tall. */
private val MARK_SPACE = 10.dp

private fun glyphOf(accidental: Accidental): AccidentalGlyph = when (accidental) {
    Accidental.FLAT -> FLAT_GLYPH
    Accidental.NATURAL -> NATURAL_GLYPH
    Accidental.SHARP -> SHARP_GLYPH
}

/**
 * A straight stroke of an accidental, in staff spaces from the glyph's left edge at its
 * note's centre line; y grows downwards.
 */
private class GlyphLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val width: Float,
)

/**
 * An accidental drawn as strokes: its [width] in staff spaces, its straight [lines], and for
 * a ♭ the bowl, from [bowlStart] through the cubic curves in [bowl] (six numbers each).
 */
private class AccidentalGlyph(
    val width: Float,
    val lines: List<GlyphLine>,
    val bowlStart: Pair<Float, Float>? = null,
    val bowl: List<Float> = emptyList(),
)

/** A ♯, 2.5 spaces tall: two thin uprights, the left one lower, and two thick rising bars. */
private val SHARP_GLYPH = AccidentalGlyph(
    width = 1.0f,
    lines = listOf(
        GlyphLine(x1 = 0.3f, y1 = -1.05f, x2 = 0.3f, y2 = 1.25f, width = 0.12f),
        GlyphLine(x1 = 0.7f, y1 = -1.25f, x2 = 0.7f, y2 = 1.05f, width = 0.12f),
        GlyphLine(x1 = 0f, y1 = -0.3f, x2 = 1.0f, y2 = -0.6f, width = 0.32f),
        GlyphLine(x1 = 0f, y1 = 0.6f, x2 = 1.0f, y2 = 0.3f, width = 0.32f),
    ),
)

/** A ♭, 2.2 spaces tall: an upright rising well above the note, with a bowl round it. */
private val FLAT_GLYPH = AccidentalGlyph(
    width = 0.9f,
    lines = listOf(GlyphLine(x1 = 0.08f, y1 = -1.75f, x2 = 0.08f, y2 = 0.45f, width = 0.13f)),
    bowlStart = 0.08f to 0.45f,
    bowl = listOf(
        0.55f, 0.15f, 0.95f, -0.25f, 0.7f, -0.5f,
        0.5f, -0.7f, 0.2f, -0.5f, 0.08f, -0.2f,
    ),
)
private const val FLAT_BOWL_STROKE = 0.17f

/** A ♮, 2.5 spaces tall: offset uprights joined by two thick rising bars. */
private val NATURAL_GLYPH = AccidentalGlyph(
    width = 0.72f,
    lines = listOf(
        GlyphLine(x1 = 0.08f, y1 = -1.25f, x2 = 0.08f, y2 = 0.5f, width = 0.12f),
        GlyphLine(x1 = 0.64f, y1 = -0.5f, x2 = 0.64f, y2 = 1.25f, width = 0.12f),
        GlyphLine(x1 = 0.08f, y1 = -0.2f, x2 = 0.64f, y2 = -0.42f, width = 0.3f),
        GlyphLine(x1 = 0.08f, y1 = 0.42f, x2 = 0.64f, y2 = 0.2f, width = 0.3f),
    ),
)
