package org.pashri.soundcheck.di

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import org.pashri.soundcheck.audio.AndroidAudioFocus
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.NativeAudioEngine
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.ui.metronome.MetronomeViewModel

/**
 * Manually constructed dependencies.
 *
 * A DI framework would add build time and indirection without buying anything at this size.
 *
 * @param context the application context.
 */
class AppContainer(context: Context) {
    /** The one audio output every tool plays through. */
    val soundOutput: SoundOutput by lazy { NativeAudioEngine() }

    /** Audio focus for tools that make sound. */
    val audioFocus: FocusGate = AndroidAudioFocus(context)

    /** Builds the Metronome screen's view model. */
    val metronomeViewModelFactory: ViewModelProvider.Factory by lazy {
        MetronomeViewModel.Factory(soundOutput, audioFocus, SystemClock::elapsedRealtime)
    }
}
