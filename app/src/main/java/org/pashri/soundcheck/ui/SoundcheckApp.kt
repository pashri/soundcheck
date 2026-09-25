package org.pashri.soundcheck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.pashri.soundcheck.di.AppContainer
import org.pashri.soundcheck.ui.components.ManuscriptNavBar
import org.pashri.soundcheck.ui.components.NotYetBuiltScreen
import org.pashri.soundcheck.ui.components.Tab
import org.pashri.soundcheck.ui.metronome.MetronomeRoute
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.tuner.TunerRoute

/**
 * The whole app: the current tool above the tab bar.
 *
 * @param container shared dependencies.
 */
@Composable
fun SoundcheckApp(container: AppContainer) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = Tab.entries.firstOrNull { it.route == entry?.destination?.route }
        ?: Tab.Metronome
    Column(Modifier.fillMaxSize().background(Manuscript.colors.paper)) {
        NavHost(
            navController = navController,
            startDestination = Tab.Metronome.route,
            modifier = Modifier.weight(1f),
        ) {
            composable(Tab.Tuner.route) {
                TunerRoute(factory = container.tunerViewModelFactory)
            }
            composable(Tab.Metronome.route) {
                MetronomeRoute(factory = container.metronomeViewModelFactory)
            }
            composable(Tab.WarmUp.route) { NotYetBuiltScreen(title = "Warm-up") }
        }
        ManuscriptNavBar(current = current, onSelect = { navController.openTab(it) })
    }
}

private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
