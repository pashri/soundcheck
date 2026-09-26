package org.pashri.soundcheck.audio

/** Frames per second of every sound Soundcheck plays; the engine resamples to the device. */
const val SAMPLE_RATE: Int = 48_000

/** A [SoundOutput.schedule] length that plays the whole sample. */
const val WHOLE_SAMPLE: Long = 0L

/**
 * Identifies a sample slot in a [SoundOutput].
 *
 * @property value the slot number, 0 to 255.
 */
@JvmInline
value class SampleId(val value: Int)

/**
 * Sample slots reserved by each tool: 0–15 the Metronome, 16–47 Announcements (shared
 * least-recently-used by label, one Sound's clip per slot, up to [ANNOUNCEMENT_SLOTS] at
 * once) and 64–95 the piano.
 */
object SampleIds {
    /** The Metronome's ordinary click. */
    val METRONOME_CLICK: SampleId = SampleId(0)

    /** The Metronome's accented click. */
    val METRONOME_ACCENT: SampleId = SampleId(1)

    /** How many Sounds can have an Announcement loaded. */
    const val ANNOUNCEMENT_SLOTS: Int = 32

    /** How many piano samples there is room for. */
    const val PIANO_SLOTS: Int = 32

    /**
     * One of the shared Announcement slots, handed out least-recently-used by label rather
     * than fixed to a Sound.
     *
     * @param index the slot's position among the Announcement slots, from 0.
     * @return its slot.
     * @throws IllegalArgumentException if [index] is outside 0 until [ANNOUNCEMENT_SLOTS].
     */
    fun announcement(index: Int): SampleId {
        require(value = index in 0 until ANNOUNCEMENT_SLOTS) { "No Announcement slot $index" }
        return SampleId(FIRST_ANNOUNCEMENT + index)
    }

    /**
     * The slot for a piano sample.
     *
     * @param index the sample's position, lowest key first, from 0.
     * @return its slot.
     * @throws IllegalArgumentException if [index] is outside 0 until [PIANO_SLOTS].
     */
    fun piano(index: Int): SampleId {
        require(value = index in 0 until PIANO_SLOTS) { "No piano slot $index" }
        return SampleId(FIRST_PIANO + index)
    }

    private const val FIRST_ANNOUNCEMENT = 16
    private const val FIRST_PIANO = 64
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
     * Starts rendering audio. Starting a running output does nothing.
     *
     * @return whether the device output is running.
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
     * Plays sample [id] starting at [frame]; a frame already rendered plays at once. The
     * sound waits without using a voice until its frame arrives.
     *
     * @param id the sample to play.
     * @param frame the frame to start on.
     * @param gain loudness multiplier, 1 for as recorded.
     * @param rate playback speed: 1 as recorded, 2 an octave higher; see
     *     [org.pashri.soundcheck.music.frequencyRatio].
     * @param lengthFrames frames to hold it before it fades out over 100 ms, or
     *     [WHOLE_SAMPLE] to play it to its end.
     * @return false if the engine's queue is full and the sound was dropped. A full pending
     *     list (256 sounds already waiting to start) drops it too, silently, even though this
     *     still returns true.
     */
    fun schedule(
        id: SampleId,
        frame: Long,
        gain: Float,
        rate: Float = 1f,
        lengthFrames: Long = WHOLE_SAMPLE,
    ): Boolean

    /**
     * Cancels every sound that has not started and starts at or after [frame].
     *
     * @param frame the first frame to cancel from.
     */
    fun cancelFrom(frame: Long)

    /** Stops every playing and scheduled sound at once. */
    fun silence()

    /** Fades every playing sound out over 100 ms and drops every sound not yet started. */
    fun fadeOut()

    /**
     * Whether the output failed to start, or closed (headphones unplugged, Bluetooth gone)
     * and could not be reopened. The next successful [start] clears it.
     *
     * @return true while the output is silently down.
     */
    fun hasFailed(): Boolean

    /**
     * The frame the output will render next.
     *
     * @return the running frame count.
     */
    fun framePosition(): Long
}
