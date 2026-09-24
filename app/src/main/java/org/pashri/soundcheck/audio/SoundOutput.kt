package org.pashri.soundcheck.audio

/** Frames per second of every sound Soundcheck plays; the engine resamples to the device. */
const val SAMPLE_RATE: Int = 48_000

/**
 * Identifies a sample slot in a [SoundOutput].
 *
 * @property value the slot number, 0 to 255.
 */
@JvmInline
value class SampleId(val value: Int)

/** Sample slots reserved by each tool. Slots 0–15 belong to the Metronome. */
object SampleIds {
    /** The Metronome's ordinary click. */
    val METRONOME_CLICK: SampleId = SampleId(0)

    /** The Metronome's accented click. */
    val METRONOME_ACCENT: SampleId = SampleId(1)
}

/**
 * The single audio output every tool plays through.
 *
 * Time is counted in frames at [SAMPLE_RATE]. The count moves only while the output is
 * running and never resets, even when the device output is reopened, so a sound scheduled
 * for a frame always plays at that frame.
 */
interface SoundOutput {
    /**
     * Starts rendering audio.
     *
     * @return whether the device output started.
     */
    fun start(): Boolean

    /** Stops rendering audio and releases the device output. */
    fun stop()

    /**
     * Stores mono PCM at [SAMPLE_RATE] under [id], replacing any earlier sample.
     *
     * @param id the slot to fill.
     * @param pcm samples in the range −1 to 1.
     * @return false if [id] is out of range.
     */
    fun loadSample(id: SampleId, pcm: FloatArray): Boolean

    /**
     * Plays sample [id] starting at [frame]; a frame already rendered plays at once.
     *
     * @param id the sample to play.
     * @param frame the frame to start on.
     * @param gain loudness multiplier, 1 for as recorded.
     * @return false if the engine's queue is full and the sound was dropped.
     */
    fun schedule(id: SampleId, frame: Long, gain: Float): Boolean

    /**
     * Cancels every sound that has not started and starts at or after [frame].
     *
     * @param frame the first frame to cancel from.
     */
    fun cancelFrom(frame: Long)

    /** Stops every playing and scheduled sound at once. */
    fun silence()

    /**
     * The frame the output will render next.
     *
     * @return the running frame count.
     */
    fun framePosition(): Long
}
