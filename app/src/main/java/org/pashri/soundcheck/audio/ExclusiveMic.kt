package org.pashri.soundcheck.audio

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

/**
 * Lets only one [MicSession] be open at a time, whichever tool asks (the Tuner or the
 * Sounds recorder): a second [open] while one is open gets null instead of a second
 * recorder on the same microphone. Closing the session frees the microphone for the next.
 *
 * @param mic the real microphone.
 */
class ExclusiveMic(private val mic: MicInput) : MicInput {
    private val busy = AtomicBoolean(false)

    override fun open(): MicSession? {
        if (!busy.compareAndSet(false, true)) return null
        var session: MicSession? = null
        try {
            session = mic.open()
        } finally {
            if (session == null) busy.set(false)
        }
        return session?.let(::OnlySession)
    }

    /** The one open session; closing it a second time frees nothing. */
    private inner class OnlySession(private val session: MicSession) : MicSession {
        private val closed = AtomicBoolean(false)

        override suspend fun read(buffer: FloatArray): Int = session.read(buffer)

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            session.close()
            busy.set(false)
        }
    }
}

/** How many times [openRetrying] tries the microphone. */
const val MIC_OPEN_TRIES: Int = 5

/** The wait between [openRetrying]'s tries; five tries span 400 ms. */
const val MIC_OPEN_RETRY_MS: Long = 100L

/**
 * Opens the microphone, trying again for a moment in case the other tool that listens (the
 * Tuner or the Sounds recorder) is still letting go of it.
 *
 * @param stillWanted checked before each try; once it says false, no more tries are made.
 * @return an open session, or null if every try failed or the microphone stopped being wanted.
 */
suspend fun MicInput.openRetrying(stillWanted: () -> Boolean = { true }): MicSession? {
    repeat(times = MIC_OPEN_TRIES) { attempt ->
        if (!stillWanted()) return null
        open()?.let { return it }
        if (attempt < MIC_OPEN_TRIES - 1) delay(MIC_OPEN_RETRY_MS)
    }
    return null
}
