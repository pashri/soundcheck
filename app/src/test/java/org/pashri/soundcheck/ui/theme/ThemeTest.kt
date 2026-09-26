package org.pashri.soundcheck.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    private fun ColorScheme.slots(): Map<String, Color> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "surfaceBright" to surfaceBright,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceDim" to surfaceDim,
    )

    private fun ManuscriptColors.all(): Set<Color> =
        setOf(paper, ink, muted, rule, faint, accent, onAccent, accentText, raised)

    @Test
    fun `every colour Material draws with is one of Manuscript's, by day and by night`() {
        mapOf(false to DayColors, true to NightColors).forEach { (dark, colors) ->
            val scheme = manuscriptColorScheme(colors = colors, dark = dark)
            scheme.slots().forEach { (slot, color) ->
                val message = "$slot (dark = $dark) is not a Manuscript colour"
                assertTrue(message, color in colors.all())
            }
            assertTrue("the scrim is black", scheme.scrim == Color.Black)
        }
    }

    @Test
    fun `Material's type scale uses only the Manuscript faces`() {
        val typography = ManuscriptTypography
        val titles = listOf(
            typography.displayLarge,
            typography.displayMedium,
            typography.displaySmall,
            typography.headlineLarge,
            typography.headlineMedium,
            typography.headlineSmall,
        )
        val text = listOf(
            typography.titleLarge,
            typography.titleMedium,
            typography.titleSmall,
            typography.bodyLarge,
            typography.bodyMedium,
            typography.bodySmall,
            typography.labelLarge,
            typography.labelMedium,
            typography.labelSmall,
        )
        assertTrue(titles.all { it.fontFamily == SerifFamily })
        assertTrue(text.all { it.fontFamily == SansFamily })
    }
}
