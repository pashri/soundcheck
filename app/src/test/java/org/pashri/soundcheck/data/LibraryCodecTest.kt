package org.pashri.soundcheck.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.SavedProgramme
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepKey

class LibraryCodecTest {
    private val fixture: String =
        checkNotNull(javaClass.getResource("/data/library-v2.json")).readText()

    private val fixtureV1: String =
        checkNotNull(javaClass.getResource("/data/library-v1.json")).readText()

    private val humClip = RecordedClip(
        name = ClipName("0b7c9e2a-5d41-4f6e-9a3b-2c8d1e0f4a67.wav"),
        lengthMs = 640,
    )

    private val fixtureLibrary = Library(
        patterns = listOf(
            StarterPatterns.TRIAD,
            Pattern(
                id = PatternId("p-2"),
                name = "Minor sigh",
                notes = PatternNotation.parse("♭3e ♯4e 2e 1w"),
                keyChord = KeyChord.MINOR,
            ),
        ),
        sounds = listOf(StarterSounds.HUM.copy(clip = humClip)),
        programmes = listOf(
            SavedProgramme(
                id = ProgrammeId("morning"),
                name = "Morning",
                steps = listOf(
                    SavedStep(
                        key = StepKey("s-1"),
                        patternId = StarterPatterns.TRIAD.id,
                        soundId = StarterSounds.HUM.id,
                        bpm = 90,
                        direction = Direction.START_HIGH,
                        rangeOffset = RangeOffset(bottom = -1, top = 2),
                        guideMelody = false,
                    ),
                ),
            ),
        ),
    )

    private fun refuses(text: String) {
        assertThrows(IllegalArgumentException::class.java) { LibraryCodec.decode(text) }
    }

    @Test
    fun `the starter library survives a round trip`() {
        val library = StarterLibrary.LIBRARY
        assertEquals(library, LibraryCodec.decode(LibraryCodec.encode(library)))
    }

    @Test
    fun `the file format is pinned`() {
        assertEquals(fixtureLibrary, LibraryCodec.decode(fixture))
        assertEquals(
            Json.parseToJsonElement(fixture),
            Json.parseToJsonElement(LibraryCodec.encode(fixtureLibrary)),
        )
    }

    @Test
    fun `a version 1 file reads as the same library with no clips`() {
        val noClips = fixtureLibrary.copy(sounds = listOf(StarterSounds.HUM))
        assertEquals(noClips, LibraryCodec.decode(fixtureV1))
    }

    @Test
    fun `a Sound with no clip is written without one`() {
        assertEquals(
            Json.parseToJsonElement(fixtureV1.replace(
                oldValue = "\"version\": 1",
                newValue = "\"version\": 2",
            )),
            Json.parseToJsonElement(
                LibraryCodec.encode(fixtureLibrary.copy(sounds = listOf(StarterSounds.HUM))),
            ),
        )
    }

    @Test
    fun `a file from a newer version is refused`() {
        refuses(fixture.replace(oldValue = "\"version\": 2", newValue = "\"version\": 3"))
    }

    @Test
    fun `a version 1 file with a clip is refused`() {
        refuses(fixture.replace(oldValue = "\"version\": 2", newValue = "\"version\": 1"))
    }

    @Test
    fun `a clip file name that could reach outside the clips folder is refused`() {
        refuses(fixture.replace(
            oldValue = "0b7c9e2a-5d41-4f6e-9a3b-2c8d1e0f4a67.wav",
            newValue = "../library.json",
        ))
    }

    @Test
    fun `an empty clip is refused`() {
        refuses(fixture.replace(oldValue = "\"ms\": 640", newValue = "\"ms\": 0"))
    }

    @Test
    fun `a Pattern with an unknown Key Chord is refused`() {
        refuses(fixture.replace(oldValue = "\"MINOR\"", newValue = "\"BLUES\""))
    }

    @Test
    fun `a malformed note is refused`() {
        refuses(fixture.replace(oldValue = "♭3e ♯4e 2e 1w", newValue = "b3e ♯4e 2e 1w"))
    }

    @Test
    fun `a Step naming a missing Pattern is refused`() {
        refuses(fixture.replace(
            oldValue = "\"pattern\": \"triad\"",
            newValue = "\"pattern\": \"gone\"",
        ))
    }

    @Test
    fun `a missing field is refused`() {
        refuses(fixture.replace(oldValue = "\"offsetTop\": 2,", newValue = ""))
    }

    @Test
    fun `text that is not JSON is refused`() {
        refuses("not json")
    }
}
