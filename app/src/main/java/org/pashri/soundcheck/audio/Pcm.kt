package org.pashri.soundcheck.audio

import kotlin.math.abs

/**
 * Converts mono audio from one sample rate to another by straight-line interpolation.
 *
 * @param frames the audio.
 * @param fromRate its sample rate.
 * @param toRate the sample rate wanted.
 * @return the converted audio, `frames.size × toRate / fromRate` frames long; a copy when the
 *     rates match.
 * @throws IllegalArgumentException if either rate is not positive.
 */
fun resample(frames: FloatArray, fromRate: Int, toRate: Int): FloatArray {
    require(fromRate > 0 && toRate > 0) { "Sample rates must be positive" }
    if (fromRate == toRate || frames.isEmpty()) return frames.copyOf()
    val length = (frames.size.toLong() * toRate / fromRate).toInt()
    val step = fromRate.toDouble() / toRate
    return FloatArray(length) { valueAt(frames = frames, position = it * step) }
}

/**
 * Cuts the quiet stretches off both ends of a recording.
 *
 * @param frames the audio.
 * @param threshold the level, 0 to 1, below which a frame counts as silence.
 * @param marginFrames how much quiet to keep before the first and after the last loud frame.
 * @return the trimmed audio; empty if every frame is quiet.
 */
fun trimSilence(frames: FloatArray, threshold: Float, marginFrames: Int): FloatArray {
    val first = frames.indexOfFirst { abs(it) >= threshold }
    if (first < 0) return FloatArray(0)
    val last = frames.indexOfLast { abs(it) >= threshold }
    val start = maxOf(0, first - marginFrames)
    val end = minOf(frames.size, last + 1 + marginFrames)
    return frames.copyOfRange(start, end)
}

/**
 * Scales audio so its loudest frame reaches [peak].
 *
 * @param frames the audio.
 * @param peak the level, 0 to 1, for the loudest frame.
 * @return the scaled audio; silence stays silent.
 */
fun normalizePeak(frames: FloatArray, peak: Float): FloatArray {
    val loudest = frames.maxOfOrNull { abs(it) } ?: return FloatArray(0)
    if (loudest == 0f) return frames.copyOf()
    val scale = peak / loudest
    return FloatArray(frames.size) { frames[it] * scale }
}

private fun valueAt(frames: FloatArray, position: Double): Float {
    val index = position.toInt()
    val next = minOf(index + 1, frames.lastIndex)
    val fraction = (position - index).toFloat()
    return frames[index] + (frames[next] - frames[index]) * fraction
}
