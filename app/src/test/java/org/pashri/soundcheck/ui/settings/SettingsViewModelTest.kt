package org.pashri.soundcheck.ui.settings

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
import org.pashri.soundcheck.data.FakeStore
import org.pashri.soundcheck.music.Pitch
import org.pashri.soundcheck.warmup.VoiceType
import org.pashri.soundcheck.warmup.WarmupSettings

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

    private fun viewModel(store: FakeStore<WarmupSettings> = settings): SettingsViewModel =
        SettingsViewModel.Factory(settings = store).create(SettingsViewModel::class.java)

    private fun TestScope.state(viewModel: SettingsViewModel): SettingsUiState? {
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `nothing is shown until the settings have loaded`() = runTest(dispatcher) {
        assertNull(state(viewModel(store = FakeStore(null))))
    }

    @Test
    fun `picking Bass sets the Bass Range`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.selectVoiceType(VoiceType.BASS)
        assertEquals(VoiceType.BASS.range, settings.value.range)
        assertEquals("E2", state(viewModel)?.lowest)
    }

    @Test
    fun `the note buttons move the Range a half-step at a time`() = runTest(dispatcher) {
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
    fun `the switch turns Play over other audio on and off`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPlayOverOtherAudio(true)
        assertTrue(settings.value.playOverOtherAudio)
        assertEquals(true, state(viewModel)?.playOverOtherAudio)
        viewModel.setPlayOverOtherAudio(false)
        assertFalse(settings.value.playOverOtherAudio)
    }
}
