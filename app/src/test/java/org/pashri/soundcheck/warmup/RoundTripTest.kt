package org.pashri.soundcheck.warmup

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.music.Pitch

class RoundTripTest {
    private val tenor = VoiceType.TENOR.range
    private val arpeggio8Hold = pattern("1 3 5 8 8 8 8 5 3 1")
    private val doubleArpeggio = pattern("1 3 5 8 10 12 11 9 7 5 4 2 1")
    private val belowTheRoot = pattern("♭1 1 5 8")
    private val sharpFourToNine = pattern("1 ♯4 5 ♭7 9")
    private val patterns = listOf(arpeggio8Hold, doubleArpeggio, belowTheRoot, sharpFourToNine)
    private val offsets = listOf(
        RangeOffset.NONE,
        RangeOffset(top = 2),
        RangeOffset(bottom = 1, top = -1),
    )

    private fun pattern(notation: String): Pattern = Pattern(
        id = PatternId(notation),
        name = notation,
        notes = PatternNotation.parse(notation),
        keyChord = KeyChord.MAJOR,
    )

    private fun fits(
        pattern: Pattern,
        direction: Direction,
        range: Range = tenor,
        offset: RangeOffset = RangeOffset.NONE,
    ): RoundTrip.Fits {
        val trip = planRoundTrip(
            range = range,
            offset = offset,
            span = pattern.span,
            direction = direction,
        )
        assertTrue("${pattern.name} in $range with $offset gave $trip", trip is RoundTrip.Fits)
        return trip as RoundTrip.Fits
    }

    @Test
    fun `the arpeggio 8-hold from low on a tenor turns at A3 after 19 iterations`() {
        val trip = fits(pattern = arpeggio8Hold, direction = Direction.START_LOW)
        assertEquals(
            listOf(
                "C3", "D♭3", "D3", "E♭3", "E3", "F3", "F♯3", "G3", "A♭3", "A3",
                "A♭3", "G3", "F♯3", "F3", "E3", "E♭3", "D3", "D♭3", "C3",
            ),
            trip.keys.map { it.name },
        )
        assertEquals(Pitch.parse("A3"), trip.turnKey)
        assertEquals(Pitch.parse("C3"), trip.startKey)
    }

    @Test
    fun `starting high begins at the top key and turns at the bottom`() {
        val trip = fits(pattern = doubleArpeggio, direction = Direction.START_HIGH)
        assertEquals(listOf("D3", "D♭3", "C3", "D♭3", "D3"), trip.keys.map { it.name })
        assertEquals(Pitch.parse("C3"), trip.turnKey)
    }

    @Test
    fun `every trip moves a half-step at a time and plays its turn key once`() {
        patterns.forEach { pattern ->
            VoiceType.entries.forEach { voice ->
                Direction.entries.forEach { direction ->
                    val keys = fits(
                        pattern = pattern,
                        direction = direction,
                        range = voice.range,
                    ).keys
                    val label = "${pattern.name} ${voice.label} $direction"
                    assertEquals(label, keys.first(), keys.last())
                    assertEquals(label, 1, keys.size % 2)
                    assertTrue(label, keys.zipWithNext().all { (a, b) -> abs(b - a) == 1 })
                    val turn = keys[keys.size / 2]
                    assertEquals(label, 1, keys.count { it == turn })
                    val extreme = if (direction == Direction.START_LOW) keys.max() else keys.min()
                    assertEquals(label, extreme, turn)
                }
            }
        }
    }

    @Test
    fun `no sung note of any iteration leaves the Range and the trip reaches both edges`() {
        patterns.forEach { pattern ->
            VoiceType.entries.forEach { voice ->
                offsets.forEach { offset ->
                    Direction.entries.forEach { direction ->
                        val effective = requireNotNull(voice.range.offsetBy(offset))
                        val trip = fits(
                            pattern = pattern,
                            direction = direction,
                            range = voice.range,
                            offset = offset,
                        )
                        val sung = trip.keys.flatMap { pattern.pitchesIn(it) }
                        val label = "${pattern.name} ${voice.label} $offset $direction"
                        assertTrue(label, sung.all { it in effective })
                        assertEquals(label, effective.lowest, sung.min())
                        assertEquals(label, effective.highest, sung.max())
                    }
                }
            }
        }
    }

    @Test
    fun `a note below the root starts the trip above the bottom of the Range`() {
        val trip = fits(pattern = belowTheRoot, direction = Direction.START_LOW)
        assertEquals(Pitch.parse("D♭3"), trip.startKey)
        assertEquals(Pitch.parse("A3"), trip.turnKey)
        assertEquals(Pitch.parse("C3"), belowTheRoot.pitchesIn(trip.startKey).min())
    }

    @Test
    fun `a Range exactly as wide as the pattern plays a single iteration`() {
        val exact = Range(lowest = Pitch.parse("C3"), highest = Pitch.parse("G4"))
        Direction.entries.forEach { direction ->
            assertEquals(
                RoundTrip.Fits(keys = listOf(Pitch.parse("C3"))),
                planRoundTrip(
                    range = exact,
                    offset = RangeOffset.NONE,
                    span = doubleArpeggio.span,
                    direction = direction,
                ),
            )
        }
    }

    @Test
    fun `a pattern wider than the Range does not fit`() {
        assertEquals(
            RoundTrip.DoesNotFit(neededHalfSteps = 19, availableHalfSteps = 18),
            planRoundTrip(
                range = tenor,
                offset = RangeOffset(top = -3),
                span = doubleArpeggio.span,
                direction = Direction.START_LOW,
            ),
        )
    }

    @Test
    fun `an offset that closes the Range does not fit`() {
        assertEquals(
            RoundTrip.DoesNotFit(neededHalfSteps = 0, availableHalfSteps = 0),
            planRoundTrip(
                range = tenor,
                offset = RangeOffset(bottom = -11, top = -11),
                span = pattern("1").span,
                direction = Direction.START_HIGH,
            ),
        )
    }

    @Test
    fun `a Range Offset moves where the trip turns`() {
        val trip = fits(
            pattern = arpeggio8Hold,
            direction = Direction.START_LOW,
            offset = RangeOffset(top = 2),
        )
        assertEquals(Pitch.parse("B3"), trip.turnKey)
        assertEquals(23, trip.keys.size)
    }

    @Test
    fun `a Fits with no keys throws`() {
        assertThrows(IllegalArgumentException::class.java) { RoundTrip.Fits(emptyList()) }
    }

    @Test
    fun `a single note so high that no key can reach it does not fit`() {
        val trip = planRoundTrip(
            range = tenor,
            offset = RangeOffset.NONE,
            span = pattern("99").span,
            direction = Direction.START_LOW,
        )
        assertTrue("$trip", trip is RoundTrip.DoesNotFit)
    }

    @Test
    fun `a high-only Pattern whose offset reaches A0 starts on the lowest MIDI key`() {
        val trip = fits(
            pattern = pattern("15"),
            direction = Direction.START_LOW,
            offset = RangeOffset(bottom = 30),
        )
        assertEquals(Pitch(0), trip.startKey)
        assertTrue(trip.keys.all { it.midi >= 0 })
    }
}
