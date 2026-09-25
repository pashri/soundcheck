package org.pashri.soundcheck.warmup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.FakeSoundOutput.Scheduled
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.piano.FakePianoSource
import org.pashri.soundcheck.piano.Piano

/**
 * Frames in these tests are traced in the task text: origin 4 800, Demo at 52 800, Iteration
 * k from 196 800 + 240 000k, the third Step's Announcement at 1 444 800, the end at 60 100 ms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgrammePlayerTest {
    private val range = Range(lowest = Pitch(60), highest = Pitch(69))
    private val mim = SoundId("mim")
    private val neh = SoundId("neh")
    private val hum = SoundId("hum")
    private val triad = Step(
        pattern = StarterPatterns.TRIAD,
        soundId = mim,
        bpm = 120,
        direction = Direction.START_LOW,
    )
    private val tooWide = Step(
        pattern = StarterPatterns.DOUBLE_ARPEGGIO,
        soundId = neh,
        bpm = 120,
        direction = Direction.START_LOW,
    )
    private val programme =
        Programme(name = "Test", steps = listOf(triad, tooWide, triad.copy(soundId = hum)))
    private val announcements = FakeAnnouncements()

    private class Rig(val output: FakeSoundOutput, val piano: Piano, val player: ProgrammePlayer)

    private fun TestScope.rig(): Rig {
        val output = FakeSoundOutput(clockMs = { testScheduler.currentTime })
        val piano = Piano(source = FakePianoSource(), output = output)
        val player = ProgrammePlayer(
            output = output,
            piano = piano,
            announcements = announcements,
            scope = backgroundScope,
        )
        return Rig(output = output, piano = piano, player = player)
    }

    private fun TestScope.runUntil(ms: Long) {
        advanceTimeBy(ms - testScheduler.currentTime)
        runCurrent()
    }

    private val FakeSoundOutput.frames: List<Long> get() = scheduled.map { it.frame }

    private fun announcement(sound: SoundId, frame: Long): Scheduled =
        Scheduled(announcements.clipOf(sound).id, frame, ProgrammePlayer.ANNOUNCEMENT_GAIN)

    private fun at(step: Int, iteration: Int?, playing: Boolean): Playback = Playback(
        programme = programme,
        range = range,
        stepIndex = step,
        iteration = iteration,
        playing = playing,
    )

    @Test
    fun `playing starts the first Step's Announcement one start margin ahead`() = runTest {
        val rig = rig()
        assertTrue(rig.player.play(programme, range))
        runCurrent()
        assertEquals(listOf(announcement(mim, 4_800)), rig.output.scheduled)
    }

    @Test
    fun `the Demo's first note follows the Announcement and the gap, held for its length`() =
        runTest {
            val rig = rig()
            rig.player.play(programme, range)
            runUntil(200)
            val c4 = rig.piano.keyFor(Pitch(60))
            val expected = Scheduled(
                id = c4.id,
                frame = 52_800,
                gain = ProgrammePlayer.MELODY_GAIN,
                rate = c4.rate,
                lengthFrames = 24_000,
            )
            assertEquals(expected, rig.output.scheduled[1])
        }

    @Test
    fun `only the sounds within the next second are handed to the engine`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runCurrent()
        assertEquals(listOf(4_800L), rig.output.frames)
        runUntil(100)
        assertEquals(listOf(4_800L), rig.output.frames)
        runUntil(200)
        assertEquals(listOf(4_800L, 52_800L), rig.output.frames)
    }

    @Test
    fun `every event of a Tenor Arpeggio 8-hold Step is scheduled on its frame and in time`() =
        runTest {
            val rig = rig()
            val tenor = VoiceType.TENOR.range
            val step = Step(
                pattern = StarterPatterns.ARPEGGIO_8_HOLD,
                soundId = mim,
                bpm = 100,
                direction = Direction.START_LOW,
            )
            val timeline = requireNotNull(
                buildStepTimeline(step = step, range = tenor, announcementFrames = 24_000),
            )
            rig.player.play(Programme(name = "Long", steps = listOf(step)), tenor)
            runUntil((4_800 + timeline.lengthFrames) / 48 - 10)
            assertEquals(258, rig.output.scheduled.size)
            assertEquals(timeline.events.map { 4_800 + it.startFrame }, rig.output.frames)
            assertTrue(rig.output.lateSchedules.isEmpty())
        }

    @Test
    fun `Steps play in order, skipping one that does not fit, with a gap between them`() =
        runTest {
            val rig = rig()
            rig.player.play(programme, range)
            runUntil(30_200)
            assertTrue(announcement(hum, 1_444_800) in rig.output.scheduled)
            assertTrue(rig.output.scheduled.none { it.id == announcements.clipOf(neh).id })
            assertEquals(2, rig.player.playback.value?.stepIndex)
        }

    @Test
    fun `the Programme stops by itself after its last Step`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(60_000)
        assertEquals(2, rig.player.playback.value?.stepIndex)
        runUntil(61_000)
        assertNull(rig.player.playback.value)
        assertFalse(rig.output.running)
    }

    @Test
    fun `playback reports the Step and Iteration sounding now`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runCurrent()
        assertEquals(at(step = 0, iteration = null, playing = true), rig.player.playback.value)
        runUntil(10_600)
        assertEquals(at(step = 0, iteration = 1, playing = true), rig.player.playback.value)
    }

    @Test
    fun `pausing mid-Iteration and resuming replays that Iteration from its Key Chord`() =
        runTest {
            val rig = rig()
            rig.player.play(programme, range)
            runUntil(10_600)
            rig.player.pause()
            assertEquals(at(step = 0, iteration = 1, playing = false), rig.player.playback.value)
            runUntil(20_000)
            assertFalse(rig.output.running)
            val before = rig.output.scheduled.size
            assertTrue(rig.player.resume())
            runCurrent()
            val chord = listOf(61, 65, 68).map { rig.piano.keyFor(Pitch(it)) }
            val expected = chord.map {
                Scheduled(
                    id = it.id,
                    frame = 520_800,
                    gain = ProgrammePlayer.CHORD_GAIN,
                    rate = it.rate,
                    lengthFrames = 96_000,
                )
            }
            assertEquals(expected, rig.output.scheduled.drop(before))
        }

    @Test
    fun `pausing just after resuming keeps the Iteration it resumed`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(10_600)
        rig.player.pause()
        runUntil(20_000)
        rig.player.resume()
        runUntil(20_050)
        rig.player.pause()
        assertEquals(1, rig.player.playback.value?.iteration)
    }

    @Test
    fun `pausing during the Demo resumes the whole Step from its Announcement`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(2_000)
        rig.player.pause()
        assertEquals(at(step = 0, iteration = null, playing = false), rig.player.playback.value)
        runUntil(3_000)
        val before = rig.output.scheduled.size
        rig.player.resume()
        runCurrent()
        assertEquals(announcement(mim, 108_000), rig.output.scheduled[before])
    }

    @Test
    fun `pausing in the gap between Steps resumes on the next Step`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(29_500)
        rig.player.pause()
        assertEquals(at(step = 2, iteration = null, playing = false), rig.player.playback.value)
    }

    @Test
    fun `next plays the following Step that fits at once`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(10_600)
        rig.player.next()
        runCurrent()
        assertEquals(announcement(hum, 513_600), rig.output.scheduled.last())
        assertEquals(at(step = 2, iteration = null, playing = true), rig.player.playback.value)
    }

    @Test
    fun `next on the last Step ends the Programme`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(10_600)
        rig.player.next()
        runCurrent()
        rig.player.next()
        assertNull(rig.player.playback.value)
        assertFalse(rig.output.running)
    }

    @Test
    fun `previous goes back over a Step that does not fit`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(1_000)
        rig.player.next()
        runUntil(2_000)
        rig.player.previous()
        runCurrent()
        assertEquals(0, rig.player.playback.value?.stepIndex)
        assertEquals(announcement(mim, 100_800), rig.output.scheduled.last())
    }

    @Test
    fun `previous on the first Step restarts it`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(10_600)
        rig.player.previous()
        runCurrent()
        assertEquals(at(step = 0, iteration = null, playing = true), rig.player.playback.value)
        assertEquals(announcement(mim, 513_600), rig.output.scheduled.last())
    }

    @Test
    fun `a pause without fading silences and stops the output at once`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(1_000)
        rig.player.pause(fade = false)
        assertFalse(rig.output.running)
        assertTrue(rig.output.scheduled.isEmpty())
    }

    @Test
    fun `the delayed stop after a pause never stops a resumed Programme`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(1_000)
        rig.player.pause()
        runUntil(1_050)
        rig.player.resume()
        runUntil(2_000)
        assertTrue(rig.output.running)
        assertEquals(true, rig.player.playback.value?.playing)
    }

    @Test
    fun `a failed output pauses the Programme and playing again restarts it`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runUntil(1_000)
        rig.output.failed = true
        runUntil(1_100)
        assertEquals(false, rig.player.playback.value?.playing)
        assertFalse(rig.output.running)
        assertTrue(rig.player.resume())
        assertTrue(rig.output.running)
    }

    @Test
    fun `an output that will not start leaves the Programme paused on its first Step`() =
        runTest {
            val rig = rig()
            rig.output.startResult = false
            assertFalse(rig.player.play(programme, range))
            val expected = at(step = 0, iteration = null, playing = false)
            assertEquals(expected, rig.player.playback.value)
            rig.output.startResult = true
            assertTrue(rig.player.resume())
            assertEquals(true, rig.player.playback.value?.playing)
        }

    @Test
    fun `a Programme where no Step fits does not play`() = runTest {
        val rig = rig()
        assertFalse(rig.player.play(Programme(name = "Wide", steps = listOf(tooWide)), range))
        assertNull(rig.player.playback.value)
    }

    @Test
    fun `with no voice a Step plays without its Announcement`() = runTest {
        val rig = rig()
        announcements.voice = false
        rig.player.play(programme, range)
        runCurrent()
        assertEquals(listOf(28_800L), rig.output.frames)
    }

    @Test
    fun `starting prepares the Announcement of every Step that fits`() = runTest {
        val rig = rig()
        rig.player.play(programme, range)
        runCurrent()
        assertEquals(setOf(mim, hum), announcements.prepared)
    }

    @Test
    fun `pause without fade cancels a pending fade tail so it never stops later playback`() =
        runTest {
            val rig = rig()
            rig.player.play(programme, range)
            runUntil(1_000)
            rig.player.pause()
            runUntil(1_050)
            rig.player.pause(fade = false)
            rig.output.start()
            runUntil(1_250)
            assertTrue(rig.output.running)
        }
}
