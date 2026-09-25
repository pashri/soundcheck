package org.pashri.soundcheck.audio

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.pashri.soundcheck.tuner.HOP_SIZE

/**
 * A [MicInput] that plays queued test audio, one hop per [HOP_MS] of virtual time, and
 * silence once the queue runs dry. Like AudioRecord's blocking read, a read in progress
 * can't be interrupted: cancelling the reader takes effect when the hop arrives.
 */
class FakeMicInput : MicInput {
    private val queued = ArrayDeque<FloatArray>()
    private var broken = false

    /** Whether [open] succeeds; set false to act like a refused microphone. */
    var available: Boolean = true

    /** How many times the microphone was opened. */
    var timesOpened: Int = 0
        private set

    /** How many sessions are open now. */
    var openNow: Int = 0
        private set

    /** The most sessions that were ever open at the same time. */
    var mostOpenAtOnce: Int = 0
        private set

    override fun open(): MicSession? {
        if (!available) return null
        timesOpened++
        openNow++
        mostOpenAtOnce = maxOf(mostOpenAtOnce, openNow)
        return Session()
    }

    /**
     * Queues audio for the microphone to hear.
     *
     * @param signal samples; its length must be a whole number of [HOP_SIZE] hops.
     */
    fun play(signal: FloatArray) {
        require(signal.size % HOP_SIZE == 0) { "play whole hops" }
        for (start in signal.indices step HOP_SIZE) {
            queued.addLast(signal.copyOfRange(start, start + HOP_SIZE))
        }
    }

    /** Makes every later read fail, like a microphone taken away mid-capture. */
    fun breakMic() {
        broken = true
    }

    /** Called once at the end of each [Session.read], after it checks cancellation. */
    var onHop: (() -> Unit)? = null

    private inner class Session : MicSession {
        private var closed = false

        override suspend fun read(buffer: FloatArray): Int {
            withContext(NonCancellable) { delay(HOP_MS) }
            currentCoroutineContext().ensureActive()
            onHop?.invoke()
            if (broken) return -1
            val next = queued.removeFirstOrNull()
            if (next == null) buffer.fill(0f) else next.copyInto(buffer)
            return buffer.size
        }

        override fun close() {
            if (closed) return
            closed = true
            openNow--
        }
    }

    /** Timing constants. */
    companion object {
        /** Virtual milliseconds per hop, near the real 21.3 ms. */
        const val HOP_MS: Long = 21
    }
}
