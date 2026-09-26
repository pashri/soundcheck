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
 * Whether the Warm-up's media session should take the headphone button. With "Play over
 * other audio" on, the button stays with the other app.
 *
 * @param settings the saved settings, or null before they have loaded.
 * @return false only while playing over other audio.
 */
fun takesHeadphoneButton(settings: WarmupSettings?): Boolean =
    settings?.playOverOtherAudio != true
