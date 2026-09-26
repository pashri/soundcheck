package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType

/** The corner rounding of every bordered control in the Manuscript design. */
val ControlShape: Shape = RoundedCornerShape(6.dp)

/**
 * A small uppercase monospace heading such as "PROGRAMMES".
 *
 * @param text the heading.
 * @param modifier modifier for the text.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = ManuscriptType.label,
        color = Manuscript.colors.muted,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * A bordered card showing one thing: a small label, a serif value and an optional detail
 * line. When [onClick] is set it has a chevron and opens that thing.
 *
 * @param label the small uppercase label, e.g. "YOUR RANGE".
 * @param value the main text, e.g. "Tenor · C3 – A4".
 * @param onClick what a tap does, or null for a card that only shows.
 * @param modifier modifier for the card.
 * @param detail a muted line under the value, or null.
 */
@Composable
fun LinkCard(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val colors = Manuscript.colors
    val spoken = spokenRow(parts = listOf(label, value, detail))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(ControlShape)
            .border(width = 1.dp, color = colors.rule, shape = ControlShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = spoken
                if (onClick != null) {
                    role = Role.Button
                    onClick(label = null, action = { onClick.invoke(); true })
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = ManuscriptType.label, color = colors.muted)
            MusicText(text = value, style = ManuscriptType.chip, color = colors.ink)
            if (detail != null) {
                MusicText(text = detail, style = ManuscriptType.body, color = colors.muted)
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = ManuscriptIcons.ChevronRight,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * A bordered button such as Edit.
 *
 * @param text the label.
 * @param onClick what a tap does.
 * @param modifier modifier for the button.
 * @param enabled false greys it out and ignores taps.
 * @param description what TalkBack says instead of [text], e.g. "Edit Morning", or null.
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    val colors = Manuscript.colors
    val tint = if (enabled) colors.ink else colors.faint
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ControlShape)
            .border(width = 1.dp, color = tint, shape = ControlShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ManuscriptType.button,
            color = tint,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A 48 dp square, bordered icon button in the Manuscript design, greyed out and ignoring
 * taps when [enabled] is false, as on a Pattern's or a Sound's delete control.
 *
 * @param icon the icon.
 * @param description what TalkBack says, e.g. "Delete hum".
 * @param onClick what a tap does.
 * @param modifier modifier for the button.
 * @param enabled false greys it out and ignores taps.
 */
@Composable
fun OutlineIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Manuscript.colors
    val tint = if (enabled) colors.ink else colors.faint
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(ControlShape)
            .border(width = 1.dp, color = tint, shape = ControlShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

/**
 * The small "CHOSEN" tag beside a row a picker has selected.
 *
 * @param modifier modifier for the text.
 */
@Composable
fun ChosenBadge(modifier: Modifier = Modifier) {
    Text(
        text = "CHOSEN",
        style = ManuscriptType.label,
        color = Manuscript.colors.accentText,
        modifier = modifier.padding(horizontal = 8.dp),
    )
}

/**
 * A vermilion button such as Start.
 *
 * @param text the label.
 * @param onClick what a tap does.
 * @param modifier modifier for the button.
 * @param icon an icon before the label, or null.
 * @param enabled false greys it out and ignores taps.
 * @param description what TalkBack says instead of [text], e.g. "Start Morning", or null.
 */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    description: String? = null,
) {
    val colors = Manuscript.colors
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(ControlShape)
            .background(if (enabled) colors.accent else colors.faint)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ManuscriptType.button.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onAccent,
            textAlign = TextAlign.Center,
        )
    }
}
