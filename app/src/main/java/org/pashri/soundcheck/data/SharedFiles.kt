package org.pashri.soundcheck.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Files the person picked with the system's file picker (Drive, Downloads, …), named by
 * their `content://` address. No storage permission is needed: the picker grants access to
 * the one file.
 */
interface SharedFiles {
    /**
     * Writes a whole text file.
     *
     * @param uri the file's address, from the picker.
     * @param text what to write, as UTF-8.
     * @param deleteOnFailure removes the picked file if the write fails; only safe for a
     *     file the caller knows was empty (just created) before this write, so pass true
     *     only when [sizeOf] returned 0, never when it returned null. Left false, a failed
     *     write leaves whatever was there before, cut off, rather than risk an existing file.
     * @return false if it couldn't be written.
     */
    suspend fun write(uri: String, text: String, deleteOnFailure: Boolean = false): Boolean

    /**
     * The size of a picked file, to tell a fresh, empty document from an existing one
     * before writing to it.
     *
     * @param uri the file's address, from the picker.
     * @return its size in bytes, or null if it couldn't be queried.
     */
    suspend fun sizeOf(uri: String): Long?

    /**
     * Reads a whole text file of at most [MAX_SHARED_FILE_BYTES].
     *
     * @param uri the file's address, from the picker.
     * @return its text as UTF-8, or null if it couldn't be read or is larger than that.
     */
    suspend fun read(uri: String): String?
}

/** The largest file [SharedFiles.read] reads: far more than any library needs. */
const val MAX_SHARED_FILE_BYTES: Int = 5 * 1_024 * 1_024

/**
 * Reads a stream to its end, unless it holds more than [maxBytes].
 *
 * @param input the stream; not closed here.
 * @param maxBytes the most to read.
 * @return every byte, or null if there were more than [maxBytes].
 * @throws IOException if the stream fails.
 */
fun readAtMost(input: InputStream, maxBytes: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(size = READ_CHUNK_BYTES)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) return out.toByteArray()
        if (out.size() + count > maxBytes) return null
        out.write(buffer, 0, count)
    }
}

private const val READ_CHUNK_BYTES = 8_192

/**
 * Writes [text] as UTF-8 to the stream [open] gives, closing it after. When the file can't
 * be opened or written whole and [deleteOnFailure] is true, [discard] is called, so a
 * half-written file this export just created isn't left behind. [deleteOnFailure] must only
 * be true for a file known to have been empty before the write, never for one that may have
 * held someone else's data.
 *
 * @param open opens the file; null if it has no stream.
 * @param text what to write.
 * @param deleteOnFailure whether a failed write should remove the picked file.
 * @param discard removes the picked file after a failed write; it must not throw.
 * @return false if it couldn't be opened or written, including when a provider rejects
 *     the truncating mode with [IllegalArgumentException] or
 *     [UnsupportedOperationException].
 */
fun writeWhole(
    open: () -> OutputStream?,
    text: String,
    deleteOnFailure: Boolean = false,
    discard: () -> Unit = {},
): Boolean {
    val written = try {
        val stream = open()
        stream?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        stream != null
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    } catch (e: UnsupportedOperationException) {
        false
    }
    if (!written && deleteOnFailure) discard()
    return written
}

/** Opens a picked file for writing and cuts off whatever it held before. */
private const val WRITE_TRUNCATE = "wt"

/**
 * [SharedFiles] through Android's content resolver.
 *
 * @param context any context; only the application context is kept.
 */
class AndroidSharedFiles(context: Context) : SharedFiles {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun write(
        uri: String,
        text: String,
        deleteOnFailure: Boolean,
    ): Boolean = withContext(context = Dispatchers.IO) {
        val address = Uri.parse(uri)
        writeWhole(
            open = { resolver.openOutputStream(address, WRITE_TRUNCATE) },
            text = text,
            deleteOnFailure = deleteOnFailure,
            discard = { delete(address) },
        )
    }

    override suspend fun sizeOf(uri: String): Long? = withContext(context = Dispatchers.IO) {
        try {
            resolver.query(Uri.parse(uri), arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val column = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && column >= 0 && !cursor.isNull(column)) {
                        cursor.getLong(column)
                    } else {
                        null
                    }
                }
        } catch (e: SecurityException) {
            null
        }
    }

    override suspend fun read(uri: String): String? = withContext(context = Dispatchers.IO) {
        try {
            resolver.openInputStream(Uri.parse(uri))?.use { input ->
                readAtMost(input = input, maxBytes = MAX_SHARED_FILE_BYTES)
            }?.toString(Charsets.UTF_8)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Removes a picked file an export couldn't finish; a provider that refuses keeps it. */
    private fun delete(address: Uri) {
        try {
            DocumentsContract.deleteDocument(resolver, address)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "The unfinished backup had already gone", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to remove the unfinished backup", e)
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "This provider can't remove the unfinished backup", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "The unfinished backup isn't a document that can be removed", e)
        }
    }

    private companion object {
        const val TAG = "AndroidSharedFiles"
    }
}
