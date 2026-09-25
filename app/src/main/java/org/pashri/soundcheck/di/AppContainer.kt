package org.pashri.soundcheck.di

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.pashri.soundcheck.audio.AndroidAudioFocus
import org.pashri.soundcheck.audio.AndroidMic
import org.pashri.soundcheck.audio.AndroidSpeech
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.NativeAudioEngine
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.SpeechSynth
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.piano.AssetPianoSource
import org.pashri.soundcheck.piano.Piano
import org.pashri.soundcheck.ui.metronome.MetronomeViewModel
import org.pashri.soundcheck.ui.tuner.TunerViewModel
import org.pashri.soundcheck.ui.warmup.WarmupViewModel
import org.pashri.soundcheck.warmup.ProgrammePlayer
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SpokenAnnouncements
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController

/**
 * Manually constructed dependencies.
 *
 * A DI framework would add build time and indirection without buying anything at this size.
 *
 * @param context the application context.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /** Work that outlives every screen, such as a Programme playing with the screen off. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The one audio output every tool plays through. */
    val soundOutput: SoundOutput by lazy { NativeAudioEngine() }

    /** Audio focus for tools that make sound. */
    val audioFocus: FocusGate = AndroidAudioFocus(context)

    /** Keeps one tool making sound or listening at a time. */
    val toolArbiter: ToolArbiter = ToolArbiter()

    /** Builds the Metronome screen's view model. */
    val metronomeViewModelFactory: ViewModelProvider.Factory by lazy {
        MetronomeViewModel.Factory(
            output = soundOutput,
            focus = audioFocus,
            clockMs = SystemClock::elapsedRealtime,
            arbiter = toolArbiter,
        )
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
        TunerViewModel.Factory(
            mic = micInput,
            focus = tunerFocus,
            worker = Dispatchers.Default,
            arbiter = toolArbiter,
        )
    }

    /** The Sound library: the starter Sounds until the library screens arrive. */
    val sounds: List<Sound> = StarterSounds.ALL

    /** The Range Programmes play through: the Tenor preset until Settings arrive. */
    val warmupRange: Range = VoiceType.TENOR.range

    /** The sampled grand piano, loaded into [soundOutput] when a Programme first plays. */
    private val piano: Piano by lazy {
        Piano(source = AssetPianoSource(appContext.assets), output = soundOutput)
    }

    /** The phone's voice, for Announcements. */
    private val speech: SpeechSynth by lazy { AndroidSpeech(appContext) }

    /** The Warm-up's own audio focus, held while a Programme is playing or paused. */
    private val warmupFocus: FocusGate = AndroidAudioFocus(context)

    /** Plays Programmes; it belongs to the app, not to the Warm-up screen. */
    val warmup: WarmupController by lazy {
        val announcements =
            SpokenAnnouncements(output = soundOutput, speech = speech, sounds = sounds)
        val player = ProgrammePlayer(
            output = soundOutput,
            piano = piano,
            announcements = announcements,
            scope = appScope,
        )
        WarmupController(
            player = player,
            focus = warmupFocus,
            arbiter = toolArbiter,
            scope = appScope,
        )
    }

    /** Builds the Warm-up screen's view model. */
    val warmupViewModelFactory: ViewModelProvider.Factory by lazy {
        WarmupViewModel.Factory(
            controller = warmup,
            programme = StarterProgrammes.WARM_UP,
            range = warmupRange,
            sounds = sounds,
        )
    }
}
