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
     * @param fallbackLabel the label to speak if [soundId] is no longer in the library (it
     *     was deleted while a Programme using it kept playing), or null to give up instead.
     * @return its clip, or null if it has none (no working voice, or an unknown Sound).
     */
    suspend fun prepare(soundId: SoundId, fallbackLabel: String? = null): Clip?

    /**
     * Protects the Announcements for [labels] from being given up while every other slot is
     * also protected; call with an empty set to release them. A running Programme calls this
     * with its own Sounds' labels so its Segments' clips are never overwritten mid-playback.
     *
     * @param labels the labels to keep, replacing any kept before.
     */
    fun keep(labels: Set<String>)
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
    private val accessOrder = true

    /** Clips by label, least recently used first. */
    private val clips = LinkedHashMap<String, Clip>(INITIAL_CAPACITY, LOAD_FACTOR, accessOrder)
    private val freeSlots =
        ArrayDeque((0 until SampleIds.ANNOUNCEMENT_SLOTS).map(SampleIds::announcement))
    private val mutex = Mutex()

    /** Labels a running Programme is sounding now; [giveUpOldest] never picks one of these. */
    private var pinned: Set<String> = emptySet()

    override suspend fun prepare(soundId: SoundId, fallbackLabel: String?): Clip? =
        mutex.withLock {
            val label = labelOf(soundId) ?: fallbackLabel ?: return@withLock null
            clips[label] ?: speak(label)?.also { clips[label] = it }
        }

    override fun keep(labels: Set<String>) {
        pinned = labels
    }

    private suspend fun speak(label: String): Clip? {
        val pcm = speech.speak(label) ?: return null
        val margin = msToFrames(TRIM_MARGIN_MS).toInt()
        val trimmed =
            trimSilence(frames = pcm, threshold = SILENCE_THRESHOLD, marginFrames = margin)
        if (trimmed.isEmpty()) return null
        val slot = freeSlots.removeFirstOrNull() ?: giveUpOldest() ?: return null
        val loaded = try {
            output.loadSample(id = slot, pcm = normalizePeak(trimmed, peak = ANNOUNCEMENT_PEAK))
        } catch (error: RuntimeException) {
            freeSlots.addFirst(slot)
            throw error
        }
        if (!loaded) {
            freeSlots.addFirst(slot)
            return null
        }
        return Clip(id = slot, lengthFrames = trimmed.size.toLong())
    }

    /**
     * The slot of the clip used longest ago among those not in [pinned].
     *
     * @return the slot, or null if every clip is pinned (nothing safe to overwrite).
     */
    private fun giveUpOldest(): SampleId? {
        val oldest = clips.entries.firstOrNull { it.key !in pinned } ?: return null
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
