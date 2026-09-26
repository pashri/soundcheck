package org.pashri.soundcheck.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconTest {
    private val bars = "M32,47V57M41,42V62M50,35V69M59,40V64M68,30V74M77,44V60"
    private val staff = "M18,38H90M18,45H90M18,52H90M18,59H90M18,66H90"

    @Test
    fun `the launcher icon and its round twin are adaptive with a themed-icon layer`() {
        val layers = listOf(
            "<background android:drawable=\"@color/ic_launcher_background\"",
            "<foreground android:drawable=\"@drawable/ic_launcher_foreground\"",
            "<monochrome android:drawable=\"@drawable/ic_launcher_monochrome\"",
        )
        listOf("ic_launcher", "ic_launcher_round").forEach { name ->
            val icon = resFile(path = "res/mipmap-anydpi-v26/$name.xml").readText()
            layers.forEach { layer -> assertTrue("$name lacks $layer", icon.contains(layer)) }
        }
        val manifest = resFile(path = "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test
    fun `the icon is vermilion bars on a faint staff, on the day paper`() {
        assertEquals(
            hexOf(DayColors.paper),
            colorResource(folder = "values", name = "ic_launcher_background"),
        )
        val foreground = resFile(path = "res/drawable/ic_launcher_foreground.xml").readText()
        assertTrue(foreground.contains("android:pathData=\"$staff\""))
        assertTrue(foreground.contains("android:strokeAlpha=\"0.35\""))
        assertTrue(foreground.contains("android:pathData=\"$bars\""))
        assertTrue(foreground.contains("android:strokeColor=\"#C23B22\""))
        val monochrome = resFile(path = "res/drawable/ic_launcher_monochrome.xml").readText()
        assertTrue(monochrome.contains("android:pathData=\"$staff\""))
        assertTrue(monochrome.contains("android:pathData=\"$bars\""))
    }

    @Test
    fun `the notification icon is the six bars alone, in white`() {
        val icon = resFile(path = "res/drawable/ic_notification.xml").readText()
        val small = "M3,10V14M6.6,8V16M10.2,5.2V18.8M13.8,7.2V16.8M17.4,3.2V20.8M21,8.8V15.2"
        assertTrue(icon.contains("android:pathData=\"$small\""))
        assertTrue(icon.contains("android:strokeColor=\"#FFFFFFFF\""))
        assertFalse(icon.contains("H90"))
    }
}
