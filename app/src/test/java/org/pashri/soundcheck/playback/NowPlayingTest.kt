package org.pashri.soundcheck.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import org.pashri.soundcheck.metronome.MetronomeStatus
import org.pashri.soundcheck.ui.warmup.WarmupUiState
import org.pashri.soundcheck.ui.warmup.warmupUiState
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType

class NowPlayingTest {
    private fun state(iteration: Int?, playing: Boolean): WarmupUiState {
        val programme = StarterProgrammes.WARM_UP
        val tenor = VoiceType.TENOR.range
        val playback = Playback(
            programme = programme,
            range = tenor,
            stepIndex = 2,
            iteration = iteration,
            playing = playing,
        )
        return warmupUiState(
            playback = playback,
            programme = programme,
            range = tenor,
            sounds = StarterSounds.ALL,
        )
    }

    @Test
    fun `the notification names the Sound, the Step, the key and the Iteration`() {
        val expected = NowPlaying(
            title = "mim",
            text = "Step 3 of 6 · E♭ major · 4 / 19 ↑",
            subText = "Starter warm-up",
            playing = true,
        )
        assertEquals(expected, nowPlaying(state(iteration = 3, playing = true)))
    }

    @Test
    fun `before the first Iteration it leaves the progress out`() {
        assertEquals(
            "Step 3 of 6 · C major",
            nowPlaying(state(iteration = null, playing = true)).text,
        )
    }

    @Test
    fun `a paused Programme shows as paused`() {
        assertEquals(false, nowPlaying(state(iteration = 3, playing = false)).playing)
    }

    @Test
    fun `with nothing loaded it just says Warm-up`() {
        assertEquals(
            NowPlaying(title = "Warm-up", text = "", subText = "", playing = false),
            nowPlaying(null),
        )
    }

    @Test
    fun `the Metronome's notification gives its tempo and accent`() {
        val status = MetronomeStatus(bpm = 96, accentEvery = 4, running = true)
        val expected = NowPlaying(
            title = "Metronome",
            text = "96 bpm · accent 4",
            subText = "",
            playing = true,
        )
        assertEquals(expected, metronomeNowPlaying(status))
    }

    @Test
    fun `a paused Metronome says so in its title`() {
        val status = MetronomeStatus(bpm = 96, accentEvery = 4, running = false, paused = true)
        val now = metronomeNowPlaying(status)
        assertEquals("Metronome · paused", now.title)
        assertEquals("96 bpm · accent 4", now.text)
        assertEquals(false, now.playing)
    }

    @Test
    fun `a Metronome without an accent says so`() {
        val status = MetronomeStatus(bpm = 72, accentEvery = null, running = true)
        assertEquals("72 bpm · no accent", metronomeNowPlaying(status).text)
    }
}
