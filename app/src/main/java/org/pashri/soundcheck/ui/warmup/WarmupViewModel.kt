package org.pashri.soundcheck.ui.warmup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.WarmupController

/**
 * Runs the playing screen. It only shows and steers the [WarmupController]; the Programme
 * keeps playing when the screen goes away.
 *
 * @param controller plays Programmes.
 * @param library the saved library, for the Sounds' current labels.
 */
class WarmupViewModel(
    private val controller: WarmupController,
    library: StateFlow<Library?>,
) : ViewModel(), WarmupActions {
    /**
     * Everything but the moving note, rebuilt only when the playback or the library changes,
     * not on every note.
     */
    private val base: Flow<WarmupUiState?> = combine(
        flow = controller.playback,
        flow2 = library,
    ) { now, saved ->
        playingState(playback = now, library = saved)
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
        )?.withNote(controller.note.value),
    )

    override fun playPause() {
        controller.toggle()
    }

    override fun next() {
        controller.next()
    }

    override fun previous() {
        controller.previous()
    }

    override fun stop() {
        controller.stop()
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

private fun playingState(playback: Playback?, library: Library?): WarmupUiState? =
    playback?.let {
        warmupUiState(
            playback = it,
            programme = it.programme,
            range = it.range,
            sounds = library?.sounds.orEmpty(),
        )
    }
