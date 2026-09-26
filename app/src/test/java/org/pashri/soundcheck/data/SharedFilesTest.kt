package org.pashri.soundcheck.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
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
}
