package org.pashri.soundcheck.data

import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.WarmupSettings

/** How an import went. */
enum class ImportOutcome {
    /** The library and the settings are the backup's now. */
    IMPORTED,

    /** Nothing changed: a document couldn't be written, or hadn't been read yet. */
    NOTHING_CHANGED,

    /**
     * The library is the backup's but the settings aren't: the settings couldn't be
     * written, and the old library couldn't be put back either.
     */
    LIBRARY_ONLY,
}

/**
 * Replaces the library and the settings with a backup's, all or nothing as far as the phone
 * allows. Each document's file is first copied to "<file>.backup-[stamp]". The library is
 * written first; if the settings then can't be written, the library's backup is renamed back
 * (a rename needs no free space), so nothing has changed. Only if that rename fails too does
 * the library stay imported without the settings, and the outcome says so.
 *
 * Every Sound whose id is still in the library keeps the recording it has now; a Sound new to
 * the library has none, so the phone's voice reads it. Recordings of Sounds the backup
 * doesn't have are named by the library's "<file>.backup-[stamp]" copy, so the sweep keeps
 * them and a copy put back by hand keeps its recordings; they stay on the phone until the
 * copies are deleted.
 *
 * @param backup what to import.
 * @param library the saved library.
 * @param settings the saved settings.
 * @param stamp names the backups, e.g. the time in ms.
 * @return how it went.
 */
suspend fun importBackup(
    backup: Backup,
    library: Store<Library>,
    settings: Store<WarmupSettings>,
    stamp: Long,
): ImportOutcome {
    val oldLibrary = library.data.value ?: return ImportOutcome.NOTHING_CHANGED
    if (settings.data.value == null) return ImportOutcome.NOTHING_CHANGED
    val incoming = backup.library.keepingClipsOf(oldLibrary)
    if (!library.replace(value = incoming, stamp = stamp)) return ImportOutcome.NOTHING_CHANGED
    if (settings.replace(value = backup.settings, stamp = stamp)) return ImportOutcome.IMPORTED
    val undone = library.undoReplace(stamp = stamp, previous = oldLibrary)
    return if (undone) ImportOutcome.NOTHING_CHANGED else ImportOutcome.LIBRARY_ONLY
}

/**
 * This library with each Sound given the recording the Sound with its id has in [current].
 *
 * @param current the library as it is now.
 * @return the library with recordings matched by Sound id.
 */
fun Library.keepingClipsOf(current: Library): Library =
    copy(sounds = sounds.map { it.copy(clip = current.sound(it.id)?.clip) })
