package org.pashri.soundcheck.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File

/**
 * A file under the app's main source set; unit tests run with the `app` module as their
 * folder.
 *
 * @param path e.g. "res/values/colors.xml".
 * @return the file.
 */
fun resFile(path: String): File = File("src/main/$path")

/**
 * A colour resource's value.
 *
 * @param folder e.g. "values" or "values-night".
 * @param name the colour's name.
 * @return its value in upper case, e.g. "#F4EEE3".
 */
fun colorResource(folder: String, name: String): String {
    val text = resFile(path = "res/$folder/colors.xml").readText()
    val match = Regex("<color name=\"$name\">(#[0-9A-Fa-f]{6})</color>").find(text)
    return checkNotNull(match) { "no colour $name in $folder" }.groupValues[1].uppercase()
}

/**
 * A colour as a resource writes it.
 *
 * @param color an opaque colour.
 * @return e.g. "#F4EEE3".
 */
fun hexOf(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)
