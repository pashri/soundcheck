package org.pashri.soundcheck.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.BackHeader
import org.pashri.soundcheck.ui.components.SectionLabel
import org.pashri.soundcheck.ui.components.Segmented
import org.pashri.soundcheck.ui.components.StepperRow
import org.pashri.soundcheck.ui.components.SwitchRow
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.VoiceType

/**
 * The Settings screen, wired to its view model.
 *
 * @param factory builds the [SettingsViewModel].
 * @param onBack goes back to the Warm-up home.
 */
@Composable
fun SettingsRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shown = state ?: return
    SettingsScreen(state = shown, actions = viewModel, onBack = onBack)
}

/**
 * Range, Voice Type, "Play over other audio" and what the headphone button does, in the
 * Manuscript design. Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param actions what the controls do.
 * @param onBack goes back.
 */
@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, onBack: () -> Unit) {
    val colors = Manuscript.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        BackHeader(backLabel = "Warm-up", title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel(text = "RANGE")
            Text(text = RANGE_NOTE, style = ManuscriptType.body, color = colors.muted)
            Segmented(
                options = VoiceType.entries,
                selected = state.voiceType,
                label = { it.label },
                onSelect = actions::selectVoiceType,
            )
            StepperRow(
                label = "Lowest",
                value = state.lowest,
                lowerDescription = "Lower the lowest note",
                raiseDescription = "Raise the lowest note",
                canLower = state.canLowerLowest,
                canRaise = state.canRaiseLowest,
                onLower = actions::lowerLowest,
                onRaise = actions::raiseLowest,
            )
            StepperRow(
                label = "Highest",
                value = state.highest,
                lowerDescription = "Lower the highest note",
                raiseDescription = "Raise the highest note",
                canLower = state.canLowerHighest,
                canRaise = state.canRaiseHighest,
                onLower = actions::lowerHighest,
                onRaise = actions::raiseHighest,
            )
            HorizontalDivider(thickness = 1.dp, color = colors.rule)
            SectionLabel(text = "AUDIO")
            SwitchRow(
                title = "Play over other audio",
                note = state.audioNote,
                checked = state.playOverOtherAudio,
                onCheckedChange = actions::setPlayOverOtherAudio,
            )
            HorizontalDivider(thickness = 1.dp, color = colors.rule)
            SectionLabel(text = "HEADPHONE BUTTON")
            Text(
                text = "When Play over other audio is off",
                style = ManuscriptType.body,
                color = colors.muted,
            )
            PressRow(presses = "1 press", action = "Pause, or resume this Iteration")
            PressRow(presses = "2 press", action = "Next Step")
            PressRow(presses = "3 press", action = "Previous Step")
        }
    }
}

@Composable
private fun PressRow(presses: String, action: String) {
    val colors = Manuscript.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = presses,
            style = ManuscriptType.label,
            color = colors.muted,
            modifier = Modifier.widthIn(min = 88.dp).padding(end = 12.dp),
        )
        Text(text = action, style = ManuscriptType.body, color = colors.ink)
    }
}
