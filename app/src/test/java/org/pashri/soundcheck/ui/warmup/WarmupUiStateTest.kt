package org.pashri.soundcheck.ui.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
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
    fun `Key Chords are named as a singer reads them`() {
        val eFlat = Pitch.parse("E♭3")
        assertEquals(
            listOf("E♭ major", "E♭ minor", "E♭7", "E♭maj7", "E♭m7", "E♭dim", "E♭aug", "E♭"),
            KeyChord.entries.map { keyLabel(key = eFlat, chord = it) },
        )
    }

    @Test
    fun `a key label is spoken with its symbols spelled out`() {
        assertEquals("E flat major", spokenKeyLabel("E♭ major"))
        assertEquals("E flat", spokenKeyLabel("E♭"))
        assertEquals("C major", spokenKeyLabel("C major"))
        assertEquals("F sharp7", spokenKeyLabel("F♯7"))
    }

    @Test
    fun `progress is spoken as an Iteration, a direction, the Demo or not started`() {
        val playing = checkNotNull(state(stepIndex = 2, iteration = 3).iterations)
        assertEquals("Iteration 4 of 19, going up", spokenProgress(playing, active = true))
        val homeward = checkNotNull(state(stepIndex = 2, iteration = 12).iterations)
        assertEquals("Iteration 13 of 19, going down", spokenProgress(homeward, active = true))
        val demo = checkNotNull(state(stepIndex = 2, iteration = null).iterations)
        assertEquals("Demo", spokenProgress(demo, active = true))
        val notStarted = checkNotNull(state(stepIndex = null, iteration = null).iterations)
        assertEquals("not started", spokenProgress(notStarted, active = false))
    }

    @Test
    fun `the turn labels are spoken as one sentence`() {
        val view = checkNotNull(state(stepIndex = 2, iteration = 3).iterations)
        assertEquals("Starts C3, turns at A3, back to C3", spokenTurn(view))
        val highStart = checkNotNull(state(stepIndex = 3, iteration = 0).iterations)
        assertEquals("Starts D4, turns at C3, back to D4", spokenTurn(highStart))
    }
}
