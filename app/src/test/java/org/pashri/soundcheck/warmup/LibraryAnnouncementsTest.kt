package org.pashri.soundcheck.warmup

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.FakeSpeech
import org.pashri.soundcheck.audio.SampleId
import org.pashri.soundcheck.audio.SampleIds
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.data.ClipFiles

class LibraryAnnouncementsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val output = FakeSoundOutput(clockMs = { 0L })
    private val speech = FakeSpeech()
    private val sounds: MutableMap<SoundId, Sound> =
        StarterSounds.ALL.associateBy { it.id }.toMutableMap()
    private var takes = 0
    private val clips: ClipFiles by lazy {
        ClipFiles(
            directory = File(folder.root, "clips"),
            io = Dispatchers.IO,
            newName = { "take-${++takes}" },
        )
    }
    private val announcements: LibraryAnnouncements by lazy { announcementsOn(output) }

    private fun announcementsOn(output: SoundOutput): LibraryAnnouncements =
        LibraryAnnouncements(
            output = output,
            speech = speech,
            loadClip = clips::load,
            soundOf = { sounds[it] },
        )

    /** 4 800 silent frames, 2 400 frames at 0.35, then 4 800 silent frames. */
    private val spokenWord = FloatArray(size = 12_000) {
        if (it in 4_800 until 7_200) 0.35f else 0f
    }

    /** A recording as the Sounds screen keeps it: already trimmed, 3 600 frames. */
    private val recordedWord = FloatArray(size = 3_600) { if (it % 2 == 0) 0.5f else -0.5f }

    private fun rename(id: SoundId, label: String) {
        sounds[id] = sounds.getValue(id).copy(label = label)
    }

    private suspend fun record(id: SoundId, pcm: FloatArray = recordedWord): RecordedClip {
        val clip = RecordedClip(name = clips.save(pcm), lengthMs = pcm.size / 48L)
        sounds[id] = sounds.getValue(id).copy(clip = clip)
        return clip
    }

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
        speech.pcm = FloatArray(size = 1_000)
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
        rename(id = StarterSounds.MIM.id, label = "mmm")
        val clip = announcements.prepare(StarterSounds.MIM.id)
        assertEquals(listOf("mim", "mmm"), speech.spoken)
        assertEquals(SampleIds.announcement(1), clip?.id)
    }

    @Test
    fun `with more labels than slots the one used longest ago gives up its slot`() = runTest {
        speech.pcm = spokenWord
        val ids = (0..SampleIds.ANNOUNCEMENT_SLOTS).map { SoundId("s$it") }
        ids.forEachIndexed { index, id -> sounds[id] = Sound(id = id, label = "word $index") }
        ids.take(SampleIds.ANNOUNCEMENT_SLOTS).forEach { announcements.prepare(it) }
        announcements.prepare(ids[0])
        val clip = announcements.prepare(ids.last())
        assertEquals(SampleIds.announcement(1), clip?.id)
        assertEquals(SampleIds.ANNOUNCEMENT_SLOTS + 1, speech.spoken.size)
        announcements.prepare(ids[1])
        assertEquals(SampleIds.ANNOUNCEMENT_SLOTS + 2, speech.spoken.size)
    }

    @Test
    fun `a kept label's slot is never given up, however many others are prepared`() = runTest {
        speech.pcm = spokenWord
        val kept = checkNotNull(announcements.prepare(StarterSounds.MIM.id))
        announcements.keep(setOf("mim"))
        val loadedBefore = output.loaded.getValue(kept.id)
        val others = (0 until SampleIds.ANNOUNCEMENT_SLOTS + 4).map { SoundId("extra-$it") }
        others.forEachIndexed { index, id -> sounds[id] = Sound(id = id, label = "extra $index") }
        others.forEach { announcements.prepare(it) }
        assertTrue(output.loaded.getValue(kept.id) === loadedBefore)
    }

    @Test
    fun `a Sound's cached Announcement still plays by its fallback label once deleted`() =
        runTest {
            speech.pcm = spokenWord
            val prepared = announcements.prepare(StarterSounds.MIM.id)
            sounds.remove(StarterSounds.MIM.id)
            val clip = announcements.prepare(soundId = StarterSounds.MIM.id, fallbackLabel = "mim")
            assertEquals(prepared, clip)
            assertEquals(listOf("mim"), speech.spoken)
        }

    @Test
    fun `a failed load returns its slot rather than losing it`() = runTest {
        speech.pcm = spokenWord
        val announcements = announcementsOn(FailOnceOutput(output))
        try {
            announcements.prepare(StarterSounds.MIM.id)
        } catch (_: IllegalStateException) {
            // expected: the engine failed to load the first Announcement.
        }
        val clip = announcements.prepare(StarterSounds.HUM.id)
        assertEquals(SampleIds.announcement(0), clip?.id)
    }

    @Test
    fun `a recorded Sound announces its recording instead of the phone's voice`() = runTest {
        speech.pcm = spokenWord
        record(StarterSounds.MIM.id)
        val clip = announcements.prepare(StarterSounds.MIM.id)
        assertEquals(Clip(id = SampleIds.announcement(0), lengthFrames = 3_600), clip)
        assertTrue(speech.spoken.isEmpty())
        assertEquals(0.7f, output.loaded.getValue(SampleIds.announcement(0)).max(), 1e-4f)
    }

    @Test
    fun `a recording is read once, even if its file goes afterwards`() = runTest {
        record(StarterSounds.HUM.id)
        val first = announcements.prepare(StarterSounds.HUM.id)
        File(folder.root, "clips").listFiles()?.forEach { it.delete() }
        assertEquals(first, announcements.prepare(StarterSounds.HUM.id))
        assertTrue(speech.spoken.isEmpty())
    }

    @Test
    fun `a new take is announced in place of the old one`() = runTest {
        record(StarterSounds.MIM.id)
        announcements.prepare(StarterSounds.MIM.id)
        record(id = StarterSounds.MIM.id, pcm = FloatArray(size = 7_200) { 0.25f })
        val clip = announcements.prepare(StarterSounds.MIM.id)
        assertEquals(Clip(id = SampleIds.announcement(1), lengthFrames = 7_200), clip)
    }

    @Test
    fun `a recording that can't be read gives way to the phone's voice`() = runTest {
        speech.pcm = spokenWord
        val missing = RecordedClip(name = ClipName("never-saved.wav"), lengthMs = 600)
        sounds[StarterSounds.NEH.id] = StarterSounds.NEH.copy(clip = missing)
        val clip = announcements.prepare(StarterSounds.NEH.id)
        assertEquals(listOf("neh"), speech.spoken)
        assertEquals(4_320L, clip?.lengthFrames)
    }
}

/** A [SoundOutput] whose first [loadSample] throws, so the caller can prove no slot is lost. */
private class FailOnceOutput(private val delegate: SoundOutput) : SoundOutput by delegate {
    private var failed = false

    override fun loadSample(id: SampleId, pcm: FloatArray): Boolean {
        if (!failed) {
            failed = true
            error("The engine failed to load the sample")
        }
        return delegate.loadSample(id = id, pcm = pcm)
    }
}
