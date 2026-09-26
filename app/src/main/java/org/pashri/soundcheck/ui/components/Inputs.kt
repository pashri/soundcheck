package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType

/**
 * An editor screen's header: a back link, the title (with a pencil when it can be renamed),
 * an optional note on the right, and a rule below.
 *
 * @param backLabel where back goes, e.g. "Warm-up".
 * @param title the screen's title.
 * @param onBack goes back.
 * @param trailing a short uppercase note such as "5 STEPS", or null.
 * @param onRename opens a rename dialog when the title is tapped, or null.
 */
@Composable
fun BackHeader(
    backLabel: String,
    title: String,
    onBack: () -> Unit,
    trailing: String? = null,
    onRename: (() -> Unit)? = null,
) {
    val colors = Manuscript.colors
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, top = 8.dp)
                .heightIn(min = 48.dp)
                .clip(ControlShape)
                .clickable(role = Role.Button, onClickLabel = "Go back", onClick = onBack)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ManuscriptIcons.ChevronLeft,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            MusicText(text = backLabel, style = ManuscriptType.body, color = colors.ink)
        }
        TitleAndNote(title = title, onRename = onRename, trailing = trailing)
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}

/**
 * The title with its note on the right. At large text sizes the note moves under the title
 * rather than squeezing it or touching it: the title never gives up width to the note, and
 * the two keep at least [TITLE_NOTE_GAP] apart on one line.
 *
 * @param title the screen's title.
 * @param onRename opens a rename dialog when the title is tapped, or null.
 * @param trailing a short uppercase note such as "A4 = 440 Hz", or null.
 * @param top the space above the title.
 * @param noteLift how far the note's bottom sits above the title's bottom.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TitleAndNote(
    title: String,
    onRename: (() -> Unit)?,
    trailing: String?,
    top: Dp = 0.dp,
    noteLift: Dp = 8.dp,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = top, bottom = 14.dp),
        horizontalArrangement = SpaceBetweenAtLeast(minimum = TITLE_NOTE_GAP),
    ) {
        HeaderTitle(title = title, onRename = onRename, modifier = Modifier)
        if (trailing != null) {
            Text(
                text = trailing,
                style = ManuscriptType.label,
                color = Manuscript.colors.muted,
                modifier = Modifier
                    .align(alignment = Alignment.Bottom)
                    .padding(bottom = noteLift),
            )
        }
    }
}

@Composable
private fun HeaderTitle(title: String, onRename: (() -> Unit)?, modifier: Modifier) {
    val colors = Manuscript.colors
    val action = if (onRename != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = "Rename", onClick = onRename)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(action)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicText(
            text = title,
            style = ManuscriptType.screenTitle,
            color = colors.ink,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        if (onRename != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = ManuscriptIcons.Edit,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Joined buttons, exactly one of which is chosen, such as the Voice Types. They sit in one
 * row while every label's longest word fits its button; at large sizes they wrap onto more
 * rows (four become two rows of two) rather than break a word. Buttons in a row grow in
 * height together to fit their labels.
 *
 * @param options the choices, in order.
 * @param selected the chosen one.
 * @param label each choice's text.
 * @param onSelect called with the choice tapped.
 * @param modifier modifier for the group.
 * @param spoken what TalkBack says for each choice.
 * @param mark draws a choice in place of its label, in the given ink, or null for the label.
 */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    spoken: (T) -> String = { spokenMusic(label(it)) },
    mark: (@Composable (option: T, ink: Color) -> Unit)? = null,
) {
    val colors = Manuscript.colors
    val widest = rememberWidestWord(texts = options.map(label), style = ManuscriptType.button)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .border(width = 1.dp, color = colors.ink, shape = ControlShape)
            .selectableGroup(),
    ) {
        val padding = with(receiver = LocalDensity.current) {
            (SEGMENT_PADDING * 2 + SEGMENT_SLACK).toPx()
        }
        val columns = if (mark != null) options.size else fittingColumns(
            count = options.size,
            widest = widest + padding,
            available = constraints.maxWidth.toFloat(),
        )
        Column {
            options.chunked(size = columns).forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = colors.ink)
                SegmentRow(
                    row = row,
                    columns = columns,
                    cell = { option ->
                        Segment(
                            on = option == selected,
                            text = label(option),
                            spoken = spoken(option),
                            onClick = { onSelect(option) },
                            mark = mark?.let { draw -> { ink -> draw(option, ink) } },
                        )
                    },
                )
            }
        }
    }
}

/** One row of a [Segmented] group, with empty space after a short last row. */
@Composable
private fun <T> SegmentRow(row: List<T>, columns: Int, cell: @Composable (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        row.forEach { option ->
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { cell(option) }
        }
        repeat(times = columns - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun Segment(
    on: Boolean,
    text: String,
    spoken: String,
    onClick: () -> Unit,
    mark: (@Composable (ink: Color) -> Unit)?,
) {
    val colors = Manuscript.colors
    val ink = if (on) colors.paper else colors.ink
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .heightIn(min = 48.dp)
            .background(if (on) colors.ink else Color.Transparent)
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = spoken }
            .padding(horizontal = SEGMENT_PADDING, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (mark != null) {
            mark(ink)
        } else {
            Text(
                text = text,
                style = ManuscriptType.button,
                color = ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * A square − or + button. Its symbol is sized in dp, like an icon, so it never outgrows the
 * 48 dp square at large font sizes.
 *
 * @param symbol "−" or "+".
 * @param description what TalkBack says, e.g. "Raise the lowest note".
 * @param enabled false greys it out and ignores taps.
 * @param onClick what a tap does.
 */
@Composable
fun StepperButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Manuscript.colors
    val tint = if (enabled) colors.ink else colors.faint
    val size = with(receiver = LocalDensity.current) { STEPPER_SYMBOL.toSp() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(ControlShape)
            .border(width = 1.dp, color = tint, shape = ControlShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = ManuscriptType.button.copy(fontSize = size, lineHeight = size),
            color = tint,
        )
    }
}

/**
 * A label and a value with − and + buttons, such as "Lowest C3". The value sits on a fixed
 * baseline, so a ♭ or ♯ (drawn from a fallback font with taller metrics) never moves the row.
 *
 * @param label the small label, e.g. "Lowest".
 * @param value the value, e.g. "B♭2".
 * @param lowerDescription what TalkBack says for −.
 * @param raiseDescription what TalkBack says for +.
 * @param canLower false greys out −.
 * @param canRaise false greys out +.
 * @param onLower what − does.
 * @param onRaise what + does.
 * @param modifier modifier for the row.
 * @param detail a muted line under the value, also on a fixed baseline, or null.
 */
@Composable
fun StepperRow(
    label: String,
    value: String,
    lowerDescription: String,
    raiseDescription: String,
    canLower: Boolean,
    canRaise: Boolean,
    onLower: () -> Unit,
    onRaise: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val colors = Manuscript.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val spoken = spokenRow(parts = listOf(label, value, detail))
        val merged = Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken }
        Column(modifier = merged) {
            Text(text = label, style = ManuscriptType.label, color = colors.muted)
            MusicText(
                text = value,
                style = ManuscriptType.chip,
                color = colors.ink,
                modifier = Modifier.paddingFromBaseline(
                    top = VALUE_ABOVE_BASELINE,
                    bottom = VALUE_BELOW_BASELINE,
                ),
            )
            if (detail != null) {
                MusicText(
                    text = detail,
                    style = ManuscriptType.body,
                    color = colors.muted,
                    modifier = Modifier.paddingFromBaseline(
                        top = DETAIL_ABOVE_BASELINE,
                        bottom = DETAIL_BELOW_BASELINE,
                    ),
                )
            }
        }
        StepperButton(
            symbol = "−",
            description = lowerDescription,
            enabled = canLower,
            onClick = onLower,
        )
        Spacer(Modifier.width(8.dp))
        StepperButton(
            symbol = "+",
            description = raiseDescription,
            enabled = canRaise,
            onClick = onRaise,
        )
    }
}

/**
 * A setting with a title, a note under it and a switch; the whole row toggles.
 *
 * @param title e.g. "Play over other audio".
 * @param note what the switch does in its current position.
 * @param checked whether it is on.
 * @param onCheckedChange called with the new position.
 * @param modifier modifier for the row.
 */
@Composable
fun SwitchRow(
    title: String,
    note: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Manuscript.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = ManuscriptType.button, color = colors.ink)
            Text(text = note, style = ManuscriptType.body, color = colors.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.faint,
                uncheckedTrackColor = colors.paper,
                uncheckedBorderColor = colors.faint,
            ),
        )
    }
}

private val STEPPER_SYMBOL = 22.dp

/** The least space between a header's title and its note on one line. */
private val TITLE_NOTE_GAP = 12.dp

/** The space either side of a segment's label. */
private val SEGMENT_PADDING = 4.dp

/** Room for the group's border and rounding when judging whether a label fits. */
private val SEGMENT_SLACK = 2.dp

/** 24 sp values: the baseline 30 sp down, 10 sp above the bottom, room for any accidental. */
private val VALUE_ABOVE_BASELINE = 30.sp
private val VALUE_BELOW_BASELINE = 10.sp

/** 15 sp details: the baseline 18 sp down, 6 sp above the bottom. */
private val DETAIL_ABOVE_BASELINE = 18.sp
private val DETAIL_BELOW_BASELINE = 6.sp
