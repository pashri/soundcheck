package org.pashri.soundcheck.ui.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.BackHeader
import org.pashri.soundcheck.ui.components.ConfirmDialog
import org.pashri.soundcheck.ui.components.ControlShape
import org.pashri.soundcheck.ui.components.EditorFrame
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.NameDialog
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.components.QuietButton
import org.pashri.soundcheck.ui.components.SectionLabel
import org.pashri.soundcheck.ui.components.Segmented
import org.pashri.soundcheck.ui.components.StepperRow
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.NoteLength

/**
 * The Pattern editor, wired to its view model; it closes when the Pattern is deleted.
 *
 * @param factory builds the [PatternEditorViewModel].
 * @param onBack closes the editor.
 */
@Composable
fun PatternEditorRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val viewModel: PatternEditorViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EditorFrame(state = state, onGone = onBack) { shown ->
        PatternEditorScreen(state = shown, actions = viewModel, onBack = onBack)
    }
}

/**
 * One Pattern's notes, degrees, accidentals, lengths and Key Chord in the Manuscript design.
 * Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param actions what the controls do.
 * @param onBack closes the editor.
 */
@Composable
fun PatternEditorScreen(
    state: PatternEditorUiState,
    actions: PatternEditorActions,
    onBack: () -> Unit,
) {
    val colors = Manuscript.colors
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(colors.paper)) {
        BackHeader(
            backLabel = "Patterns",
            title = state.name,
            onBack = onBack,
            trailing = state.summary,
            onRename = { renaming = true },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = state.fit, style = ManuscriptType.body, color = colors.muted)
            NoteChips(notes = state.notes, onSelect = actions::select)
            NoteTools(state = state, actions = actions)
            StepperRow(
                label = "DEGREE",
                value = state.degree,
                detail = state.degreeName,
                lowerDescription = "Lower degree",
                raiseDescription = "Raise degree",
                canLower = state.canLowerDegree,
                canRaise = state.canRaiseDegree,
                onLower = actions::lowerDegree,
                onRaise = actions::raiseDegree,
            )
            Segmented(
                options = Accidental.entries,
                selected = state.accidental,
                label = ::accidentalLabel,
                onSelect = actions::setAccidental,
                spoken = ::accidentalSpoken,
            )
            Segmented(
                options = NoteLength.entries,
                selected = state.length,
                label = ::lengthLabel,
                onSelect = actions::setLength,
            )
            SectionLabel(text = "KEY CHORD")
            KeyChordChips(selected = state.keyChord, onSelect = actions::setKeyChord)
            if (state.canDelete) {
                QuietButton(
                    text = "Delete pattern",
                    onClick = { deleting = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
    if (renaming) {
        NameDialog(
            title = "Rename pattern",
            initial = state.name,
            taken = state.otherNames,
            confirmLabel = "Rename",
            onConfirm = {
                actions.rename(it)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = "Delete ${state.name}?",
            text = state.deleteNote,
            confirmLabel = "Delete",
            onConfirm = {
                deleting = false
                actions.delete()
            },
            onDismiss = { deleting = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteChips(notes: List<NoteChip>, onSelect: (Int) -> Unit) {
    val colors = Manuscript.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        notes.forEachIndexed { index, chip ->
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
                    .clip(ControlShape)
                    .background(if (chip.selected) colors.accent else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (chip.selected) colors.accent else colors.faint,
                        shape = ControlShape,
                    )
                    .selectable(
                        selected = chip.selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .semantics { contentDescription = chip.description }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chip.text,
                    style = ManuscriptType.chip,
                    color = if (chip.selected) colors.onAccent else colors.ink,
                    modifier = Modifier.paddingFromBaseline(top = 30.sp, bottom = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun NoteTools(state: PatternEditorUiState, actions: PatternEditorActions) {
    val colors = Manuscript.colors
    val tint = if (state.canDeleteNote) colors.ink else colors.faint
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = state.noteLabel,
            style = ManuscriptType.label,
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
        OutlineButton(text = "+ Note", onClick = actions::addNote)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(ControlShape)
                .border(width = 1.dp, color = tint, shape = ControlShape)
                .clickable(
                    enabled = state.canDeleteNote,
                    role = Role.Button,
                    onClick = actions::deleteNote,
                )
                .semantics { contentDescription = "Delete note" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = ManuscriptIcons.Delete, contentDescription = null, tint = tint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyChordChips(selected: KeyChord, onSelect: (KeyChord) -> Unit) {
    val colors = Manuscript.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeyChord.entries.forEach { chord ->
            val on = chord == selected
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
                    .clip(ControlShape)
                    .background(if (on) colors.accent else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (on) colors.accent else colors.faint,
                        shape = ControlShape,
                    )
                    .selectable(
                        selected = on,
                        role = Role.RadioButton,
                        onClick = { onSelect(chord) },
                    )
                    .semantics { contentDescription = chordSpoken(chord) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chordChipLabel(chord),
                    style = ManuscriptType.button,
                    color = if (on) colors.onAccent else colors.ink,
                )
            }
        }
    }
}
