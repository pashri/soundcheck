package org.pashri.soundcheck.ui.warmup

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import org.pashri.soundcheck.di.AppContainer
import org.pashri.soundcheck.ui.components.Tab
import org.pashri.soundcheck.ui.programme.ProgrammeEditorRoute
import org.pashri.soundcheck.ui.programme.ProgrammeLinks
import org.pashri.soundcheck.ui.settings.SettingsRoute
import org.pashri.soundcheck.warmup.ProgrammeId

/**
 * The Warm-up tab's screens, nested under the tab's route so the tab bar stays on the
 * Warm-up whichever of them is showing. Each screen's back pops that screen's own route, so
 * a screen that closes itself late never closes another.
 *
 * @param container shared dependencies.
 * @param navController moves between the screens.
 */
fun NavGraphBuilder.warmupGraph(container: AppContainer, navController: NavHostController) {
    val openPlaying: () -> Unit = {
        navController.navigate(WarmupRoutes.PLAYING) { launchSingleTop = true }
    }
    val closing: (String) -> () -> Unit = { route ->
        { navController.popBackStack(route, inclusive = true) }
    }
    navigation(route = Tab.WarmUp.route, startDestination = WarmupRoutes.HOME) {
        composable(WarmupRoutes.HOME) {
            WarmupHomeRoute(
                factory = container.warmupHomeViewModelFactory,
                links = HomeLinks(
                    openPlaying = openPlaying,
                    openSettings = { navController.navigate(WarmupRoutes.SETTINGS) },
                    editProgramme = { navController.navigate(WarmupRoutes.programme(it)) },
                ),
            )
        }
        composable(WarmupRoutes.PLAYING) {
            WarmupRoute(
                factory = container.warmupViewModelFactory,
                onFinished = closing(WarmupRoutes.PLAYING),
            )
        }
        composable(WarmupRoutes.SETTINGS) {
            SettingsRoute(
                factory = container.settingsViewModelFactory,
                onBack = closing(WarmupRoutes.SETTINGS),
            )
        }
        composable(
            route = WarmupRoutes.PROGRAMME,
            arguments = listOf(requiredArg(WarmupRoutes.ARG_PROGRAMME)),
        ) { entry ->
            val id = ProgrammeId(entry.requireArg(WarmupRoutes.ARG_PROGRAMME))
            ProgrammeEditorRoute(
                factory = container.programmeEditorFactory(id),
                links = ProgrammeLinks(
                    back = closing(WarmupRoutes.PROGRAMME),
                    openPlaying = openPlaying,
                ),
            )
        }
    }
}

private fun requiredArg(name: String): NamedNavArgument =
    navArgument(name) { type = NavType.StringType }

private fun NavBackStackEntry.requireArg(name: String): String =
    checkNotNull(arguments?.getString(name)) { "The route has no $name" }
