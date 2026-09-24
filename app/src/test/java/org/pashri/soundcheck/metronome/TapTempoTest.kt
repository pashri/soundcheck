package org.pashri.soundcheck.metronome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapTempoTest {
    private var now = 0L
    private val tapTempo = TapTempo(clockMs = { now })

    private fun tapAt(vararg times: Long): Int? =
        times.map { now = it; tapTempo.tap() }.last()

    @Test
    fun `a single tap does not set a tempo`() {
        assertNull(tapAt(0))
    }

    @Test
    fun `four taps half a second apart give 120 bpm`() {
        assertEquals(120, tapAt(0, 500, 1_000, 1_500))
    }

    @Test
    fun `a long pause starts counting again`() {
        tapAt(0, 500)
        assertNull(tapAt(4_000))
        assertEquals(120, tapAt(4_500))
    }

    @Test
    fun `only the most recent taps count`() {
        tapAt(0, 1_000, 2_000, 3_000, 4_000)
        assertEquals(120, tapAt(4_500, 5_000, 5_500, 6_000))
    }

    @Test
    fun `tapping very fast is capped at the fastest tempo`() {
        assertEquals(MAX_BPM, tapAt(0, 100, 200, 300))
    }

    @Test
    fun `tapping very slowly is held at the slowest tempo`() {
        assertEquals(MIN_BPM, tapAt(0, 2_400))
    }
}
