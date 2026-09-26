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

    /**
     * Swaps the whole document for [value], first keeping a copy of the saved file as
     * "<file>.backup-[stamp]". The new document shows only once it is saved.
     *
     * @param value the new document.
     * @param stamp names the backup, e.g. the time in ms; [undoReplace] needs the same one.
     * @return false, with the saved document and what is shown unchanged, if the backup or
     *     the new document couldn't be written, a backup with this stamp already exists, or
     *     the saved file must never be overwritten.
     */
    suspend fun replace(value: T, stamp: Long): Boolean

    /**
     * Takes back a [replace]: moves its backup back over the file (a rename, so it needs no
     * free space) and shows [previous] again.
     *
     * @param stamp the stamp [replace] was given.
     * @param previous the document shown before the replace.
     * @return false if no [replace] with this stamp succeeded, or its backup couldn't be put
     *     back; the file is then left as it is.
     */
    suspend fun undoReplace(stamp: Long, previous: T): Boolean
}
