package org.pashri.soundcheck.playback

import android.view.KeyEvent
import org.pashri.soundcheck.warmup.WarmupSettings

/** What a media key event means for a playing Programme. */
enum class MediaKeyAction {
    /** One press of the play/pause button, to be counted with its neighbours. */
    PRESS,

    /** Go to the next Step now. */
    NEXT,

    /** Go to the previous Step now. */
    PREVIOUS,

    /** A key the Warm-up owns, but this event (a release or a repeat) does nothing. */
    CONSUME,

    /** Not a key the Warm-up handles; leave it to the system. */
    IGNORE,
}

/**
 * Classifies a media key event. Every play/pause-type key counts as a press, because a
 * Bluetooth headset may send PLAY and PAUSE alternately for the same button.
 *
 * @param keyCode the event's `KeyEvent.KEYCODE_*`.
 * @param action `KeyEvent.ACTION_DOWN` or `ACTION_UP`.
 * @param repeatCount 0 for the first event of a press, more while it is held.
 * @return what to do with it.
 */
fun mediaKeyAction(keyCode: Int, action: Int, repeatCount: Int): MediaKeyAction {
    val role = when (keyCode) {
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        -> MediaKeyAction.PRESS
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> MediaKeyAction.NEXT
        KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD ->
            MediaKeyAction.PREVIOUS
        else -> return MediaKeyAction.IGNORE
    }
    return if (action == KeyEvent.ACTION_DOWN && repeatCount == 0) role else MediaKeyAction.CONSUME
}

/**
 * Whether Soundcheck's media session should take the headphone button. With "Play over
 * other audio" on, the button stays with the other app.
 *
 * @param settings the saved settings, or null before they have loaded.
 * @return false only while playing over other audio.
 */
fun takesHeadphoneButton(settings: WarmupSettings?): Boolean =
    settings?.playOverOtherAudio != true

/** What happens to the app's one media session when the settings or its users change. */
enum class SessionChange {
    /** Make a session, so the headphone button reaches the Warm-up or the Metronome. */
    CREATE,

    /** Release the session: nothing needs it, or the button stays with the other app. */
    RELEASE,

    /** Leave things as they are. */
    KEEP,
}

/**
 * Whether to create or release the media session. While playing over other audio Soundcheck
 * has no session at all: Android 12 and later route the headphone button to the app that
 * last played audio, even to an inactive session. Nor is there one before the settings have
 * loaded, since they may yet say to mix; a Programme can't load before them.
 *
 * @param hasSession whether the session exists now.
 * @param settings the saved settings, or null before they have loaded.
 * @param needed whether anything can take presses (a Programme is loaded or the
 *     Metronome's screen shows or it plays); with nothing, there is no session either.
 * @return the change that makes the session match the loaded settings,
 *     [takesHeadphoneButton] and [needed].
 */
fun sessionChange(
    hasSession: Boolean,
    settings: WarmupSettings?,
    needed: Boolean = true,
): SessionChange {
    val wanted = settings != null && takesHeadphoneButton(settings) && needed
    return when {
        wanted && !hasSession -> SessionChange.CREATE
        !wanted && hasSession -> SessionChange.RELEASE
        else -> SessionChange.KEEP
    }
}

/**
 * Whether the media session must be reset to show nothing playing: no title, and paused with
 * only play/pause. The session outlives a Programme while the Metronome takes presses, and
 * must not keep a finished Programme's title or its "playing" state, which on Android 11
 * would draw other apps' button presses to it.
 *
 * @param hasSession whether the session exists.
 * @param programmeLoaded whether a Programme is playing or paused; while one is, the
 *     playback service keeps the session showing it.
 * @return true when the session should be reset now.
 */
fun resetsSession(hasSession: Boolean, programmeLoaded: Boolean): Boolean =
    hasSession && !programmeLoaded
