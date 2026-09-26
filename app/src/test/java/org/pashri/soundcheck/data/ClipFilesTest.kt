package org.pashri.soundcheck.data

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pashri.soundcheck.warmup.ClipName

class ClipFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var names = 0

    private fun clips(directory: File = File(folder.root, "clips")): ClipFiles =
        ClipFiles(directory = directory, io = Dispatchers.IO, newName = { "take-${++names}" })

    private val word = FloatArray(size = 4_800) { if (it % 2 == 0) 0.5f else -0.25f }

    /** Makes a file in the clips folder that looks as if it was written before [time]. */
    private fun oldFile(name: String, time: Long = 1_000L): File =
        File(folder.root, "clips/$name").apply {
            parentFile?.mkdirs()
            writeText("x")
            setLastModified(time)
        }

    @Test
    fun `a saved clip reads back as it was recorded, with nothing left over`() = runTest {
        val clips = clips()
        val name = clips.save(word)
        assertEquals(ClipName("take-1.wav"), name)
        assertArrayEquals(word, clips.load(name), 1e-4f)
        assertEquals(listOf("take-1.wav"), File(folder.root, "clips").list()?.toList())
    }

    @Test
    fun `each take gets its own file, so a new take never overwrites the last`() = runTest {
        val clips = clips()
        val first = clips.save(word)
        val second = clips.save(FloatArray(size = 480) { 0.1f })
        assertNotEquals(first, second)
        assertArrayEquals(word, clips.load(first), 1e-4f)
    }

    @Test
    fun `a missing or damaged clip reads as nothing`() = runTest {
        val clips = clips()
        assertNull(clips.load(ClipName("never-saved.wav")))
        oldFile(name = "broken.wav")
        assertNull(clips.load(ClipName("broken.wav")))
    }

    @Test
    fun `a clip that can't be written says so and leaves nothing behind`() = runTest {
        val blocked = folder.newFile("clips")
        var failed = false
        try {
            clips(directory = blocked).save(word)
        } catch (e: IOException) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(listOf("clips"), folder.root.list()?.toList())
    }

    @Test
    fun `the sweep deletes only old clips nothing uses and old temporary files`() = runTest {
        oldFile(name = "kept.wav")
        oldFile(name = "unused.wav")
        oldFile(name = "half-written.wav.tmp")
        oldFile(name = "notes.txt")
        oldFile(name = "new.wav", time = 5_000L)
        val deleted = clips().sweep(keep = setOf(ClipName("kept.wav")), before = 2_000L)
        assertEquals(2, deleted)
        assertEquals(
            setOf("kept.wav", "notes.txt", "new.wav"),
            File(folder.root, "clips").list()?.toSet(),
        )
    }

    @Test
    fun `sweeping before any clip was saved deletes nothing`() = runTest {
        assertEquals(0, clips().sweep(keep = emptySet(), before = Long.MAX_VALUE))
    }
}
