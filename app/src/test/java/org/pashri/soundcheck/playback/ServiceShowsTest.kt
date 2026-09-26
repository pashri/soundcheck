package org.pashri.soundcheck.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.metronome.MetronomeController
import org.pashri.soundcheck.metronome.MetronomeStatus
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupController
import org.pashri.soundcheck.warmup.testController

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceShowsTest {
    @Test
    fun `a loaded Programme needs the service and its notification comes first`() {
        assertEquals(
            ServiceShows.WARM_UP,
            serviceShows(programmeLoaded = true, metronomeHeld = false),
        )
        assertEquals(
            ServiceShows.WARM_UP,
            serviceShows(programmeLoaded = true, metronomeHeld = true),
        )
    }

    @Test
    fun `a playing or paused Metronome on its own needs the service, with its notification`() {
        assertEquals(
            ServiceShows.METRONOME,
            serviceShows(programmeLoaded = false, metronomeHeld = true),
        )
        assertTrue(MetronomeStatus(running = false, paused = true).held)
        assertTrue(MetronomeStatus(running = true).held)
        assertFalse(MetronomeStatus().held)
    }

    @Test
    fun `with neither the service isn't needed`() {
        assertNull(serviceShows(programmeLoaded = false, metronomeHeld = false))
    }

    private fun TestScope.metronome(arbiter: ToolArbiter, warmup: WarmupController) =
        MetronomeController(
            output = FakeSoundOutput(clockMs = { testScheduler.currentTime }),
            focus = FakeFocusGate(),
            arbiter = arbiter,
            headphones = HeadphoneButton(
                arbiter = arbiter,
                warmup = warmup,
                scope = backgroundScope,
            ),
            scope = backgroundScope,
        )

    @Test
    fun `unplugging headphones pauses the Metronome, as it does the Programme`() = runTest {
        val arbiter = ToolArbiter()
        val warmup = testController(arbiter = arbiter)
        val metronome = metronome(arbiter = arbiter, warmup = warmup)
        metronome.toggle()
        runCurrent()

        onHeadphonesUnplugged(warmup = warmup, metronome = metronome)

        assertFalse(metronome.status.value.running)
        assertTrue(metronome.status.value.paused)
    }

    @Test
    fun `unplugging headphones pauses the Programme`() = runTest {
        val arbiter = ToolArbiter()
        val warmup = testController(arbiter = arbiter)
        val metronome = metronome(arbiter = arbiter, warmup = warmup)
        warmup.play(programme = StarterProgrammes.WARM_UP, range = VoiceType.TENOR.range)
        runCurrent()

        onHeadphonesUnplugged(warmup = warmup, metronome = metronome)
        runCurrent()

        assertEquals(false, warmup.playback.value?.playing)
    }
}
