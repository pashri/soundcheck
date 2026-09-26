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
 * Clips named by a library copy an import kept, or by an unreadable one set aside at an
 * earlier start, are kept too, so a copy put back by hand still has its recordings; if one
 * of those copies can't be read, nothing is deleted.
 * Files made since [startedAtMs] are kept, so a take recorded meanwhile is safe.
 *
 * @param library the saved library.
 * @param readFromFile whether the library's file existed when the app started.
 * @param clips the clips folder.
 * @param startedAtMs when the app started, in milliseconds since the epoch.
 * @param backedUp the clips the kept library copies name, or null if one can't be read.
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

/** What an unreadable saved file is renamed with when it's set aside: ".unreadable-<ms>". */
internal const val UNREADABLE_MARK: String = ".unreadable-"

/**
 * Every clip named by the copies of the library kept beside [libraryFile]: those an import
 * kept ("library.json.backup-<ms>") and those set aside as unreadable at any start
 * ("library.json.unreadable-<ms>"). A set-aside copy usually can't be decoded, so its raw
 * text is searched for clip names instead.
 *
 * @param libraryFile the saved library's file.
 * @return the clips those copies name, or null if one of them can't be read (or a backup
 *   can't be understood).
 */
fun clipsNamedByBackups(libraryFile: File): Set<ClipName>? {
    val directory = libraryFile.absoluteFile.parentFile ?: return emptySet()
    val backup = "${libraryFile.name}$BACKUP_MARK"
    val unreadable = "${libraryFile.name}$UNREADABLE_MARK"
    val copies = directory.listFiles().orEmpty()
    return copies.flatMap { copy ->
        when {
            copy.name.startsWith(backup) -> clipNamesIn(copy) ?: return null
            copy.name.startsWith(unreadable) -> clipNamesMentionedIn(copy) ?: return null
            else -> emptySet()
        }
    }.toSet()
}

private fun clipNamesIn(copy: File): Set<ClipName>? =
    try {
        LibraryCodec.decode(copy.readText()).clipNames()
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

private fun clipNamesMentionedIn(copy: File): Set<ClipName>? =
    try {
        ClipName.findIn(text = copy.readText())
    } catch (e: IOException) {
        null
    }
