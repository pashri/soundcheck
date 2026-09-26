package org.pashri.soundcheck.sounds

import kotlin.math.abs
import org.pashri.soundcheck.audio.SAMPLE_RATE
import org.pashri.soundcheck.audio.framesToMs
import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.audio.normalizePeak
import org.pashri.soundcheck.audio.trimSilence
import org.pashri.soundcheck.warmup.LibraryAnnouncements

/** The longest a recording runs before it stops by itself. */
const val MAX_TAKE_MS: Long = 5_000L

/** How much of the start of a recording is dropped: the press and the microphone starting. */
const val PRESS_SKIP_MS: Long = 50L

/** How much of the end of a recording is dropped: the finger lifting off the button. */
const val RELEASE_SKIP_MS: Long = 100L

/** Below this level, 0 to 1, a whole recording counts as nothing heard. */
const val MIN_PEAK: Float = 0.03f

/** Quiet at either end is anything below this share of the loudest moment... */
const val RELATIVE_THRESHOLD: Float = 0.1f

/** ...and never below this level, so a quiet voice in a quiet room is still trimmed. */
const val MIN_THRESHOLD: Float = 0.01f

/** Quiet kept before and after the word, so soft starts and endings survive. */
const val TAKE_MARGIN_MS: Long = 50L

/** A trimmed recording shorter than this is too short to be a word. */
const val MIN_TAKE_MS: Long = 150L

/** What a recording came to. */
sealed interface Take {
    /**
     * A clip worth keeping: trimmed and levelled to the Announcements' peak.
     *
     * @property pcm mono audio at [SAMPLE_RATE].
     * @property cutShort true if the recording reached [MAX_TAKE_MS] and stopped by itself.
     */
    class Kept(val pcm: FloatArray, val cutShort: Boolean) : Take {
        /** How long it plays, in whole milliseconds. */
        val lengthMs: Long
            get() = framesToMs(frames = pcm.size.toLong())
    }

    /** Nothing loud enough was heard. */
    data object TooQuiet : Take

    /** What was heard was too short to be a word. */
    data object TooShort : Take

    /** The microphone couldn't be opened, or stopped delivering sound. */
    data object MicUnavailable : Take

    /** Another tool, or a call, took over before the recording finished. */
    data object Interrupted : Take
}

/**
 * Turns a raw recording into a [Take]. Drops the first [PRESS_SKIP_MS] and, unless the
 * recording was cut short (the finger was still down), the last [RELEASE_SKIP_MS]; trims
 * the quiet from both ends, quiet being anything below [RELATIVE_THRESHOLD] of the loudest
 * moment and never below [MIN_THRESHOLD], keeping [TAKE_MARGIN_MS] either side; and levels
 * what is left.
 *
 * @param frames the recording, mono at [SAMPLE_RATE].
 * @param cutShort whether it reached [MAX_TAKE_MS] and stopped by itself.
 * @return the clip, or why there isn't one.
 */
fun takeOf(frames: FloatArray, cutShort: Boolean): Take {
    val body = withoutHandling(frames = frames, cutShort = cutShort)
    val peak = body.maxOfOrNull { abs(it) } ?: 0f
    if (peak < MIN_PEAK) return Take.TooQuiet
    val threshold = maxOf(a = peak * RELATIVE_THRESHOLD, b = MIN_THRESHOLD)
    val margin = msToFrames(TAKE_MARGIN_MS).toInt()
    val trimmed = trimSilence(frames = body, threshold = threshold, marginFrames = margin)
    if (trimmed.size < msToFrames(MIN_TAKE_MS)) return Take.TooShort
    val levelled = normalizePeak(frames = trimmed, peak = LibraryAnnouncements.ANNOUNCEMENT_PEAK)
    return Take.Kept(pcm = levelled, cutShort = cutShort)
}

/** [frames] without the press at the start and, unless [cutShort], the release at the end. */
private fun withoutHandling(frames: FloatArray, cutShort: Boolean): FloatArray {
    val start = minOf(a = frames.size, b = msToFrames(PRESS_SKIP_MS).toInt())
    val release = if (cutShort) 0 else msToFrames(RELEASE_SKIP_MS).toInt()
    val end = maxOf(a = start, b = frames.size - release)
    return frames.copyOfRange(fromIndex = start, toIndex = end)
}
