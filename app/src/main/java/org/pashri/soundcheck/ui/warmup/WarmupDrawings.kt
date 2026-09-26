package org.pashri.soundcheck.ui.warmup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.ui.components.spokenMusic
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptColors
import org.pashri.soundcheck.ui.theme.SerifFamily
import org.pashri.soundcheck.warmup.Accidental

/**
 * The Pattern on a five-line staff, as the design draws it under the Sound, with its clef:
 * heads, stems, beams, flags, ledger lines and accidentals, with the note being sung in
 * vermilion. It is sized in dp from the width it gets, so a large font never distorts it,
 * and TalkBack reads it as one sentence naming the clef.
 *
 * @param view what to draw.
 * @param modifier modifier for the drawing.
 */
@Composable
fun PatternStaff(view: StaffView, modifier: Modifier = Modifier) {
    val colors = Manuscript.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = staffGap(width = maxWidth, notes = view.layout.notes.size)
        val accidentalStyle = TextStyle(
            fontSize = with(receiver = density) { (gap * ACCIDENTAL_SIZE).toSp() },
            color = colors.ink,
        )
        val eightStyle = TextStyle(
            fontFamily = SerifFamily,
            fontSize = with(receiver = density) { (gap * EIGHT_SIZE).toSp() },
            color = colors.ink,
        )
        val glyphs = remember(key1 = measurer, key2 = accidentalStyle, key3 = eightStyle) {
            StaffGlyphs(
                flat = measurer.measure(text = Accidental.FLAT.symbol, style = accidentalStyle),
                sharp = measurer.measure(text = Accidental.SHARP.symbol, style = accidentalStyle),
                natural = measurer.measure(text = NATURAL_SIGN, style = accidentalStyle),
                eight = measurer.measure(text = "8", style = eightStyle),
            )
        }
        val steps = view.layout.top - view.layout.bottom + 2
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(gap / 2 * steps)
                .clearAndSetSemantics { contentDescription = spokenMusic(view.description) },
        ) {
            val painter = StaffPainter(
                layout = view.layout,
                now = view.now,
                colors = colors,
                glyphs = glyphs,
                gap = gap.toPx(),
            )
            with(receiver = painter) { paint() }
        }
    }
}

/**
 * The Step's Range as a keyboard, as the design draws it under the key: the key in
 * vermilion, the top sung note marked, the white keys between tinted. 64 dp high whatever
 * the font size; TalkBack reads it as one sentence.
 *
 * @param view the keys.
 * @param modifier modifier for the drawing.
 */
@Composable
fun RangeKeyboard(view: KeyboardView, modifier: Modifier = Modifier) {
    val colors = Manuscript.colors
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(KEYBOARD_HEIGHT)
            .clearAndSetSemantics { contentDescription = spokenMusic(view.description) },
    ) {
        val white = size.width / view.whites.size
        view.whites.forEachIndexed { index, key ->
            val topLeft = Offset(x = index * white, y = 0f)
            val whole = Size(width = white, height = size.height)
            drawRect(
                color = whiteColor(mark = key.mark, colors = colors),
                topLeft = topLeft,
                size = whole,
            )
            drawRect(
                color = colors.keyBorder,
                topLeft = topLeft,
                size = whole,
                style = Stroke(width = 1.dp.toPx()),
            )
            if (key.mark.outlined) drawOutline(topLeft = topLeft, size = whole, colors = colors)
        }
        val blackWidth = white * BLACK_WIDTH
        view.blacks.forEach { key ->
            val topLeft = Offset(x = (key.afterWhite + 1) * white - blackWidth / 2, y = 0f)
            val whole = Size(width = blackWidth, height = size.height * BLACK_HEIGHT)
            drawRect(
                color = blackColor(mark = key.mark, colors = colors),
                topLeft = topLeft,
                size = whole,
            )
            if (key.mark.outlined) drawOutline(topLeft = topLeft, size = whole, colors = colors)
        }
    }
}

/** The staff's ♭, ♯, ♮ and the treble clef's 8, measured once for its size. */
private class StaffGlyphs(
    val flat: TextLayoutResult,
    val sharp: TextLayoutResult,
    val natural: TextLayoutResult,
    val eight: TextLayoutResult,
)

/**
 * Draws one [StaffLayout], with [gap] pixels between staff lines. Steps become pixels
 * downwards from the layout's top; the clef takes the first [CLEF_GAPS] spaces.
 */
private class StaffPainter(
    private val layout: StaffLayout,
    private val now: Int?,
    private val colors: ManuscriptColors,
    private val glyphs: StaffGlyphs,
    private val gap: Float,
) {
    private val headWidth = gap * HEAD_WIDTH
    private val headHeight = gap * HEAD_HEIGHT

    /** Draws the staff lines and the clef, then each note, then the beams. */
    fun DrawScope.paint() {
        val clefWidth = gap * CLEF_GAPS
        val slot = (size.width - clefWidth) / layout.notes.size
        val xs = layout.notes.indices.map { clefWidth + slot * (it + 0.5f) }
        staffLines()
        drawClef()
        layout.notes.forEachIndexed { index, note ->
            drawNote(note = note, x = xs[index], color = colorOf(index))
        }
        layout.beams.forEach { beam ->
            val top = layout.notes[beam.first].stemTop ?: return@forEach
            drawBeam(from = stemX(xs[beam.first]), to = stemX(xs[beam.last]), top = top)
        }
    }

    private fun colorOf(index: Int): Color = if (index == now) colors.accent else colors.ink

    private fun yOf(step: Int): Float = (layout.top - step + 1) * gap / 2

    private fun stemX(x: Float): Float = x + headWidth / 2 - gap * STEM_WIDTH / 2

    private fun DrawScope.staffLines() {
        for (step in 0..STAFF_SPAN step 2) {
            val y = yOf(step)
            drawLine(
                color = colors.faint,
                start = Offset(x = 0f, y = y),
                end = Offset(x = size.width, y = y),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }

    /**
     * The clef, drawn from paths in staff spaces: a treble clef curls round the G line
     * (step 2), a bass clef's head sits on the F line (step 6) with its two dots either side.
     */
    private fun DrawScope.drawClef() {
        val x = gap * CLEF_LEFT
        when (layout.clef) {
            Clef.BASS -> drawBassClef(origin = Offset(x = x, y = yOf(BASS_CLEF_LINE)))
            Clef.TREBLE, Clef.TREBLE_8VB ->
                drawTrebleClef(origin = Offset(x = x, y = yOf(TREBLE_CLEF_LINE)))
        }
        if (layout.clef != Clef.TREBLE_8VB) return
        val eight = glyphs.eight
        val topLeft = Offset(
            x = x + gap * EIGHT_X - eight.size.width / 2f,
            y = yOf(TREBLE_CLEF_LINE) + gap * EIGHT_Y,
        )
        drawText(textLayoutResult = eight, topLeft = topLeft)
    }

    private fun DrawScope.drawTrebleClef(origin: Offset) {
        val path = clefPath(origin = origin, start = TREBLE_START, curves = TREBLE_CURVES)
        drawPath(
            path = path,
            color = colors.ink,
            style = Stroke(width = gap * CLEF_STROKE, cap = StrokeCap.Round),
        )
        drawCircle(
            color = colors.ink,
            radius = gap * TREBLE_DOT_RADIUS,
            center = origin + Offset(x = gap * TREBLE_DOT.first, y = gap * TREBLE_DOT.second),
        )
    }

    private fun DrawScope.drawBassClef(origin: Offset) {
        val path = clefPath(origin = origin, start = BASS_START, curves = BASS_CURVES)
        drawPath(
            path = path,
            color = colors.ink,
            style = Stroke(width = gap * CLEF_STROKE, cap = StrokeCap.Round),
        )
        drawCircle(color = colors.ink, radius = gap * BASS_HEAD_RADIUS, center = origin)
        listOf(-BASS_DOT_Y, BASS_DOT_Y).forEach { dy ->
            drawCircle(
                color = colors.ink,
                radius = gap * BASS_DOT_RADIUS,
                center = origin + Offset(x = gap * BASS_DOT_X, y = gap * dy),
            )
        }
    }

    /** A path of cubic curves, each six numbers in staff spaces from [origin]. */
    private fun clefPath(origin: Offset, start: Pair<Float, Float>, curves: List<Float>): Path =
        Path().apply {
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

    private fun DrawScope.drawNote(note: StaffNote, x: Float, color: Color) {
        val y = yOf(note.step)
        note.ledgers.forEach { ledger ->
            drawLine(
                color = colors.ink,
                start = Offset(x = x - headWidth * LEDGER_REACH, y = yOf(ledger)),
                end = Offset(x = x + headWidth * LEDGER_REACH, y = yOf(ledger)),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawHead(
            center = Offset(x = x, y = y),
            hollow = note.head == NoteHead.HOLLOW,
            color = color,
        )
        note.stemTop?.let { top ->
            drawStem(x = stemX(x), from = y, to = yOf(top), flag = note.flag, color = color)
        }
        drawAccidental(note = note, x = x, y = y)
    }

    private fun DrawScope.drawHead(center: Offset, hollow: Boolean, color: Color) {
        val topLeft = Offset(x = center.x - headWidth / 2, y = center.y - headHeight / 2)
        val oval = Size(width = headWidth, height = headHeight)
        val style = if (hollow) Stroke(width = gap * HOLLOW_STROKE) else Fill
        rotate(degrees = HEAD_TILT, pivot = center) {
            drawOval(color = color, topLeft = topLeft, size = oval, style = style)
        }
    }

    private fun DrawScope.drawStem(x: Float, from: Float, to: Float, flag: Boolean, color: Color) {
        drawLine(
            color = color,
            start = Offset(x = x, y = from),
            end = Offset(x = x, y = to),
            strokeWidth = gap * STEM_WIDTH,
        )
        if (!flag) return
        drawLine(
            color = color,
            start = Offset(x = x, y = to),
            end = Offset(x = x + gap * FLAG_REACH, y = to + gap * FLAG_DROP),
            strokeWidth = gap * FLAG_WIDTH,
            cap = StrokeCap.Round,
        )
    }

    private fun DrawScope.drawBeam(from: Float, to: Float, top: Int) {
        val y = yOf(top) + gap * BEAM_WIDTH / 2
        drawLine(
            color = colors.ink,
            start = Offset(x = from, y = y),
            end = Offset(x = to, y = y),
            strokeWidth = gap * BEAM_WIDTH,
        )
    }

    /** The ♭ or ♯ before the head, or a courtesy ♮; nothing for a plain natural. */
    private fun DrawScope.drawAccidental(note: StaffNote, x: Float, y: Float) {
        val glyph = when (note.accidental) {
            Accidental.FLAT -> glyphs.flat
            Accidental.SHARP -> glyphs.sharp
            Accidental.NATURAL -> if (note.courtesyNatural) glyphs.natural else return
        }
        val left = x - headWidth / 2 - glyph.size.width - gap * ACCIDENTAL_SPACE
        drawText(
            textLayoutResult = glyph,
            topLeft = Offset(x = left, y = y - glyph.size.height / 2f),
        )
    }
}

/**
 * Whether a key's mark gets an outline. At night the key's vermilion and the top note's
 * rose sit about 1.4:1 against a white key, too close to see, so a marked key is ringed in
 * the key edge colour, which stands out against white keys in both themes.
 */
private val KeyMark.outlined: Boolean
    get() = this == KeyMark.ROOT || this == KeyMark.TOP

/** A [MARK_OUTLINE] ring just inside a marked key's edges. */
private fun DrawScope.drawOutline(topLeft: Offset, size: Size, colors: ManuscriptColors) {
    val inset = MARK_OUTLINE.toPx() / 2
    drawRect(
        color = colors.keyBorder,
        topLeft = topLeft + Offset(x = inset, y = inset),
        size = Size(width = size.width - 2 * inset, height = size.height - 2 * inset),
        style = Stroke(width = MARK_OUTLINE.toPx()),
    )
}

private fun whiteColor(mark: KeyMark, colors: ManuscriptColors): Color = when (mark) {
    KeyMark.ROOT -> colors.accent
    KeyMark.TOP -> colors.topKey
    KeyMark.SUNG -> colors.keyTint
    KeyMark.PLAIN -> colors.key
}

private fun blackColor(mark: KeyMark, colors: ManuscriptColors): Color = when (mark) {
    KeyMark.ROOT -> colors.accent
    KeyMark.TOP -> colors.topKey
    KeyMark.SUNG, KeyMark.PLAIN -> colors.blackKey
}

/** Proportions of the design's staff (a 10-unit gap), as multiples of the gap. */
private const val HEAD_WIDTH = 1.3f
private const val HEAD_HEIGHT = 0.92f
private const val HEAD_TILT = -20f
private const val HOLLOW_STROKE = 0.16f
private const val STEM_WIDTH = 0.14f
private const val BEAM_WIDTH = 0.5f
private const val FLAG_REACH = 0.8f
private const val FLAG_DROP = 1.5f
private const val FLAG_WIDTH = 0.2f
private const val LEDGER_REACH = 0.8f
private const val ACCIDENTAL_SPACE = 0.15f
private const val ACCIDENTAL_SIZE = 1.6f

/** The courtesy natural; [Accidental.NATURAL] has no symbol, since a plain note shows none. */
private const val NATURAL_SIGN = "♮"

/** The clefs, in staff spaces from the line each curls round; y grows downwards. */
private const val CLEF_LEFT = 0.9f
private const val CLEF_STROKE = 0.2f
private const val TREBLE_CLEF_LINE = 2
private const val BASS_CLEF_LINE = 6
private val TREBLE_START = 0.15f to 0.25f
private val TREBLE_CURVES = listOf(
    0.15f, -0.3f, 1.0f, -0.4f, 1.05f, 0.2f,
    1.1f, 0.9f, -0.6f, 1.0f, -0.65f, 0.0f,
    -0.7f, -1.0f, 0.9f, -2.2f, 0.9f, -3.4f,
    0.9f, -4.3f, 0.35f, -4.6f, 0.15f, -4.0f,
    -0.1f, -3.2f, 0.1f, -1.0f, 0.35f, 0.8f,
    0.45f, 1.4f, 0.55f, 1.8f, 0.55f, 2.0f,
    0.6f, 2.6f, -0.2f, 2.8f, -0.35f, 2.3f,
)
private val TREBLE_DOT = -0.2f to 2.2f
private const val TREBLE_DOT_RADIUS = 0.25f
private const val EIGHT_X = 0.2f
private const val EIGHT_Y = 2.9f
private const val EIGHT_SIZE = 1.2f
private val BASS_START = 0.1f to -0.05f
private val BASS_CURVES = listOf(
    0.3f, -1.15f, 1.9f, -1.2f, 1.9f, 0.15f,
    1.9f, 1.5f, 0.9f, 2.3f, -0.05f, 2.7f,
)
private const val BASS_HEAD_RADIUS = 0.32f
private const val BASS_DOT_X = 2.45f
private const val BASS_DOT_Y = 0.5f
private const val BASS_DOT_RADIUS = 0.16f

/** Proportions of the design's keyboard: 26-wide white keys, 16-wide black keys 40 of 64 high. */
private val KEYBOARD_HEIGHT = 64.dp
private val MARK_OUTLINE = 2.dp
private const val BLACK_WIDTH = 16f / 26f
private const val BLACK_HEIGHT = 40f / 64f
