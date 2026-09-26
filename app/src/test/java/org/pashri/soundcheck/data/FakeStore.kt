package org.pashri.soundcheck.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [Store] held in memory: every edit applies at once and nothing is written.
 *
 * @param initial the document, or null to act as if it hasn't loaded yet.
 */
class FakeStore<T : Any>(initial: T?) : Store<T> {
    private val _data = MutableStateFlow(initial)

    override val data: StateFlow<T?> = _data.asStateFlow()

    /** Set to act as if the last save failed. */
    override val saveFailed: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Set to act as if an unreadable file were set aside and replaced by the seed. */
    override val setAside: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Set to act as if an unreadable file couldn't be set aside, so nothing is saved. */
    override val unopened: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** The document now; the test fails if it hasn't loaded. */
    val value: T get() = checkNotNull(_data.value)

    override fun edit(change: (T) -> T) {
        _data.value = _data.value?.let(change)
    }

    /**
     * Replaces the document, as a load finishing or another screen editing would.
     *
     * @param document the new document.
     */
    fun set(document: T) {
        _data.value = document
    }
}
