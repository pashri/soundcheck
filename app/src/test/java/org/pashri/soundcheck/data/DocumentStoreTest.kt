package org.pashri.soundcheck.data

import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** One line per item; a document starting with "#" can't be read. */
private object LinesCodec : TextCodec<List<String>> {
    override fun encode(value: List<String>): String = value.joinToString(separator = "\n")

    override fun decode(text: String): List<String> {
        require(!text.startsWith("#")) { "Unreadable: $text" }
        return if (text.isEmpty()) emptyList() else text.split("\n")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var seeds = 0
    private var now = 1_000L

    private val file: File get() = File(folder.root, "doc.json")

    private fun aside(at: Long): File = File(folder.root, "doc.json.unreadable-$at")

    private fun TestScope.store(): DocumentStore<List<String>> = DocumentStore(
        file = file,
        codec = LinesCodec,
        seed = {
            seeds++
            listOf("seed")
        },
        scope = this,
        io = StandardTestDispatcher(testScheduler),
        clockMs = { now },
    )

    private fun TestScope.loaded(): DocumentStore<List<String>> =
        store().also {
            it.load()
            advanceUntilIdle()
        }

    @Test
    fun `nothing is shown until the file is read`() = runTest {
        val store = store()
        assertNull(store.data.value)
        store.load()
        advanceUntilIdle()
        assertEquals(listOf("seed"), store.data.value)
    }

    @Test
    fun `with no file the seed is shown and saved`() = runTest {
        loaded()
        assertEquals("seed", file.readText())
        assertEquals(1, seeds)
    }

    @Test
    fun `an existing file is read and the seed is not used`() = runTest {
        file.writeText("mine\nyours")
        assertEquals(listOf("mine", "yours"), loaded().data.value)
        assertEquals(0, seeds)
    }

    @Test
    fun `a document emptied by the user stays empty after a restart`() = runTest {
        val first = loaded()
        first.edit { emptyList() }
        advanceUntilIdle()
        val second = loaded()
        assertEquals(emptyList<String>(), second.data.value)
        assertEquals(1, seeds)
    }

    @Test
    fun `an edit shows at once and is saved`() = runTest {
        val store = loaded()
        store.edit { it + "more" }
        assertEquals(listOf("seed", "more"), store.data.value)
        advanceUntilIdle()
        assertEquals("seed\nmore", file.readText())
    }

    @Test
    fun `quick edits leave the newest on disk`() = runTest {
        val store = loaded()
        store.edit { it + "1" }
        store.edit { it + "2" }
        advanceUntilIdle()
        assertEquals("seed\n1\n2", file.readText())
    }

    @Test
    fun `edits before the file is read are ignored`() = runTest {
        val store = store()
        store.edit { listOf("early") }
        store.load()
        advanceUntilIdle()
        assertEquals(listOf("seed"), store.data.value)
    }

    @Test
    fun `an unreadable file is set aside with the time and the seed takes its place`() = runTest {
        file.writeText("#garbled")
        assertEquals(listOf("seed"), loaded().data.value)
        assertEquals("#garbled", aside(at = 1_000L).readText())
        assertEquals("seed", file.readText())
    }

    @Test
    fun `every unreadable file is kept`() = runTest {
        file.writeText("#first")
        loaded()
        file.writeText("#second")
        now = 2_000L
        loaded()
        assertEquals("#first", aside(at = 1_000L).readText())
        assertEquals("#second", aside(at = 2_000L).readText())
    }

    @Test
    fun `a file that can't be read is set aside too`() = runTest {
        file.mkdir()
        assertEquals(listOf("seed"), loaded().data.value)
        assertTrue(aside(at = 1_000L).isDirectory)
        assertEquals("seed", file.readText())
    }

    @Test
    fun `a file that can't be set aside is never overwritten`() = runTest {
        file.writeText("#garbled")
        aside(at = 1_000L).writeText("older")
        val store = loaded()
        assertEquals(listOf("seed"), store.data.value)
        assertTrue(store.saveFailed.value)
        store.edit { it + "more" }
        advanceUntilIdle()
        assertEquals("#garbled", file.readText())
        assertEquals("older", aside(at = 1_000L).readText())
        assertTrue(store.saveFailed.value)
    }

    @Test
    fun `saving leaves no temporary file behind`() = runTest {
        loaded().edit { it + "more" }
        advanceUntilIdle()
        assertFalse(File(folder.root, "doc.json.tmp").exists())
        assertEquals(listOf("doc.json"), folder.root.list()?.toList())
    }

    @Test
    fun `a failed save is reported and the next edit saves again`() = runTest {
        val store = loaded()
        val blocker = File(folder.root, "doc.json.tmp").apply { mkdir() }
        store.edit { it + "kept" }
        advanceUntilIdle()
        assertTrue(store.saveFailed.value)
        assertEquals("seed", file.readText())
        blocker.delete()
        store.edit { it + "again" }
        advanceUntilIdle()
        assertFalse(store.saveFailed.value)
        assertEquals("seed\nkept\nagain", file.readText())
    }
}
