package org.pashri.soundcheck.warmup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.pashri.soundcheck.audio.SampleId
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.SpeechSynth
import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.audio.normalizePeak
import org.pashri.soundcheck.audio.trimSilence

/**
 * A Sound's Announcement, loaded in the engine and ready to schedule.
 *
 * @property id the sample slot it is loaded in.
 * @property lengthFrames how long it plays.
 */
data class Clip(val id: SampleId, val lengthFrames: Long)

/** Gets each Sound's Announcement ready to play. */
interface Announcements {
    /**
     * Makes sure [soundId]'s Announcement is loaded. Safe to call from several coroutines.
     *
     * @param soundId the Sound to announce.
     * @return its clip, or null if it has none (no working voice, or an unknown Sound).
     */
    suspend fun prepare(soundId: SoundId): Clip?
}

/**
 * Announcements spoken by the phone's voice reading each Sound's current label. A clip is
 * made once per label and kept in one of the engine's [SampleIds.ANNOUNCEMENT_SLOTS] slots;
 * when every slot is taken, the clip used longest ago gives up its slot. A renamed Sound is
 * spoken afresh. Recorded clips replace these in a later plan.
 *
 * @param output the engine the clips are loaded into.
 * @param speech the voice.
 * @param labelOf a Sound's label now, or null for a Sound that isn't in the library.
 */
class SpokenAnnouncements(
    private val output: SoundOutput,
    private val speech: SpeechSynth,
    private val labelOf: (SoundId) -> String?,
) : Announcements {
    /** Clips by label, least recently used first. */
    private val clips = LinkedHashMap<String, Clip>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private val freeSlots =
        ArrayDeque((0 until SampleIds.ANNOUNCEMENT_SLOTS).map(SampleIds::announcement))
    private val mutex = Mutex()

    override suspend fun prepare(soundId: SoundId): Clip? = mutex.withLock {
        val label = labelOf(soundId) ?: return@withLock null
        clips[label] ?: speak(label)?.also { clips[label] = it }
    }

    private suspend fun speak(label: String): Clip? {
        val pcm = speech.speak(label) ?: return null
        val margin = msToFrames(TRIM_MARGIN_MS).toInt()
        val trimmed = trimSilence(pcm, threshold = SILENCE_THRESHOLD, marginFrames = margin)
        if (trimmed.isEmpty()) return null
        val slot = freeSlots.removeFirstOrNull() ?: giveUpOldest()
        output.loadSample(slot, normalizePeak(trimmed, peak = ANNOUNCEMENT_PEAK))
        return Clip(id = slot, lengthFrames = trimmed.size.toLong())
    }

    private fun giveUpOldest(): SampleId {
        val oldest = clips.entries.first()
        clips.remove(oldest.key)
        return oldest.value.id
    }

    /** How Announcements are cleaned up. */
    companion object {
        /** The level the loudest moment of an Announcement is scaled to. */
        const val ANNOUNCEMENT_PEAK: Float = 0.7f

        /** Below this level, 0 to 1, a frame at either end counts as silence. */
        const val SILENCE_THRESHOLD: Float = 0.02f

        /** Quiet kept before and after the speech, so soft starts and ends survive. */
        const val TRIM_MARGIN_MS: Long = 20L

        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}
