package org.pashri.soundcheck.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningAdviceTest {
    @Test
    fun `the reading counts cents flat or sharp`() {
        val expected = mapOf(
            0 to "spot on",
            -1 to "1 cent flat",
            1 to "1 cent sharp",
            -4 to "4 cents flat",
            23 to "23 cents sharp",
            -50 to "50 cents flat",
            50 to "50 cents sharp",
        )
        expected.forEach { (cents, text) ->
            assertEquals("$cents", text, TuningAdvice.reading(cents))
        }
    }

    @Test
    fun `each band of advice starts and ends where it should`() {
        val expected = mapOf(
            0 to "in tune, hold it there",
            -2 to "in tune, hold it there",
            2 to "in tune, hold it there",
            -3 to "a touch low, nearly there",
            -4 to "a touch low, nearly there",
            -10 to "a touch low, nearly there",
            3 to "a touch high, nearly there",
            10 to "a touch high, nearly there",
            -11 to "low, bring it up",
            -25 to "low, bring it up",
            11 to "high, bring it down",
            25 to "high, bring it down",
            -26 to "very low, bring it up",
            -50 to "very low, bring it up",
            26 to "very high, bring it down",
            50 to "very high, bring it down",
        )
        expected.forEach { (cents, text) ->
            assertEquals("$cents", text, TuningAdvice.advice(cents))
        }
    }

    @Test
    fun `flat is always called low and sharp is always called high`() {
        (-50..50).forEach { cents ->
            val advice = TuningAdvice.advice(cents)
            assertEquals("$cents: $advice", cents < -2, "low" in advice)
            assertEquals("$cents: $advice", cents > 2, "high" in advice)
            assertTrue("$cents: $advice", ("in tune" in advice) == (cents in -2..2))
        }
    }
}
