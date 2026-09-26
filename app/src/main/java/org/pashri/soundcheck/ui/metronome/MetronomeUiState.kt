package org.pashri.soundcheck.ui.metronome

import org.pashri.soundcheck.metronome.DEFAULT_ACCENT
import org.pashri.soundcheck.metronome.DEFAULT_BPM
import org.pashri.soundcheck.metronome.TempoMarking

/**
 * Everything the Metronome screen shows.
 *
 * @property bpm the tempo.
 * @property accentEvery beats per bar, or null with the accent off.
 * @property running whether it is clicking.
 * @property paused whether the headphone button or the notification paused it off screen;
 *   Resume starts it again and Stop ends it.
 * @property beatInBar which beat of the bar is sounding, or null when silent.
 * @property beatIndex beats since clicking started, or null when silent; changes on every
 *   beat regardless of accent, so a single notehead can still pulse with the accent off.
 */
data class MetronomeUiState(
    val bpm: Int = DEFAULT_BPM,
    val accentEvery: Int? = DEFAULT_ACCENT,
    val running: Boolean = false,
    val paused: Boolean = false,
    val beatInBar: Int? = null,
    val beatIndex: Long? = null,
) {
    /** The Italian tempo marking, e.g. "Andante". */
    val tempoMarking: String get() = TempoMarking.forBpm(bpm)

    /** How many beat marks to draw: one per beat of the bar, or one with no accent. */
    val beatsInBar: Int get() = accentEvery ?: 1

    /** The big button's label: "Stop" while clicking, "Resume" while paused, else "Start". */
    val startLabel: String
        get() = when {
            running -> "Stop"
            paused -> "Resume"
            else -> "Start"
        }

    /** "PAUSED" under the tempo while paused, else null. */
    val caption: String? get() = "PAUSED".takeIf { paused }

    /** The header note, e.g. "ACCENT / 4". */
    val accentLabel: String get() = accentEvery?.let { "ACCENT / $it" } ?: "NO ACCENT"
}

/** What the Metronome screen's controls do. */
interface MetronomeActions {
    /** One bpm slower. */
    fun slower()

    /** One bpm faster. */
    fun faster()

    /**
     * Sets the tempo, clamped to the allowed range.
     *
     * @param bpm the new tempo.
     */
    fun setBpm(bpm: Int)

    /**
     * Sets the accent.
     *
     * @param every beats per bar, or null for none.
     */
    fun setAccent(every: Int?)

    /** Records a tap-tempo tap. */
    fun tap()

    /** Starts the Metronome if stopped, resumes it if paused, stops it if running. */
    fun toggle()

    /** Ends the Metronome, running or paused. */
    fun stop()
}
