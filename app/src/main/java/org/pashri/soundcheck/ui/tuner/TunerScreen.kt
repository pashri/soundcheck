package org.pashri.soundcheck.ui.tuner

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin
import org.pashri.soundcheck.music.midiOf
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.NoteReading
import org.pashri.soundcheck.ui.components.ScreenHeader
import org.pashri.soundcheck.ui.components.hasMicPermission
import org.pashri.soundcheck.ui.components.openAppSettings
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.ui.theme.SansFamily
import org.pashri.soundcheck.ui.theme.SerifFamily
import org.pashri.soundcheck.ui.theme.SoundcheckTheme

/**
 * The Tuner tab, wired to its view model. Listens while the tab is shown and the app is in
 * the foreground; stops when the tab is switched away (the screen leaves composition) or
 * the app leaves the foreground, but not on a configuration change. Asks for the
 * microphone the first time it is shown without it.
 *
 * @param factory builds the [TunerViewModel].
 */
@Composable
fun TunerRoute(factory: ViewModelProvider.Factory) {
    val viewModel: TunerViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val canAskAgain =
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        viewModel.onPermissionResult(granted = granted, canAskAgain = canAskAgain == true)
    }
    // ON_START is replayed when this screen enters composition, and fires again on return
    // from Settings, so the permission is rechecked every time the Tuner comes into view.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onShown(granted = activity?.hasMicPermission() == true)
        if (viewModel.shouldAskOnOpen()) launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    // ON_STOP also fires on a config change (rotation, dark-theme toggle); only stop when the
    // app is actually leaving the screen, not being recreated in place.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.stop()
    }
    // Switching tabs removes this screen from composition before ON_STOP reaches it, so
    // stop here too; this is what keeps the Tuner and the Metronome from running together.
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.stop() }
    }
    val actions = remember(viewModel, activity, launcher) {
        object : TunerActions {
            override fun allowMicrophone() = launcher.launch(Manifest.permission.RECORD_AUDIO)
            override fun openSettings() {
                activity?.openAppSettings()
            }
            override fun retry() = viewModel.retry()
        }
    }
    TunerScreen(state = state, actions = actions)
}

/**
 * The Tuner in the Manuscript design: the note on a staff, its frequency, a curved
 * sharp/flat needle, the cents and a plain-language line. Scrolls when the font is large.
 *
 * @param state what to show.
 * @param actions what the button does when a message is showing.
 */
@Composable
fun TunerScreen(state: TunerUiState, actions: TunerActions) {
    Column(Modifier.fillMaxSize().background(Manuscript.colors.paper)) {
        ScreenHeader(title = "Tuner", trailing = "A4 = 440 Hz")
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val message = state.message
                if (message == null) TunerFace(state) else MessagePanel(message, state, actions)
            }
        }
    }
}

@Composable
private fun TunerFace(state: TunerUiState) {
    val colors = Manuscript.colors
    NoteOnStaff(state.note)
    Text(
        text = state.hzLabel,
        style = ManuscriptType.label.copy(fontSize = 13.sp, letterSpacing = 0.sp),
        color = colors.muted,
    )
    Spacer(Modifier.height(14.dp))
    Needle(degrees = state.needleDegrees)
    Text(
        text = state.readingLabel,
        style = ManuscriptType.displayItalic,
        color = colors.ink,
        textAlign = TextAlign.Center,
    )
    Text(
        text = state.adviceLabel,
        style = ADVICE_STYLE,
        color = colors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.heightIn(min = 22.dp),
    )
}

@Composable
private fun NoteOnStaff(note: NoteReading?) {
    val colors = Manuscript.colors
    val description = note?.let { spokenNoteName(it.name, it.octave) }
    val semanticsModifier = if (description != null) {
        Modifier.semantics { contentDescription = description }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .widthIn(max = STAFF_WIDTH)
            .fillMaxWidth()
            .heightIn(min = STAFF_HEIGHT)
            .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Anchored to the box's vertical centre, not its top, so the middle line stays
            // under the letter's centre even as heightIn(min) lets the box grow taller.
            val middle = size.height / 2
            STAFF_LINES_Y.forEach { y ->
                val top = middle + (y - STAFF_MIDDLE_LINE_Y).toPx()
                drawLine(colors.rule, Offset(0f, top), Offset(size.width, top), 1.2.dp.toPx())
            }
        }
        if (note != null) NoteName(note)
    }
}

/**
 * The note in words, for screen readers: a bundled font glyph such as "♭" doesn't always
 * speak, so it is spelled out.
 *
 * @param name the note's letter and optional accidental, e.g. "B♭".
 * @param octave the note's octave number.
 * @return the letter, then "flat" or "sharp" when there is an accidental, then the octave,
 *   e.g. "B flat 4".
 */
internal fun spokenNoteName(name: String, octave: Int): String {
    val letter = name.take(1)
    val spoken = when (val symbol = name.drop(1)) {
        "♭" -> "$letter flat"
        "♯" -> "$letter sharp"
        else -> letter + symbol
    }
    return "$spoken $octave"
}

@Composable
private fun NoteName(note: NoteReading) {
    val colors = Manuscript.colors
    val density = LocalDensity.current
    val letter = note.name.take(1)
    val accidental = note.name.drop(1)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(letter, style = serifDp(LETTER_SIZE, density), color = colors.ink)
        if (accidental.isNotEmpty()) {
            Text(
                text = accidental,
                style = serifDp(ACCIDENTAL_SIZE, density),
                color = colors.ink,
                modifier = Modifier.align(Alignment.Top).padding(top = 24.dp),
            )
        }
        Text(
            text = note.octave.toString(),
            style = serifDp(OCTAVE_SIZE, density).copy(fontStyle = FontStyle.Italic),
            color = colors.muted,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
        )
    }
}

private fun serifDp(size: Dp, density: Density): TextStyle =
    TextStyle(
        fontFamily = SerifFamily,
        fontSize = with(density) { size.toSp() },
        lineHeight = 0.8.em,
    )

@Composable
private fun Needle(degrees: Float?) {
    val colors = Manuscript.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val signStyle = serifDp(SIGN_SIZE, density).copy(color = colors.muted)
    // Pre-measured: drawText(measurer, text, ...) lays text out against the unscaled
    // DrawScope size minus its offset, which goes negative once the dial is narrower than
    // the mockup and crashes. Measuring unconstrained and drawing the TextLayoutResult
    // sidesteps that constraint while keeping the same position and size.
    val flat = remember(signStyle, measurer) { measurer.measure("♭", signStyle) }
    val sharp = remember(signStyle, measurer) { measurer.measure("♯", signStyle) }
    Canvas(
        // Decorative: the reading and advice lines below already speak the value.
        Modifier
            .widthIn(max = DIAL_WIDTH)
            .fillMaxWidth()
            .aspectRatio(DIAL_WIDTH / DIAL_HEIGHT),
    ) {
        // Draw in the mockup's own coordinates, scaled down on a narrow phone.
        val unit = size.width / DIAL_WIDTH.toPx()
        withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
            val pivot = Offset(DIAL_WIDTH.toPx() / 2, PIVOT_Y.toPx())
            val radius = DIAL_RADIUS.toPx()
            drawTicks(pivot, radius, colors.ink, colors.faint)
            drawText(flat, topLeft = Offset(12.dp.toPx(), 124.dp.toPx()))
            drawText(sharp, topLeft = Offset(296.dp.toPx(), 124.dp.toPx()))
            val needleColor = if (degrees == null) colors.faint else colors.accent
            val tip = pointOnDial(pivot, radius - NEEDLE_SHORTFALL.toPx(), degrees ?: 0f)
            drawLine(needleColor, pivot, tip, 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(needleColor, 6.dp.toPx(), pivot)
        }
    }
}

private fun DrawScope.drawTicks(pivot: Offset, radius: Float, major: Color, minor: Color) {
    for (cents in -50..50 step 5) {
        val isMajor = cents % 25 == 0
        val degrees = needleDegrees(cents.toDouble())
        val inner = radius - (if (isMajor) MAJOR_TICK else MINOR_TICK).toPx()
        drawLine(
            color = if (isMajor) major else minor,
            start = pointOnDial(pivot, radius, degrees),
            end = pointOnDial(pivot, inner, degrees),
            strokeWidth = (if (isMajor) 1.6.dp else 1.dp).toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun pointOnDial(pivot: Offset, radius: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    return Offset(
        x = pivot.x + radius * sin(radians).toFloat(),
        y = pivot.y - radius * cos(radians).toFloat(),
    )
}

@Composable
private fun MessagePanel(message: TunerMessage, state: TunerUiState, actions: TunerActions) {
    val colors = Manuscript.colors
    NoteOnStaff(note = null)
    Text(
        text = message.title,
        style = ManuscriptType.displayItalic,
        color = colors.ink,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = message.body,
        style = ManuscriptType.body,
        color = colors.muted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    val onClick: () -> Unit = when (state.mode) {
        TunerMode.OpenSettings -> actions::openSettings
        TunerMode.MicUnavailable, TunerMode.Yielded -> actions::retry
        else -> actions::allowMicrophone
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message.button,
            style = ManuscriptType.button.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onAccent,
            textAlign = TextAlign.Center,
        )
    }
}

private object PreviewActions : TunerActions {
    override fun allowMicrophone() = Unit
    override fun openSettings() = Unit
    override fun retry() = Unit
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TunerDayPreview() {
    SoundcheckTheme(dark = false) {
        val note = NoteReading.of(midiOf(109.746))
        TunerScreen(TunerUiState(MicAccess.Granted, MicStatus.Listening, note), PreviewActions)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TunerNightPreview() {
    SoundcheckTheme(dark = true) {
        val note = NoteReading.of(midiOf(466.9))
        TunerScreen(TunerUiState(MicAccess.Granted, MicStatus.Listening, note), PreviewActions)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TunerPermissionPreview() {
    SoundcheckTheme(dark = false) {
        TunerScreen(TunerUiState(access = MicAccess.Blocked), PreviewActions)
    }
}

private val ADVICE_STYLE = TextStyle(fontFamily = SansFamily, fontSize = 14.sp)
private val STAFF_WIDTH = 342.dp
private val STAFF_HEIGHT = 230.dp
private val STAFF_LINES_Y = listOf(75.dp, 95.dp, 115.dp, 135.dp, 155.dp)
private val STAFF_MIDDLE_LINE_Y = 115.dp
private val LETTER_SIZE = 200.dp
private val ACCIDENTAL_SIZE = 96.dp
private val OCTAVE_SIZE = 52.dp
private val SIGN_SIZE = 24.dp
private val DIAL_WIDTH = 320.dp
private val DIAL_HEIGHT = 190.dp
private val PIVOT_Y = 172.dp
private val DIAL_RADIUS = 150.dp
private val MAJOR_TICK = 22.dp
private val MINOR_TICK = 11.dp
private val NEEDLE_SHORTFALL = 34.dp
