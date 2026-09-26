package org.pashri.soundcheck.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.RecordedClip
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.withClip

@OptIn(ExperimentalCoroutinesApi::class)
class ClipSweepTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val directory: File get() = File(folder.root, "clips")

    private val recorded: Library = StarterLibrary.LIBRARY.withClip(
        id = StarterSounds.MIM.id,
        clip = RecordedClip(name = ClipName("mim-take.wav"), lengthMs = 600),
    )

    private fun oldClip(name: String): File = File(directory, name).apply {
        parentFile?.mkdirs()
        writeText("x")
        setLastModified(1_000L)
    }

    private suspend fun sweep(
        store: Store<Library>,
        readFromFile: Boolean = true,
        backedUp: Set<ClipName>? = emptySet(),
    ): Int =
        sweepUnusedClips(
            library = store,
            readFromFile = readFromFile,
            clips = ClipFiles(directory = directory, io = Dispatchers.IO, newName = { "n" }),
            startedAtMs = 2_000L,
            backedUp = { backedUp },
        )

    private fun files(): Set<String> = directory.list()?.toSet().orEmpty()

    @Test
    fun `clips the saved library no longer uses are deleted`() = runTest {
        oldClip("mim-take.wav")
        oldClip("deleted-sound.wav")
        assertEquals(1, sweep(FakeStore(recorded)))
        assertEquals(setOf("mim-take.wav"), files())
    }

    @Test
    fun `nothing is deleted on a fresh install`() = runTest {
        oldClip("stray.wav")
        assertEquals(0, sweep(store = FakeStore(StarterLibrary.LIBRARY), readFromFile = false))
        assertEquals(setOf("stray.wav"), files())
    }

    @Test
    fun `nothing is deleted when an unreadable library was set aside`() = runTest {
        oldClip("from-the-old-library.wav")
        val store = FakeStore(StarterLibrary.LIBRARY).apply { setAside.value = true }
        assertEquals(0, sweep(store))
        assertEquals(setOf("from-the-old-library.wav"), files())
    }

    @Test
    fun `nothing is deleted when the library's file couldn't be opened`() = runTest {
        oldClip("mim-take.wav")
        val store = FakeStore(StarterLibrary.LIBRARY).apply { unopened.value = true }
        assertEquals(0, sweep(store))
        assertEquals(setOf("mim-take.wav"), files())
    }

    @Test
    fun `the sweep waits for the library to be read`() = runTest {
        oldClip("deleted-sound.wav")
        val store = FakeStore<Library>(null)
        val sweeping = launch { sweep(store) }
        runCurrent()
        assertEquals(setOf("deleted-sound.wav"), files())
        store.set(recorded)
        sweeping.join()
        assertEquals(emptySet<String>(), files())
    }

    @Test
    fun `clips an import's library backup names are kept`() = runTest {
        oldClip("mim-take.wav")
        oldClip("before-the-import.wav")
        oldClip("deleted-sound.wav")
        val backedUp = setOf(ClipName("before-the-import.wav"))
        assertEquals(1, sweep(store = FakeStore(recorded), backedUp = backedUp))
        assertEquals(setOf("mim-take.wav", "before-the-import.wav"), files())
    }

    @Test
    fun `nothing is deleted when a library backup can't be read`() = runTest {
        oldClip("deleted-sound.wav")
        assertEquals(0, sweep(store = FakeStore(recorded), backedUp = null))
        assertEquals(setOf("deleted-sound.wav"), files())
    }

    @Test
    fun `the library backups beside the library name their clips`() {
        val libraryFile = File(folder.root, "library.json")
        File(folder.root, "library.json.backup-42").writeText(LibraryCodec.encode(recorded))
        File(folder.root, "settings.json.backup-42").writeText("not a library")
        assertEquals(
            setOf(ClipName("mim-take.wav")),
            clipsNamedByBackups(libraryFile = libraryFile),
        )
        File(folder.root, "library.json.backup-43").writeText("#garbled")
        assertNull(clipsNamedByBackups(libraryFile = libraryFile))
    }

    @Test
    fun `clips named by an unreadable library set aside at an earlier start are kept`() =
        runTest {
            oldClip("mim-take.wav")
            oldClip("in-the-set-aside-library.wav")
            oldClip("deleted-sound.wav")
            val libraryFile = File(folder.root, "library.json")
            File(folder.root, "library.json.unreadable-42")
                .writeText("{\"sounds\":[{\"clip\":\"in-the-set-aside-library.wav\"")
            val backedUp = clipsNamedByBackups(libraryFile = libraryFile)
            assertEquals(1, sweep(store = FakeStore(recorded), backedUp = backedUp))
            assertEquals(setOf("mim-take.wav", "in-the-set-aside-library.wav"), files())
        }

    @Test
    fun `an unreadable library with garbage around a clip name still pins it`() {
        val libraryFile = File(folder.root, "library.json")
        File(folder.root, "library.json.unreadable-42")
            .writeText("\u0000#{]]\"take-1.wav\" ~~ ::\u0007")
        assertEquals(
            setOf(ClipName("take-1.wav")),
            clipsNamedByBackups(libraryFile = libraryFile),
        )
    }

    @Test
    fun `clips no library or set-aside copy names are still swept`() = runTest {
        oldClip("mim-take.wav")
        oldClip("named-nowhere.wav")
        val libraryFile = File(folder.root, "library.json")
        File(folder.root, "library.json.unreadable-42").writeText("#garbled, no clips")
        val backedUp = clipsNamedByBackups(libraryFile = libraryFile)
        assertEquals(1, sweep(store = FakeStore(recorded), backedUp = backedUp))
        assertEquals(setOf("mim-take.wav"), files())
    }
}
