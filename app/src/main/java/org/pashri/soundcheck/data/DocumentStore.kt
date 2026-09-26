package org.pashri.soundcheck.data

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
 * "[file].backup-<stamp>" before it writes, and [undoReplace] renames that copy back.
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

    override suspend fun replace(value: T, stamp: Long): Boolean = mutex.withLock {
        if (_unopened.value) return@withLock false
        val saved = withContext(context = io) { tryBackUpAndSave(value = value, stamp = stamp) }
        if (saved) {
            _data.value = value
            _saveFailed.value = false
        }
        saved
    }

    override suspend fun undoReplace(stamp: Long, previous: T): Boolean = mutex.withLock {
        val restored = withContext(context = io) { tryRestore(stamp) }
        if (restored) _data.value = previous
        restored
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
            val aside = File(file.absoluteFile.parentFile, "${file.name}.unreadable-${clockMs()}")
            Files.move(file.toPath(), aside.toPath())
            true
        } catch (e: IOException) {
            false
        }

    private fun backupOf(stamp: Long): File =
        File(file.absoluteFile.parentFile, "${file.name}$BACKUP_MARK$stamp")

    private fun tryBackUpAndSave(value: T, stamp: Long): Boolean =
        try {
            if (file.exists()) writeSynced(file = backupOf(stamp), bytes = file.readBytes())
            save(value)
            true
        } catch (e: IOException) {
            backupOf(stamp).delete()
            false
        }

    private fun tryRestore(stamp: Long): Boolean =
        try {
            val backup = backupOf(stamp)
            if (backup.exists()) {
                Files.move(
                    backup.toPath(),
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
