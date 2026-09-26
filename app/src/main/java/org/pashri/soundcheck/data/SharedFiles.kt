package org.pashri.soundcheck.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
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
     * @return false if it couldn't be written.
     */
    suspend fun write(uri: String, text: String): Boolean

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

/** Opens a picked file for writing and cuts off whatever it held before. */
private const val WRITE_TRUNCATE = "wt"

/**
 * [SharedFiles] through Android's content resolver.
 *
 * @param context any context; only the application context is kept.
 */
class AndroidSharedFiles(context: Context) : SharedFiles {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun write(uri: String, text: String): Boolean =
        withContext(context = Dispatchers.IO) {
            try {
                val stream = resolver.openOutputStream(Uri.parse(uri), WRITE_TRUNCATE)
                    ?: return@withContext false
                stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                true
            } catch (e: IOException) {
                false
            } catch (e: SecurityException) {
                false
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
}
