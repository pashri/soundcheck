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
import org.pashri.soundcheck.ui.pattern.PatternEditorRoute
import org.pashri.soundcheck.ui.pattern.PatternListLinks
import org.pashri.soundcheck.ui.pattern.PatternListRoute
import org.pashri.soundcheck.ui.programme.ProgrammeEditorRoute
import org.pashri.soundcheck.ui.programme.ProgrammeLinks
import org.pashri.soundcheck.ui.settings.SettingsRoute
import org.pashri.soundcheck.ui.sounds.SoundsRoute
import org.pashri.soundcheck.ui.step.StepEditorRoute
import org.pashri.soundcheck.ui.step.StepLinks
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef

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
        navController.navigate(route = WarmupRoutes.PLAYING) { launchSingleTop = true }
    }
    val closing: (String) -> () -> Unit = { route ->
        { navController.popBackStack(route = route, inclusive = true) }
    }
    navigation(route = Tab.WarmUp.route, startDestination = WarmupRoutes.HOME) {
        composable(route = WarmupRoutes.HOME) {
            WarmupHomeRoute(
                factory = container.warmupHomeViewModelFactory,
                links = HomeLinks(
                    openPlaying = openPlaying,
                    openSettings = { navController.navigate(WarmupRoutes.SETTINGS) },
                    editProgramme = { navController.navigate(WarmupRoutes.programme(it)) },
                    openPatterns = {
                        navController.navigate(WarmupRoutes.patterns(pickFor = null))
                    },
                    openSounds = { navController.navigate(WarmupRoutes.sounds(pickFor = null)) },
                ),
            )
        }
        composable(route = WarmupRoutes.PLAYING) {
            WarmupRoute(
                factory = container.warmupViewModelFactory,
                onFinished = closing(WarmupRoutes.PLAYING),
            )
        }
        composable(route = WarmupRoutes.SETTINGS) {
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
                    editStep = { key ->
                        val ref = StepRef(programmeId = id, key = key)
                        navController.navigate(WarmupRoutes.step(ref))
                    },
                ),
            )
        }
        composable(
            route = WarmupRoutes.STEP,
            arguments = listOf(
                requiredArg(WarmupRoutes.ARG_PROGRAMME),
                requiredArg(WarmupRoutes.ARG_STEP),
            ),
        ) { entry ->
            val ref = StepRef(
                programmeId = ProgrammeId(entry.requireArg(WarmupRoutes.ARG_PROGRAMME)),
                key = StepKey(entry.requireArg(WarmupRoutes.ARG_STEP)),
            )
            StepEditorRoute(
                factory = container.stepEditorFactory(ref),
                links = StepLinks(
                    back = closing(WarmupRoutes.STEP),
                    choosePattern = {
                        navController.navigate(WarmupRoutes.patterns(pickFor = ref))
                    },
                    chooseSound = { navController.navigate(WarmupRoutes.sounds(pickFor = ref)) },
                ),
            )
        }
        composable(
            route = WarmupRoutes.PATTERNS,
            arguments = listOf(
                optionalArg(WarmupRoutes.ARG_PROGRAMME),
                optionalArg(WarmupRoutes.ARG_STEP),
            ),
        ) { entry ->
            PatternListRoute(
                factory = container.patternListFactory(entry.pickFor()),
                links = PatternListLinks(
                    back = closing(WarmupRoutes.PATTERNS),
                    editPattern = { navController.navigate(WarmupRoutes.pattern(it)) },
                ),
            )
        }
        composable(
            route = WarmupRoutes.PATTERN,
            arguments = listOf(requiredArg(WarmupRoutes.ARG_PATTERN)),
        ) { entry ->
            PatternEditorRoute(
                factory = container.patternEditorFactory(
                    PatternId(entry.requireArg(WarmupRoutes.ARG_PATTERN)),
                ),
                onBack = closing(WarmupRoutes.PATTERN),
            )
        }
        composable(
            route = WarmupRoutes.SOUNDS,
            arguments = listOf(
                optionalArg(WarmupRoutes.ARG_PROGRAMME),
                optionalArg(WarmupRoutes.ARG_STEP),
            ),
        ) { entry ->
            SoundsRoute(
                factory = container.soundsFactory(entry.pickFor()),
                onBack = closing(WarmupRoutes.SOUNDS),
            )
        }
    }
}

private fun requiredArg(name: String): NamedNavArgument =
    navArgument(name = name) { type = NavType.StringType }

private fun optionalArg(name: String): NamedNavArgument = navArgument(name = name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

private fun NavBackStackEntry.requireArg(name: String): String =
    checkNotNull(value = arguments?.getString(name)) { "The route has no $name" }

private fun NavBackStackEntry.pickFor(): StepRef? = WarmupRoutes.pickFor(
    programme = arguments?.getString(WarmupRoutes.ARG_PROGRAMME),
    step = arguments?.getString(WarmupRoutes.ARG_STEP),
)
