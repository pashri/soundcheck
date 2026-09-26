package org.pashri.soundcheck.data

import kotlinx.coroutines.CompletableDeferred
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

    /** Set false to act as if [replace] couldn't write (storage full). */
    var replaces: Boolean = true

    /** Set false to act as if [undoReplace] couldn't put the backup back. */
    var undoes: Boolean = true

    /**
     * Set to make [replace] wait, once it has applied, until this completes: like a real save
     * whose file is written but whose caller is only told afterward.
     */
    var replaceGate: CompletableDeferred<Unit>? = null

    /** Every backup [replace] kept, by stamp, holding the document as it was then. */
    val backups: MutableMap<Long, T?> = mutableMapOf()

    /** The document now; the test fails if it hasn't loaded. */
    val value: T get() = checkNotNull(_data.value)

    override fun edit(change: (T) -> T) {
        _data.value = _data.value?.let(change)
    }

    override suspend fun replace(value: T, stamp: Long): Boolean {
        if (!replaces) return false
        backups[stamp] = _data.value
        _data.value = value
        replaceGate?.await()
        return true
    }

    override suspend fun undoReplace(stamp: Long, previous: T): Boolean {
        if (!undoes) return false
        backups.remove(stamp)
        _data.value = previous
        return true
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
