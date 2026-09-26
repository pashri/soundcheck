package org.pashri.soundcheck.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * A [MicInput] that, while [toneHz] is set, hears a steady sine instead of the microphone.
 * For screenshots and emulator checks only: the emulator hangs when the real microphone
 * opens. It lives in the debug source set, so a release build never contains it.
 *
 * @param real the microphone to open while no demo tone is set.
 */
class DemoToneMic(private val real: MicInput) : MicInput {
    /** The demo tone's frequency in Hz, or null to listen through [real]. */
    @Volatile
    var toneHz: Double? = null

    override fun open(): MicSession? = toneHz?.let { DemoToneSession(hz = it) } ?: real.open()

    private class DemoToneSession(private val hz: Double) : MicSession {
        private var played = 0L

        override suspend fun read(buffer: FloatArray): Int {
            delay(timeMillis = buffer.size * MS_PER_SECOND / SAMPLE_RATE)
            for (i in buffer.indices) {
                val seconds = (played + i).toDouble() / SAMPLE_RATE
                buffer[i] = (AMPLITUDE * sin(2 * PI * hz * seconds)).toFloat()
            }
            played += buffer.size
            return buffer.size
        }

        override fun close() = Unit
    }

    private companion object {
        const val AMPLITUDE = 0.5
        const val MS_PER_SECOND = 1_000L
    }
}
