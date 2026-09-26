package org.pashri.soundcheck.ui.programme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.AccentButton
import org.pashri.soundcheck.ui.components.BackHeader
import org.pashri.soundcheck.ui.components.ConfirmDialog
import org.pashri.soundcheck.ui.components.EditorFrame
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.MusicText
import org.pashri.soundcheck.ui.components.NameDialog
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.components.QuietButton
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.StepKey

/**
 * Where the Programme editor's links go.
 *
 * @property back closes the editor.
 * @property openPlaying opens the playing screen after Start.
 * @property editStep opens a Step's editor, or null while there is none.
 */
data class ProgrammeLinks(
    val back: () -> Unit,
    val openPlaying: () -> Unit,
    val editStep: ((StepKey) -> Unit)? = null,
)

/**
 * What the Programme editor's controls do.
 *
 * @property links where the screen's links go.
 * @property rename renames the Programme.
 * @property delete deletes it.
 * @property addStep adds a Step (and opens it, once Steps have an editor).
 * @property removeStep removes a Step.
 * @property moveStep moves a Step from one position to another.
 * @property start plays the Programme.
 */
data class ProgrammeEditorActions(
    val links: ProgrammeLinks,
    val rename: (String) -> Unit,
    val delete: () -> Unit,
    val addStep: () -> Unit,
    val removeStep: (StepKey) -> Unit,
    val moveStep: (Int, Int) -> Unit,
    val start: () -> Unit,
)

/**
 * The Programme editor, wired to its view model; it closes when the Programme is deleted.
 *
 * @param factory builds the [ProgrammeEditorViewModel].
 * @param links where the screen's links go.
 */
@Composable
fun ProgrammeEditorRoute(factory: ViewModelProvider.Factory, links: ProgrammeLinks) {
    val viewModel: ProgrammeEditorViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EditorFrame(state = state, onGone = links.back) { shown ->
        ProgrammeEditorScreen(
            state = shown,
            actions = ProgrammeEditorActions(
                links = links,
                rename = viewModel::rename,
                delete = viewModel::delete,
                addStep = {
                    val key = viewModel.addStep()
                    if (key != null) links.editStep?.invoke(key)
                },
                removeStep = viewModel::removeStep,
                moveStep = viewModel::moveStep,
                start = { if (viewModel.start()) links.openPlaying() },
            ),
        )
    }
}

/**
 * One Programme's Steps in the Manuscript design, with reordering, the "doesn't fit"
 * warnings, Start and Delete. Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param actions what the controls do.
 */
@Composable
fun ProgrammeEditorScreen(state: ProgrammeEditorUiState, actions: ProgrammeEditorActions) {
    val colors = Manuscript.colors
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(colors.paper)) {
        BackHeader(
            backLabel = "Warm-up",
            title = state.name,
            onBack = actions.links.back,
            trailing = state.stepsLabel,
            onRename = { renaming = true },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            StepList(rows = state.rows, actions = actions, onRemove = { removing = it.value })
            Spacer(Modifier.height(16.dp))
            OutlineButton(
                text = "+ Add step",
                onClick = actions.addStep,
                modifier = Modifier.fillMaxWidth(),
            )
            state.problem?.let {
                Text(
                    text = it,
                    style = ManuscriptType.body,
                    color = colors.accentText,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            AccentButton(
                text = state.startLabel,
                onClick = actions.start,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                icon = ManuscriptIcons.Play,
                enabled = state.canStart,
            )
            QuietButton(
                text = "Delete programme",
                onClick = { deleting = true },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }
    }
    if (renaming) {
        NameDialog(
            title = "Rename programme",
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
    val row = state.rows.firstOrNull { it.key.value == removing }
    if (row != null) {
        ConfirmDialog(
            title = "Remove step ${row.number}?",
            text = "${row.sound} · ${row.meta} comes out of ${state.name}. " +
                "Its Pattern and Sound stay in the library.",
            confirmLabel = "Remove",
            onConfirm = {
                removing = null
                actions.removeStep(row.key)
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun StepList(
    rows: List<StepRow>,
    actions: ProgrammeEditorActions,
    onRemove: (StepKey) -> Unit,
) {
    val heights = remember { mutableStateMapOf<StepKey, Int>() }
    var dragging by remember { mutableStateOf<StepKey?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val currentRows by rememberUpdatedState(rows)
    val onDrop by rememberUpdatedState {
        val from = currentRows.indexOfFirst { it.key == dragging }
        if (from >= 0) {
            val to = dropIndex(
                from = from,
                dragPx = offset,
                heights = currentRows.map { heights[it.key] ?: 0 },
            )
            actions.moveStep(from, to)
        }
        dragging = null
        offset = 0f
    }
    Column {
        rows.forEachIndexed { index, row ->
            key(row.key) {
                val dragged = dragging == row.key
                StepEntry(
                    row = row,
                    position = RowPosition(isFirst = index == 0, isLast = index == rows.lastIndex),
                    modifier = Modifier
                        .onSizeChanged { heights[row.key] = it.height }
                        .zIndex(if (dragged) 1f else 0f)
                        .graphicsLayer { translationY = if (dragged) offset else 0f },
                    drag = Modifier.pointerInput(row.key) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = row.key
                                offset = 0f
                            },
                            onDragEnd = { onDrop() },
                            onDragCancel = {
                                dragging = null
                                offset = 0f
                            },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                offset += amount
                            },
                        )
                    },
                    onOpen = actions.links.editStep?.let { edit -> { edit(row.key) } },
                    onMoveUp = { actions.moveStep(index, index - 1) },
                    onMoveDown = { actions.moveStep(index, index + 1) },
                    onRemove = { onRemove(row.key) },
                )
            }
        }
    }
}

/** Whether a Step is first or last, which switches off moving it further that way. */
private data class RowPosition(val isFirst: Boolean, val isLast: Boolean)

@Composable
private fun StepEntry(
    row: StepRow,
    position: RowPosition,
    modifier: Modifier,
    drag: Modifier,
    onOpen: (() -> Unit)?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = Manuscript.colors
    val moves = listOfNotNull(
        CustomAccessibilityAction(label = "Move up") { onMoveUp(); true }
            .takeIf { !position.isFirst },
        CustomAccessibilityAction(label = "Move down") { onMoveDown(); true }
            .takeIf { !position.isLast },
    )
    val open = if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier
    val spoken = spokenStep(row = row)
    Column(modifier.fillMaxWidth().background(colors.paper)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepHandle(
                number = row.number,
                position = position,
                drag = drag,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
            Text(
                text = row.number.toString(),
                style = ManuscriptType.label,
                color = colors.muted,
                modifier = Modifier.widthIn(min = 24.dp).clearAndSetSemantics {},
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .then(open)
                    .clearAndSetSemantics {
                        contentDescription = spoken
                        customActions = moves
                        if (onOpen != null) {
                            role = Role.Button
                            onClick(label = null, action = { onOpen.invoke(); true })
                        }
                    }
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = row.sound, style = SOUND_STYLE, color = colors.ink)
                    MusicText(text = row.meta, style = ManuscriptType.body, color = colors.muted)
                }
                if (onOpen != null) {
                    Icon(
                        imageVector = ManuscriptIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        row.warning?.let { StepWarning(text = it) }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}

@Composable
private fun StepHandle(
    number: Int,
    position: RowPosition,
    drag: Modifier,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(drag)
                .clickable(role = Role.Button, onClick = { menuOpen = true })
                .semantics { contentDescription = "Reorder step $number" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ManuscriptIcons.Handle,
                contentDescription = null,
                tint = Manuscript.colors.muted,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(text = "Move up") },
                enabled = !position.isFirst,
                onClick = {
                    menuOpen = false
                    onMoveUp()
                },
            )
            DropdownMenuItem(
                text = { Text(text = "Move down") },
                enabled = !position.isLast,
                onClick = {
                    menuOpen = false
                    onMoveDown()
                },
            )
            DropdownMenuItem(
                text = { Text(text = "Remove step") },
                onClick = {
                    menuOpen = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun StepWarning(text: String) {
    val colors = Manuscript.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .padding(start = 52.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = ManuscriptIcons.Warning,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.padding(top = 3.dp).size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, style = ManuscriptType.body, color = colors.accentText)
    }
}

private val SOUND_STYLE: TextStyle = ManuscriptType.displayItalic.copy(fontSize = 24.sp)
