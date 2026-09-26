package org.pashri.soundcheck.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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
            notificationsShown = { true },
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

    @Test
    fun `a change while a start settles is paired with the Metronome as it is now`() = runTest {
        val playback = MutableStateFlow(0)
        val status = MutableStateFlow(MetronomeStatus())
        val seen = mutableListOf<Pair<Int, MetronomeStatus>>()
        backgroundScope.launch {
            withSettledMetronome(other = playback, status = status, settleMs = 250L)
                .collect { seen.add(it) }
        }
        runCurrent()
        status.value = MetronomeStatus(running = true)
        advanceTimeBy(100)
        playback.value = 1
        runCurrent()

        assertEquals(1 to MetronomeStatus(running = true), seen.last())
    }

    @Test
    fun `a held Metronome's changes settle, and ending comes through at once`() = runTest {
        val status = MutableStateFlow(MetronomeStatus(running = true))
        val seen = mutableListOf<MetronomeStatus>()
        backgroundScope.launch {
            withSettledMetronome(other = flowOf(Unit), status = status, settleMs = 250L)
                .collect { seen.add(it.second) }
        }
        advanceTimeBy(300)
        seen.clear()
        status.value = MetronomeStatus(bpm = 100, running = true)
        advanceTimeBy(50)
        status.value = MetronomeStatus(bpm = 101, running = true)
        advanceTimeBy(300)
        assertEquals(listOf(MetronomeStatus(bpm = 101, running = true)), seen)
        status.value = MetronomeStatus(bpm = 101)
        runCurrent()
        assertEquals(MetronomeStatus(bpm = 101), seen.last())
    }
}
