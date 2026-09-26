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
 * Announcements spoken by the phone's voice reading each Sound's label, each loaded once
 * into its own slot. Recorded clips replace these in a later plan.
 *
 * @param output the engine the clips are loaded into.
 * @param speech the voice.
 * @param sounds the Sound library, at most [SampleIds.ANNOUNCEMENT_SLOTS] of them.
 * @throws IllegalArgumentException if there are more Sounds than slots.
 */
class SpokenAnnouncements(
    private val output: SoundOutput,
    private val speech: SpeechSynth,
    sounds: List<Sound>,
) : Announcements {
    init {
        require(sounds.size <= SampleIds.ANNOUNCEMENT_SLOTS) {
            "${sounds.size} Sounds, but only ${SampleIds.ANNOUNCEMENT_SLOTS} slots"
        }
    }

    private val labels: Map<SoundId, String> = sounds.associate { it.id to it.label }
    private val slots: Map<SoundId, SampleId> = sounds.withIndex()
        .associate { (index, sound) -> sound.id to SampleIds.announcement(index) }
    private val clips = mutableMapOf<SoundId, Clip>()
    private val mutex = Mutex()

    override suspend fun prepare(soundId: SoundId): Clip? = mutex.withLock {
        clips[soundId] ?: speak(soundId)?.also { clips[soundId] = it }
    }

    private suspend fun speak(soundId: SoundId): Clip? {
        val label = labels[soundId] ?: return null
        val pcm = speech.speak(label) ?: return null
        val margin = msToFrames(TRIM_MARGIN_MS).toInt()
        val trimmed = trimSilence(pcm, threshold = SILENCE_THRESHOLD, marginFrames = margin)
        if (trimmed.isEmpty()) return null
        val slot = slots.getValue(soundId)
        output.loadSample(slot, normalizePeak(trimmed, peak = ANNOUNCEMENT_PEAK))
        return Clip(id = slot, lengthFrames = trimmed.size.toLong())
    }

    /** How Announcements are cleaned up. */
    companion object {
        /** The level the loudest moment of an Announcement is scaled to. */
        const val ANNOUNCEMENT_PEAK: Float = 0.7f

        /** Below this level, 0 to 1, a frame at either end counts as silence. */
        const val SILENCE_THRESHOLD: Float = 0.02f

        /** Quiet kept before and after the speech, so soft starts and ends survive. */
        const val TRIM_MARGIN_MS: Long = 20L
    }
}
