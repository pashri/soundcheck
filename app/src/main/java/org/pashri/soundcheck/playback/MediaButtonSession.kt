package org.pashri.soundcheck.playback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.WarmupSettings

/**
 * Soundcheck's one media session, owned by the app rather than by the playback service, so
 * the headphone button can reach the Metronome as well as the Warm-up. It exists while
 * [HeadphoneButton.needed] (a Programme is loaded or the Metronome's screen shows) and the
 * loaded settings have "Play over other audio" off: Android 12 and later give the button to
 * the app that last played, even to an inactive session, so while mixing there is no
 * session at all. The playback service puts [session] on the lock-screen card while a
 * Programme is loaded; with none loaded the session shows nothing playing (see
 * [resetsSession]). Use from the main thread.
 *
 * @param context any context; only the application context is kept.
 * @param button where presses go.
 * @param warmup the Warm-up, for the lock screen's play, pause, next, previous and stop.
 * @param settings the saved settings.
 * @param scope follows the settings, the Warm-up and [HeadphoneButton.needed].
 * @property button where presses go; the Metronome offers itself to it.
 */
class MediaButtonSession(
    context: Context,
    val button: HeadphoneButton,
    private val warmup: WarmupController,
    private val settings: StateFlow<WarmupSettings?>,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val _session = MutableStateFlow<MediaSessionCompat?>(null)
    private var started = false

    /**
     * The session, or null while nothing needs the button, the settings haven't loaded or
     * Soundcheck mixes.
     */
    val session: StateFlow<MediaSessionCompat?> = _session.asStateFlow()

    /**
     * Starts following the settings, the Warm-up and [HeadphoneButton.needed]; later calls do
     * nothing.
     */
    fun start() {
        if (started) return
        started = true
        val loaded = warmup.playback.map { it != null }.distinctUntilChanged()
        scope.launch {
            combine(
                flow = button.needed,
                flow2 = settings,
                flow3 = loaded,
            ) { needed, saved, programmeLoaded ->
                Triple(first = needed, second = saved, third = programmeLoaded)
            }.collect { (needed, saved, programmeLoaded) ->
                sync(needed = needed, settings = saved, programmeLoaded = programmeLoaded)
            }
        }
    }

    private fun sync(needed: Boolean, settings: WarmupSettings?, programmeLoaded: Boolean) {
        val change = sessionChange(
            hasSession = _session.value != null,
            settings = settings,
            needed = needed,
        )
        when (change) {
            SessionChange.CREATE -> _session.value = createSession()
            SessionChange.RELEASE -> releaseSession()
            SessionChange.KEEP -> Unit
        }
        val session = _session.value
        if (resetsSession(hasSession = session != null, programmeLoaded = programmeLoaded)) {
            session?.let(PlaybackNotifications::clearSession)
        }
    }

    private fun createSession(): MediaSessionCompat =
        MediaSessionCompat(appContext, SESSION_TAG).apply {
            setCallback(SessionCallback())
            setMediaButtonReceiver(null)
            isActive = true
        }

    private fun releaseSession() {
        _session.value?.let {
            it.isActive = false
            it.release()
        }
        _session.value = null
    }

    /** Headphone, car and lock-screen controls, delivered on the main thread. */
    private inner class SessionCallback : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val event = IntentCompat.getParcelableExtra(
                mediaButtonEvent,
                Intent.EXTRA_KEY_EVENT,
                KeyEvent::class.java,
            ) ?: return false
            val action = mediaKeyAction(
                keyCode = event.keyCode,
                action = event.action,
                repeatCount = event.repeatCount,
            )
            when (action) {
                MediaKeyAction.PRESS -> button.press()
                MediaKeyAction.NEXT -> button.next()
                MediaKeyAction.PREVIOUS -> button.previous()
                MediaKeyAction.CONSUME -> Unit
                MediaKeyAction.IGNORE -> return super.onMediaButtonEvent(mediaButtonEvent)
            }
            return true
        }

        override fun onPlay() {
            warmup.resume()
        }

        override fun onPause() {
            warmup.pause()
        }

        override fun onSkipToNext() {
            warmup.next()
        }

        override fun onSkipToPrevious() {
            warmup.previous()
        }

        override fun onStop() {
            warmup.stop()
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            if (action == PlaybackService.ACTION_STOP) warmup.stop()
        }
    }

    private companion object {
        const val SESSION_TAG = "Soundcheck"
    }
}
