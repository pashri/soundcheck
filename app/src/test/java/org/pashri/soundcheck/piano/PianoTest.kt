package org.pashri.soundcheck.piano

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.music.Pitch

class PianoTest {
    private val output = FakeSoundOutput(clockMs = { 0L })
    private val source = FakePianoSource()
    private val piano = Piano(source = source, output = output)

    @Test
    fun `thirty samples a minor third apart cover A0 to C8`() {
        assertEquals(30, PianoSamples.ALL.size)
        assertEquals(21, PianoSamples.ALL.first().midi)
        assertEquals(108, PianoSamples.ALL.last().midi)
        assertTrue(PianoSamples.ALL.zipWithNext().all { (low, high) -> high.midi - low.midi == 3 })
        assertEquals("piano/midi-021.wav", PianoSamples.ALL.first().assetPath)
    }

    @Test
    fun `a sampled key plays its own sample with its tuning correction`() {
        assertEquals(PianoKey(id = SampleIds.piano(9), rate = 1f), piano.keyFor(Pitch.parse("C3")))
        val c4 = piano.keyFor(Pitch.parse("C4"))
        assertEquals(SampleIds.piano(13), c4.id)
        assertEquals(0.996540f, c4.rate, 1e-6f)
    }

    @Test
    fun `a key between samples re-pitches the nearest one by a half-step`() {
        val dFlat4 = piano.keyFor(Pitch.parse("D♭4"))
        assertEquals(SampleIds.piano(13), dFlat4.id)
        assertEquals(1.055798f, dFlat4.rate, 1e-6f)
        val d4 = piano.keyFor(Pitch.parse("D4"))
        assertEquals(SampleIds.piano(14), d4.id)
        assertEquals(0.942240f, d4.rate, 1e-6f)
    }

    @Test
    fun `no key on the piano is re-pitched by more than a half-step and its tuning`() {
        (21..108).forEach { midi ->
            val rate = piano.keyFor(Pitch(midi)).rate
            assertTrue("MIDI $midi plays at $rate", rate in 0.9233f..1.0830f)
        }
    }

    @Test
    fun `keys beyond the piano use its end samples`() {
        assertEquals(SampleIds.piano(0), piano.keyFor(Pitch(20)).id)
        assertEquals(SampleIds.piano(29), piano.keyFor(Pitch(110)).id)
    }

    @Test
    fun `loading fills the thirty piano slots once`() = runTest {
        piano.load()
        piano.load()
        assertEquals((0 until 30).map(SampleIds::piano).toSet(), output.loaded.keys)
        assertEquals(30, source.reads)
    }

    @Test
    fun `each slot holds the recording of its own key`() = runTest {
        piano.load()
        assertEquals(60 / 1000f, output.loaded.getValue(SampleIds.piano(13))[0], 0f)
    }
}
