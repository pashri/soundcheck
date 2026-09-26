package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType

/**
 * The three tools, in bottom-bar order.
 *
 * @property route navigation route.
 * @property label text under the icon.
 * @property icon the tab's icon.
 */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Tuner("tuner", "Tuner", ManuscriptIcons.Tuner),
    Metronome("metronome", "Metronome", ManuscriptIcons.Metronome),
    WarmUp("warmup", "Warm-up", ManuscriptIcons.WarmUp),
}

/**
 * The [Tab] whose [Tab.route] matches [route], for opening the tab an intent asked for.
 *
 * @param route a tab's route, or null.
 * @return the matching tab, or null when [route] names none.
 */
fun tabForRoute(route: String?): Tab? = Tab.entries.firstOrNull { it.route == route }

/**
 * Bottom navigation in the Manuscript style: a rule above, and a vermilion bar over the
 * selected tab.
 *
 * @param current the tab being shown.
 * @param onSelect called with the tab the user picks.
 * @param modifier modifier for the bar's outer column.
 */
@Composable
fun ManuscriptNavBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val colors = Manuscript.colors
    Column(modifier.fillMaxWidth().background(colors.paper).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Tab.entries.forEach { tab ->
                NavItem(tab = tab, selected = tab == current, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val colors = Manuscript.colors
    val tint = if (selected) colors.ink else colors.muted
    Column(
        modifier = Modifier
            .width(96.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .drawBehind {
                if (selected) drawRect(colors.accent, size = Size(size.width, 2.dp.toPx()))
            }
            .padding(top = 12.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint)
        Text(
            text = tab.label,
            style = ManuscriptType.navLabel.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = tint,
        )
    }
}
