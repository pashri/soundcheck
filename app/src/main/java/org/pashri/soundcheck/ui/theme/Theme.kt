package org.pashri.soundcheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalManuscriptColors = staticCompositionLocalOf { DayColors }

/** Access to the current Manuscript palette from inside a Composable. */
object Manuscript {
    /** The palette for the current day/night setting. */
    val colors: ManuscriptColors
        @Composable
        @ReadOnlyComposable
        get() = LocalManuscriptColors.current
}

/**
 * Applies the Manuscript design, following the system dark theme by default.
 *
 * @param dark whether to use the night palette.
 * @param content the screens to theme.
 */
@Composable
fun SoundcheckTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) NightColors else DayColors
    CompositionLocalProvider(LocalManuscriptColors provides colors) {
        MaterialTheme(colorScheme = colors.toColorScheme(dark), content = content)
    }
}

private fun ManuscriptColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        outline = rule,
    )
}
