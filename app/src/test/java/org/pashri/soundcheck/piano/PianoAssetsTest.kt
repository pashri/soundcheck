package org.pashri.soundcheck.piano

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.SAMPLE_RATE
import org.pashri.soundcheck.audio.WavReader

/** Checks the bundled files themselves; unit tests run with the `app` module as their folder. */
class PianoAssetsTest {
    private val assets = File("src/main/assets")

    @Test
    fun `every sampled key has a 48 kHz recording of at least three seconds`() {
        PianoSamples.ALL.forEach { sample ->
            val audio = WavReader.read(File(assets, sample.assetPath).readBytes())
            assertEquals(sample.assetPath, SAMPLE_RATE, audio.sampleRate)
            assertTrue(sample.assetPath, audio.frames.size >= 3 * SAMPLE_RATE)
        }
    }

    @Test
    fun `the bundled piano stays within 15 MB`() {
        val bytes = PianoSamples.ALL.sumOf { File(assets, it.assetPath).length() }
        assertTrue("$bytes bytes", bytes <= 15L * 1024 * 1024)
    }

    @Test
    fun `the credits ship with the samples`() {
        val credits = File(assets, "piano/CREDITS.txt").readText()
        assertTrue(credits.contains("Alexander Holm"))
        assertTrue(credits.contains("Creative Commons Attribution 3.0"))
    }
}
