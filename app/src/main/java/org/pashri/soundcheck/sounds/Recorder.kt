package org.pashri.soundcheck.sounds

import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pashri.soundcheck.audio.ExclusiveMic
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.MicSession
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.audio.openRetrying

/**
 * Records one take for a Sound's clip: from [start] until [stop], or [MAX_TAKE_MS] at most.
 * Recording is a tool of its own ([Tool.RECORDING]): starting it pauses a playing Programme
 * and stops an audition, and any tool starting after it ends the take and throws it away.
 * It holds transient audio focus while it listens, so a podcast pauses rather than being
 * recorded, and a call ends the take; if focus is refused (a call is on), nothing is
 * recorded and the take is [Take.Interrupted]. The microphone is opened as the take starts
 * and released before the take is handed over. Opening the microphone and trimming the
 * take run on [worker]; call everything else from the main thread.
 *
 * @param mic the microphone, shared with the Tuner through an [ExclusiveMic].
 * @param focus the recorder's own audio focus.
 * @param arbiter keeps one tool sounding or listening at a time.
 * @param scope runs the recording; cancelling it ends the take without handing it over. It
 *     must run started work at once (e.g. `viewModelScope`).
 * @param worker where the microphone is opened and the take trimmed, off the main thread.
 */
class Recorder(
    private val mic: MicInput,
    private val focus: FocusGate,
    private val arbiter: ToolArbiter,
    private val scope: CoroutineScope,
    private val worker: CoroutineDispatcher,
) {
    private val _recording = MutableStateFlow(false)

    /** Whether a take is being recorded. */
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _levels = MutableStateFlow<List<Float>>(emptyList())

    /** How loud the last [LEVEL_COUNT] moments were, 0 to 1, oldest first; empty when idle. */
    val levels: StateFlow<List<Float>> = _levels.asStateFlow()

    private var job: Job? = null
    @Volatile
    private var ending: Ending? = null

    /**
     * Starts a take, unless one is being recorded already.
     *
     * @param onTake gets the take when it ends by [stop], by reaching [MAX_TAKE_MS], or by
     *     being interrupted; never called after [cancel].
     * @return false, changing nothing, if a take is being recorded already.
     */
    fun start(onTake: (Take) -> Unit): Boolean {
        if (_recording.value) return false
        ending = null
        _recording.value = true
        arbiter.claim(tool = Tool.RECORDING, onEvicted = ::interrupt)
        job = scope.launch { onTake(recordUntilEnded()) }
        return true
    }

    /** Ends the take and hands it over. Does nothing when not recording. */
    fun stop() {
        if (_recording.value && ending == null) ending = Ending.STOPPED
    }

    /** Ends the take and throws it away. Safe when not recording. */
    fun cancel() {
        job?.cancel()
        job = null
    }

    private fun interrupt() {
        if (ending == null) ending = Ending.INTERRUPTED
    }

    private suspend fun recordUntilEnded(): Take {
        try {
            if (!focus.acquire(onLost = ::interrupt)) return Take.Interrupted
            val session = openMic() ?: return endedBeforeListening()
            try {
                return listen(session)
            } finally {
                session.close()
            }
        } finally {
            focus.release()
            arbiter.release(Tool.RECORDING)
            _levels.value = emptyList()
            _recording.value = false
        }
    }

    /**
     * Opens the microphone on [worker], giving up as soon as the take ends. A session that
     * opens as the recording is cancelled is closed rather than left open.
     */
    private suspend fun openMic(): MicSession? {
        var opened: MicSession? = null
        try {
            return withContext(context = worker) {
                mic.openRetrying(stillWanted = { ending == null }).also { opened = it }
            }
        } catch (e: CancellationException) {
            opened?.close()
            throw e
        }
    }

    /** What a take that ended before the microphone opened comes to. */
    private fun endedBeforeListening(): Take =
        when (ending) {
            null -> Take.MicUnavailable
            Ending.STOPPED -> Take.TooQuiet
            Ending.INTERRUPTED -> Take.Interrupted
        }

    private suspend fun listen(session: MicSession): Take {
        val heard = FloatArray(size = msToFrames(MAX_TAKE_MS).toInt())
        val chunk = FloatArray(size = CHUNK_FRAMES)
        var size = 0
        while (ending == null && size < heard.size) {
            if (session.read(chunk) < chunk.size) return Take.MicUnavailable
            val count = minOf(a = chunk.size, b = heard.size - size)
            chunk.copyInto(destination = heard, destinationOffset = size, endIndex = count)
            size += count
            _levels.value = (_levels.value + chunk.maxOf { abs(it) }).takeLast(LEVEL_COUNT)
        }
        if (ending == Ending.INTERRUPTED) return Take.Interrupted
        return withContext(context = worker) {
            takeOf(frames = heard.copyOf(size), cutShort = size >= heard.size)
        }
    }

    /** Why a take is ending. */
    private enum class Ending { STOPPED, INTERRUPTED }

    /** Sizes. */
    companion object {
        /** How many recent loudness readings [levels] holds, for the waveform. */
        const val LEVEL_COUNT: Int = 40

        /** Frames read from the microphone at a time, about 21 ms. */
        const val CHUNK_FRAMES: Int = 1_024
    }
}
