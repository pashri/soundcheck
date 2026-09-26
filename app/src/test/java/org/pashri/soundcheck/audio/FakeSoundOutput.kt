package org.pashri.soundcheck.audio

/**
 * A [SoundOutput] that records what would play, with frames driven by a test clock.
 *
 * @param clockMs the test's virtual time in milliseconds.
 */
class FakeSoundOutput(private val clockMs: () -> Long) : SoundOutput {
    /**
     * One scheduled sound.
     *
     * @property id the sample.
     * @property frame its start frame.
     * @property gain its loudness.
     * @property rate its playback speed.
     * @property lengthFrames how long it is held, or [WHOLE_SAMPLE].
     */
    data class Scheduled(
        val id: SampleId,
        val frame: Long,
        val gain: Float,
        val rate: Float = 1f,
        val lengthFrames: Long = WHOLE_SAMPLE,
    )

    private val _scheduled = mutableListOf<Scheduled>()
    private val _loaded = mutableMapOf<SampleId, FloatArray>()
    private val _lateSchedules = mutableListOf<Scheduled>()
    private var framesBeforeStart = 0L
    private var startedAtMs = 0L

    /** Every sound scheduled and not cancelled, in the order scheduled. */
    val scheduled: List<Scheduled> get() = _scheduled

    /** Every sample loaded, by slot. */
    val loaded: Map<SampleId, FloatArray> get() = _loaded

    /** Sounds scheduled for a frame the output had already played. */
    val lateSchedules: List<Scheduled> get() = _lateSchedules

    /** Whether the output is running. */
    var running: Boolean = false
        private set

    /** What the next [start] returns; set false to simulate a device that won't open. */
    var startResult: Boolean = true

    /**
     * Set true to act like an output that failed to start or closed and could not reopen;
     * [start] clears it. Setting it true freezes [framePosition] at its current value, like
     * [stop], but leaves [running] alone.
     */
    var failed: Boolean = false
        set(value) {
            if (value && !field) framesBeforeStart = framePosition()
            field = value
        }

    /** How many times [fadeOut] was called. */
    var fadeOuts: Int = 0
        private set

    override fun start(): Boolean {
        if (!startResult) {
            failed = true
            return false
        }
        if (running && !failed) return true
        startedAtMs = clockMs()
        running = true
        failed = false
        return true
    }

    override fun stop() {
        framesBeforeStart = framePosition()
        running = false
    }

    override fun loadSample(id: SampleId, pcm: FloatArray): Boolean {
        _loaded[id] = pcm
        return true
    }

    override fun schedule(
        id: SampleId,
        frame: Long,
        gain: Float,
        rate: Float,
        lengthFrames: Long,
    ): Boolean {
        val scheduled = Scheduled(
            id = id,
            frame = frame,
            gain = gain,
            rate = rate,
            lengthFrames = lengthFrames,
        )
        if (frame < framePosition()) _lateSchedules.add(scheduled)
        return _scheduled.add(scheduled)
    }

    override fun cancelFrom(frame: Long) {
        _scheduled.removeAll { it.frame >= frame }
    }

    override fun silence() {
        _scheduled.clear()
    }

    override fun fadeOut() {
        fadeOuts++
        val now = framePosition()
        _scheduled.removeAll { it.frame >= now }
    }

    override fun hasFailed(): Boolean = failed

    override fun framePosition(): Long =
        if (running && !failed) {
            framesBeforeStart + msToFrames(clockMs() - startedAtMs)
        } else {
            framesBeforeStart
        }
}
