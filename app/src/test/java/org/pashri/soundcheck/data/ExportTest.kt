package org.pashri.soundcheck.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Direction
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.ProgrammeId
import org.pashri.soundcheck.warmup.Range
import org.pashri.soundcheck.warmup.RangeOffset
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.SavedProgramme
import org.pashri.soundcheck.warmup.SavedStep
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.StepKey
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.withClip

class ExportTest {
    private val fixture: String =
        checkNotNull(javaClass.getResource("/data/export-v1.json")).readText()

    private val fixtureBackup = Backup(
        library = Library(
            patterns = listOf(
                StarterPatterns.TRIAD,
                Pattern(
                    id = PatternId("p-2"),
                    name = "Minor sigh",
                    notes = PatternNotation.parse("♭3e ♯4e 2e 1w"),
                    keyChord = KeyChord.MINOR,
                ),
            ),
            sounds = listOf(StarterSounds.HUM),
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
        ),
        settings = WarmupSettings(
            voiceType = VoiceType.BASS,
            range = Range(lowest = Pitch(38), highest = Pitch(64)),
            playOverOtherAudio = true,
        ),
    )

    private fun notAnExport(text: String) {
        assertEquals(ExportRead.NotAnExport, readExport(text))
    }

    @Test
    fun `a backup survives the round trip`() {
        val backup = Backup(library = StarterLibrary.LIBRARY, settings = WarmupSettings.DEFAULT)
        assertEquals(ExportRead.Valid(backup), readExport(ExportCodec.encode(backup)))
    }

    @Test
    fun `the backup format is pinned`() {
        assertEquals(ExportRead.Valid(fixtureBackup), readExport(fixture))
        assertEquals(
            Json.parseToJsonElement(fixture),
            Json.parseToJsonElement(ExportCodec.encode(fixtureBackup)),
        )
    }

    @Test
    fun `recordings are never written into a backup`() {
        val clip = RecordedClip(name = ClipName("mim.wav"), lengthMs = 600)
        val library = StarterLibrary.LIBRARY.withClip(id = StarterSounds.MIM.id, clip = clip)
        val text = ExportCodec.encode(Backup(library = library, settings = WarmupSettings.DEFAULT))
        assertFalse(text.contains("clip"))
        val read = readExport(text) as ExportRead.Valid
        assertNull(read.backup.library.sound(StarterSounds.MIM.id)?.clip)
    }

    @Test
    fun `a file in another format is not a backup`() {
        notAnExport(fixture.replace(oldValue = "soundcheck-export", newValue = "something-else"))
    }

    @Test
    fun `a backup from a newer version is too new`() {
        val newer = fixture.replaceFirst(oldValue = "\"version\": 1", newValue = "\"version\": 2")
        assertEquals(ExportRead.TooNew, readExport(newer))
    }

    @Test
    fun `a backup holding a library from a newer version is too new`() {
        val newer = fixture.replace(oldValue = "\"version\": 2", newValue = "\"version\": 3")
        assertEquals(ExportRead.TooNew, readExport(newer))
    }

    @Test
    fun `a backup holding settings from a newer version is too new`() {
        val newer = fixture.replace(
            oldValue = "\"settings\": {\n        \"version\": 1",
            newValue = "\"settings\": {\n        \"version\": 2",
        )
        assertEquals(ExportRead.TooNew, readExport(newer))
    }

    @Test
    fun `an empty file is not a backup`() {
        notAnExport("")
    }

    @Test
    fun `the library's own file is not a backup`() {
        notAnExport(checkNotNull(javaClass.getResource("/data/library-v2.json")).readText())
    }

    @Test
    fun `text that isn't JSON is not a backup`() {
        notAnExport("not json")
    }

    @Test
    fun `a Step naming a missing Sound is not a backup`() {
        notAnExport(
            fixture.replace(oldValue = "\"sound\": \"hum\"", newValue = "\"sound\": \"gone\""),
        )
    }

    @Test
    fun `two Patterns sharing an id are not a backup`() {
        notAnExport(fixture.replace(oldValue = "\"id\": \"p-2\"", newValue = "\"id\": \"triad\""))
    }

    @Test
    fun `a backup naming a recording is not a backup`() {
        notAnExport(fixture.replace(
            oldValue = "\"label\": \"hum\"",
            newValue = "\"label\": \"hum\", \"clip\": { \"file\": \"a.wav\", \"ms\": 500 }",
        ))
    }

    @Test
    fun `settings reaching past the piano are not a backup`() {
        notAnExport(fixture.replace(oldValue = "\"lowest\": 38", newValue = "\"lowest\": 5"))
    }

    @Test
    fun `a backup missing a field is not a backup`() {
        notAnExport(fixture.replace(oldValue = "\"highest\": 64,", newValue = ""))
    }
}
