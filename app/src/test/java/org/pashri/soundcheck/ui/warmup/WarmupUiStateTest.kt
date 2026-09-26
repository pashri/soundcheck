package org.pashri.soundcheck.ui.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.ui.components.spokenMusic
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.Programme
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.RoundTrip
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.Step
import org.pashri.soundcheck.warmup.VoiceType

class WarmupUiStateTest {
    private val programme = StarterProgrammes.WARM_UP
    private val tenor = VoiceType.TENOR.range

    private fun state(stepIndex: Int?, iteration: Int?, playing: Boolean = true): WarmupUiState {
        val playback = stepIndex?.let {
            Playback(
                programme = programme,
                range = tenor,
                stepIndex = it,
                iteration = iteration,
                playing = playing,
            )
        }
        return warmupUiState(
            playback = playback,
            programme = programme,
            range = tenor,
            sounds = StarterSounds.ALL,
        )
    }

    @Test
    fun `mid-Programme it reads exactly like the mockup`() {
        val state = state(stepIndex = 2, iteration = 3)
        assertEquals("Starter warm-up", state.programmeName)
        assertEquals("STEP 3 / 6", state.stepLabel)
        assertEquals("mim", state.soundLabel)
        assertEquals("on Arpeggio 8-hold, starting low", state.stepDetail)
        val iterations = checkNotNull(state.iterations)
        assertEquals("E♭ major", iterations.keyLabel)
        assertEquals("4 / 19 ↑", iterations.progressLabel)
        assertEquals(19, iterations.count)
        assertEquals(3, iterations.now)
        assertEquals(9, iterations.turnIndex)
        assertEquals("C3 ↑", iterations.startLabel)
        assertEquals("TURN AT A3", iterations.turnLabel)
        assertEquals("↓ C3", iterations.endLabel)
        assertEquals("oo", state.nextSound)
        assertEquals("on 1-5-1 siren", state.nextDetail)
        assertEquals("Pause", state.playLabel)
    }

    @Test
    fun `after the turn the arrow points home`() {
        assertEquals("10 / 19 ↑", state(stepIndex = 2, iteration = 9).iterations?.progressLabel)
        assertEquals("13 / 19 ↓", state(stepIndex = 2, iteration = 12).iterations?.progressLabel)
    }

    @Test
    fun `a Step that starts high counts down first and names a lone root`() {
        val iterations = checkNotNull(state(stepIndex = 3, iteration = 0).iterations)
        assertEquals("D", iterations.keyLabel)
        assertEquals("1 / 29 ↓", iterations.progressLabel)
        assertEquals("D4 ↓", iterations.startLabel)
        assertEquals("TURN AT C3", iterations.turnLabel)
        assertEquals("↑ D4", iterations.endLabel)
    }

    @Test
    fun `before anything plays it shows the first Step, ready to start`() {
        val state = state(stepIndex = null, iteration = null)
        assertEquals("STEP 1 / 6", state.stepLabel)
        assertEquals("lip trill", state.soundLabel)
        assertEquals("on 5-note scale, starting low", state.stepDetail)
        assertEquals("C major", state.iterations?.keyLabel)
        assertEquals("– / 33", state.iterations?.progressLabel)
        assertFalse(state.active)
        assertFalse(state.playing)
        assertEquals("Start", state.playLabel)
    }

    @Test
    fun `while paused the button offers to resume`() {
        val state = state(stepIndex = 2, iteration = 3, playing = false)
        assertTrue(state.active)
        assertEquals("Resume", state.playLabel)
    }

    @Test
    fun `during the Demo the key shown is the starting key and no Iteration is lit`() {
        val iterations = checkNotNull(state(stepIndex = 2, iteration = null).iterations)
        assertEquals("C major", iterations.keyLabel)
        assertEquals("– / 19", iterations.progressLabel)
        assertNull(iterations.now)
    }

    @Test
    fun `the last Step has nothing next`() {
        val state = state(stepIndex = 5, iteration = 0)
        assertNull(state.nextSound)
        assertNull(state.nextDetail)
    }

    @Test
    fun `a deleted Sound keeps the label its Step started with, not its raw id`() {
        val step = Step(
            pattern = programme.steps[2].pattern,
            soundId = SoundId("uuid-of-a-deleted-sound"),
            bpm = 90,
            direction = Direction.START_LOW,
            soundLabel = "vroom",
        )
        val deleted = Programme(name = programme.name, steps = listOf(step))
        val playback = Playback(
            programme = deleted,
            range = tenor,
            stepIndex = 0,
            iteration = 0,
            playing = true,
        )
        val state = warmupUiState(
            playback = playback,
            programme = deleted,
            range = tenor,
            sounds = emptyList(),
        )
        assertEquals("vroom", state.soundLabel)
    }

    @Test
    fun `Key Chords are named as a singer reads them`() {
        val eFlat = Pitch.parse("E♭3")
        assertEquals(
            listOf("E♭ major", "E♭ minor", "E♭7", "E♭maj7", "E♭m7", "E♭dim", "E♭aug", "E♭"),
            KeyChord.entries.map { keyLabel(key = eFlat, chord = it) },
        )
    }

    @Test
    fun `a key label is spoken with its symbols spelled out`() {
        assertEquals("E flat major", spokenMusic("E♭ major"))
        assertEquals("E flat", spokenMusic("E♭"))
        assertEquals("C major", spokenMusic("C major"))
        assertEquals("F sharp 7", spokenMusic("F♯7"))
    }

    @Test
    fun `progress is spoken as an Iteration, a direction, the Demo or not started`() {
        val playing = checkNotNull(state(stepIndex = 2, iteration = 3).iterations)
        assertEquals("Iteration 4 of 19, going up", spokenProgress(view = playing, active = true))
        val homeward = checkNotNull(state(stepIndex = 2, iteration = 12).iterations)
        assertEquals(
            "Iteration 13 of 19, going down",
            spokenProgress(view = homeward, active = true),
        )
        val demo = checkNotNull(state(stepIndex = 2, iteration = null).iterations)
        assertEquals("Demo", spokenProgress(view = demo, active = true))
        val notStarted = checkNotNull(state(stepIndex = null, iteration = null).iterations)
        assertEquals("not started", spokenProgress(view = notStarted, active = false))
    }

    @Test
    fun `the turn labels are spoken as one sentence`() {
        val view = checkNotNull(state(stepIndex = 2, iteration = 3).iterations)
        assertEquals("Starts C3, turns at A3, back to C3", spokenTurn(view))
        val highStart = checkNotNull(state(stepIndex = 3, iteration = 0).iterations)
        assertEquals("Starts D4, turns at C3, back to D4", spokenTurn(highStart))
    }

    @Test
    fun `the staff shows the Step's Pattern and the note being sung`() {
        val playback = Playback(
            programme = programme,
            range = tenor,
            stepIndex = 2,
            iteration = 3,
            playing = true,
        )
        val state = warmupUiState(
            playback = playback,
            programme = programme,
            range = tenor,
            sounds = StarterSounds.ALL,
            note = 4,
        )
        val staff = checkNotNull(state.staff)
        val expected = staffLayout(
            pattern = StarterPatterns.ARPEGGIO_8_HOLD,
            key = Pitch.parse("E♭3"),
            clef = Clef.TREBLE_8VB,
        )
        assertEquals(expected, staff.layout)
        assertEquals(4, staff.now)
        assertEquals(
            "Pattern on a staff, treble clef, an octave lower: 1 3 5 8 8 8 8 5 3 1",
            staff.description,
        )
    }

    @Test
    fun `the keyboard spans the Step's Range and marks the key and its top note`() {
        val keyboard = checkNotNull(state(stepIndex = 2, iteration = 3).keyboard)
        assertEquals("C3", keyboard.whites.first().pitch.name)
        assertEquals("A4", keyboard.whites.last().pitch.name)
        assertEquals("E♭3", keyboard.blacks.single { it.mark == KeyMark.ROOT }.pitch.name)
        assertEquals("E♭4", keyboard.blacks.single { it.mark == KeyMark.TOP }.pitch.name)
    }

    @Test
    fun `a Range Offset widens the keyboard with the Step's Range`() {
        val keyboard = checkNotNull(state(stepIndex = 0, iteration = null).keyboard)
        assertEquals("B4", keyboard.whites.last().pitch.name)
        assertEquals("C3", keyboard.whites.single { it.mark == KeyMark.ROOT }.pitch.name)
    }

    @Test
    fun `the keyboard names its top note as the staff spells it`() {
        val keyboard = checkNotNull(state(stepIndex = 0, iteration = 6).keyboard)
        assertEquals("Keyboard C3 to B4, key F♯3, top note C♯4", keyboard.description)
    }

    @Test
    fun `a Step that doesn't fit has no staff and no keyboard`() {
        val narrow = Range(lowest = Pitch.parse("C4"), highest = Pitch.parse("D4"))
        val state = warmupUiState(
            playback = null,
            programme = programme,
            range = narrow,
            sounds = StarterSounds.ALL,
        )
        assertNull(state.iterations)
        assertNull(state.keyboard)
        assertNull(state.staff)
    }

    @Test
    fun `the staff keeps the same room through every Iteration of a Step`() {
        val trip = programme.steps[2].roundTrip(tenor) as RoundTrip.Fits
        val bounds = stepStaffBounds(
            pattern = StarterPatterns.ARPEGGIO_8_HOLD,
            keys = trip.keys,
            clef = Clef.TREBLE_8VB,
        )
        listOf(null, 0, 3, 12).forEach { iteration ->
            val staff = checkNotNull(state(stepIndex = 2, iteration = iteration).staff)
            assertEquals(bounds, staff.reserved)
        }
    }
}
