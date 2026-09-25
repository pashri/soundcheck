package org.pashri.soundcheck.ui.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.Tuner

/**
 * Runs the Tuner screen: listens while the screen is shown and the microphone is allowed,
 * holding audio focus meanwhile so a podcast pauses and resumes afterwards. Listening takes
 * the one sound-or-listening slot, so it pauses a Warm-up; a Warm-up starting makes the
 * Tuner give way, and it does not reclaim the slot on its own — only [retry] asks to listen
 * again, so returning to a screen the Warm-up already took over never pauses it back.
 *
 * @param mic the microphone.
 * @param focus audio focus, held while listening.
 * @param worker where reading and pitch detection run, off the main thread.
 * @param arbiter keeps one tool sounding or listening at a time.
 */
class TunerViewModel(
    mic: MicInput,
    private val focus: FocusGate,
    worker: CoroutineDispatcher,
    private val arbiter: ToolArbiter,
) : ViewModel() {
    private val tuner = Tuner(mic, viewModelScope, worker)
    private val access = MutableStateFlow(MicAccess.Unknown)
    private val yielded = MutableStateFlow(false)
    private var shown = false
    private var askedOnOpen = false
    private val wantsToListen = MutableStateFlow(false)

    /** Everything the screen shows. */
    val uiState: StateFlow<TunerUiState> =
        combine(access, tuner.state, yielded) { access, heard, gaveWay ->
            TunerUiState(access = access, mic = heard.mic, note = heard.note, yielded = gaveWay)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TunerUiState())

    init {
        viewModelScope.launch { holdFocusWhileListening() }
    }

    /**
     * The screen became visible; listens if the microphone is allowed and the Tuner has not
     * given way to a Warm-up. While it has, this only rechecks the permission, so ON_START
     * firing again on screen off/on or rotation never pauses a Programme the user resumed.
     *
     * @param granted whether Soundcheck holds the microphone permission right now.
     */
    fun onShown(granted: Boolean) {
        shown = true
        if (granted) {
            access.value = MicAccess.Granted
        } else if (access.value == MicAccess.Granted) {
            access.value = MicAccess.Unknown
        }
        if (!yielded.value) listenIfAllowed()
    }

    /**
     * Android's permission dialog answered.
     *
     * @param granted whether the user allowed the microphone.
     * @param canAskAgain whether Android would show the dialog again.
     */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        access.value = micAccessAfterRequest(access.value, granted, canAskAgain)
        listenIfAllowed()
    }

    /**
     * Whether to show Android's permission dialog without waiting for a tap: true once,
     * the first time the screen is shown without the permission.
     *
     * @return true if the caller should show the dialog now.
     */
    fun shouldAskOnOpen(): Boolean {
        if (askedOnOpen || access.value != MicAccess.Unknown) return false
        askedOnOpen = true
        return true
    }

    /** Tries the microphone again after it was unavailable, or after giving way. */
    fun retry() {
        yielded.value = false
        listenIfAllowed()
    }

    /** The screen was hidden: stops listening and releases the microphone and focus. */
    fun stop() {
        shown = false
        stopListening()
    }

    override fun onCleared() {
        stopListening()
        // viewModelScope is cancelled before onCleared runs, so the focus collector is gone.
        focus.release()
    }

    private fun listenIfAllowed() {
        if (shown && access.value == MicAccess.Granted) startListening() else stopListening()
    }

    private fun startListening() {
        yielded.value = false
        arbiter.claim(Tool.TUNER, onEvicted = ::giveWay)
        wantsToListen.value = true
        tuner.start()
    }

    private fun stopListening() {
        wantsToListen.value = false
        tuner.stop()
        arbiter.release(Tool.TUNER)
    }

    private fun giveWay() {
        yielded.value = true
        stopListening()
    }

    /**
     * Holds audio focus exactly while the screen wants to listen and the microphone is
     * actually listening, so a microphone that won't open never pauses a podcast. A refused
     * request only means other audio keeps playing; the Tuner still tunes.
     */
    private suspend fun holdFocusWhileListening() {
        combine(wantsToListen, tuner.state) { wants, heard ->
            wants && heard.mic == MicStatus.Listening
        }.distinctUntilChanged().collect { listening ->
            if (listening) focus.acquire(onLost = ::onFocusLost) else focus.release()
        }
    }

    /**
     * Another app took audio focus (a call, a video). The Tuner keeps listening: the mic
     * still works, and stopping would leave the user facing a dead screen mid-tune.
     */
    private fun onFocusLost() = Unit

    /**
     * Builds [TunerViewModel]s.
     *
     * @param mic the microphone.
     * @param focus audio focus, held while listening.
     * @param worker where reading and pitch detection run.
     * @param arbiter keeps one tool sounding or listening at a time.
     */
    class Factory(
        private val mic: MicInput,
        private val focus: FocusGate,
        private val worker: CoroutineDispatcher,
        private val arbiter: ToolArbiter,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TunerViewModel(mic, focus, worker = worker, arbiter = arbiter) as T
    }
}
