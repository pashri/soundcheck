package org.pashri.soundcheck.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.Sound
import org.pashri.soundcheck.warmup.SoundId
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.deleteProgramme
import org.pashri.soundcheck.warmup.withClip
import org.pashri.soundcheck.warmup.withVoiceType

class ImportTest {
    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val settings = FakeStore(WarmupSettings.DEFAULT)
    private val bass = WarmupSettings.DEFAULT.withVoiceType(VoiceType.BASS)
    private val mimClip = RecordedClip(name = ClipName("mim.wav"), lengthMs = 600)
    private val humClip = RecordedClip(name = ClipName("hum.wav"), lengthMs = 700)

    /** A backup with one Pattern, "mim" renamed "mmm", a new Sound, and no Programmes. */
    private val theirs = Backup(
        library = Library(
            patterns = listOf(StarterPatterns.TRIAD),
            sounds = listOf(
                StarterSounds.MIM.copy(label = "mmm"),
                Sound(id = SoundId("brr"), label = "brr"),
            ),
            programmes = emptyList(),
        ),
        settings = bass,
    )

    private suspend fun importTheirs(): ImportOutcome =
        importBackup(backup = theirs, library = library, settings = settings, stamp = 42L)

    @Test
    fun `an import replaces the library and the settings`() = runTest {
        assertEquals(ImportOutcome.IMPORTED, importTheirs())
        assertEquals(theirs.library, library.value)
        assertEquals(bass, settings.value)
    }

    @Test
    fun `a Sound still in the library keeps its recording, matched by id`() = runTest {
        library.set(
            StarterLibrary.LIBRARY
                .withClip(id = StarterSounds.MIM.id, clip = mimClip)
                .withClip(id = StarterSounds.HUM.id, clip = humClip),
        )
        importTheirs()
        assertEquals(mimClip, library.value.sound(StarterSounds.MIM.id)?.clip)
        assertEquals("mmm", library.value.sound(StarterSounds.MIM.id)?.label)
        assertNull(library.value.sound(SoundId("brr"))?.clip)
        assertNull(library.value.sound(StarterSounds.HUM.id))
    }

    @Test
    fun `both documents are backed up before either is replaced`() = runTest {
        importTheirs()
        assertEquals(StarterLibrary.LIBRARY, library.backups[42L])
        assertEquals(WarmupSettings.DEFAULT, settings.backups[42L])
    }

    @Test
    fun `a library that can't be written changes nothing`() = runTest {
        library.replaces = false
        assertEquals(ImportOutcome.NOTHING_CHANGED, importTheirs())
        assertEquals(StarterLibrary.LIBRARY, library.value)
        assertEquals(WarmupSettings.DEFAULT, settings.value)
        assertTrue(settings.backups.isEmpty())
    }

    @Test
    fun `settings that can't be written put the old library back`() = runTest {
        settings.replaces = false
        assertEquals(ImportOutcome.NOTHING_CHANGED, importTheirs())
        assertEquals(StarterLibrary.LIBRARY, library.value)
        assertEquals(WarmupSettings.DEFAULT, settings.value)
    }

    @Test
    fun `if the old library can't be put back, the import says only the library came in`() =
        runTest {
            settings.replaces = false
            library.undoes = false
            assertEquals(ImportOutcome.LIBRARY_ONLY, importTheirs())
            assertEquals(theirs.library, library.value)
        }

    @Test
    fun `nothing is imported before both documents have been read`() = runTest {
        val unread = FakeStore<WarmupSettings>(null)
        val outcome =
            importBackup(backup = theirs, library = library, settings = unread, stamp = 42L)
        assertEquals(ImportOutcome.NOTHING_CHANGED, outcome)
        assertEquals(StarterLibrary.LIBRARY, library.value)
    }

    @Test
    fun `an exported library imports back as it was, recordings and all`() = runTest {
        library.set(StarterLibrary.LIBRARY.withClip(id = StarterSounds.MIM.id, clip = mimClip))
        val saved = library.value
        val text = ExportCodec.encode(Backup(library = saved, settings = settings.value))
        library.set(saved.deleteProgramme(StarterProgrammes.SAVED_WARM_UP.id))
        settings.set(bass)
        val read = readExport(text) as ExportRead.Valid
        val outcome =
            importBackup(backup = read.backup, library = library, settings = settings, stamp = 1L)
        assertEquals(ImportOutcome.IMPORTED, outcome)
        assertEquals(saved, library.value)
        assertEquals(WarmupSettings.DEFAULT, settings.value)
    }
}
