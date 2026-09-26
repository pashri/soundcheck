package org.pashri.soundcheck.ui.sounds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.BackHeader
import org.pashri.soundcheck.ui.components.ChosenBadge
import org.pashri.soundcheck.ui.components.ConfirmDialog
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.NameDialog
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.components.OutlineIconButton
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.SoundId

/**
 * What the Sounds list's controls do.
 *
 * @property back closes the list.
 * @property choose chooses a Sound for the Step and goes back to it.
 * @property add adds a Sound with a label.
 * @property rename renames a Sound.
 * @property delete deletes a Sound and its Steps.
 */
data class SoundsActions(
    val back: () -> Unit,
    val choose: (SoundId) -> Unit,
    val add: (String) -> Unit,
    val rename: (SoundId, String) -> Unit,
    val delete: (SoundId) -> Unit,
)

/**
 * The Sounds list, wired to its view model.
 *
 * @param factory builds the [SoundsViewModel].
 * @param onBack closes the list.
 */
@Composable
fun SoundsRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val viewModel: SoundsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shown = state ?: return
    SoundsScreen(
        state = shown,
        actions = SoundsActions(
            back = onBack,
            choose = { id ->
                viewModel.choose(id)
                onBack()
            },
            add = { viewModel.add(it) },
            rename = viewModel::rename,
            delete = viewModel::delete,
        ),
    )
}

/**
 * Every Sound's label, in the Manuscript design. Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param actions what the controls do.
 */
@Composable
fun SoundsScreen(state: SoundsUiState, actions: SoundsActions) {
    val colors = Manuscript.colors
    var adding by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(colors.paper)) {
        BackHeader(
            backLabel = state.backLabel,
            title = state.title,
            onBack = actions.back,
            trailing = state.countLabel,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            state.rows.forEach { row ->
                SoundEntry(
                    row = row,
                    picking = state.picking,
                    canDelete = state.canDelete,
                    onOpen = {
                        if (state.picking) actions.choose(row.id) else renaming = row.id.value
                    },
                    onDelete = { deleting = row.id.value },
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlineButton(
                text = "+ New sound",
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (adding) {
        NameDialog(
            title = "New sound",
            initial = "",
            taken = state.labels,
            confirmLabel = "Add",
            onConfirm = {
                actions.add(it)
                adding = false
            },
            onDismiss = { adding = false },
            capitalize = false,
        )
    }
    state.rows.firstOrNull { it.id.value == renaming }?.let { row ->
        NameDialog(
            title = "Rename sound",
            initial = row.label,
            taken = state.labels - row.label,
            confirmLabel = "Rename",
            onConfirm = {
                actions.rename(row.id, it)
                renaming = null
            },
            onDismiss = { renaming = null },
            capitalize = false,
        )
    }
    state.rows.firstOrNull { it.id.value == deleting }?.let { row ->
        ConfirmDialog(
            title = "Delete “${row.label}”?",
            text = row.deleteNote,
            confirmLabel = "Delete",
            onConfirm = {
                deleting = null
                actions.delete(row.id)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SoundEntry(
    row: SoundRow,
    picking: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = Manuscript.colors
    val action = if (picking) {
        Modifier.selectable(selected = row.chosen, role = Role.RadioButton, onClick = onOpen)
    } else {
        Modifier.clickable(role = Role.Button, onClickLabel = "Rename", onClick = onOpen)
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).then(action),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = row.label, style = LABEL_STYLE, color = colors.ink)
                    Text(
                        text = "${row.detail} · ${row.usage}",
                        style = ManuscriptType.body,
                        color = colors.muted,
                    )
                }
                if (row.chosen) {
                    ChosenBadge()
                }
            }
            if (!picking && canDelete) {
                OutlineIconButton(
                    icon = ManuscriptIcons.Delete,
                    description = "Delete ${row.label}",
                    onClick = onDelete,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}

private val LABEL_STYLE: TextStyle = ManuscriptType.displayItalic.copy(fontSize = 24.sp)
