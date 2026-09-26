package org.pashri.soundcheck.ui.pattern

import org.pashri.soundcheck.ui.components.usageLabel
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNotation
import org.pashri.soundcheck.warmup.StepRef
import org.pashri.soundcheck.warmup.patternUsage

/** What a new Pattern is called, before "New pattern 2" and so on. */
const val NEW_PATTERN_NAME: String = "New pattern"

/**
 * One Pattern in the Patterns list.
 *
 * @property id the Pattern.
 * @property name e.g. "Triad".
 * @property detail its degrees and Key Chord, e.g. "1 3 5 3 1 · major".
 * @property usage e.g. "Used in 2 Steps".
 * @property chosen whether it is the Step's Pattern, when choosing for a Step.
 */
data class PatternRow(
    val id: PatternId,
    val name: String,
    val detail: String,
    val usage: String,
    val chosen: Boolean,
)

/**
 * Everything the Patterns list shows.
 *
 * @property title "Patterns", or "Choose a Pattern" when choosing for a Step.
 * @property backLabel where back goes: "Warm-up", or "Step".
 * @property countLabel e.g. "8 PATTERNS".
 * @property rows every Pattern, in library order.
 * @property picking whether a tap chooses the Pattern for a Step.
 */
data class PatternListUiState(
    val title: String,
    val backLabel: String,
    val countLabel: String,
    val rows: List<PatternRow>,
    val picking: Boolean,
)

/**
 * The Patterns list.
 *
 * @param library the saved library.
 * @param pickFor the Step to choose a Pattern for, or null to browse.
 * @return what to show.
 */
fun patternListUiState(library: Library, pickFor: StepRef?): PatternListUiState {
    val chosen = pickFor?.let { library.programme(it.programmeId)?.step(it.key)?.patternId }
    val count = library.patterns.size
    return PatternListUiState(
        title = if (pickFor != null) "Choose a Pattern" else "Patterns",
        backLabel = if (pickFor != null) "Step" else "Warm-up",
        countLabel = if (count == 1) "1 PATTERN" else "$count PATTERNS",
        rows = library.patterns.map {
            patternRow(library = library, pattern = it, chosen = chosen)
        },
        picking = pickFor != null,
    )
}

private fun patternRow(library: Library, pattern: Pattern, chosen: PatternId?): PatternRow =
    PatternRow(
        id = pattern.id,
        name = pattern.name,
        detail = "${PatternNotation.degrees(pattern.notes)} · ${pattern.keyChord.label}",
        usage = usageLabel(library.patternUsage(pattern.id)),
        chosen = pattern.id == chosen,
    )
