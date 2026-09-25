package org.pashri.soundcheck.audio

/** [SoundOutput] backed by the C++ Oboe engine in `src/main/cpp`. Create one per process. */
class NativeAudioEngine : SoundOutput {
    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    private val handle: Long = nativeCreate()

    override fun start(): Boolean = nativeStart(handle)

    override fun stop() {
        nativeStop(handle)
    }

    override fun loadSample(id: SampleId, pcm: FloatArray): Boolean =
        nativeLoadSample(handle, id.value, pcm)

    override fun schedule(
        id: SampleId,
        frame: Long,
        gain: Float,
        rate: Float,
        lengthFrames: Long,
    ): Boolean = nativeSchedule(
        handle = handle,
        id = id.value,
        frame = frame,
        gain = gain,
        rate = rate,
        lengthFrames = lengthFrames,
    )

    override fun cancelFrom(frame: Long) {
        nativeCancelFrom(handle, frame)
    }

    override fun silence() {
        nativeSilence(handle)
    }

    override fun fadeOut() {
        nativeFadeOut(handle)
    }

    override fun hasFailed(): Boolean = nativeHasFailed(handle)

    override fun framePosition(): Long = nativeFramePosition(handle)

    private external fun nativeCreate(): Long
    private external fun nativeStart(handle: Long): Boolean
    private external fun nativeStop(handle: Long)
    private external fun nativeLoadSample(handle: Long, id: Int, pcm: FloatArray): Boolean
    private external fun nativeSchedule(
        handle: Long,
        id: Int,
        frame: Long,
        gain: Float,
        rate: Float,
        lengthFrames: Long,
    ): Boolean
    private external fun nativeCancelFrom(handle: Long, frame: Long)
    private external fun nativeSilence(handle: Long)
    private external fun nativeFadeOut(handle: Long)
    private external fun nativeHasFailed(handle: Long): Boolean
    private external fun nativeFramePosition(handle: Long): Long

    private companion object {
        const val LIBRARY_NAME = "soundcheck"
    }
}
