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
 * Each Sound's Announcement: its recorded clip when it has one, otherwise the phone's voice
 * reading its current label. A clip is loaded once per recording (by its file name) or per
 * label and kept in one of the engine's [SampleIds.ANNOUNCEMENT_SLOTS] slots; when every
 * slot is taken, the clip used longest ago gives up its slot. A renamed or re-recorded Sound
 * is loaded afresh. A recording that can't be read (its file is missing or damaged) is
 * replaced by the phone's voice, so a Step never loses its Announcement to a bad file.
 *
 * @param output the engine the clips are loaded into.
 * @param speech the phone's voice.
 * @param loadClip reads a recorded clip's audio, or null if it can't be read.
 * @param soundOf a Sound as the library has it now, or null for one that isn't in it.
 */
class LibraryAnnouncements(
    private val output: SoundOutput,
    private val speech: SpeechSynth,
    private val loadClip: suspend (ClipName) -> FloatArray?,
    private val soundOf: (SoundId) -> Sound?,
) : Announcements {
    private val accessOrder = true

    /** Loaded clips by where they came from, least recently used first. */
    private val loaded =
        LinkedHashMap<Source, Loaded>(INITIAL_CAPACITY, LOAD_FACTOR, accessOrder)
    private val freeSlots =
        ArrayDeque((0 until SampleIds.ANNOUNCEMENT_SLOTS).map(SampleIds::announcement))
    private val mutex = Mutex()

    /** Labels a running Programme is sounding now; [giveUpOldest] never picks one of these. */
    private var pinned: Set<String> = emptySet()

    override suspend fun prepare(soundId: SoundId, fallbackLabel: String?): Clip? =
        mutex.withLock {
            val sound = soundOf(soundId)
            val label = sound?.label ?: fallbackLabel ?: return@withLock null
            sound?.clip?.let { recorded(name = it.name, label = label) } ?: spoken(label)
        }

    override fun keep(labels: Set<String>) {
        pinned = labels
    }

    private suspend fun recorded(name: ClipName, label: String): Clip? =
        cached(source = Source.Recorded(name), label = label) { loadClip(name) }

    private suspend fun spoken(label: String): Clip? =
        cached(source = Source.Spoken(label), label = label) { speakTrimmed(label) }

    private suspend fun cached(
        source: Source,
        label: String,
        pcm: suspend () -> FloatArray?,
    ): Clip? {
        loaded[source]?.let { return it.clip }
        val frames = pcm()?.takeIf { it.isNotEmpty() } ?: return null
        val clip = load(frames) ?: return null
        loaded[source] = Loaded(clip = clip, label = label)
        return clip
    }

    private suspend fun speakTrimmed(label: String): FloatArray? {
        val pcm = speech.speak(label) ?: return null
        val margin = msToFrames(TRIM_MARGIN_MS).toInt()
        return trimSilence(frames = pcm, threshold = SILENCE_THRESHOLD, marginFrames = margin)
    }

    private fun load(frames: FloatArray): Clip? {
        val slot = freeSlots.removeFirstOrNull() ?: giveUpOldest() ?: return null
        val stored = try {
            output.loadSample(
                id = slot,
                pcm = normalizePeak(frames = frames, peak = ANNOUNCEMENT_PEAK),
            )
        } catch (error: RuntimeException) {
            freeSlots.addFirst(slot)
            throw error
        }
        if (!stored) {
            freeSlots.addFirst(slot)
            return null
        }
        return Clip(id = slot, lengthFrames = frames.size.toLong())
    }

    /**
     * The slot of the clip used longest ago whose label is not in [pinned].
     *
     * @return the slot, or null if every clip is pinned (nothing safe to overwrite).
     */
    private fun giveUpOldest(): SampleId? {
        val oldest = loaded.entries.firstOrNull { it.value.label !in pinned } ?: return null
        loaded.remove(oldest.key)
        return oldest.value.clip.id
    }

    /** Where a loaded clip came from. */
    private sealed interface Source {
        /** The phone's voice reading [label]. */
        data class Spoken(val label: String) : Source

        /** The recording in file [name]. */
        data class Recorded(val name: ClipName) : Source
    }

    /** A clip in the engine and the label it announces, for pinning. */
    private class Loaded(val clip: Clip, val label: String)

    /** How Announcements are cleaned up. */
    companion object {
        /** The level the loudest moment of an Announcement is scaled to. */
        const val ANNOUNCEMENT_PEAK: Float = 0.7f

        /** Below this level, 0 to 1, a frame at either end of speech counts as silence. */
        const val SILENCE_THRESHOLD: Float = 0.02f

        /** Quiet kept before and after the speech, so soft starts and ends survive. */
        const val TRIM_MARGIN_MS: Long = 20L

        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}
