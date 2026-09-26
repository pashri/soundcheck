package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            HeaderTitle(title = title, onRename = onRename, modifier = Modifier.weight(1f))
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = ManuscriptType.label,
                    color = colors.muted,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
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
            modifier = Modifier.weight(1f, fill = false),
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
 * A row of joined buttons, exactly one of which is chosen, such as the Voice Types. Each
 * button grows in height to fit its label at large font sizes; all grow together.
 *
 * @param options the choices, in order.
 * @param selected the chosen one.
 * @param label each choice's text.
 * @param onSelect called with the choice tapped.
 * @param modifier modifier for the row.
 * @param spoken what TalkBack says for each choice.
 */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    spoken: (T) -> String = { spokenMusic(label(it)) },
) {
    val colors = Manuscript.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(ControlShape)
            .border(width = 1.dp, color = colors.ink, shape = ControlShape)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 48.dp)
                    .background(if (on) colors.ink else Color.Transparent)
                    .selectable(
                        selected = on,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .semantics { contentDescription = spoken(option) }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = ManuscriptType.button,
                    color = if (on) colors.paper else colors.ink,
                    textAlign = TextAlign.Center,
                )
            }
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
    val size = with(LocalDensity.current) { STEPPER_SYMBOL.toSp() }
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
        Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken }) {
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

/** 24 sp values: the baseline 30 sp down, 10 sp above the bottom, room for any accidental. */
private val VALUE_ABOVE_BASELINE = 30.sp
private val VALUE_BELOW_BASELINE = 10.sp

/** 15 sp details: the baseline 18 sp down, 6 sp above the bottom. */
private val DETAIL_ABOVE_BASELINE = 18.sp
private val DETAIL_BELOW_BASELINE = 6.sp
