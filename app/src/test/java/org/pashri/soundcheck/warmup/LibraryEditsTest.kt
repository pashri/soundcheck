package org.pashri.soundcheck.warmup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryEditsTest {
    private val library = StarterLibrary.LIBRARY
    private val starter = StarterProgrammes.SAVED_WARM_UP
    private val morning = ProgrammeId("morning")

    private fun keys(of: Library, id: ProgrammeId = starter.id): List<String> =
        checkNotNull(of.programme(id)).steps.map { it.key.value }

    @Test
    fun `a new Programme is empty, trimmed and last`() {
        val edited = library.addProgramme(id = morning, name = "  Morning ")
        assertEquals(
            SavedProgramme(id = morning, name = "Morning", steps = emptyList()),
            edited.programmes.last(),
        )
        assertEquals(2, edited.programmes.size)
    }

    @Test
    fun `renaming a Programme trims the name and keeps its Steps`() {
        val edited = library.renameProgramme(id = starter.id, name = " Evening ")
        assertEquals("Evening", edited.programme(starter.id)?.name)
        assertEquals(starter.steps, edited.programme(starter.id)?.steps)
    }

    @Test
    fun `deleting a Programme leaves the Patterns and Sounds`() {
        val edited = library.deleteProgramme(starter.id)
        assertTrue(edited.programmes.isEmpty())
        assertEquals(StarterPatterns.ALL, edited.patterns)
        assertEquals(StarterSounds.ALL, edited.sounds)
    }

    @Test
    fun `a new Step uses the first Pattern and Sound at 90 bpm, low, no offset, guided`() {
        val expected = SavedStep(
            key = StepKey("k"),
            patternId = StarterPatterns.FIVE_NOTE_SCALE.id,
            soundId = StarterSounds.LIP_TRILL.id,
            bpm = 90,
            direction = Direction.START_LOW,
        )
        assertEquals(expected, library.newStep(StepKey("k")))
    }

    @Test
    fun `with no Sounds there is no new Step`() {
        val bare = Library(
            patterns = StarterPatterns.ALL,
            sounds = emptyList(),
            programmes = emptyList(),
        )
        assertNull(bare.newStep(StepKey("k")))
    }

    @Test
    fun `a Step is added at the end, changed and removed by its key`() {
        val step = checkNotNull(library.newStep(StepKey("new")))
        val ref = StepRef(programmeId = starter.id, key = step.key)
        val added = library.addStep(programmeId = starter.id, step = step)
        assertEquals("new", keys(added).last())
        val faster = added.updateStep(ref) { it.copy(bpm = 150) }
        assertEquals(150, faster.programme(starter.id)?.step(step.key)?.bpm)
        assertEquals(starter.steps, faster.programme(starter.id)?.steps?.dropLast(1))
        assertEquals(keys(library), keys(faster.removeStep(ref)))
    }

    @Test
    fun `moving a Step reorders the Programme and keeps the keys`() {
        val moved = library.moveStep(programmeId = starter.id, from = 0, to = 2)
        assertEquals(
            listOf("starter-2", "starter-3", "starter-1", "starter-4", "starter-5", "starter-6"),
            keys(moved),
        )
        val back = moved.moveStep(programmeId = starter.id, from = 2, to = 0)
        assertEquals(keys(library), keys(back))
    }

    @Test
    fun `a move outside the list changes nothing`() {
        assertEquals(library, library.moveStep(programmeId = starter.id, from = 0, to = 6))
        assertEquals(library, library.moveStep(programmeId = starter.id, from = -1, to = 0))
    }

    @Test
    fun `a new Pattern comes last and a saved one replaces the old`() {
        val fresh = newPattern(id = PatternId("p"), name = "New pattern")
        val added = library.addPattern(fresh)
        assertEquals(fresh, added.patterns.last())
        val renamed = fresh.copy(name = "Sigh")
        assertEquals(renamed, added.savePattern(renamed).pattern(fresh.id))
        assertEquals(9, added.savePattern(renamed).patterns.size)
    }

    @Test
    fun `deleting a Pattern removes the Steps that use it`() {
        val edited = library.deletePattern(StarterPatterns.TRIAD.id)
        assertNull(edited.pattern(StarterPatterns.TRIAD.id))
        assertEquals(
            listOf("starter-1", "starter-3", "starter-4", "starter-5", "starter-6"),
            keys(edited),
        )
    }

    @Test
    fun `the last Pattern can't be deleted`() {
        val one = Library(
            patterns = listOf(StarterPatterns.TRIAD),
            sounds = StarterSounds.ALL,
            programmes = emptyList(),
        )
        assertEquals(one, one.deletePattern(StarterPatterns.TRIAD.id))
    }

    @Test
    fun `deleting a Sound removes the Steps that use it`() {
        val edited = library.deleteSound(StarterSounds.HUM.id)
        assertNull(edited.sound(StarterSounds.HUM.id))
        assertEquals(
            listOf("starter-1", "starter-3", "starter-4", "starter-5", "starter-6"),
            keys(edited),
        )
    }

    @Test
    fun `the last Sound can't be deleted`() {
        val one = Library(
            patterns = StarterPatterns.ALL,
            sounds = listOf(StarterSounds.HUM),
            programmes = emptyList(),
        )
        assertEquals(one, one.deleteSound(StarterSounds.HUM.id))
    }

    @Test
    fun `a Sound can be added, and renaming one trims it and keeps its Steps`() {
        val added = library.addSound(Sound(id = SoundId("s"), label = "vroom"))
        assertEquals("vroom", added.sounds.last().label)
        val renamed = library.renameSound(id = StarterSounds.MIM.id, label = " mmm ")
        assertEquals("mmm", renamed.sound(StarterSounds.MIM.id)?.label)
        assertEquals(starter.steps, renamed.programme(starter.id)?.steps)
    }

    @Test
    fun `usage counts the Steps in each Programme that use a Pattern or a Sound`() {
        val step = SavedStep(
            key = StepKey("m1"),
            patternId = StarterPatterns.TRIAD.id,
            soundId = StarterSounds.HUM.id,
            bpm = 90,
            direction = Direction.START_LOW,
        )
        val edited = library.addProgramme(id = morning, name = "Morning")
            .addStep(programmeId = morning, step = step)
            .addStep(programmeId = morning, step = step.copy(key = StepKey("m2")))
        val expected = listOf(
            Usage(programmeName = "Starter warm-up", steps = 1),
            Usage(programmeName = "Morning", steps = 2),
        )
        assertEquals(expected, edited.patternUsage(StarterPatterns.TRIAD.id))
        assertEquals(expected, edited.soundUsage(StarterSounds.HUM.id))
        assertEquals(emptyList<Usage>(), edited.soundUsage(StarterSounds.EE.id))
    }

    @Test
    fun `moved moves one item and keeps the rest in order`() {
        val letters = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", "d", "b", "c"), letters.moved(from = 3, to = 1))
        assertEquals(listOf("b", "a"), listOf("a", "b").moved(from = 0, to = 1))
    }
}
