package org.pashri.soundcheck.ui.step

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.BackHeader
import org.pashri.soundcheck.ui.components.ConfirmDialog
import org.pashri.soundcheck.ui.components.EditorFrame
import org.pashri.soundcheck.ui.components.LinkCard
import org.pashri.soundcheck.ui.components.MusicText
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.components.QuietButton
import org.pashri.soundcheck.ui.components.SectionLabel
import org.pashri.soundcheck.ui.components.Segmented
import org.pashri.soundcheck.ui.components.StepperRow
import org.pashri.soundcheck.ui.components.SwitchRow
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.Direction

/**
 * Where the Step editor's links go.
 *
 * @property back closes the editor.
 * @property choosePattern opens the Patterns library to choose this Step's Pattern, or null
 *     while there is none.
 * @property chooseSound opens the Sounds library to choose this Step's Sound, or null while
 *     there is none.
 */
data class StepLinks(
    val back: () -> Unit,
    val choosePattern: (() -> Unit)? = null,
    val chooseSound: (() -> Unit)? = null,
)

/**
 * The Step editor, wired to its view model; it closes when the Step is removed.
 *
 * @param factory builds the [StepEditorViewModel].
 * @param links where the screen's links go.
 */
@Composable
fun StepEditorRoute(factory: ViewModelProvider.Factory, links: StepLinks) {
    val viewModel: StepEditorViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val auditioning by viewModel.auditioning.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.stopAudition()
    }
    DisposableEffect(key1 = viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.stopAudition() }
    }
    EditorFrame(state = state, onGone = links.back) { shown ->
        StepEditorScreen(
            state = shown,
            actions = viewModel,
            links = links,
            auditioning = auditioning,
        )
    }
}

/**
 * One Step's Pattern, Sound, tempo, Direction, Range Offset and Guide Melody in the
 * Manuscript design. Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param actions what the controls do.
 * @param links where the cards and back go.
 * @param auditioning whether the Demo is sounding, which turns its button into Stop.
 */
@Composable
fun StepEditorScreen(
    state: StepEditorUiState,
    actions: StepEditorActions,
    links: StepLinks,
    auditioning: Boolean,
) {
    val colors = Manuscript.colors
    var removing by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        BackHeader(
            backLabel = state.programmeName,
            title = state.title,
            onBack = links.back,
            trailing = state.stepLabel,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LinkCard(
                label = "PATTERN",
                value = state.patternName,
                detail = state.patternDetail,
                onClick = links.choosePattern,
            )
            LinkCard(
                label = "SOUND",
                value = state.soundLabel,
                detail = state.soundDetail,
                onClick = links.chooseSound,
            )
            StepperRow(
                label = "TEMPO",
                value = state.bpm,
                lowerDescription = "Slower",
                raiseDescription = "Faster",
                canLower = state.canSlower,
                canRaise = state.canFaster,
                onLower = actions::slower,
                onRaise = actions::faster,
            )
            SectionLabel(text = "DIRECTION")
            Segmented(
                options = Direction.entries,
                selected = state.direction,
                label = ::directionLabel,
                onSelect = actions::setDirection,
                spoken = ::directionSpoken,
            )
            RangeOffsetControls(state = state, actions = actions)
            SwitchRow(
                title = "Guide melody",
                note = "Piano plays the Pattern with you",
                checked = state.guideMelody,
                onCheckedChange = actions::setGuideMelody,
            )
            OutlineButton(
                text = if (auditioning) "Stop the Demo" else "Hear the Demo",
                onClick = actions::hearDemo,
                modifier = Modifier.fillMaxWidth(),
                enabled = auditioning || state.canHearDemo,
            )
            QuietButton(
                text = "Remove step",
                onClick = { removing = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
    if (removing) {
        ConfirmDialog(
            title = "Remove this step?",
            text = "It comes out of ${state.programmeName}. " +
                "Its Pattern and Sound stay in the library.",
            confirmLabel = "Remove",
            onConfirm = {
                removing = false
                actions.remove()
            },
            onDismiss = { removing = false },
        )
    }
}

@Composable
private fun RangeOffsetControls(state: StepEditorUiState, actions: StepEditorActions) {
    val colors = Manuscript.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(text = "RANGE OFFSET")
        MusicText(
            text = state.tripLabel,
            style = ManuscriptType.body,
            color = colors.muted,
            modifier = Modifier.paddingFromBaseline(top = 20.sp, bottom = 6.sp),
        )
        state.warning?.let {
            Text(text = it, style = ManuscriptType.body, color = colors.accentText)
        }
        StepperRow(
            label = "Bottom",
            value = state.bottom,
            lowerDescription = "Lower bottom",
            raiseDescription = "Raise bottom",
            canLower = state.canLowerBottom,
            canRaise = state.canRaiseBottom,
            onLower = actions::lowerBottom,
            onRaise = actions::raiseBottom,
        )
        StepperRow(
            label = "Top",
            value = state.top,
            lowerDescription = "Lower top",
            raiseDescription = "Raise top",
            canLower = state.canLowerTop,
            canRaise = state.canRaiseTop,
            onLower = actions::lowerTop,
            onRaise = actions::raiseTop,
        )
    }
}

private fun directionLabel(direction: Direction): String = when (direction) {
    Direction.START_LOW -> "Start low ↑"
    Direction.START_HIGH -> "Start high ↓"
}

private fun directionSpoken(direction: Direction): String = when (direction) {
    Direction.START_LOW -> "Start low"
    Direction.START_HIGH -> "Start high"
}
