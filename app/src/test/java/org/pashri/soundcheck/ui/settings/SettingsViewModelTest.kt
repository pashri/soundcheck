package org.pashri.soundcheck.ui.settings

import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.pashri.soundcheck.data.Backup
import org.pashri.soundcheck.data.ExportCodec
import org.pashri.soundcheck.data.ExportRead
import org.pashri.soundcheck.data.FakeSharedFiles
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.data.readExport
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.StarterLibrary
import org.pashri.soundcheck.warmup.StarterPatterns
import org.pashri.soundcheck.warmup.StarterSounds
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.withVoiceType

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeStore(WarmupSettings.DEFAULT)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val library = FakeStore(StarterLibrary.LIBRARY)
    private val files = FakeSharedFiles()
    private val uri = "content://picked/backup.json"
    private val bass = WarmupSettings.DEFAULT.withVoiceType(VoiceType.BASS)

    /** A backup with one Pattern, two Sounds and no Programmes, on Bass. */
    private val theirs = Backup(
        library = Library(
            patterns = listOf(StarterPatterns.TRIAD),
            sounds = listOf(StarterSounds.HUM, StarterSounds.EE),
            programmes = emptyList(),
        ),
        settings = bass,
    )

    private fun viewModel(store: FakeStore<WarmupSettings> = settings): SettingsViewModel =
        SettingsViewModel.Factory(
            settings = store,
            library = library,
            files = files,
            today = { LocalDate.of(2026, 9, 26) },
            clockMs = { 42L },
        ).create(SettingsViewModel::class.java)

    /** Picks [text] as the file to import and waits for it to be read. */
    private fun TestScope.pick(viewModel: SettingsViewModel, text: String): SettingsUiState? {
        files.files[uri] = text
        viewModel.importFrom(uri)
        return state(viewModel)
    }

    private fun TestScope.state(viewModel: SettingsViewModel): SettingsUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `nothing is shown until the settings have loaded`() = runTest(context = dispatcher) {
        assertNull(state(viewModel(store = FakeStore(null))))
    }

    @Test
    fun `picking Bass sets the Bass Range`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.selectVoiceType(VoiceType.BASS)
        assertEquals(VoiceType.BASS.range, settings.value.range)
        assertEquals("E2", state(viewModel)?.lowest)
    }

    @Test
    fun `the note buttons move the Range a half-step at a time`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.lowerLowest()
        viewModel.raiseHighest()
        assertEquals(Pitch.parse("B2"), settings.value.range.lowest)
        assertEquals(Pitch.parse("B♭4"), settings.value.range.highest)
        viewModel.raiseLowest()
        viewModel.lowerHighest()
        assertEquals(VoiceType.TENOR.range, settings.value.range)
    }

    @Test
    fun `the switch turns Play over other audio on and off`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.setPlayOverOtherAudio(true)
        assertTrue(settings.value.playOverOtherAudio)
        assertEquals(true, state(viewModel)?.playOverOtherAudio)
        viewModel.setPlayOverOtherAudio(false)
        assertFalse(settings.value.playOverOtherAudio)
    }

    @Test
    fun `export writes the library and settings to the picked file`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            assertEquals("soundcheck-2026-09-26.json", viewModel.exportFileName())
            viewModel.exportTo(uri)
            val state = state(viewModel)
            val backup =
                Backup(library = StarterLibrary.LIBRARY, settings = WarmupSettings.DEFAULT)
            assertEquals(ExportRead.Valid(backup), readExport(files.files.getValue(uri)))
            assertEquals(
                "Saved a backup of 1 programme · 8 patterns · 8 sounds.",
                state?.backupMessage,
            )
        }

    @Test
    fun `an export that can't be written says so`() = runTest(context = dispatcher) {
        files.writes = false
        val viewModel = viewModel()
        viewModel.exportTo(uri)
        assertEquals(
            "Couldn't save the backup there. Try another place.",
            state(viewModel)?.backupMessage,
        )
    }

    @Test
    fun `a picked backup is asked about, with its counts, before anything changes`() =
        runTest(context = dispatcher) {
            val state = pick(viewModel = viewModel(), text = ExportCodec.encode(theirs))
            assertEquals("0 programmes · 1 pattern · 2 sounds", state?.importQuestion)
            assertEquals(StarterLibrary.LIBRARY, library.value)
            assertEquals(WarmupSettings.DEFAULT, settings.value)
        }

    @Test
    fun `saying yes replaces the library and settings and says so`() =
        runTest(context = dispatcher) {
            val viewModel = viewModel()
            pick(viewModel = viewModel, text = ExportCodec.encode(theirs))
            viewModel.confirmImport()
            val state = state(viewModel)
            assertEquals(theirs.library, library.value)
            assertEquals(bass, settings.value)
            assertNull(state?.importQuestion)
            assertEquals(
                "Imported 0 programmes · 1 pattern · 2 sounds. Your previous library and " +
                    "settings are kept on the phone.",
                state?.backupMessage,
            )
        }

    @Test
    fun `saying no changes nothing`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        pick(viewModel = viewModel, text = ExportCodec.encode(theirs))
        viewModel.cancelImport()
        viewModel.confirmImport()
        val state = state(viewModel)
        assertNull(state?.importQuestion)
        assertNull(state?.backupMessage)
        assertEquals(StarterLibrary.LIBRARY, library.value)
    }

    @Test
    fun `a file that isn't a backup says so and changes nothing`() =
        runTest(context = dispatcher) {
            val state = pick(viewModel = viewModel(), text = "not json")
            assertNull(state?.importQuestion)
            assertEquals(
                "That file isn't a Soundcheck backup, so nothing was changed.",
                state?.backupMessage,
            )
            assertEquals(StarterLibrary.LIBRARY, library.value)
        }

    @Test
    fun `a backup from a newer Soundcheck says to update the app`() =
        runTest(context = dispatcher) {
            val newer = ExportCodec.encode(theirs)
                .replaceFirst(oldValue = "\"version\": 1", newValue = "\"version\": 9")
            assertEquals(
                "That backup is from a newer Soundcheck. Update the app to import it.",
                pick(viewModel = viewModel(), text = newer)?.backupMessage,
            )
        }

    @Test
    fun `a file that can't be read says so`() = runTest(context = dispatcher) {
        val viewModel = viewModel()
        viewModel.importFrom("content://picked/gone.json")
        assertEquals("Couldn't read that file.", state(viewModel)?.backupMessage)
    }

    @Test
    fun `an import that can't be written says nothing changed`() =
        runTest(context = dispatcher) {
            library.replaces = false
            val viewModel = viewModel()
            pick(viewModel = viewModel, text = ExportCodec.encode(theirs))
            viewModel.confirmImport()
            assertEquals(
                "Couldn't import, so nothing was changed. Is the phone's storage full?",
                state(viewModel)?.backupMessage,
            )
            assertEquals(StarterLibrary.LIBRARY, library.value)
        }
}
