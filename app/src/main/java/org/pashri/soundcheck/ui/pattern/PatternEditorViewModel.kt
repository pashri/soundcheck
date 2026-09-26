package org.pashri.soundcheck.ui.pattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.pashri.soundcheck.data.Store
import org.pashri.soundcheck.ui.components.EditorState
import org.pashri.soundcheck.ui.components.readyOrGone
import org.pashri.soundcheck.warmup.Accidental
import org.pashri.soundcheck.warmup.Audition
import org.pashri.soundcheck.warmup.KeyChord
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.NoteLength
import org.pashri.soundcheck.warmup.Pattern
import org.pashri.soundcheck.warmup.PatternId
import org.pashri.soundcheck.warmup.PatternNote
import org.pashri.soundcheck.warmup.WarmupSettings
import org.pashri.soundcheck.warmup.deletePattern
import org.pashri.soundcheck.warmup.lowered
import org.pashri.soundcheck.warmup.patternDemoNotes
import org.pashri.soundcheck.warmup.raised
import org.pashri.soundcheck.warmup.savePattern
import org.pashri.soundcheck.warmup.withNote
import org.pashri.soundcheck.warmup.withNoteCopiedAfter
import org.pashri.soundcheck.warmup.withoutNote

/** What the Pattern editor's controls do. */
interface PatternEditorActions {
    /**
     * Selects a note for the controls to change.
     *
     * @param index its position, from 0.
     */
    fun select(index: Int)

    /** Inserts a copy of the selected note after it and selects the copy. */
    fun addNote()

    /** Deletes the selected note, unless it is the only one. */
    fun deleteNote()

    /** Moves the selected note down a scale degree. */
    fun lowerDegree()

    /** Moves the selected note up a scale degree. */
    fun raiseDegree()

    /**
     * Sets the selected note's accidental.
     *
     * @param accidental ♭, ♮ or ♯.
     */
    fun setAccidental(accidental: Accidental)

    /**
     * Sets the selected note's length.
     *
     * @param length the length.
     */
    fun setLength(length: NoteLength)

    /**
     * Sets the Pattern's Key Chord.
     *
     * @param chord the Key Chord.
     */
    fun setKeyChord(chord: KeyChord)

    /**
     * Renames the Pattern.
     *
     * @param name the new name; the dialog has already checked it.
     */
    fun rename(name: String)

    /** Deletes the Pattern and the Steps that use it; the editor then closes. */
    fun delete()

    /** Plays the Pattern once, or stops it if it is sounding. */
    fun playPattern()

    /** Stops the Pattern playing, as the screen goes away. */
    fun stopAudition()
}

/**
 * Runs one Pattern's editor. Every change is saved at once, and every Step that uses the
 * Pattern follows it.
 *
 * @param patternId the Pattern.
 * @param library the saved library.
 * @param settings the saved settings, for the key the Pattern plays in.
 * @param audition plays the Pattern.
 */
class PatternEditorViewModel(
    private val patternId: PatternId,
    private val library: Store<Library>,
    private val settings: Store<WarmupSettings>,
    private val audition: Audition,
) : ViewModel(), PatternEditorActions {
    private val selected = MutableStateFlow(0)

    /** Whether the Pattern is sounding. */
    val auditioning: StateFlow<Boolean> = audition.playing

    /** What the editor shows. */
    val uiState: StateFlow<EditorState<PatternEditorUiState>> =
        combine(flow = library.data, flow2 = selected) { saved, index ->
            if (saved == null) {
                EditorState.Loading
            } else {
                readyOrGone(
                    patternEditorUiState(library = saved, id = patternId, selected = index),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EditorState.Loading,
        )

    override fun select(index: Int) {
        selected.value = index
    }

    override fun addNote() {
        val index = currentIndex()
        edit { it.withNoteCopiedAfter(index) }
        selected.value = index + 1
    }

    override fun deleteNote() {
        val index = currentIndex()
        edit { it.withoutNote(index) }
    }

    override fun lowerDegree() {
        editNote { it.lowered() }
    }

    override fun raiseDegree() {
        editNote { it.raised() }
    }

    override fun setAccidental(accidental: Accidental) {
        editNote { it.copy(accidental = accidental) }
    }

    override fun setLength(length: NoteLength) {
        editNote { it.copy(length = length) }
    }

    override fun setKeyChord(chord: KeyChord) {
        edit { it.copy(keyChord = chord) }
    }

    override fun rename(name: String) {
        edit { it.copy(name = name.trim()) }
    }

    override fun delete() {
        library.edit { it.deletePattern(patternId) }
    }

    override fun playPattern() {
        if (audition.playing.value) return audition.stop()
        val pattern = library.data.value?.pattern(patternId) ?: return
        val range = settings.data.value?.range ?: return
        audition.play(patternDemoNotes(pattern = pattern, range = range))
    }

    override fun stopAudition() {
        audition.stop()
    }

    /** The selected note's position, kept inside the Pattern as it is now. */
    private fun currentIndex(): Int {
        val notes = library.data.value?.pattern(patternId)?.notes ?: return 0
        return selected.value.coerceIn(minimumValue = 0, maximumValue = notes.lastIndex)
    }

    private fun editNote(change: (PatternNote) -> PatternNote) {
        val index = currentIndex()
        edit { it.withNote(index = index, change = change) }
    }

    private fun edit(change: (Pattern) -> Pattern) {
        library.edit { saved ->
            saved.pattern(patternId)?.let { saved.savePattern(change(it)) } ?: saved
        }
    }

    /**
     * Builds [PatternEditorViewModel]s.
     *
     * @param patternId the Pattern.
     * @param library the saved library.
     * @param settings the saved settings.
     * @param audition plays the Pattern.
     */
    class Factory(
        private val patternId: PatternId,
        private val library: Store<Library>,
        private val settings: Store<WarmupSettings>,
        private val audition: Audition,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PatternEditorViewModel(
            patternId = patternId,
            library = library,
            settings = settings,
            audition = audition,
        ) as T
    }
}
