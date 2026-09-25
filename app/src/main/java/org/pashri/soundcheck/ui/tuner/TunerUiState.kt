package org.pashri.soundcheck.ui.tuner

import java.util.Locale
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.NoteReading
import org.pashri.soundcheck.tuner.TuningAdvice

/** Whether Soundcheck may use the microphone. */
enum class MicAccess {
    /** Not granted and not yet asked on this visit. */
    Unknown,

    /** Granted. */
    Granted,

    /** Refused, and Android will still show its dialog if asked again. */
    Denied,

    /** Refused for good: only the app's page in system Settings can grant it now. */
    Blocked,
}

/**
 * Where the answer to a microphone permission request leaves access.
 *
 * On Android 11+, dismissing the very first dialog (tapping outside it) also reports "can't
 * ask again", although the dialog would still show. So a first answer of that kind reads as
 * [MicAccess.Denied]; only a refusal after an earlier one reads as [MicAccess.Blocked].
 *
 * @param previous access before the request; [MicAccess.Unknown] if never answered.
 * @param granted whether the user allowed the microphone.
 * @param canAskAgain whether Android would show its dialog again, from
 *   `shouldShowRequestPermissionRationale`.
 * @return the new access.
 */
fun micAccessAfterRequest(
    previous: MicAccess,
    granted: Boolean,
    canAskAgain: Boolean,
): MicAccess = when {
    granted -> MicAccess.Granted
    canAskAgain || previous == MicAccess.Unknown -> MicAccess.Denied
    else -> MicAccess.Blocked
}

/** Which face the Tuner screen shows. */
enum class TunerMode {
    /** The note, needle and cents. */
    Listening,

    /** Explains the microphone and offers Android's permission dialog. */
    AskPermission,

    /** Explains that access is off and offers the app's system Settings page. */
    OpenSettings,

    /** The microphone couldn't be opened; offers to try again. */
    MicUnavailable,

    /** A Warm-up started, so the Tuner stopped listening; offers to listen instead. */
    Yielded,
}

/**
 * A message shown instead of the tuner, with one button.
 *
 * @property title the italic headline.
 * @property body one or two plain sentences under it.
 * @property button the button's label.
 */
data class TunerMessage(val title: String, val body: String, val button: String)

/** How far the needle swings either side of centre at ±50 cents, as in the mockup. */
const val NEEDLE_SWING_DEGREES: Float = 70f

/**
 * The needle's angle for an offset.
 *
 * @param cents the offset from the note; values beyond ±50 pin to the end of the scale.
 * @return degrees from upright, negative to the flat side.
 */
fun needleDegrees(cents: Double): Float =
    (cents.coerceIn(-MAX_CENTS, MAX_CENTS) / MAX_CENTS * NEEDLE_SWING_DEGREES).toFloat()

private const val MAX_CENTS = 50.0

/**
 * Everything the Tuner screen shows.
 *
 * @property access whether the microphone may be used.
 * @property mic what the microphone is doing.
 * @property note the note heard, or null when there is none to show.
 * @property yielded whether the Tuner gave way to a Warm-up that started playing.
 */
data class TunerUiState(
    val access: MicAccess = MicAccess.Unknown,
    val mic: MicStatus = MicStatus.Off,
    val note: NoteReading? = null,
    val yielded: Boolean = false,
) {
    /** Which face to show. */
    val mode: TunerMode
        get() = when {
            access == MicAccess.Blocked -> TunerMode.OpenSettings
            access != MicAccess.Granted -> TunerMode.AskPermission
            yielded -> TunerMode.Yielded
            mic == MicStatus.Unavailable -> TunerMode.MicUnavailable
            else -> TunerMode.Listening
        }

    /** The frequency under the note, e.g. "109.7 Hz", or "listening…" with no note. */
    val hzLabel: String
        get() = note?.let { String.format(Locale.ROOT, "%.1f Hz", it.hz) } ?: "listening…"

    /** The big italic line, e.g. "4 cents flat", or an invitation with no note. */
    val readingLabel: String
        get() = note?.let { TuningAdvice.reading(it.roundedCents) } ?: "Play or sing a note"

    /** The plain-language line, e.g. "a touch low, nearly there"; empty with no note. */
    val adviceLabel: String get() = note?.let { TuningAdvice.advice(it.roundedCents) } ?: ""

    /** The needle's angle, or null to rest it at centre, dimmed. */
    val needleDegrees: Float? get() = note?.let { needleDegrees(it.cents) }

    /** The message to show instead of the tuner, or null in [TunerMode.Listening]. */
    val message: TunerMessage?
        get() = when (mode) {
            TunerMode.Listening -> null
            TunerMode.AskPermission -> ASK_MESSAGE
            TunerMode.OpenSettings -> SETTINGS_MESSAGE
            TunerMode.MicUnavailable -> UNAVAILABLE_MESSAGE
            TunerMode.Yielded -> YIELDED_MESSAGE
        }

    private companion object {
        val ASK_MESSAGE = TunerMessage(
            title = "The Tuner needs the microphone",
            body = "It listens only while this tab is open. Nothing is recorded or kept.",
            button = "Allow microphone",
        )
        val SETTINGS_MESSAGE = TunerMessage(
            title = "The Tuner needs the microphone",
            body = "Microphone access is off for Soundcheck. Turn it on under Permissions.",
            button = "Open settings",
        )
        val UNAVAILABLE_MESSAGE = TunerMessage(
            title = "The microphone isn't available",
            body = "Another app may be using it.",
            button = "Try again",
        )
        val YIELDED_MESSAGE = TunerMessage(
            title = "The Warm-up is playing",
            body = "The Tuner stops listening while a Programme plays.",
            button = "Listen instead",
        )
    }
}

/** What the Tuner screen's button does, depending on [TunerUiState.mode]. */
interface TunerActions {
    /** Shows Android's microphone permission dialog. */
    fun allowMicrophone()

    /** Opens Soundcheck's page in system Settings. */
    fun openSettings()

    /** Tries to open the microphone again. */
    fun retry()
}
