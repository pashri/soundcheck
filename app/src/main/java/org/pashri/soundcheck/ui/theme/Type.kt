package org.pashri.soundcheck.ui.theme

import androidx.compose.material3.Typography
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
    Font(
        resId = R.font.instrument_serif_regular,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
    ),
    Font(
        resId = R.font.instrument_serif_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
    ),
)

/** IBM Plex Sans: body text and buttons. */
val SansFamily: FontFamily = FontFamily(
    Font(resId = R.font.ibm_plex_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.ibm_plex_sans_semibold, weight = FontWeight.SemiBold),
)

/** IBM Plex Mono: small uppercase labels. */
val MonoFamily: FontFamily = FontFamily(
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_mono_medium, weight = FontWeight.Medium),
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

/**
 * Material's type scale in the Manuscript faces, for the text Material's own components
 * draw: display and headline styles (a dialog's title) in Instrument Serif, everything else
 * (a dialog's text, a menu item, a button) in IBM Plex Sans. Sizes stay Material's.
 */
val ManuscriptTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = SerifFamily),
        displayMedium = base.displayMedium.copy(fontFamily = SerifFamily),
        displaySmall = base.displaySmall.copy(fontFamily = SerifFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = SerifFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = SerifFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = SerifFamily),
        titleLarge = base.titleLarge.copy(fontFamily = SansFamily),
        titleMedium = base.titleMedium.copy(fontFamily = SansFamily),
        titleSmall = base.titleSmall.copy(fontFamily = SansFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = SansFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = SansFamily),
        bodySmall = base.bodySmall.copy(fontFamily = SansFamily),
        labelLarge = base.labelLarge.copy(fontFamily = SansFamily),
        labelMedium = base.labelMedium.copy(fontFamily = SansFamily),
        labelSmall = base.labelSmall.copy(fontFamily = SansFamily),
    )
}
