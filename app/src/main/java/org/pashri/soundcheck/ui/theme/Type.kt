package org.pashri.soundcheck.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.pashri.soundcheck.R

/** Instrument Serif: titles, note names and big numbers. */
val SerifFamily: FontFamily = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** IBM Plex Sans: body text and buttons. */
val SansFamily: FontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

/** IBM Plex Mono: small uppercase labels. */
val MonoFamily: FontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/** Text styles of the Manuscript design, named by role. */
object ManuscriptType {
    /** Screen titles such as "Metronome". */
    val screenTitle: TextStyle = TextStyle(fontFamily = SerifFamily, fontSize = 36.sp)

    /** The giant BPM number; callers size it in dp so it ignores the font scale. */
    val displayNumber: TextStyle = TextStyle(fontFamily = SerifFamily, lineHeight = 0.9.em)

    /** Italic display text such as the tempo marking "Andante". */
    val displayItalic: TextStyle = TextStyle(
        fontFamily = SerifFamily,
        fontStyle = FontStyle.Italic,
        fontSize = 34.sp,
    )

    /** Numerals on choice chips. */
    val chip: TextStyle = TextStyle(fontFamily = SerifFamily, fontSize = 24.sp)

    /** Body text. */
    val body: TextStyle = TextStyle(fontFamily = SansFamily, fontSize = 15.sp, lineHeight = 22.sp)

    /** Button labels. */
    val button: TextStyle = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    )

    /** Small uppercase monospace labels such as "BEATS PER MINUTE". */
    val label: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 12.sp,
        letterSpacing = 0.1.em,
    )

    /** Bottom navigation labels. */
    val navLabel: TextStyle = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    )
}
