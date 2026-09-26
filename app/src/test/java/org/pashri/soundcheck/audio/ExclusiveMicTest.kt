package org.pashri.soundcheck.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExclusiveMicTest {
    private val fake = FakeMicInput()
    private val mic = ExclusiveMic(fake)

    @Test
    fun `a second open while one is open gets nothing`() {
        assertNotNull(mic.open())
        assertNull(mic.open())
        assertEquals(1, fake.mostOpenAtOnce)
    }

    @Test
    fun `closing frees the microphone for the next tool`() {
        checkNotNull(mic.open()).close()
        assertNotNull(mic.open())
        assertEquals(2, fake.timesOpened)
        assertEquals(1, fake.mostOpenAtOnce)
    }

    @Test
    fun `closing a session twice frees nothing the second time`() {
        val first = checkNotNull(mic.open())
        first.close()
        checkNotNull(mic.open())
        first.close()
        assertNull(mic.open())
    }

    @Test
    fun `a microphone that won't open doesn't stay taken`() {
        fake.available = false
        assertNull(mic.open())
        fake.available = true
        assertNotNull(mic.open())
    }
}
