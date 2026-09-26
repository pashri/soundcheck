package org.pashri.soundcheck.warmup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.pashri.soundcheck.audio.FakeFocusGate
import org.pashri.soundcheck.audio.FakeSoundOutput
import org.pashri.soundcheck.audio.FocusGate
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.piano.FakePianoSource
import org.pashri.soundcheck.piano.Piano

/**
 * A [WarmupController] on fakes, running on the test's scheduler, for view model tests.
 *
 * @param focus the audio focus it asks for.
 * @return the controller.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.testController(focus: FocusGate = FakeFocusGate()): WarmupController {
    val output = FakeSoundOutput(clockMs = { testScheduler.currentTime })
    val player = ProgrammePlayer(
        output = output,
        piano = Piano(source = FakePianoSource(), output = output),
        announcements = FakeAnnouncements(),
        scope = backgroundScope,
    )
    return WarmupController(
        player = player,
        focus = focus,
        arbiter = ToolArbiter(),
        scope = backgroundScope,
    )
}

/**
 * An [Audition] on fakes, running on the test's scheduler, for view model tests.
 *
 * @param focus the audio focus it asks for.
 * @param arbiter the tool slot it takes.
 * @return the audition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.testAudition(
    focus: FocusGate = FakeFocusGate(),
    arbiter: ToolArbiter = ToolArbiter(),
): Audition {
    val output = FakeSoundOutput(clockMs = { testScheduler.currentTime })
    return Audition(
        output = output,
        piano = Piano(source = FakePianoSource(), output = output),
        focus = focus,
        arbiter = arbiter,
        scope = backgroundScope,
    )
}
