package org.pashri.soundcheck.ui.tuner

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.pashri.soundcheck.music.midiOf
import org.pashri.soundcheck.tuner.MicStatus
import org.pashri.soundcheck.tuner.NoteReading

class TunerUiStateTest {
    private val listening = TunerUiState(access = MicAccess.Granted, mic = MicStatus.Listening)

    @Test
    fun `an A2 four cents flat reads exactly like the mockup`() {
        val state = listening.copy(note = NoteReading.of(midiOf(109.746)))
        assertEquals("A", state.note?.name)
        assertEquals(2, state.note?.octave)
        assertEquals("109.7 Hz", state.hzLabel)
        assertEquals("4 cents flat", state.readingLabel)
        assertEquals("a touch low, nearly there", state.adviceLabel)
        assertEquals(-5.6f, checkNotNull(state.needleDegrees), 0.01f)
    }

    @Test
    fun `with no note it invites one and rests the needle`() {
        assertEquals("listening…", listening.hzLabel)
        assertEquals("Play or sing a note", listening.readingLabel)
        assertEquals("", listening.adviceLabel)
        assertNull(listening.needleDegrees)
        assertNull(listening.message)
    }

    @Test
    fun `the needle swings 70 degrees at 50 cents and no further`() {
        assertEquals(0f, needleDegrees(0.0), 0f)
        assertEquals(-70f, needleDegrees(-50.0), 1e-4f)
        assertEquals(70f, needleDegrees(50.0), 1e-4f)
        assertEquals(35f, needleDegrees(25.0), 1e-4f)
        assertEquals(70f, needleDegrees(80.0), 1e-4f)
        assertEquals(-70f, needleDegrees(-80.0), 1e-4f)
    }

    @Test
    fun `each access and microphone state picks its face`() {
        val expected = mapOf(
            TunerUiState(access = MicAccess.Unknown) to TunerMode.AskPermission,
            TunerUiState(access = MicAccess.Denied) to TunerMode.AskPermission,
            TunerUiState(access = MicAccess.Blocked) to TunerMode.OpenSettings,
            TunerUiState(access = MicAccess.Granted) to TunerMode.Listening,
            listening to TunerMode.Listening,
            TunerUiState(MicAccess.Granted, MicStatus.Unavailable) to TunerMode.MicUnavailable,
        )
        expected.forEach { (state, mode) -> assertEquals("$state", mode, state.mode) }
    }

    @Test
    fun `each message has the button that does its job`() {
        assertEquals("Allow microphone", TunerUiState(access = MicAccess.Denied).message?.button)
        assertEquals("Open settings", TunerUiState(access = MicAccess.Blocked).message?.button)
        val unavailable = TunerUiState(MicAccess.Granted, MicStatus.Unavailable)
        assertEquals("Try again", unavailable.message?.button)
    }

    @Test
    fun `the frequency is written with a decimal point whatever the phone's language`() {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val state = listening.copy(note = NoteReading.of(midiOf(109.7)))
            assertEquals("109.7 Hz", state.hzLabel)
        } finally {
            Locale.setDefault(default)
        }
    }
}
