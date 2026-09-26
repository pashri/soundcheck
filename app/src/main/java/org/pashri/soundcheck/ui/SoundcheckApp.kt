package org.pashri.soundcheck.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.pashri.soundcheck.di.AppContainer
import org.pashri.soundcheck.ui.components.ManuscriptNavBar
import org.pashri.soundcheck.ui.components.Tab
import org.pashri.soundcheck.ui.components.tabFor
import org.pashri.soundcheck.ui.metronome.MetronomeRoute
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.tuner.TunerRoute
import org.pashri.soundcheck.ui.warmup.WarmupRoutes
import org.pashri.soundcheck.ui.warmup.warmupGraph

/**
 * The whole app: the current tool above the tab bar.
 *
 * @param container shared dependencies.
 * @param openTab a tab to navigate to once, e.g. from the playback notification, or null.
 *     The Warm-up opens on its playing screen, since only the notification asks for it.
 * @param onTabOpened called once [openTab] has been acted on, so it is not repeated.
 */
@Composable
fun SoundcheckApp(container: AppContainer, openTab: Tab? = null, onTabOpened: () -> Unit = {}) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val routes = entry?.destination?.hierarchy?.map { it.route } ?: emptySequence()
    val current = tabFor(routes) ?: Tab.Metronome
    LaunchedEffect(key1 = openTab) {
        openTab?.let {
            navController.openTab(it)
            if (it == Tab.WarmUp) {
                navController.navigate(route = WarmupRoutes.PLAYING) { launchSingleTop = true }
            }
            onTabOpened()
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(Manuscript.colors.paper)) {
        NavHost(
            navController = navController,
            startDestination = Tab.Metronome.route,
            modifier = Modifier.weight(1f),
            // No transition animation: the default fade kept the outgoing tab composed (and
            // its view model running, e.g. the Metronome still clicking) while the incoming
            // tab had already started, instead of handing tabs over at once.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(route = Tab.Tuner.route) {
                TunerRoute(factory = container.tunerViewModelFactory)
            }
            composable(route = Tab.Metronome.route) {
                MetronomeRoute(factory = container.metronomeViewModelFactory)
            }
            warmupGraph(container = container, navController = navController)
        }
        ManuscriptNavBar(current = current, onSelect = { navController.openTab(it) })
    }
}

private fun NavHostController.openTab(tab: Tab) {
    navigate(route = tab.route) {
        popUpTo(id = graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
