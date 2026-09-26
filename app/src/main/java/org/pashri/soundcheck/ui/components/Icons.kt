package org.pashri.soundcheck.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Line icons drawn to match the Manuscript mockups; tint them with `Icon(tint = …)`. */
object ManuscriptIcons {
    /** A tuning fork. */
    val Tuner: ImageVector = strokeIcon("Tuner", "M8 3v7a4 4 0 0 0 8 0V3", "M12 14v7")

    /** A metronome with its arm swung right. */
    val Metronome: ImageVector = strokeIcon("Metronome", "M7 21h10L14 3h-4z", "M12 16l5-8")

    /** A voice waveform. */
    val WarmUp: ImageVector = strokeIcon("WarmUp", "M4 10v4M8 7v10M12 4v16M16 7v10M20 10v4")

    /** A filled play triangle. */
    val Play: ImageVector = fillIcon("Play", "M7 4l13 8-13 8z")

    /** A filled stop square. */
    val Stop: ImageVector = fillIcon("Stop", "M6 6h12v12H6z")

    /** Two filled bars: pause. */
    val Pause: ImageVector = fillIcon("Pause", "M6 5h4v14H6zM14 5h4v14h-4z")

    /** A left-pointing triangle against a bar: previous. */
    val Previous: ImageVector = fillIcon("Previous", "M18 6l-9 6 9 6zM5 6h2v12H5z")

    /** A right-pointing triangle against a bar: next. */
    val Next: ImageVector = fillIcon("Next", "M6 6l9 6-9 6zM17 6h2v12h-2z")

    /** Two sliders: settings. */
    val Settings: ImageVector = strokeIcon(
        "Settings",
        "M4 7h10M18 7h2M4 17h4M12 17h8",
        "M14 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M8 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )

    /** A chevron pointing right: opens something. */
    val ChevronRight: ImageVector = strokeIcon("ChevronRight", "M9 5l7 7-7 7")

    /** A chevron pointing left: back. */
    val ChevronLeft: ImageVector = strokeIcon("ChevronLeft", "M15 5l-7 7 7 7")

    /** A pencil: rename. */
    val Edit: ImageVector = strokeIcon("Edit", "M4 20h4L19 9l-4-4L4 16z", "M13 7l4 4")
}

private fun strokeIcon(name: String, vararg paths: String): ImageVector =
    iconBuilder(name).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun fillIcon(name: String, path: String): ImageVector =
    iconBuilder(name).apply {
        addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    }.build()

private fun iconBuilder(name: String): ImageVector.Builder = ImageVector.Builder(
    name = name,
    defaultWidth = 22.dp,
    defaultHeight = 22.dp,
    viewportWidth = VIEWPORT,
    viewportHeight = VIEWPORT,
)

private const val VIEWPORT = 24f
private const val STROKE_WIDTH = 1.6f
