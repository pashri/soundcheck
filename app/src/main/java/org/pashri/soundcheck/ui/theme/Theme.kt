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
import androidx.compose.ui.graphics.Color

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
 * Applies the Manuscript design, following the system dark theme by default. Material's
 * components (dialogs, menus, text fields) get every colour and text style from it too.
 *
 * @param dark whether to use the night palette.
 * @param content the screens to theme.
 */
@Composable
fun SoundcheckTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) NightColors else DayColors
    CompositionLocalProvider(LocalManuscriptColors provides colors) {
        MaterialTheme(
            colorScheme = manuscriptColorScheme(colors = colors, dark = dark),
            typography = ManuscriptTypography,
            content = content,
        )
    }
}

/**
 * Material's colour scheme with every slot taken from [colors], so no Material default
 * (lavender surfaces, purple-grey text) reaches a dialog, a menu or a text field. Only the
 * scrim, always drawn translucent, stays black.
 *
 * @param colors the palette.
 * @param dark whether it is the night palette.
 * @return the scheme.
 */
fun manuscriptColorScheme(colors: ManuscriptColors, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = colors.raised,
        onPrimaryContainer = colors.ink,
        inversePrimary = colors.accent,
        secondary = colors.ink,
        onSecondary = colors.paper,
        secondaryContainer = colors.raised,
        onSecondaryContainer = colors.ink,
        tertiary = colors.accentText,
        onTertiary = colors.paper,
        tertiaryContainer = colors.raised,
        onTertiaryContainer = colors.ink,
        background = colors.paper,
        onBackground = colors.ink,
        surface = colors.paper,
        onSurface = colors.ink,
        surfaceVariant = colors.raised,
        onSurfaceVariant = colors.muted,
        surfaceTint = colors.paper,
        inverseSurface = colors.ink,
        inverseOnSurface = colors.paper,
        error = colors.accentText,
        onError = colors.paper,
        errorContainer = colors.raised,
        onErrorContainer = colors.accentText,
        outline = colors.muted,
        outlineVariant = colors.rule,
        scrim = Color.Black,
        surfaceBright = colors.raised,
        surfaceContainer = colors.raised,
        surfaceContainerHigh = colors.raised,
        surfaceContainerHighest = colors.raised,
        surfaceContainerLow = colors.paper,
        surfaceContainerLowest = colors.paper,
        surfaceDim = colors.paper,
    )
}
