package org.pashri.soundcheck.ui.sounds

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.BackHeader
import org.pashri.soundcheck.ui.components.ChosenBadge
import org.pashri.soundcheck.ui.components.ConfirmDialog
import org.pashri.soundcheck.ui.components.ControlShape
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.NameDialog
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.components.OutlineIconButton
import org.pashri.soundcheck.ui.components.QuietButton
import org.pashri.soundcheck.ui.components.hasMicPermission
import org.pashri.soundcheck.ui.components.openAppSettings
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
 * @property play plays a Sound's recording, or stops it.
 * @property toggleRecorder opens or closes a Sound's recorder.
 * @property allowMicrophone shows Android's permission dialog.
 * @property openSettings opens Soundcheck's page in the phone's Settings.
 * @property startRecording the record button went down.
 * @property stopRecording the record button came up.
 * @property undo takes back a Sound's last change of recording.
 * @property usePhoneVoice gives up a Sound's recording.
 */
data class SoundsActions(
    val back: () -> Unit,
    val choose: (SoundId) -> Unit,
    val add: (String) -> Unit,
    val rename: (SoundId, String) -> Unit,
    val delete: (SoundId) -> Unit,
    val play: (SoundId) -> Unit,
    val toggleRecorder: (SoundId) -> Unit,
    val allowMicrophone: () -> Unit,
    val openSettings: () -> Unit,
    val startRecording: () -> Unit,
    val stopRecording: () -> Unit,
    val undo: (SoundId) -> Unit,
    val usePhoneVoice: (SoundId) -> Unit,
)

/**
 * The Sounds list, wired to its view model. The microphone permission is checked each time
 * the list comes into view and asked for the first time a recorder opens without it. A take
 * in progress, or a recording playing, stops when the list leaves the screen or the app goes
 * to the background, but not on rotation.
 *
 * @param factory builds the [SoundsViewModel].
 * @param onBack closes the list.
 */
@Composable
fun SoundsRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val viewModel: SoundsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val canAskAgain =
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        viewModel.onPermissionResult(granted = granted, canAskAgain = canAskAgain == true)
    }
    LifecycleEventEffect(event = Lifecycle.Event.ON_START) {
        viewModel.onShown(granted = activity?.hasMicPermission() == true)
    }
    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.onHidden()
    }
    DisposableEffect(key1 = viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.onHidden() }
    }
    val askForMic = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
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
            play = viewModel::play,
            toggleRecorder = { id -> if (viewModel.toggleRecorder(id)) askForMic() },
            allowMicrophone = askForMic,
            openSettings = { activity?.openAppSettings() },
            startRecording = viewModel::startRecording,
            stopRecording = viewModel::stopRecording,
            undo = viewModel::undo,
            usePhoneVoice = viewModel::usePhoneVoice,
        ),
    )
}

/**
 * Every Sound, with its recording's length or "phone voice", a play button for a
 * recording, a Record button, and under an opened Sound the hold-to-record panel, in the
 * Manuscript design. Scrolls at large font and display sizes.
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
    Column(modifier = Modifier.fillMaxSize().background(colors.paper)) {
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
            state.notice?.let { notice ->
                Text(
                    text = notice,
                    style = ManuscriptType.body,
                    color = colors.accentText,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(vertical = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            state.rows.forEach { row ->
                SoundEntry(
                    row = row,
                    picking = state.picking,
                    canDelete = state.canDelete,
                    onOpen = {
                        if (state.picking) actions.choose(row.id) else renaming = row.id.value
                    },
                    onDelete = { deleting = row.id.value },
                    actions = actions,
                )
                state.panel?.takeIf { row.open }?.let { panel ->
                    RecordPanelView(panel = panel, actions = actions)
                }
                HorizontalDivider(thickness = 1.dp, color = colors.rule)
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

/**
 * One Sound's row. Its controls wrap under the label when the font or display is too large
 * for them to fit beside it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundEntry(
    row: SoundRow,
    picking: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    actions: SoundsActions,
) {
    if (picking) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = row.chosen, role = Role.RadioButton, onClick = onOpen)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoundText(row = row, modifier = Modifier.weight(1f))
            if (row.chosen) ChosenBadge()
        }
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoundText(
            row = row,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClickLabel = "Rename", onClick = onOpen)
                .padding(end = 12.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (row.recorded) {
                OutlineIconButton(
                    icon = if (row.playing) ManuscriptIcons.Stop else ManuscriptIcons.Play,
                    description = if (row.playing) "Stop ${row.label}" else "Play ${row.label}",
                    onClick = { actions.play(row.id) },
                )
            }
            RecordPill(
                open = row.open,
                label = row.label,
                onClick = { actions.toggleRecorder(row.id) },
            )
            if (canDelete) {
                OutlineIconButton(
                    icon = ManuscriptIcons.Delete,
                    description = "Delete ${row.label}",
                    onClick = onDelete,
                )
            }
        }
    }
}

/** The label in italic serif, and under it how it is announced and where it is used. */
@Composable
private fun SoundText(row: SoundRow, modifier: Modifier = Modifier) {
    val colors = Manuscript.colors
    val spoken = "${row.label}, ${row.spokenDetail}, ${row.usage}"
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = row.label,
            style = LABEL_STYLE,
            color = colors.ink,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = "${row.detail} · ${row.usage}",
            style = ManuscriptType.body,
            color = colors.muted,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** The vermilion-outlined "Record" button; "Done" while the Sound's recorder is open. */
@Composable
private fun RecordPill(open: Boolean, label: String, onClick: () -> Unit) {
    val colors = Manuscript.colors
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(ControlShape)
            .border(width = 1.dp, color = colors.accent, shape = ControlShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (open) {
                    "Done, close the recorder for $label"
                } else {
                    "Record $label"
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (open) "Done" else "Record",
            style = ManuscriptType.button.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accentText,
        )
    }
}

/** The recorder under an opened Sound, as `Manuscript-Sounds` shows it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordPanelView(panel: RecordPanel, actions: SoundsActions) {
    val colors = Manuscript.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (panel.mode == PanelMode.READY || panel.mode == PanelMode.RECORDING) {
                HoldToRecord(panel = panel, actions = actions)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = panel.headline, style = HEADLINE_STYLE, color = colors.ink)
                Text(text = panel.body, style = ManuscriptType.body, color = colors.muted)
                if (panel.mode == PanelMode.RECORDING) Levels(levels = panel.levels)
            }
        }
        when (panel.mode) {
            PanelMode.ASK ->
                OutlineButton(text = "Allow microphone", onClick = actions.allowMicrophone)
            PanelMode.SETTINGS ->
                OutlineButton(text = "Open settings", onClick = actions.openSettings)
            else -> Unit
        }
        panel.message?.let { message ->
            Text(
                text = message,
                style = ManuscriptType.body,
                color = colors.accentText,
                modifier = Modifier
                    .heightIn(min = 22.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (panel.canUndo) {
                QuietButton(text = "Undo", onClick = { actions.undo(panel.soundId) })
            }
            if (panel.canUsePhoneVoice) {
                QuietButton(
                    text = "Use phone voice",
                    onClick = { actions.usePhoneVoice(panel.soundId) },
                )
            }
        }
    }
}

/**
 * The round record button: recording while it is held down. TalkBack users double-tap to
 * start and double-tap again to stop, since a hold can't be made through TalkBack.
 */
@Composable
private fun HoldToRecord(panel: RecordPanel, actions: SoundsActions) {
    val colors = Manuscript.colors
    val start by rememberUpdatedState(actions.startRecording)
    val stop by rememberUpdatedState(actions.stopRecording)
    val recording = panel.mode == PanelMode.RECORDING
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = RING_ALPHA))
            .padding(6.dp)
            .clip(CircleShape)
            .background(colors.accent)
            .pointerInput(key1 = Unit) {
                detectTapGestures(
                    onPress = {
                        start()
                        try {
                            tryAwaitRelease()
                        } finally {
                            stop()
                        }
                    },
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = panel.recordDescription
                onClick(label = if (recording) "stop" else "record") {
                    if (recording) stop() else start()
                    true
                }
            },
    )
}

/** The recent loudness as a row of bars, like the design's waveform; TalkBack skips it. */
@Composable
private fun Levels(levels: List<Float>) {
    val color = Manuscript.colors.accent
    Canvas(modifier = Modifier.width(200.dp).height(24.dp).clearAndSetSemantics {}) {
        val step = 5.dp.toPx()
        val middle = size.height / 2
        levels.forEachIndexed { index, level ->
            val loudness = level.coerceIn(minimumValue = 0f, maximumValue = 1f)
            val half = maxOf(a = 1.dp.toPx(), b = loudness * middle)
            val x = index * step + 2.dp.toPx()
            drawLine(
                color = color,
                start = Offset(x = x, y = middle - half),
                end = Offset(x = x, y = middle + half),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private val LABEL_STYLE: TextStyle = ManuscriptType.displayItalic.copy(fontSize = 24.sp)
private val HEADLINE_STYLE: TextStyle = ManuscriptType.body.copy(fontWeight = FontWeight.Medium)
private const val RING_ALPHA = 0.22f
