package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType

/**
 * Stands in for a tool that a later plan builds.
 *
 * @param title the tool's name.
 */
@Composable
fun NotYetBuiltScreen(title: String) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = title)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = "Not built yet",
                style = ManuscriptType.displayItalic,
                color = Manuscript.colors.muted,
            )
        }
    }
}
