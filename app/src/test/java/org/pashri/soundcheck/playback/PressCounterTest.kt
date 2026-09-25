package org.pashri.soundcheck.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PressCounterTest {
    private val reported = mutableListOf<Int>()

    private fun TestScope.counter(): PressCounter =
        PressCounter(scope = backgroundScope, windowMs = 400, onPresses = { reported += it })

    private fun TestScope.runUntil(ms: Long) {
        advanceTimeBy(ms - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun `one press is reported once the window has passed`() = runTest {
        val counter = counter()
        counter.press()
        runUntil(399)
        assertTrue(reported.isEmpty())
        runUntil(400)
        assertEquals(listOf(1), reported)
    }

    @Test
    fun `two quick presses report two, not one and then one`() = runTest {
        val counter = counter()
        counter.press()
        runUntil(300)
        counter.press()
        runUntil(600)
        assertTrue(reported.isEmpty())
        runUntil(2_000)
        assertEquals(listOf(2), reported)
    }

    @Test
    fun `three quick presses report three`() = runTest {
        val counter = counter()
        counter.press()
        runUntil(250)
        counter.press()
        runUntil(500)
        counter.press()
        runUntil(2_000)
        assertEquals(listOf(3), reported)
    }

    @Test
    fun `presses further apart than the window are separate runs`() = runTest {
        val counter = counter()
        counter.press()
        runUntil(500)
        counter.press()
        runUntil(1_000)
        assertEquals(listOf(1, 1), reported)
    }
}
