package org.pashri.soundcheck.ui.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Playback
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StartOutcome
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.addProgramme
import org.pashri.soundcheck.warmup.addStep

class WarmupHomeUiStateTest {
    private val starter = StarterProgrammes.SAVED_WARM_UP
    private val morning = ProgrammeId("morning")

    private fun state(
        library: Library = StarterLibrary.LIBRARY,
        settings: WarmupSettings = WarmupSettings.DEFAULT,
        playback: Playback? = null,
        problem: StartProblem? = null,
        saveFailed: Boolean = false,
        restoredNotice: String? = null,
    ): WarmupHomeUiState = warmupHomeUiState(
        library = library,
        settings = settings,
        playback = playback,
        problem = problem,
        saveFailed = saveFailed,
        restoredNotice = restoredNotice,
    )

    private fun humStep(key: String): SavedStep = SavedStep(
        key = StepKey(key),
        patternId = StarterPatterns.TRIAD.id,
        soundId = StarterSounds.HUM.id,
        bpm = 90,
        direction = Direction.START_LOW,
    )

    private fun playback(playing: Boolean): Playback = Playback(
        programme = StarterProgrammes.WARM_UP,
        range = VoiceType.TENOR.range,
        stepIndex = 2,
        iteration = null,
        playing = playing,
    )

    @Test
    fun `the Range card names the Voice Type and the notes`() {
        assertEquals("Tenor · C3 – A4", state().rangeLabel)
        val tuned = WarmupSettings(
            voiceType = VoiceType.TENOR,
            range = Range(lowest = Pitch.parse("C3"), highest = Pitch.parse("B♭4")),
            playOverOtherAudio = false,
        )
        assertEquals("Tenor · C3 – B♭4", state(settings = tuned).rangeLabel)
    }

    @Test
    fun `each Programme lists its Sounds in order with its Step count`() {
        val card = state().programmes.single()
        assertEquals(starter.id, card.id)
        assertEquals("Starter warm-up", card.name)
        assertEquals("lip trill, hum, mim, oo, neh, mah", card.sounds)
        assertEquals("6 steps", card.stepsLabel)
        assertTrue(card.canStart)
        assertNull(card.problem)
    }

    @Test
    fun `a Sound used twice is listed once`() {
        val library = StarterLibrary.LIBRARY.addProgramme(id = morning, name = "Morning")
            .addStep(programmeId = morning, step = humStep("a"))
            .addStep(programmeId = morning, step = humStep("b"))
        val card = state(library = library).programmes[1]
        assertEquals("hum", card.sounds)
        assertEquals("2 steps", card.stepsLabel)
    }

    @Test
    fun `an empty Programme can't start and says it has no Steps`() {
        val library = StarterLibrary.LIBRARY.addProgramme(id = morning, name = "Morning")
        val card = state(library = library).programmes[1]
        assertEquals("", card.sounds)
        assertEquals("No steps yet", card.stepsLabel)
        assertFalse(card.canStart)
    }

    @Test
    fun `a Programme with one Step says 1 step`() {
        val library = StarterLibrary.LIBRARY.addProgramme(id = morning, name = "Morning")
            .addStep(programmeId = morning, step = humStep("a"))
        assertEquals("1 step", state(library = library).programmes[1].stepsLabel)
    }

    @Test
    fun `a start problem shows under its own Programme only`() {
        val library = StarterLibrary.LIBRARY.addProgramme(id = morning, name = "Morning")
        val problem = StartProblem(programmeId = starter.id, outcome = StartOutcome.NOTHING_FITS)
        val cards = state(library = library, problem = problem).programmes
        assertEquals(startProblemMessage(StartOutcome.NOTHING_FITS), cards[0].problem)
        assertNull(cards[1].problem)
    }

    @Test
    fun `each start problem has its own explanation`() {
        assertNull(startProblemMessage(StartOutcome.PLAYING))
        assertEquals(
            "No Step fits your Range, so there's nothing to play.",
            startProblemMessage(StartOutcome.NOTHING_FITS),
        )
        assertEquals(
            "Another app is holding on to the sound. Try again in a moment.",
            startProblemMessage(StartOutcome.AUDIO_BUSY),
        )
        assertEquals(
            "The sound wouldn't start. Open it from Paused and press Resume to try again.",
            startProblemMessage(StartOutcome.OUTPUT_FAILED),
        )
    }

    @Test
    fun `a playing Programme shows as now playing and a paused one as paused`() {
        assertEquals(
            NowPlayingCard(label = "NOW PLAYING", title = "Starter warm-up · Step 3 of 6"),
            state(playback = playback(playing = true)).nowPlaying,
        )
        assertEquals("PAUSED", state(playback = playback(playing = false)).nowPlaying?.label)
        assertNull(state().nowPlaying)
    }

    @Test
    fun `the library tiles count the Patterns and the Sounds`() {
        assertEquals(8, state().patternCount)
        assertEquals(8, state().soundCount)
    }

    @Test
    fun `a failed save is explained`() {
        assertNull(state().saveProblem)
        assertEquals(
            "Couldn't save your last change. Is the phone's storage full?",
            state(saveFailed = true).saveProblem,
        )
    }

    @Test
    fun `nothing is shown by default about a restored document`() {
        assertNull(state().restoredNotice)
    }

    @Test
    fun `a restored document's notice is shown as given`() {
        assertEquals(
            LIBRARY_RESTORED_NOTICE,
            state(restoredNotice = LIBRARY_RESTORED_NOTICE).restoredNotice,
        )
        assertEquals(
            SETTINGS_RESTORED_NOTICE,
            state(restoredNotice = SETTINGS_RESTORED_NOTICE).restoredNotice,
        )
    }
}
