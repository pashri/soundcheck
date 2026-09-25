package org.pashri.soundcheck.ui.warmup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.ScreenHeader
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.ui.theme.SerifFamily
import org.pashri.soundcheck.ui.theme.SoundcheckTheme
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType

/**
 * The Warm-up tab, wired to its view model. Unlike the Metronome and the Tuner, leaving the
 * tab or the app doesn't stop anything: a Programme keeps playing in the background.
 *
 * @param factory builds the [WarmupViewModel].
 */
@Composable
fun WarmupRoute(factory: ViewModelProvider.Factory) {
    val viewModel: WarmupViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WarmupScreen(state = state, actions = viewModel)
}

/**
 * The playing Programme in the Manuscript design: the Sound, the key, the Iterations, the
 * next Step and the transport. Scrolls when the system font is large.
 *
 * @param state what to show.
 * @param actions what the controls do.
 */
@Composable
fun WarmupScreen(state: WarmupUiState, actions: WarmupActions) {
    Column(Modifier.fillMaxSize().background(Manuscript.colors.paper)) {
        ScreenHeader(title = state.programmeName, trailing = state.stepLabel)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    StepHeading(state)
                    state.iterations?.let { IterationPanel(view = it, active = state.active) }
                    NextStep(state)
                }
                Transport(state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun StepHeading(state: WarmupUiState) {
    val colors = Manuscript.colors
    val size = with(LocalDensity.current) { SOUND_LABEL_SIZE.toSp() }
    Text(
        text = state.soundLabel,
        style = ManuscriptType.displayItalic.copy(fontSize = size, lineHeight = size),
        color = colors.ink,
    )
    Text(text = state.stepDetail, style = ManuscriptType.body, color = colors.muted)
}

@Composable
private fun IterationPanel(view: IterationView, active: Boolean) {
    val colors = Manuscript.colors
    val keySize = with(LocalDensity.current) { KEY_LABEL_SIZE.toSp() }
    Spacer(Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = view.keyLabel,
            style = ManuscriptType.displayNumber.copy(fontSize = keySize),
            color = colors.ink,
            modifier = Modifier
                .weight(1f, fill = false)
                .clearAndSetSemantics { contentDescription = spokenKeyLabel(view.keyLabel) },
        )
        Text(
            text = view.progressLabel,
            style = ManuscriptType.label,
            color = colors.muted,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = spokenProgress(view, active)
            },
        )
    }
    Spacer(Modifier.height(12.dp))
    IterationCells(view)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spokenTurn(view) },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TurnLabel(text = view.startLabel, align = TextAlign.Start)
        TurnLabel(text = view.turnLabel, align = TextAlign.Center)
        TurnLabel(text = view.endLabel, align = TextAlign.End)
    }
}

@Composable
private fun RowScope.TurnLabel(text: String, align: TextAlign) {
    Text(
        text = text,
        style = ManuscriptType.label,
        color = Manuscript.colors.muted,
        textAlign = align,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun IterationCells(view: IterationView) {
    val colors = Manuscript.colors
    val now = view.now
    val description = now?.let { "Iteration ${it + 1} of ${view.count}" }
        ?: "${view.count} Iterations"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(view.count) { index ->
            val look = when {
                now != null && index == now -> Modifier.background(colors.accent)
                now != null && index < now -> Modifier.background(colors.faint)
                index == view.turnIndex -> Modifier.border(2.dp, colors.ink)
                else -> Modifier.border(1.dp, colors.ink)
            }
            Box(Modifier.weight(1f).height(CELL_HEIGHT).then(look))
        }
    }
}

@Composable
private fun NextStep(state: WarmupUiState) {
    val sound = state.nextSound ?: return
    val colors = Manuscript.colors
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(thickness = 1.dp, color = colors.rule)
    Spacer(Modifier.height(10.dp))
    val text = buildAnnotatedString {
        append("Next · ")
        val soundStyle = SpanStyle(
            fontFamily = SerifFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 20.sp,
            color = colors.ink,
        )
        withStyle(soundStyle) { append(sound) }
        append(" ${state.nextDetail.orEmpty()}")
    }
    Text(text = text, style = ManuscriptType.body, color = colors.muted)
}

@Composable
private fun Transport(state: WarmupUiState, actions: WarmupActions) {
    val colors = Manuscript.colors
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkipButton(
                icon = ManuscriptIcons.Previous,
                description = "Previous step",
                onClick = actions::previous,
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 60.dp)
                    .clip(shape)
                    .background(colors.accent)
                    .clickable(role = Role.Button, onClick = actions::playPause),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (state.playing) {
                        ManuscriptIcons.Pause
                    } else {
                        ManuscriptIcons.Play
                    },
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = state.playLabel,
                    style = ManuscriptType.button.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onAccent,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            SkipButton(
                icon = ManuscriptIcons.Next,
                description = "Next step",
                onClick = actions::next,
            )
        }
        if (state.active) StopButton(onClick = actions::stop)
    }
}

@Composable
private fun SkipButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = Manuscript.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 60.dp)
            .clip(shape)
            .border(1.dp, colors.ink, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.ink)
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Stop", style = ManuscriptType.button, color = Manuscript.colors.muted)
    }
}

private object PreviewActions : WarmupActions {
    override fun playPause() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun stop() = Unit
}

private fun previewState(iteration: Int?, playing: Boolean): WarmupUiState = warmupUiState(
    playback = Playback(
        programme = StarterProgrammes.WARM_UP,
        range = VoiceType.TENOR.range,
        stepIndex = 2,
        iteration = iteration,
        playing = playing,
    ),
    programme = StarterProgrammes.WARM_UP,
    range = VoiceType.TENOR.range,
    sounds = StarterSounds.ALL,
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WarmupDayPreview() {
    SoundcheckTheme(dark = false) {
        WarmupScreen(previewState(iteration = 3, playing = true), PreviewActions)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WarmupNightPreview() {
    SoundcheckTheme(dark = true) {
        WarmupScreen(previewState(iteration = null, playing = false), PreviewActions)
    }
}

private val SOUND_LABEL_SIZE = 72.dp
private val KEY_LABEL_SIZE = 40.dp
private val CELL_HEIGHT = 14.dp
