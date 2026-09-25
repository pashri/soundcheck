package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class EventsBetweenTest {
    private val step = Step(
        pattern = StarterPatterns.TRIAD,
        soundId = SoundId("mim"),
        bpm = 120,
        direction = Direction.START_LOW,
    )
    private val timeline = requireNotNull(
        buildStepTimeline(
            step = step,
            range = Range(lowest = Pitch(60), highest = Pitch(69)),
            announcementFrames = 24_000,
        ),
    )

    @Test
    fun `a window holds the events that start inside it, including its first frame`() {
        val window = timeline.eventsBetween(from = 48_000, until = 96_000)
        assertEquals(listOf(48_000L, 72_000L), window.map { it.startFrame })
    }

    @Test
    fun `back-to-back windows cover every event exactly once, in order`() {
        val windows = (0L until timeline.lengthFrames step 50_000L)
            .flatMap { timeline.eventsBetween(from = it, until = it + 50_000L) }
        assertEquals(timeline.events, windows)
    }

    @Test
    fun `an empty or backwards window holds nothing`() {
        assertTrue(timeline.eventsBetween(from = 48_000, until = 48_000).isEmpty())
        assertTrue(timeline.eventsBetween(from = 96_000, until = 48_000).isEmpty())
    }
}
