package org.pashri.soundcheck.tuner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pashri.soundcheck.audio.MicInput
import org.pashri.soundcheck.audio.MicSession

/** What the microphone is doing. */
enum class MicStatus {
    /** Not listening. */
    Off,

    /** Listening. */
    Listening,

    /** Couldn't open the microphone, or it stopped delivering sound. */
    Unavailable,
}

/**
 * What the Tuner hears.
 *
 * @property mic what the microphone is doing.
 * @property note the note being heard, or null when there is none to show.
 */
data class TunerState(val mic: MicStatus = MicStatus.Off, val note: NoteReading? = null)

/**
 * Listens to the microphone and keeps [state] up to date with the note heard.
 *
 * Reads [HOP_SIZE] samples at a time into a sliding [WINDOW_SIZE] window, finds its pitch
 * with [PitchDetector] and steadies it with [PitchSmoother]. Call [start] and [stop] from
 * one thread; the listening itself runs on [worker].
 *
 * The window, hop buffer and NSDF are allocated once per session. Each hop still
 * allocates small objects (boxed pitch results, the published TunerState), and
 * publishing briefly takes StateFlow's internal lock. The loop never logs or does
 * file I/O.
 *
 * @param mic the microphone.
 * @param scope owns the listening loop; cancelling it stops listening.
 * @param worker where reading and pitch detection run, off the main thread.
 */
class Tuner(
    private val mic: MicInput,
    private val scope: CoroutineScope,
    private val worker: CoroutineDispatcher,
) {
    private val _state = MutableStateFlow(TunerState())

    /** The microphone's status and the note heard. */
    val state: StateFlow<TunerState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Starts listening. Does nothing if already listening. After a [stop], the new session
     * waits for the old one to release the microphone, so two are never open at once. That
     * wait can't itself be cut short by a further stop: it always runs to completion before
     * the microphone opens again.
     */
    fun start() {
        if (job?.isActive == true) return
        if (_state.value.mic == MicStatus.Unavailable) _state.value = TunerState()
        val previous = job
        job = scope.launch(worker) {
            withContext(NonCancellable) { previous?.join() }
            ensureActive()
            listen()
        }
    }

    /** Stops listening and releases the microphone. Safe to call when not listening. */
    fun stop() {
        job?.cancel()
        _state.value = TunerState()
    }

    private suspend fun listen() {
        val session = mic.open()
        if (session == null) {
            _state.value = TunerState(mic = MicStatus.Unavailable)
            return
        }
        _state.value = TunerState(mic = MicStatus.Listening)
        try {
            hearUntilSilenced(session)
            _state.value = TunerState(mic = MicStatus.Unavailable)
        } catch (e: CancellationException) {
            _state.value = TunerState()
            throw e
        } finally {
            session.close()
        }
    }

    /** Returns only if the microphone stops delivering; cancellation ends it otherwise. */
    private suspend fun hearUntilSilenced(session: MicSession) {
        val detector = PitchDetector()
        val smoother = PitchSmoother()
        val window = FloatArray(WINDOW_SIZE)
        val hop = FloatArray(HOP_SIZE)
        while (session.read(hop) == HOP_SIZE) {
            slide(window, hop)
            val midi = smoother.next(detector.detect(window))
            currentCoroutineContext().ensureActive()
            _state.value = TunerState(mic = MicStatus.Listening, note = midi?.let(NoteReading::of))
        }
    }

    private fun slide(window: FloatArray, hop: FloatArray) {
        window.copyInto(window, destinationOffset = 0, startIndex = hop.size)
        hop.copyInto(window, destinationOffset = window.size - hop.size)
    }
}
