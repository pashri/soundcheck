package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.ui.theme.Manuscript

/**
 * A serif screen title with an optional monospace note on the right, over a rule. At large
 * text sizes the note moves under the title rather than touching it.
 *
 * @param title the screen's name.
 * @param trailing a short uppercase note such as "ACCENT / 4", or null for none.
 */
@Composable
fun ScreenHeader(title: String, trailing: String? = null) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        TitleAndNote(
            title = title,
            onRename = null,
            trailing = trailing,
            top = 20.dp,
            noteLift = 0.dp,
        )
        HorizontalDivider(thickness = 1.dp, color = Manuscript.colors.rule)
    }
}
