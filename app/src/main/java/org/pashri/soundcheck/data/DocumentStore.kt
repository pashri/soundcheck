package org.pashri.soundcheck.data

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Turns a saved document into text and back. */
interface TextCodec<T> {
    /**
     * Writes [value] as text.
     *
     * @param value the document.
     * @return its text.
     */
    fun encode(value: T): String

    /**
     * Reads a document.
     *
     * @param text what [encode] wrote.
     * @return the document.
     * @throws IllegalArgumentException if [text] is not a document this build can read.
     */
    fun decode(text: String): T
}

/** How every saved document is written: indented, so the file can be read by hand. */
internal val DocumentJson: Json = Json { prettyPrint = true }

/**
 * One document kept in memory and saved to [file]. The first [load] reads the file, or saves
 * [seed] if there is no file yet, so the seed is written once per install. A file that can't
 * be read or understood is moved to "[file].unreadable-<time in ms>" (every one is kept) and
 * replaced by the seed; if it can't even be moved, the seed is shown but nothing is saved,
 * so the file is never overwritten. Every save writes the whole document to a temporary
 * file and renames it over [file], so the file is always either the old document or the
 * new one. The temporary file is synced to storage before the rename, so a power cut leaves
 * either the old or the new document. [replace] keeps a synced copy of the file as
 * "[file].backup-<stamp>" before it writes (written under a temporary name and renamed, so
 * a copy with that name is always whole), and [undoReplace] renames that copy back. Both run
 * to the end even if their caller is cancelled, so what is shown always matches the file.
 *
 * @param file where the document lives.
 * @param codec turns the document into text and back.
 * @param seed the document for a fresh install.
 * @param scope runs loading and saving; it outlives every screen.
 * @param io where file reads and writes run.
 * @param clockMs the time, for naming a set-aside file.
 */
class DocumentStore<T : Any>(
    private val file: File,
    private val codec: TextCodec<T>,
    private val seed: () -> T,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val clockMs: () -> Long = System::currentTimeMillis,
) : Store<T> {
    private val _data = MutableStateFlow<T?>(null)
    private val _saveFailed = MutableStateFlow(false)
    private val mutex = Mutex()

    private val _setAside = MutableStateFlow(false)

    /** True when an unreadable file couldn't be set aside; it must never be overwritten. */
    private val _unopened = MutableStateFlow(false)

    /** What each [replace] did, by stamp, so [undoReplace] undoes exactly that. */
    private val replaced = mutableMapOf<Long, Replaced>()

    override val data: StateFlow<T?> = _data.asStateFlow()

    override val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    override val setAside: StateFlow<Boolean> = _setAside.asStateFlow()

    override val unopened: StateFlow<Boolean> = _unopened.asStateFlow()

    /**
     * Reads the document from [file], or saves [seed] there if there is no file. Call once.
     *
     * @return the loading job.
     */
    fun load(): Job = scope.launch {
        val loaded = mutex.withLock { withContext(context = io) { readOrSeed() } }
        _data.value = loaded
    }

    override fun edit(change: (T) -> T) {
        val current = _data.value ?: return
        val next = change(current)
        if (next == current) return
        _data.value = next
        scope.launch { saveLatest() }
    }

    override suspend fun replace(value: T, stamp: Long): Boolean =
        withContext(context = NonCancellable) {
            mutex.withLock {
                if (_unopened.value || stamp in replaced) return@withLock false
                val backedUp = withContext(context = io) {
                    tryBackUpAndSave(value = value, stamp = stamp)
                } ?: return@withLock false
                replaced[stamp] = Replaced(backedUp = backedUp, saveFailed = _saveFailed.value)
                _data.value = value
                _saveFailed.value = false
                true
            }
        }

    override suspend fun undoReplace(stamp: Long, previous: T): Boolean =
        withContext(context = NonCancellable) {
            mutex.withLock {
                val record = replaced[stamp] ?: return@withLock false
                val restored = withContext(context = io) {
                    tryRestore(stamp = stamp, record = record)
                }
                if (restored) {
                    replaced.remove(stamp)
                    _data.value = previous
                    _saveFailed.value = record.saveFailed
                }
                restored
            }
        }

    /** Saves whatever the document is by the time the lock is free, so the newest wins. */
    private suspend fun saveLatest() {
        mutex.withLock {
            val latest = _data.value ?: return
            _saveFailed.value = _unopened.value || !withContext(context = io) { trySave(latest) }
        }
    }

    private fun readOrSeed(): T {
        if (file.exists()) {
            readOrNull()?.let { return it }
            if (trySetAside()) {
                _setAside.value = true
            } else {
                _unopened.value = true
                _saveFailed.value = true
                return seed()
            }
        }
        return seed().also { _saveFailed.value = !trySave(it) }
    }

    /** The document in [file], or null if it can't be read (IO) or understood (codec). */
    private fun readOrNull(): T? =
        try {
            codec.decode(file.readText())
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun trySetAside(): Boolean =
        try {
            val name = "${file.name}$UNREADABLE_MARK${clockMs()}"
            val aside = File(file.absoluteFile.parentFile, name)
            Files.move(file.toPath(), aside.toPath())
            true
        } catch (e: IOException) {
            false
        }

    private fun backupOf(stamp: Long): File =
        File(file.absoluteFile.parentFile, "${file.name}$BACKUP_MARK$stamp")

    /**
     * Copies the file to its backup, then saves [value].
     *
     * @return whether a backup was made (false when there was no file), or null if nothing
     *     was changed because the backup name was taken or a write failed.
     */
    private fun tryBackUpAndSave(value: T, stamp: Long): Boolean? {
        val backup = backupOf(stamp)
        if (backup.exists()) return null
        val backedUp = file.exists()
        try {
            if (backedUp) backUp(to = backup)
        } catch (e: IOException) {
            return null
        }
        return try {
            save(value)
            backedUp
        } catch (e: IOException) {
            if (backedUp) backup.delete()
            null
        }
    }

    /** Writes a synced copy of the file under a temporary name and renames it to [to]. */
    private fun backUp(to: File) {
        val directory = checkNotNull(file.absoluteFile.parentFile)
        val temporary = File(directory, "${file.name}.tmp-backup")
        try {
            writeSynced(file = temporary, bytes = file.readBytes())
            Files.move(temporary.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            temporary.delete()
            throw e
        }
    }

    /** Renames the backup back, or, only if [replace] found no file, deletes the new one. */
    private fun tryRestore(stamp: Long, record: Replaced): Boolean =
        try {
            if (record.backedUp) {
                Files.move(
                    backupOf(stamp).toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } else {
                Files.deleteIfExists(file.toPath())
            }
            true
        } catch (e: IOException) {
            false
        }

    private fun trySave(value: T): Boolean =
        try {
            save(value)
            true
        } catch (e: IOException) {
            false
        }

    private fun save(value: T) {
        val directory = checkNotNull(file.absoluteFile.parentFile)
        directory.mkdirs()
        val temporary = File(directory, "${file.name}.tmp")
        val bytes = codec.encode(value).toByteArray(StandardCharsets.UTF_8)
        writeSynced(file = temporary, bytes = bytes)
        Files.move(
            temporary.toPath(),
            file.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}

/**
 * What one [DocumentStore.replace] did.
 *
 * @property backedUp whether it copied a file to a backup (false: there was no file).
 * @property saveFailed whether the last save had failed before it, to show again on undo.
 */
private data class Replaced(val backedUp: Boolean, val saveFailed: Boolean)
