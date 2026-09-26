package org.pashri.soundcheck.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchThemeTest {
    @Test
    fun `the launch window is the paper colour by day and by night`() {
        assertEquals(hexOf(DayColors.paper), colorResource(folder = "values", name = "paper"))
        assertEquals(
            hexOf(NightColors.paper),
            colorResource(folder = "values-night", name = "paper"),
        )
    }

    @Test
    fun `the app starts in its own theme, whose window is the paper colour`() {
        val manifest = resFile(path = "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:theme=\"@style/Theme.Soundcheck\""))
        listOf("values", "values-night").forEach { folder ->
            val themes = resFile(path = "res/$folder/themes.xml").readText()
            assertTrue(folder, themes.contains("<style name=\"Theme.Soundcheck\""))
            assertTrue(
                folder,
                themes.contains("<item name=\"android:windowBackground\">@color/paper</item>"),
            )
        }
    }
}
