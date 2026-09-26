package org.pashri.soundcheck.ui.sounds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.ui.tuner.MicAccess
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.withClip

class SoundsUiStateTest {
    private val starter = StarterProgrammes.SAVED_WARM_UP.id

    @Test
    fun `the library lists every Sound as the phone's voice reads it`() {
        val state = soundsUiState(library = StarterLibrary.LIBRARY, pickFor = null)
        assertEquals("Sounds", state.title)
        assertEquals("Warm-up", state.backLabel)
        assertEquals("0 OF 8 RECORDED", state.countLabel)
        assertFalse(state.picking)
        assertTrue(state.canDelete)
        assertEquals(
            SoundRow(
                id = StarterSounds.LIP_TRILL.id,
                label = "lip trill",
                detail = "phone voice",
                spokenDetail = "phone voice",
                recorded = false,
                playing = false,
                usage = "Used in 1 Step",
                deleteNote = "1 Step in Starter warm-up uses it; that Step goes too.",
                chosen = false,
                open = false,
            ),
            state.rows.first(),
        )
        assertEquals("Not in any Step", state.rows[4].usage)
        assertEquals(StarterSounds.ALL.map { it.label }, state.labels)
    }

    @Test
    fun `choosing for a Step marks its Sound`() {
        val mim = StepRef(programmeId = starter, key = StepKey("starter-3"))
        val state = soundsUiState(library = StarterLibrary.LIBRARY, pickFor = mim)
        assertEquals("Choose a Sound", state.title)
        assertEquals("Step", state.backLabel)
        assertTrue(state.picking)
        assertEquals(listOf("mim"), state.rows.filter { it.chosen }.map { it.label })
    }

    @Test
    fun `the last Sound can't be deleted`() {
        val one = Library(
            patterns = StarterPatterns.ALL,
            sounds = listOf(StarterSounds.HUM),
            programmes = emptyList(),
        )
        val state = soundsUiState(library = one, pickFor = null)
        assertFalse(state.canDelete)
        assertEquals("0 OF 1 RECORDED", state.countLabel)
    }

    @Test
    fun `a recorded Sound shows its length and counts as recorded`() {
        val clip = RecordedClip(name = ClipName("mim.wav"), lengthMs = 640)
        val library = StarterLibrary.LIBRARY.withClip(id = StarterSounds.MIM.id, clip = clip)
        val state = soundsUiState(library = library, pickFor = null)
        val mim = state.rows.single { it.id == StarterSounds.MIM.id }
        assertEquals("0.6 s", mim.detail)
        assertEquals("your recording, 0.6 seconds", mim.spokenDetail)
        assertTrue(mim.recorded)
        assertEquals("1 OF 8 RECORDED", state.countLabel)
    }

    @Test
    fun `only the Sound whose recording is playing is marked`() {
        val clip = RecordedClip(name = ClipName("mim.wav"), lengthMs = 640)
        val library = StarterLibrary.LIBRARY.withClip(id = StarterSounds.MIM.id, clip = clip)
        val state =
            soundsUiState(library = library, pickFor = null, playing = StarterSounds.MIM.id)
        assertEquals(listOf("mim"), state.rows.filter { it.playing }.map { it.label })
    }

    private fun panel(
        view: RecordingView,
        library: Library = StarterLibrary.LIBRARY,
    ): RecordPanel? = soundsUiState(library = library, pickFor = null, recording = view).panel

    @Test
    fun `an open recorder asks for the microphone before it has been allowed`() {
        val view = RecordingView(open = StarterSounds.NEH.id)
        val state =
            soundsUiState(library = StarterLibrary.LIBRARY, pickFor = null, recording = view)
        assertEquals(PanelMode.ASK, state.panel?.mode)
        assertEquals("Soundcheck needs the microphone", state.panel?.headline)
        assertEquals(listOf("neh"), state.rows.filter { it.open }.map { it.label })
    }

    @Test
    fun `with the microphone allowed the recorder says to hold the button`() {
        val view = RecordingView(open = StarterSounds.NEH.id, access = MicAccess.Granted)
        val expected = RecordPanel(
            soundId = StarterSounds.NEH.id,
            mode = PanelMode.READY,
            headline = "Hold to record",
            body = "Say “neh” the way you want to hear it in the car. " +
                "Silence is trimmed from both ends.",
            levels = emptyList(),
            message = null,
            recordDescription = "Hold to record neh",
            canUndo = false,
            canUsePhoneVoice = false,
        )
        assertEquals(expected, panel(view))
    }

    @Test
    fun `while recording the recorder shows the levels and offers nothing else`() {
        val clip = RecordedClip(name = ClipName("neh.wav"), lengthMs = 500)
        val library = StarterLibrary.LIBRARY.withClip(id = StarterSounds.NEH.id, clip = clip)
        val view = RecordingView(
            open = StarterSounds.NEH.id,
            access = MicAccess.Granted,
            recording = true,
            levels = listOf(0.5f),
            undoable = setOf(StarterSounds.NEH.id),
        )
        val shown = checkNotNull(panel(view = view, library = library))
        assertEquals(PanelMode.RECORDING, shown.mode)
        assertEquals(listOf(0.5f), shown.levels)
        assertEquals("Stop recording neh", shown.recordDescription)
        assertFalse(shown.canUndo)
        assertFalse(shown.canUsePhoneVoice)
    }

    @Test
    fun `a microphone refused for good sends you to Settings`() {
        val view = RecordingView(open = StarterSounds.NEH.id, access = MicAccess.Blocked)
        assertEquals(PanelMode.SETTINGS, panel(view)?.mode)
        assertEquals("The microphone is turned off", panel(view)?.headline)
    }

    @Test
    fun `choosing a Sound for a Step never opens a recorder`() {
        val mim = StepRef(programmeId = starter, key = StepKey("starter-3"))
        val view = RecordingView(open = StarterSounds.NEH.id, access = MicAccess.Granted)
        val state =
            soundsUiState(library = StarterLibrary.LIBRARY, pickFor = mim, recording = view)
        assertNull(state.panel)
        assertTrue(state.rows.none { it.open })
    }
}
