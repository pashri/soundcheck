package org.pashri.soundcheck.ui.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.Tuner

/**
 * Runs the Tuner screen: listens while the screen is shown and the microphone is allowed,
 * holding audio focus meanwhile so a podcast pauses and resumes afterwards.
 *
 * @param mic the microphone.
 * @param focus audio focus, held while listening.
 * @param worker where reading and pitch detection run, off the main thread.
 */
class TunerViewModel(
    mic: MicInput,
    private val focus: FocusGate,
    worker: CoroutineDispatcher,
) : ViewModel() {
    private val tuner = Tuner(mic, viewModelScope, worker)
    private val access = MutableStateFlow(MicAccess.Unknown)
    private var shown = false
    private var askedOnOpen = false
    private var holdingFocus = false

    /** Everything the screen shows. */
    val uiState: StateFlow<TunerUiState> =
        combine(access, tuner.state) { access, heard ->
            TunerUiState(access = access, mic = heard.mic, note = heard.note)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TunerUiState())

    init {
        viewModelScope.launch {
            tuner.state.collect { if (it.mic == MicStatus.Unavailable) releaseFocus() }
        }
    }

    /**
     * The screen became visible; listens if the microphone is allowed.
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
        listenIfAllowed()
    }

    /**
     * Android's permission dialog answered.
     *
     * @param granted whether the user allowed the microphone.
     * @param canAskAgain whether Android would show the dialog again.
     */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        access.value = micAccessAfterRequest(granted, canAskAgain)
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

    /** Tries the microphone again after it was unavailable. */
    fun retry() {
        listenIfAllowed()
    }

    /** The screen was hidden: stops listening and releases the microphone and focus. */
    fun stop() {
        shown = false
        stopListening()
    }

    override fun onCleared() {
        stopListening()
    }

    private fun listenIfAllowed() {
        if (shown && access.value == MicAccess.Granted) startListening() else stopListening()
    }

    private fun startListening() {
        if (!holdingFocus) {
            holdingFocus = true
            // A refusal only means other audio keeps playing; the Tuner still tunes.
            focus.acquire(onLost = ::onFocusLost)
        }
        tuner.start()
    }

    private fun stopListening() {
        tuner.stop()
        releaseFocus()
    }

    private fun releaseFocus() {
        holdingFocus = false
        focus.release()
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
     */
    class Factory(
        private val mic: MicInput,
        private val focus: FocusGate,
        private val worker: CoroutineDispatcher,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TunerViewModel(mic, focus, worker) as T
    }
}
