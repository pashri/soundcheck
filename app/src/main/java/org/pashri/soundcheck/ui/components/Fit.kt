package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * How many columns [count] options get when none may be narrower than [widest]: the most
 * that fit in [available] with [spacing] between them, then spread evenly over the rows they
 * need, so four options too wide for one row become two rows of two, never three and one.
 *
 * @param count how many options there are.
 * @param widest the width the widest option needs, e.g. its longest word plus padding.
 * @param available the width of the whole row.
 * @param spacing the gap between two columns, in the same unit.
 * @return the number of columns, from 1 to [count].
 */
fun fittingColumns(count: Int, widest: Float, available: Float, spacing: Float = 0f): Int {
    val most = (count downTo 1).firstOrNull { columns ->
        (available - spacing * (columns - 1)) / columns >= widest
    } ?: 1
    val rows = (count + most - 1) / most
    return (count + rows - 1) / rows
}

/**
 * The words of [text], the pieces a line may break between.
 *
 * @param text a label such as "Tap tempo".
 * @return its words, without blanks.
 */
fun words(text: String): List<String> = text.split(' ').filter { it.isNotBlank() }

/**
 * The width in pixels of the longest word in [texts] set in [style] at the current font
 * size: a box at least this wide never has to break a word.
 *
 * @param texts the labels.
 * @param style how they are set.
 * @return the widest word's width in pixels.
 */
@Composable
fun rememberWidestWord(texts: List<String>, style: TextStyle): Int {
    val measurer = rememberTextMeasurer()
    return remember(key1 = texts, key2 = style, key3 = measurer) {
        texts.flatMap(::words).maxOfOrNull { word ->
            measurer.measure(text = word, style = style, softWrap = false).size.width
        } ?: 0
    }
}

/**
 * [Arrangement.SpaceBetween] that tells a flow row to keep at least [minimum] between two
 * items: a header's note sits at the far end, and moves under the title rather than touching
 * it.
 *
 * @property minimum the smallest gap allowed on one line.
 */
class SpaceBetweenAtLeast(private val minimum: Dp) : Arrangement.Horizontal {
    override val spacing: Dp = minimum

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        with(receiver = Arrangement.SpaceBetween) {
            arrange(
                totalSize = totalSize,
                sizes = sizes,
                layoutDirection = layoutDirection,
                outPositions = outPositions,
            )
        }
    }
}
