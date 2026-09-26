package org.pashri.soundcheck.ui.warmup

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import org.pashri.soundcheck.di.AppContainer
import org.pashri.soundcheck.ui.components.Tab

/**
 * The Warm-up tab's screens, nested under the tab's route so the tab bar stays on the
 * Warm-up whichever of them is showing.
 *
 * @param container shared dependencies.
 * @param navController moves between the screens.
 */
fun NavGraphBuilder.warmupGraph(container: AppContainer, navController: NavHostController) {
    val openPlaying: () -> Unit = {
        navController.navigate(WarmupRoutes.PLAYING) { launchSingleTop = true }
    }
    navigation(route = Tab.WarmUp.route, startDestination = WarmupRoutes.HOME) {
        composable(WarmupRoutes.HOME) {
            WarmupHomeRoute(
                factory = container.warmupHomeViewModelFactory,
                links = HomeLinks(openPlaying = openPlaying),
            )
        }
        composable(WarmupRoutes.PLAYING) {
            WarmupRoute(
                factory = container.warmupViewModelFactory,
                onFinished = {
                    navController.popBackStack(WarmupRoutes.PLAYING, inclusive = true)
                },
            )
        }
    }
}
