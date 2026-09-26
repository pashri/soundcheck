package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType

/**
 * A serif screen title with an optional monospace note on the right, over a rule.
 *
 * @param title the screen's name.
 * @param trailing a short uppercase note such as "ACCENT / 4", or null for none.
 */
@Composable
fun ScreenHeader(title: String, trailing: String? = null) {
    val colors = Manuscript.colors
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = ManuscriptType.screenTitle,
                color = colors.ink,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            )
            if (trailing != null) {
                Text(text = trailing, style = ManuscriptType.label, color = colors.muted)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}
