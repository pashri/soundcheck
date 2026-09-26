package org.pashri.soundcheck.piano

/** A [PianoSampleSource] whose every sample is four frames of `midi / 1000`. */
class FakePianoSource : PianoSampleSource {
    /** How many samples were read. */
    var reads: Int = 0
        private set

    override suspend fun read(sample: PianoSample): FloatArray {
        reads++
        return FloatArray(4) { sample.midi / 1000f }
    }
}
