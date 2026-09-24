package org.pashri.soundcheck.ui.metronome

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import org.pashri.soundcheck.metronome.MAX_BPM
import org.pashri.soundcheck.metronome.MIN_BPM
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.ScreenHeader
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.ui.theme.SoundcheckTheme

/**
 * The Metronome tab, wired to its view model. Stops the Metronome when its screen leaves
 * composition (switching tabs) or the app leaves the foreground, but not on a configuration
 * change, until background playback arrives with the playback service.
 *
 * @param factory builds the [MetronomeViewModel].
 */
@Composable
fun MetronomeRoute(factory: ViewModelProvider.Factory) {
    val viewModel: MetronomeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    // ON_STOP also fires on a config change (rotation, dark-theme toggle); only stop when the
    // app is actually leaving the screen, not being recreated in place.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.stop()
    }
    // popUpTo(start) { saveState = true } keeps this entry (Metronome is the start
    // destination) on the back stack across tab switches, and the outgoing destination is
    // normally removed from composition before ON_STOP is delivered, so also stop here.
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.stop() }
    }
    MetronomeScreen(state = state, actions = viewModel)
}

/**
 * The Metronome in the Manuscript design. Scrolls when the system font is large.
 *
 * @param state what to show.
 * @param actions what the controls do.
 */
@Composable
fun MetronomeScreen(state: MetronomeUiState, actions: MetronomeActions) {
    Column(Modifier.fillMaxSize().background(Manuscript.colors.paper)) {
        ScreenHeader(title = "Metronome", trailing = state.accentLabel)
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
                TempoReadout(state)
                Spacer(Modifier.height(26.dp))
                BeatRow(beats = state.beatsInBar, playing = state.beatInBar)
                Spacer(Modifier.height(26.dp))
                TempoControls(bpm = state.bpm, actions = actions)
                Spacer(Modifier.height(22.dp))
                AccentPicker(selected = state.accentEvery, onSelect = actions::setAccent)
                Spacer(Modifier.height(22.dp))
                TransportButtons(running = state.running, actions = actions)
            }
        }
    }
}

@Composable
private fun TempoReadout(state: MetronomeUiState) {
    val colors = Manuscript.colors
    val numberSize = with(LocalDensity.current) { BPM_NUMBER_SIZE.toSp() }
    Text(state.tempoMarking, style = ManuscriptType.displayItalic, color = colors.muted)
    Text(
        text = state.bpm.toString(),
        style = ManuscriptType.displayNumber.copy(fontSize = numberSize),
        color = colors.ink,
        modifier = Modifier.semantics { contentDescription = "${state.bpm} beats per minute" },
    )
    Text("BEATS PER MINUTE", style = ManuscriptType.label, color = colors.muted)
}

@Composable
private fun BeatRow(beats: Int, playing: Int?) {
    val description = playing?.let { "Beat ${it + 1} of $beats" } ?: "$beats beats per bar"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(beats) { i -> BeatMark(accented = i == 0 && beats > 1, playing = i == playing) }
    }
}

@Composable
private fun BeatMark(accented: Boolean, playing: Boolean) {
    val colors = Manuscript.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(18.dp, 12.dp)) {
            if (accented) drawPath(accentMark(size), colors.ink, style = Stroke(1.8.dp.toPx()))
        }
        Canvas(Modifier.size(30.dp, 22.dp)) {
            val color = if (playing) colors.accent else colors.ink
            rotate(NOTEHEAD_TILT) {
                drawOval(color, style = if (playing) Fill else Stroke(2.dp.toPx()))
            }
        }
    }
}

private fun accentMark(size: Size): Path = Path().apply {
    moveTo(0f, 0f)
    lineTo(size.width, size.height / 2)
    lineTo(0f, size.height)
}

@Composable
private fun TempoControls(bpm: Int, actions: MetronomeActions) {
    val colors = Manuscript.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SquareButton(symbol = "−", description = "Slower", onClick = actions::slower)
        Slider(
            value = bpm.toFloat(),
            onValueChange = { actions.setBpm(it.roundToInt()) },
            valueRange = MIN_BPM.toFloat()..MAX_BPM.toFloat(),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.ink,
                inactiveTrackColor = colors.rule,
            ),
        )
        SquareButton(symbol = "+", description = "Faster", onClick = actions::faster)
    }
}

@Composable
private fun SquareButton(symbol: String, description: String, onClick: () -> Unit) {
    val colors = Manuscript.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .border(1.dp, colors.ink, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = ManuscriptType.button.copy(fontSize = 20.sp), color = colors.ink)
    }
}

@Composable
private fun AccentPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ACCENT EVERY", style = ManuscriptType.label, color = Manuscript.colors.muted)
        ACCENT_CHOICES.chunked(CHOICES_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { choice ->
                    AccentChip(
                        choice = choice,
                        selected = choice == selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentChip(
    choice: Int?,
    selected: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier,
) {
    val colors = Manuscript.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) colors.ink else Color.Transparent)
            .border(1.dp, if (selected) colors.ink else colors.faint, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onSelect(choice) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = choice?.toString() ?: "Off",
            style = ManuscriptType.chip,
            color = if (selected) colors.paper else colors.ink,
        )
    }
}

@Composable
private fun TransportButtons(running: Boolean, actions: MetronomeActions) {
    val colors = Manuscript.colors
    val shape = RoundedCornerShape(6.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clip(shape)
                .border(1.dp, colors.ink, shape)
                .clickable(role = Role.Button, onClick = actions::tap),
            contentAlignment = Alignment.Center,
        ) {
            Text("Tap tempo", style = ManuscriptType.button, color = colors.ink)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(colors.accent)
                .clickable(role = Role.Button, onClick = actions::toggle),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (running) ManuscriptIcons.Stop else ManuscriptIcons.Play,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (running) "Stop" else "Start",
                style = ManuscriptType.button.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onAccent,
            )
        }
    }
}

private object PreviewActions : MetronomeActions {
    override fun slower() = Unit
    override fun faster() = Unit
    override fun setBpm(bpm: Int) = Unit
    override fun setAccent(every: Int?) = Unit
    override fun tap() = Unit
    override fun toggle() = Unit
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MetronomeDayPreview() {
    SoundcheckTheme(dark = false) {
        MetronomeScreen(MetronomeUiState(beatInBar = 1, running = true), PreviewActions)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MetronomeNightPreview() {
    SoundcheckTheme(dark = true) {
        MetronomeScreen(MetronomeUiState(accentEvery = 8), PreviewActions)
    }
}

private val ACCENT_CHOICES: List<Int?> = listOf(null, 2, 3, 4, 5, 6, 7, 8)
private const val CHOICES_PER_ROW = 4
private const val NOTEHEAD_TILT = -20f
private val BPM_NUMBER_SIZE = 180.dp
