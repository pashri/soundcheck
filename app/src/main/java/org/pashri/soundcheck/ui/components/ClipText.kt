package org.pashri.soundcheck.ui.components

import org.pashri.soundcheck.warmup.Sound

/** What a Sound with no recording is announced by: the phone's voice reads its label. */
const val PHONE_VOICE: String = "phone voice"

/**
 * A clip's length to a tenth of a second, as the Sounds design shows it.
 *
 * @param ms the length in milliseconds.
 * @return e.g. "0.6 s".
 */
fun clipLengthLabel(ms: Long): String {
    val tenths = (ms + HALF_A_TENTH_MS) / TENTH_MS
    return "${tenths / TENTHS_PER_SECOND}.${tenths % TENTHS_PER_SECOND} s"
}

/**
 * A clip's length as TalkBack should say it.
 *
 * @param ms the length in milliseconds.
 * @return e.g. "0.6 seconds".
 */
fun spokenClipLength(ms: Long): String = clipLengthLabel(ms).removeSuffix(" s") + " seconds"

/**
 * How a Sound's Announcement sounds, for the Step editor's Sound card.
 *
 * @param sound the Sound, or null if it has gone from the library.
 * @return "your recording · 0.6 s", or "phone voice".
 */
fun announcementDetail(sound: Sound?): String =
    sound?.clip?.let { "your recording · ${clipLengthLabel(it.lengthMs)}" } ?: PHONE_VOICE

private const val TENTH_MS = 100L
private const val HALF_A_TENTH_MS = 50L
private const val TENTHS_PER_SECOND = 10L
