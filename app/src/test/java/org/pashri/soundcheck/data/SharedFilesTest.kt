package org.pashri.soundcheck.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedFilesTest {
    @Test
    fun `a file under the limit is read whole`() {
        val bytes = ByteArray(size = 20_000) { it.toByte() }
        val read = readAtMost(input = ByteArrayInputStream(bytes), maxBytes = 65_536)
        assertArrayEquals(bytes, read)
    }

    @Test
    fun `a file exactly at the limit is read`() {
        val bytes = ByteArray(size = 9) { 1 }
        assertArrayEquals(bytes, readAtMost(input = ByteArrayInputStream(bytes), maxBytes = 9))
    }

    @Test
    fun `a file over the limit reads as nothing`() {
        val bytes = ByteArray(size = 10) { 1 }
        assertNull(readAtMost(input = ByteArrayInputStream(bytes), maxBytes = 9))
    }

    @Test
    fun `a picked file is written whole as UTF-8`() {
        val out = ByteArrayOutputStream()
        assertTrue(writeWhole(open = { out }, text = "A\u266d"))
        assertEquals("A\u266d", out.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun `a file that won't open or has no stream isn't written`() {
        assertFalse(writeWhole(open = { throw FileNotFoundException("gone") }, text = "x"))
        assertFalse(writeWhole(open = { throw SecurityException("no") }, text = "x"))
        assertFalse(writeWhole(open = { null }, text = "x"))
    }

    @Test
    fun `a provider that rejects truncating writes isn't written, not a crash`() {
        assertFalse(writeWhole(open = { throw IllegalArgumentException("mode wt") }, text = "x"))
        assertFalse(
            writeWhole(open = { throw UnsupportedOperationException("mode wt") }, text = "x"),
        )
    }
}
