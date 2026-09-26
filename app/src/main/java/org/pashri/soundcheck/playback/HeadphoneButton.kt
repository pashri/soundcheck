package org.pashri.soundcheck.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.audio.Tool
import org.pashri.soundcheck.audio.ToolArbiter
import org.pashri.soundcheck.warmup.WarmupController

/** Which tool the headphone button steers. */
enum class PressTarget {
    /** The Warm-up: 1 press pauses or resumes, 2 go to the next Step, 3 to the previous. */
    WARM_UP,

    /** The Metronome: 1 press starts or stops it. */
    METRONOME,
}

/**
 * Which tool a run of headphone presses goes to: the one sounding now, else the last to
 * start, as long as it can take presses (the Metronome while its screen shows, the Warm-up
 * while a Programme is loaded); otherwise the other one, if it can.
 *
 * @param current the tool holding the sound slot now, or null.
 * @param last the tool that claimed the slot most recently, or null.
 * @param metronomeShown whether the Metronome's screen is showing.
 * @param programmeLoaded whether a Programme is playing or paused.
 * @return the target, or null when neither can take presses.
 */
fun pressTarget(
    current: Tool?,
    last: Tool?,
    metronomeShown: Boolean,
    programmeLoaded: Boolean,
): PressTarget? {
    val metronome = PressTarget.METRONOME.takeIf { metronomeShown }
    val warmUp = PressTarget.WARM_UP.takeIf { programmeLoaded }
    return if ((current ?: last) == Tool.METRONOME) metronome ?: warmUp else warmUp ?: metronome
}

/**
 * Where the one media session's headphone and car buttons go: counts presses with a
 * [PressCounter] and hands each run to the tool [pressTarget] picks. The Metronome offers
 * itself while its screen shows. Next and previous keys only ever reach the Warm-up. Call
 * from the main thread.
 *
 * @param arbiter says which tool sounds now and which started last.
 * @param warmup the Warm-up.
 * @param scope runs the press counting and [needed].
 * @param windowMs the longest gap between presses of one run.
 */
class HeadphoneButton(
    private val arbiter: ToolArbiter,
    private val warmup: WarmupController,
    scope: CoroutineScope,
    windowMs: Long = PressCounter.WINDOW_MS,
) {
    private val metronome = MutableStateFlow<(() -> Unit)?>(null)
    private val counter = PressCounter(scope = scope, windowMs = windowMs, onPresses = ::onPresses)

    /** Whether anything can take presses now, so the media session should exist. */
    val needed: StateFlow<Boolean> = combine(
        flow = warmup.playback,
        flow2 = metronome,
    ) { playback, toggle ->
        playback != null || toggle != null
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = warmup.playback.value != null,
    )

    /** One press of a play/pause-type button; a run of presses acts once it is over. */
    fun press() {
        counter.press()
    }

    /**
     * A next-track key: the next Step whenever a Programme is loaded, whichever tool has the
     * play/pause button (the Warm-up then takes the sound slot, as any start does).
     */
    fun next() {
        warmup.next()
    }

    /**
     * A previous-track key: the previous Step whenever a Programme is loaded, whichever tool
     * has the play/pause button.
     */
    fun previous() {
        warmup.previous()
    }

    /**
     * The Metronome's screen is showing, so one press can start or stop it.
     *
     * @param toggle starts the Metronome if it is stopped and stops it if it is running.
     */
    fun offerMetronome(toggle: () -> Unit) {
        metronome.value = toggle
    }

    /**
     * The Metronome's screen has gone, so presses no longer reach it.
     *
     * @param toggle the function [offerMetronome] was given; any other is ignored.
     */
    fun withdrawMetronome(toggle: () -> Unit) {
        if (metronome.value === toggle) metronome.value = null
    }

    private fun onPresses(count: Int) {
        when (target()) {
            PressTarget.WARM_UP -> warmup.onPresses(count)
            PressTarget.METRONOME -> if (count == 1) metronome.value?.invoke()
            null -> Unit
        }
    }

    private fun target(): PressTarget? = pressTarget(
        current = arbiter.current,
        last = arbiter.last,
        metronomeShown = metronome.value != null,
        programmeLoaded = warmup.playback.value != null,
    )
}
