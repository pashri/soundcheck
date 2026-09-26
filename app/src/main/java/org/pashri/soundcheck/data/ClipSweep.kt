package org.pashri.soundcheck.data

import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.pashri.soundcheck.warmup.ClipName
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.clipNames

/** What an import adds to a saved file's name for the copy it keeps: ".backup-<ms>". */
internal const val BACKUP_MARK: String = ".backup-"

/**
 * Deletes the clip files the saved library no longer uses (a deleted Sound's, a replaced
 * take's), once the library has been read. It runs only when the library came from its own
 * file: never on a fresh install's starter kit, never after an unreadable file was set aside
 * (its clips may still be wanted by hand), and never when that file couldn't be opened.
 * Clips named by a library copy an import kept are kept too, so a copy put back by hand
 * still has its recordings; if one of those copies can't be read, nothing is deleted.
 * Files made since [startedAtMs] are kept, so a take recorded meanwhile is safe.
 *
 * @param library the saved library.
 * @param readFromFile whether the library's file existed when the app started.
 * @param clips the clips folder.
 * @param startedAtMs when the app started, in milliseconds since the epoch.
 * @param backedUp the clips the import copies name, or null if one can't be read.
 * @return how many files were deleted.
 */
suspend fun sweepUnusedClips(
    library: Store<Library>,
    readFromFile: Boolean,
    clips: ClipFiles,
    startedAtMs: Long,
    backedUp: suspend () -> Set<ClipName>?,
): Int {
    val loaded = library.data.filterNotNull().first()
    if (!readFromFile || library.setAside.value || library.unopened.value) return 0
    val pinned = backedUp() ?: return 0
    return clips.sweep(keep = loaded.clipNames() + pinned, before = startedAtMs)
}

/**
 * Every clip named by the copies of the library an import kept beside [libraryFile]
 * ("library.json.backup-<ms>").
 *
 * @param libraryFile the saved library's file.
 * @return the clips those copies name, or null if one of them can't be read or understood.
 */
fun clipsNamedByBackups(libraryFile: File): Set<ClipName>? {
    val directory = libraryFile.absoluteFile.parentFile ?: return emptySet()
    val prefix = "${libraryFile.name}$BACKUP_MARK"
    val copies = directory.listFiles().orEmpty().filter { it.name.startsWith(prefix) }
    return copies.flatMap { copy -> clipNamesIn(copy) ?: return null }.toSet()
}

private fun clipNamesIn(copy: File): Set<ClipName>? =
    try {
        LibraryCodec.decode(copy.readText()).clipNames()
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
