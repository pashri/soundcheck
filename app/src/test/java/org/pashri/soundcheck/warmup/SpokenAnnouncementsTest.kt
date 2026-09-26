package org.pashri.soundcheck.warmup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.FakeSpeech
import org.pashri.soundcheck.audio.SampleIds

class SpokenAnnouncementsTest {
    private val output = FakeSoundOutput(clockMs = { 0L })
    private val speech = FakeSpeech()
    private val labels: MutableMap<SoundId, String> =
        StarterSounds.ALL.associate { it.id to it.label }.toMutableMap()
    private val announcements =
        SpokenAnnouncements(output = output, speech = speech, labelOf = { labels[it] })

    /** 4 800 silent frames, 2 400 frames at 0.35, then 4 800 silent frames. */
    private val spokenWord = FloatArray(12_000) { if (it in 4_800 until 7_200) 0.35f else 0f }

    @Test
    fun `a Sound's label is spoken, trimmed, levelled and loaded into a slot`() = runTest {
        speech.pcm = spokenWord
        val clip = announcements.prepare(StarterSounds.MIM.id)
        assertEquals(Clip(id = SampleIds.announcement(0), lengthFrames = 4_320), clip)
        assertEquals(listOf("mim"), speech.spoken)
        val loaded = output.loaded.getValue(SampleIds.announcement(0))
        assertEquals(4_320, loaded.size)
        assertEquals(0.7f, loaded.max(), 1e-6f)
    }

    @Test
    fun `each label is spoken only once`() = runTest {
        speech.pcm = spokenWord
        announcements.prepare(StarterSounds.HUM.id)
        announcements.prepare(StarterSounds.HUM.id)
        assertEquals(listOf("hum"), speech.spoken)
    }

    @Test
    fun `with no working voice there is no Announcement and the next try asks again`() =
        runTest {
            assertNull(announcements.prepare(StarterSounds.NEH.id))
            assertNull(announcements.prepare(StarterSounds.NEH.id))
            assertEquals(listOf("neh", "neh"), speech.spoken)
            assertTrue(output.loaded.isEmpty())
        }

    @Test
    fun `a Sound outside the library has no Announcement`() = runTest {
        speech.pcm = spokenWord
        assertNull(announcements.prepare(SoundId("whistle")))
        assertTrue(speech.spoken.isEmpty())
    }

    @Test
    fun `speech that is all silence gives no Announcement`() = runTest {
        speech.pcm = FloatArray(1_000)
        assertNull(announcements.prepare(StarterSounds.OO.id))
    }

    @Test
    fun `every starter Sound gets its own slot`() = runTest {
        speech.pcm = spokenWord
        val ids = StarterSounds.ALL.map { checkNotNull(announcements.prepare(it.id)).id }
        assertEquals(StarterSounds.ALL.size, ids.toSet().size)
    }

    @Test
    fun `a renamed Sound is spoken with its new label`() = runTest {
        speech.pcm = spokenWord
        announcements.prepare(StarterSounds.MIM.id)
        labels[StarterSounds.MIM.id] = "mmm"
        val clip = announcements.prepare(StarterSounds.MIM.id)
        assertEquals(listOf("mim", "mmm"), speech.spoken)
        assertEquals(SampleIds.announcement(1), clip?.id)
    }

    @Test
    fun `with more labels than slots the one used longest ago gives up its slot`() = runTest {
        speech.pcm = spokenWord
        val ids = (0..SampleIds.ANNOUNCEMENT_SLOTS).map { SoundId("s$it") }
        ids.forEachIndexed { index, id -> labels[id] = "word $index" }
        ids.take(SampleIds.ANNOUNCEMENT_SLOTS).forEach { announcements.prepare(it) }
        announcements.prepare(ids[0])
        val clip = announcements.prepare(ids.last())
        assertEquals(SampleIds.announcement(1), clip?.id)
        assertEquals(SampleIds.ANNOUNCEMENT_SLOTS + 1, speech.spoken.size)
        announcements.prepare(ids[1])
        assertEquals(SampleIds.ANNOUNCEMENT_SLOTS + 2, speech.spoken.size)
    }
}
