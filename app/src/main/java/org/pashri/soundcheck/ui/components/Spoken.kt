package org.pashri.soundcheck.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Music text as TalkBack should say it. The fonts' ♭, ♯ and ♮ glyphs, the minus sign and the
 * arrows aren't always spoken, so each becomes a word.
 *
 * @param text shown text, e.g. "B♭2", "top −3" or "Start low ↑".
 * @return e.g. "B flat 2", "top minus 3" or "Start low up".
 */
fun spokenMusic(text: String): String = SPOKEN_SYMBOLS.entries
    .fold(text) { spoken, (symbol, word) -> spoken.replace(symbol, " $word ") }
    .replace(SPACES, " ")
    .trim()

/**
 * What TalkBack says for a merged row such as "Lowest B♭2": its parts in order, each with
 * its music read as words, so a label beside a value isn't dropped.
 *
 * @param parts the row's texts, e.g. its label, value and detail; null parts are left out.
 * @return e.g. "Lowest, B flat 2".
 */
fun spokenRow(parts: List<String?>): String =
    parts.filterNotNull().joinToString(separator = ", ") { spokenMusic(text = it) }

/**
 * Text that may hold ♭, ♯, ♮, − or arrows, which TalkBack reads as words.
 *
 * @param text what to show.
 * @param style the text style.
 * @param color the text colour.
 * @param modifier modifier for the text.
 * @param textAlign alignment within the text's box, or null for the default.
 */
@Composable
fun MusicText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign,
        modifier = modifier.semantics { contentDescription = spokenMusic(text) },
    )
}

private val SPOKEN_SYMBOLS = mapOf(
    "♭" to "flat",
    "♯" to "sharp",
    "♮" to "natural",
    "−" to "minus",
    "↑" to "up",
    "↓" to "down",
)

private val SPACES = Regex("\\s+")
