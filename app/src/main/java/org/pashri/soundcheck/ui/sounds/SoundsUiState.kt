package org.pashri.soundcheck.ui.sounds

import org.pashri.soundcheck.ui.components.PHONE_VOICE
import org.pashri.soundcheck.ui.components.clipLengthLabel
import org.pashri.soundcheck.ui.components.spokenClipLength
import org.pashri.soundcheck.ui.components.usageLabel
import org.pashri.soundcheck.ui.components.usageText
import org.pashri.soundcheck.ui.tuner.MicAccess
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.soundUsage

/**
 * One Sound in the Sounds list.
 *
 * @property id the Sound.
 * @property label e.g. "lip trill".
 * @property detail how its Announcement sounds: its recording's length, e.g. "0.6 s", or
 *     "phone voice".
 * @property spokenDetail [detail] as TalkBack says it, e.g. "your recording, 0.6 seconds".
 * @property recorded whether it has a recording to play.
 * @property playing whether its recording is playing now.
 * @property usage e.g. "Used in 2 Steps".
 * @property deleteNote what deleting it takes with it.
 * @property chosen whether it is the Step's Sound, when choosing for a Step.
 * @property open whether its recorder is open under it.
 */
data class SoundRow(
    val id: SoundId,
    val label: String,
    val detail: String,
    val spokenDetail: String,
    val recorded: Boolean,
    val playing: Boolean,
    val usage: String,
    val deleteNote: String,
    val chosen: Boolean,
    val open: Boolean,
)

/** What the recorder under a Sound shows. */
enum class PanelMode {
    /** Soundcheck may not use the microphone yet; offers Android's permission dialog. */
    ASK,

    /** The microphone was refused for good; offers the app's page in system Settings. */
    SETTINGS,

    /** Ready: hold the button to record. */
    READY,

    /** Recording while the button is held. */
    RECORDING,
}

/**
 * The recorder open under one Sound, as the Sounds design shows it.
 *
 * @property soundId the Sound being recorded.
 * @property mode what it offers.
 * @property headline e.g. "Hold to record".
 * @property body the line under it.
 * @property levels how loud the last moments were, 0 to 1, while recording.
 * @property message what the last take came to, e.g. "Didn't hear anything…", or null.
 * @property recordDescription what TalkBack says for the record button.
 * @property canUndo whether the Sound's last change of recording can be taken back.
 * @property canUsePhoneVoice whether the Sound has a recording to give up.
 */
data class RecordPanel(
    val soundId: SoundId,
    val mode: PanelMode,
    val headline: String,
    val body: String,
    val levels: List<Float>,
    val message: String?,
    val recordDescription: String,
    val canUndo: Boolean,
    val canUsePhoneVoice: Boolean,
)

/**
 * What the recorder is doing, as the Sounds view model holds it.
 *
 * @property open the Sound whose recorder is open, or null.
 * @property access whether Soundcheck may use the microphone.
 * @property recording whether a take is being recorded.
 * @property levels how loud the last moments were, while recording.
 * @property message what the last take came to, or null.
 * @property undoable the Sounds whose last change of recording can be taken back.
 */
data class RecordingView(
    val open: SoundId? = null,
    val access: MicAccess = MicAccess.Unknown,
    val recording: Boolean = false,
    val levels: List<Float> = emptyList(),
    val message: String? = null,
    val undoable: Set<SoundId> = emptySet(),
)

/**
 * Everything the Sounds list shows.
 *
 * @property title "Sounds", or "Choose a Sound" when choosing for a Step.
 * @property backLabel where back goes: "Warm-up", or "Step".
 * @property countLabel e.g. "3 OF 8 RECORDED".
 * @property rows every Sound, in library order.
 * @property picking whether a tap chooses the Sound for a Step.
 * @property canDelete false with only one Sound.
 * @property labels every label, which a new or renamed label must avoid.
 * @property notice a problem to show under the header, e.g. a recording that won't play.
 * @property panel the recorder open under one Sound, or null.
 */
data class SoundsUiState(
    val title: String,
    val backLabel: String,
    val countLabel: String,
    val rows: List<SoundRow>,
    val picking: Boolean,
    val canDelete: Boolean,
    val labels: List<String>,
    val notice: String?,
    val panel: RecordPanel?,
)

/**
 * The Sounds list.
 *
 * @param library the saved library.
 * @param pickFor the Step to choose a Sound for, or null to browse.
 * @param playing the Sound whose recording is playing, or null.
 * @param notice a problem to show, or null.
 * @param recording what the recorder is doing.
 * @return what to show.
 */
fun soundsUiState(
    library: Library,
    pickFor: StepRef?,
    playing: SoundId? = null,
    notice: String? = null,
    recording: RecordingView = RecordingView(),
): SoundsUiState {
    val chosen = pickFor?.let { library.programme(it.programmeId)?.step(it.key)?.soundId }
    val recorded = library.sounds.count { it.clip != null }
    val panel = if (pickFor == null) recordPanel(library = library, view = recording) else null
    return SoundsUiState(
        title = if (pickFor != null) "Choose a Sound" else "Sounds",
        backLabel = if (pickFor != null) "Step" else "Warm-up",
        countLabel = "$recorded OF ${library.sounds.size} RECORDED",
        rows = library.sounds.map { sound ->
            soundRow(library = library, sound = sound, chosen = chosen, playing = playing)
                .copy(open = sound.id == panel?.soundId)
        },
        picking = pickFor != null,
        canDelete = library.sounds.size > 1,
        labels = library.sounds.map { it.label },
        notice = notice,
        panel = panel,
    )
}

/**
 * The recorder open under a Sound.
 *
 * @param library the saved library.
 * @param view what the recorder is doing.
 * @return what it shows, or null if no recorder is open or its Sound has gone.
 */
private fun recordPanel(library: Library, view: RecordingView): RecordPanel? {
    val sound = view.open?.let(library::sound) ?: return null
    val mode = when {
        view.access == MicAccess.Blocked -> PanelMode.SETTINGS
        view.access != MicAccess.Granted -> PanelMode.ASK
        view.recording -> PanelMode.RECORDING
        else -> PanelMode.READY
    }
    val quoted = "“${sound.label}”"
    return RecordPanel(
        soundId = sound.id,
        mode = mode,
        headline = HEADLINES.getValue(mode),
        body = when (mode) {
            PanelMode.ASK -> "To record $quoted, let Soundcheck use the microphone. " +
                "Nothing you record leaves the phone."
            PanelMode.SETTINGS -> "To record $quoted, allow the microphone for Soundcheck " +
                "in the phone's Settings."
            else -> "Say $quoted the way you want to hear it in the car. " +
                "Silence is trimmed from both ends."
        },
        levels = if (mode == PanelMode.RECORDING) view.levels else emptyList(),
        message = view.message,
        recordDescription = if (view.recording) {
            "Stop recording ${sound.label}"
        } else {
            "Hold to record ${sound.label}"
        },
        canUndo = sound.id in view.undoable && !view.recording,
        canUsePhoneVoice = sound.clip != null && !view.recording,
    )
}

private val HEADLINES: Map<PanelMode, String> = mapOf(
    PanelMode.ASK to "Soundcheck needs the microphone",
    PanelMode.SETTINGS to "The microphone is turned off",
    PanelMode.READY to "Hold to record",
    PanelMode.RECORDING to "Recording… let go when you're done",
)

private fun soundRow(
    library: Library,
    sound: Sound,
    chosen: SoundId?,
    playing: SoundId?,
): SoundRow {
    val usages = library.soundUsage(sound.id)
    val clip = sound.clip
    return SoundRow(
        id = sound.id,
        label = sound.label,
        detail = clip?.let { clipLengthLabel(it.lengthMs) } ?: PHONE_VOICE,
        spokenDetail = clip?.let { "your recording, ${spokenClipLength(it.lengthMs)}" }
            ?: PHONE_VOICE,
        recorded = clip != null,
        playing = clip != null && sound.id == playing,
        usage = usageLabel(usages),
        deleteNote = usageText(usages),
        chosen = sound.id == chosen,
        open = false,
    )
}
