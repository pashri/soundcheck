package org.pashri.soundcheck.ui.warmup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.WarmupController

/**
 * Runs the playing screen. It only shows and steers the [WarmupController]; the Programme
 * keeps playing when the screen goes away. When Resume, Next or Previous leaves the
 * Programme paused and the sound output didn't fail, another app holds the sound, and the
 * screen says so until the Programme plays.
 *
 * @param controller plays Programmes.
 * @param library the saved library, for the Sounds' current labels.
 */
class WarmupViewModel(
    private val controller: WarmupController,
    library: StateFlow<Library?>,
) : ViewModel(), WarmupActions {
    private val busy = MutableStateFlow(false)

    /**
     * Everything but the moving note, rebuilt only when the playback, the library or [busy]
     * changes, not on every note.
     */
    private val base: Flow<WarmupUiState?> = combine(
        flow = controller.playback,
        flow2 = library,
        flow3 = busy,
    ) { now, saved, refused ->
        playingState(playback = now, library = saved, busy = refused)
    }

    /**
     * Everything the screen shows, or null when no Programme is loaded. A note change only
     * relights the staff: every other part keeps its instance, so it isn't redrawn.
     */
    val uiState: StateFlow<WarmupUiState?> = combine(
        flow = base,
        flow2 = controller.note,
    ) { state, note ->
        state?.withNote(note)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = playingState(
            playback = controller.playback.value,
            library = library.value,
            busy = false,
        )?.withNote(controller.note.value),
    )

    init {
        viewModelScope.launch {
            controller.playback.collect { if (it?.playing != false) busy.value = false }
        }
    }

    override fun playPause() {
        if (controller.playback.value?.playing == true) {
            controller.pause()
        } else {
            tryToPlay(controller::resume)
        }
    }

    override fun next() {
        tryToPlay(controller::next)
    }

    override fun previous() {
        tryToPlay(controller::previous)
    }

    override fun stop() {
        controller.stop()
    }

    /** Runs [action], then notes whether another app kept the Programme from playing. */
    private fun tryToPlay(action: () -> Unit) {
        action()
        val now = controller.playback.value
        busy.value = now != null && !now.playing && !now.outputFailed
    }

    /**
     * Builds [WarmupViewModel]s.
     *
     * @param controller plays Programmes.
     * @param library the saved library.
     */
    class Factory(
        private val controller: WarmupController,
        private val library: StateFlow<Library?>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WarmupViewModel(controller = controller, library = library) as T
    }
}

private fun playingState(playback: Playback?, library: Library?, busy: Boolean): WarmupUiState? =
    playback?.let {
        warmupUiState(
            playback = it,
            programme = it.programme,
            range = it.range,
            sounds = library?.sounds.orEmpty(),
            busy = busy,
        )
    }
