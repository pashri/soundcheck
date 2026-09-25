package org.pashri.soundcheck.di

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import org.pashri.soundcheck.audio.AndroidAudioFocus
import org.pashri.soundcheck.audio.AndroidMic
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.NativeAudioEngine
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.ui.metronome.MetronomeViewModel
import org.pashri.soundcheck.ui.tuner.TunerViewModel

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

    /** The microphone, for the Tuner; it never goes through [soundOutput]. */
    val micInput: MicInput = AndroidMic()

    /**
     * The Tuner's own audio focus. Separate from [audioFocus] because the Metronome releases
     * its focus whenever its tab closes, which would otherwise drop the Tuner's as it opens.
     */
    val tunerFocus: FocusGate = AndroidAudioFocus(context)

    /** Builds the Tuner screen's view model; pitch detection runs on the default pool. */
    val tunerViewModelFactory: ViewModelProvider.Factory by lazy {
        TunerViewModel.Factory(micInput, tunerFocus, Dispatchers.Default)
    }
}
