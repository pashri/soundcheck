package org.pashri.soundcheck.piano

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.pashri.soundcheck.audio.SampleId
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.music.CENTS_PER_SEMITONE
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.music.frequencyRatio

/**
 * A key to strike: which sample to play and how fast.
 *
 * @property id the sample's slot.
 * @property rate playback speed; above 1 raises the pitch.
 */
data class PianoKey(val id: SampleId, val rate: Float)

/** Supplies the bundled piano's recordings. */
interface PianoSampleSource {
    /**
     * Reads one recording.
     *
     * @param sample which one.
     * @return mono PCM at the engine's sample rate.
     */
    suspend fun read(sample: PianoSample): FloatArray
}

/**
 * The sampled grand piano. Keys that weren't recorded play the nearest recording,
 * re-pitched by at most a half-step.
 *
 * @param source where the recordings come from.
 * @param output the engine they are loaded into.
 */
class Piano(private val source: PianoSampleSource, private val output: SoundOutput) {
    private val mutex = Mutex()
    private var loaded = false

    /** Loads every recording into the engine; later calls return at once. */
    suspend fun load() {
        mutex.withLock {
            if (loaded) return
            PianoSamples.ALL.forEachIndexed { index, sample ->
                output.loadSample(SampleIds.piano(index), source.read(sample))
            }
            loaded = true
        }
    }

    /**
     * How to play [pitch].
     *
     * @param pitch the key; keys beyond A0–C8 use the end recordings.
     * @return the recording and playback rate that sound [pitch] in tune.
     */
    fun keyFor(pitch: Pitch): PianoKey {
        val index = nearestIndex(pitch.midi)
        val sample = PianoSamples.ALL[index]
        val halfSteps = pitch.midi - sample.midi + sample.tuneCents / CENTS_PER_SEMITONE
        return PianoKey(id = SampleIds.piano(index), rate = frequencyRatio(halfSteps).toFloat())
    }

    private fun nearestIndex(midi: Int): Int =
        Math.floorDiv(midi - PianoSamples.LOWEST_MIDI + 1, PianoSamples.SPACING)
            .coerceIn(0, PianoSamples.ALL.lastIndex)
}
