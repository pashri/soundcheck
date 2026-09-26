package org.pashri.soundcheck.piano

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pashri.soundcheck.audio.SAMPLE_RATE
import org.pashri.soundcheck.audio.WavReader

/**
 * Reads the piano recordings bundled in the app's assets. A missing or wrong file throws:
 * `PianoAssetsTest` guarantees they are there, so a failure here is a broken build.
 *
 * @param assets the app's assets.
 */
class AssetPianoSource(private val assets: AssetManager) : PianoSampleSource {
    override suspend fun read(sample: PianoSample): FloatArray = withContext(Dispatchers.IO) {
        val bytes = assets.open(sample.assetPath).use { it.readBytes() }
        val audio = WavReader.read(bytes)
        check(audio.sampleRate == SAMPLE_RATE) {
            "${sample.assetPath} is ${audio.sampleRate} Hz, not $SAMPLE_RATE"
        }
        audio.frames
    }
}
