package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.audio.SampleId
import org.pashri.soundcheck.audio.SampleIds

/**
 * [Announcements] with a fixed length, handing out slots in the order Sounds are first seen.
 *
 * @param lengthFrames how long every Announcement lasts.
 * @property voice false acts like a phone with no working voice: no Announcements.
 */
class FakeAnnouncements(
    private val lengthFrames: Long = 24_000L,
    var voice: Boolean = true,
) : Announcements {
    private val slots = mutableMapOf<SoundId, SampleId>()
    private val _prepared = mutableSetOf<SoundId>()

    /** Every Sound [prepare] was asked for. */
    val prepared: Set<SoundId> get() = _prepared

    override suspend fun prepare(soundId: SoundId): Clip? {
        _prepared += soundId
        return if (voice) clipOf(soundId) else null
    }

    /**
     * The clip [prepare] returns for [soundId].
     *
     * @param soundId the Sound.
     * @return its clip.
     */
    fun clipOf(soundId: SoundId): Clip =
        Clip(
            id = slots.getOrPut(soundId) { SampleIds.announcement(slots.size) },
            lengthFrames = lengthFrames,
        )
}
