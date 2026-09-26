package org.pashri.soundcheck.data

import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterProgrammes
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.deleteProgramme
import org.pashri.soundcheck.warmup.withVoiceType

/** An import through the real saved files, as the app does it. */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val libraryFile: File get() = File(folder.root, "library.json")
    private val settingsFile: File get() = File(folder.root, "settings.json")

    /** A backup with no Programmes and a bass's settings. */
    private val theirs = Backup(
        library = StarterLibrary.LIBRARY.deleteProgramme(StarterProgrammes.SAVED_WARM_UP.id),
        settings = WarmupSettings.DEFAULT.withVoiceType(VoiceType.BASS),
    )

    private fun <T : Any> TestScope.loaded(
        file: File,
        codec: TextCodec<T>,
        seed: T,
    ): DocumentStore<T> = DocumentStore(
        file = file,
        codec = codec,
        seed = { seed },
        scope = this,
        io = StandardTestDispatcher(testScheduler),
    ).also {
        it.load()
        advanceUntilIdle()
    }

    private fun TestScope.library(): DocumentStore<Library> =
        loaded(file = libraryFile, codec = LibraryCodec, seed = StarterLibrary.LIBRARY)

    private fun TestScope.settings(): DocumentStore<WarmupSettings> =
        loaded(file = settingsFile, codec = SettingsCodec, seed = WarmupSettings.DEFAULT)

    @Test
    fun `when the settings can't be saved, both files are as they were`() = runTest {
        val library = library()
        val settings = settings()
        val libraryBytes = libraryFile.readBytes()
        val settingsBytes = settingsFile.readBytes()
        val blocker = File(folder.root, "settings.json.tmp").apply { mkdir() }
        val outcome =
            importBackup(backup = theirs, library = library, settings = settings, stamp = 42L)
        assertEquals(ImportOutcome.NOTHING_CHANGED, outcome)
        assertArrayEquals(libraryBytes, libraryFile.readBytes())
        assertArrayEquals(settingsBytes, settingsFile.readBytes())
        assertEquals(LibraryCodec.decode(libraryFile.readText()), library.data.value)
        assertEquals(SettingsCodec.decode(settingsFile.readText()), settings.data.value)
        blocker.delete()
        assertEquals(setOf("library.json", "settings.json"), folder.root.list()?.toSet())
    }

    @Test
    fun `an import keeps both files' backups and saves both documents`() = runTest {
        val library = library()
        val settings = settings()
        val libraryBytes = libraryFile.readBytes()
        val settingsBytes = settingsFile.readBytes()
        val outcome =
            importBackup(backup = theirs, library = library, settings = settings, stamp = 42L)
        assertEquals(ImportOutcome.IMPORTED, outcome)
        assertArrayEquals(libraryBytes, File(folder.root, "library.json.backup-42").readBytes())
        assertArrayEquals(settingsBytes, File(folder.root, "settings.json.backup-42").readBytes())
        assertEquals(theirs.library, LibraryCodec.decode(libraryFile.readText()))
        assertEquals(theirs.settings, SettingsCodec.decode(settingsFile.readText()))
        assertEquals(theirs.library, library.data.value)
        assertEquals(theirs.settings, settings.data.value)
        assertEquals(
            setOf(
                "library.json",
                "settings.json",
                "library.json.backup-42",
                "settings.json.backup-42",
            ),
            folder.root.list()?.toSet(),
        )
    }
}
