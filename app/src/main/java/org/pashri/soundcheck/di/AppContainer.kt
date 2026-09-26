package org.pashri.soundcheck.di

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.AndroidAudioFocus
import org.pashri.soundcheck.audio.AndroidMic
import org.pashri.soundcheck.audio.AndroidSpeech
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.MixingFocusGate
import org.pashri.soundcheck.audio.NativeAudioEngine
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.SpeechSynth
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.data.DocumentStore
import org.pashri.soundcheck.data.LibraryCodec
import org.pashri.soundcheck.data.SettingsCodec
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.piano.AssetPianoSource
import org.pashri.soundcheck.piano.Piano
import org.pashri.soundcheck.playback.PlaybackService
import org.pashri.soundcheck.ui.metronome.MetronomeViewModel
import org.pashri.soundcheck.ui.pattern.PatternEditorViewModel
import org.pashri.soundcheck.ui.pattern.PatternListViewModel
import org.pashri.soundcheck.ui.programme.ProgrammeEditorViewModel
import org.pashri.soundcheck.ui.settings.SettingsViewModel
import org.pashri.soundcheck.ui.step.StepEditorViewModel
import org.pashri.soundcheck.ui.tuner.TunerViewModel
import org.pashri.soundcheck.ui.warmup.WarmupHomeViewModel
import org.pashri.soundcheck.ui.warmup.WarmupViewModel
import org.pashri.soundcheck.warmup.Audition
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.ProgrammePlayer
import org.pashri.soundcheck.warmup.SpokenAnnouncements
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.WarmupSettings

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

    /** Makes ids for new Programmes, Steps, Patterns and Sounds. */
    private val newId: () -> String = { UUID.randomUUID().toString() }

    /** The one audio output every tool plays through. */
    val soundOutput: SoundOutput by lazy { NativeAudioEngine() }

    /** Audio focus for the Metronome; not asked for while "Play over other audio" is on. */
    val audioFocus: FocusGate =
        MixingFocusGate(focus = AndroidAudioFocus(context), mixing = ::playsOverOtherAudio)

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

    /**
     * The Patterns, Sounds and Programmes, saved in the app's files. A fresh install gets the
     * starter kit.
     */
    val library: Store<Library> = DocumentStore(
        file = File(appContext.filesDir, LIBRARY_FILE),
        codec = LibraryCodec,
        seed = { StarterLibrary.LIBRARY },
        scope = appScope,
        io = Dispatchers.IO,
    ).also { it.load() }

    /** The Range, Voice Type and "Play over other audio", saved in the app's files. */
    val settings: Store<WarmupSettings> = DocumentStore(
        file = File(appContext.filesDir, SETTINGS_FILE),
        codec = SettingsCodec,
        seed = { WarmupSettings.DEFAULT },
        scope = appScope,
        io = Dispatchers.IO,
    ).also { it.load() }

    /** The sampled grand piano, loaded into [soundOutput] when a Programme first plays. */
    private val piano: Piano by lazy {
        Piano(source = AssetPianoSource(appContext.assets), output = soundOutput)
    }

    /** The phone's voice, for Announcements. */
    private val speech: SpeechSynth by lazy { AndroidSpeech(appContext) }

    /**
     * The Warm-up's own audio focus, held while a Programme is playing or paused; not asked
     * for while "Play over other audio" is on.
     */
    private val warmupFocus: FocusGate =
        MixingFocusGate(focus = AndroidAudioFocus(context), mixing = ::playsOverOtherAudio)

    /** Plays Programmes; it belongs to the app, not to the Warm-up screen. */
    val warmup: WarmupController by lazy {
        val announcements = SpokenAnnouncements(
            output = soundOutput,
            speech = speech,
            sounds = StarterSounds.ALL,
        )
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
        ).also(::keepServiceWhilePlaying)
    }

    /** Builds the playing screen's view model. */
    val warmupViewModelFactory: ViewModelProvider.Factory by lazy {
        WarmupViewModel.Factory(controller = warmup, library = library.data)
    }

    /** Builds the Warm-up home's view model. */
    val warmupHomeViewModelFactory: ViewModelProvider.Factory by lazy {
        WarmupHomeViewModel.Factory(
            controller = warmup,
            library = library,
            settings = settings,
            newId = newId,
        )
    }

    /** Builds the Settings screen's view model. */
    val settingsViewModelFactory: ViewModelProvider.Factory by lazy {
        SettingsViewModel.Factory(settings = settings)
    }

    /**
     * Builds a Programme editor's view model.
     *
     * @param id the Programme.
     * @return the factory.
     */
    fun programmeEditorFactory(id: ProgrammeId): ViewModelProvider.Factory =
        ProgrammeEditorViewModel.Factory(
            programmeId = id,
            controller = warmup,
            library = library,
            settings = settings,
            newId = newId,
        )

    /**
     * Builds a Step editor's view model.
     *
     * @param ref the Step.
     * @return the factory.
     */
    fun stepEditorFactory(ref: StepRef): ViewModelProvider.Factory =
        StepEditorViewModel.Factory(
            ref = ref,
            library = library,
            settings = settings,
            audition = audition,
        )

    /**
     * Builds the Patterns list's view model.
     *
     * @param pickFor the Step to choose a Pattern for, or null to browse.
     * @return the factory.
     */
    fun patternListFactory(pickFor: StepRef?): ViewModelProvider.Factory =
        PatternListViewModel.Factory(pickFor = pickFor, library = library, newId = newId)

    /**
     * Builds a Pattern editor's view model.
     *
     * @param id the Pattern.
     * @return the factory.
     */
    fun patternEditorFactory(id: PatternId): ViewModelProvider.Factory =
        PatternEditorViewModel.Factory(
            patternId = id,
            library = library,
            settings = settings,
            audition = audition,
        )

    /**
     * The editors' audition's own audio focus: a short transient request while a Demo or
     * Pattern sounds, not asked for while "Play over other audio" is on.
     */
    private val auditionFocus: FocusGate =
        MixingFocusGate(focus = AndroidAudioFocus(context), mixing = ::playsOverOtherAudio)

    /** Plays a Step's Demo or a Pattern from the editors; it belongs to the app. */
    val audition: Audition by lazy {
        Audition(
            output = soundOutput,
            piano = piano,
            focus = auditionFocus,
            arbiter = toolArbiter,
            scope = appScope,
        )
    }

    /** Whether "Play over other audio" is on; read each time a tool asks for focus. */
    private fun playsOverOtherAudio(): Boolean = settings.data.value?.playOverOtherAudio == true

    /**
     * Starts the playback service whenever a Programme is loaded; the service stops itself
     * when it is unloaded. A Programme is only ever loaded by a tap on a Warm-up screen, so
     * the app is in the foreground and may start a foreground service.
     */
    private fun keepServiceWhilePlaying(controller: WarmupController) {
        appScope.launch {
            controller.playback
                .map { it != null }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    val intent = Intent(appContext, PlaybackService::class.java)
                    ContextCompat.startForegroundService(appContext, intent)
                }
        }
    }

    private companion object {
        const val LIBRARY_FILE = "library.json"
        const val SETTINGS_FILE = "settings.json"
    }
}
