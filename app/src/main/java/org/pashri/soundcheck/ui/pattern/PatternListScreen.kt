package org.pashri.soundcheck.ui.pattern

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
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.MusicText
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.PatternId

/**
 * Where the Patterns list's links go.
 *
 * @property back closes the list.
 * @property editPattern opens a Pattern's editor.
 */
data class PatternListLinks(val back: () -> Unit, val editPattern: (PatternId) -> Unit)

/**
 * The Patterns list, wired to its view model. Choosing a Pattern for a Step goes straight
 * back to the Step.
 *
 * @param factory builds the [PatternListViewModel].
 * @param links where the list's links go.
 */
@Composable
fun PatternListRoute(factory: ViewModelProvider.Factory, links: PatternListLinks) {
    val viewModel: PatternListViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shown = state ?: return
    PatternListScreen(
        state = shown,
        links = links,
        onChoose = { id ->
            viewModel.choose(id)
            links.back()
        },
        onAdd = { viewModel.addPattern()?.let(links.editPattern) },
    )
}

/**
 * Every Pattern with its notes, Key Chord and use. Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param links where the rows go.
 * @param onChoose chooses a Pattern for the Step, when choosing.
 * @param onAdd adds a Pattern and opens it.
 */
@Composable
fun PatternListScreen(
    state: PatternListUiState,
    links: PatternListLinks,
    onChoose: (PatternId) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = Manuscript.colors
    Column(Modifier.fillMaxSize().background(colors.paper)) {
        BackHeader(
            backLabel = state.backLabel,
            title = state.title,
            onBack = links.back,
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
                PatternEntry(
                    row = row,
                    picking = state.picking,
                    onOpen = {
                        if (state.picking) onChoose(row.id) else links.editPattern(row.id)
                    },
                    onEdit = { links.editPattern(row.id) },
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlineButton(
                text = "+ New pattern",
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PatternEntry(
    row: PatternRow,
    picking: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = Manuscript.colors
    val rowAction = if (picking) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onOpen)
    }
    val innerAction = if (picking) {
        Modifier.selectable(selected = row.chosen, role = Role.RadioButton, onClick = onOpen)
    } else {
        Modifier
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(rowAction)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).then(innerAction),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = row.name, style = NAME_STYLE, color = colors.ink)
                    MusicText(
                        text = row.detail,
                        style = ManuscriptType.body,
                        color = colors.muted,
                        modifier = Modifier.paddingFromBaseline(top = 20.sp, bottom = 6.sp),
                    )
                    Text(text = row.usage, style = ManuscriptType.label, color = colors.muted)
                }
                if (row.chosen) {
                    Text(
                        text = "CHOSEN",
                        style = ManuscriptType.label,
                        color = colors.accentText,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            if (picking) {
                OutlineButton(text = "Edit", onClick = onEdit, description = "Edit ${row.name}")
            } else {
                Icon(
                    imageVector = ManuscriptIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}

private val NAME_STYLE: TextStyle = ManuscriptType.chip.copy(fontSize = 24.sp)
