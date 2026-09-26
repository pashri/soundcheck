package org.pashri.soundcheck.data

import kotlinx.coroutines.flow.StateFlow

/** One saved document, shown at once when it changes and saved on the phone. */
interface Store<T : Any> {
    /** The document, or null until it has been read from the phone. */
    val data: StateFlow<T?>

    /** True when the latest change couldn't be saved, until a later save succeeds. */
    val saveFailed: StateFlow<Boolean>

    /** True once an unreadable saved document has been set aside and replaced by the seed. */
    val setAside: StateFlow<Boolean>

    /**
     * True when an unreadable saved document couldn't even be set aside: it is left as it is,
     * the seed is shown, and no change is saved.
     */
    val unopened: StateFlow<Boolean>

    /**
     * Changes the document. The change shows in [data] at once and is saved in the
     * background. Call from the main thread; a change before the document is read is dropped.
     *
     * @param change turns the document into its new version.
     */
    fun edit(change: (T) -> T)
}
