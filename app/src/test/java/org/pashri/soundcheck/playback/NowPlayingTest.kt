package org.pashri.soundcheck.playback

import org.junit.Assert.assertEquals
import org.junit.Test
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
}
