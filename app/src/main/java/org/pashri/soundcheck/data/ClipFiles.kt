package org.pashri.soundcheck.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.pashri.soundcheck.audio.SAMPLE_RATE
import org.pashri.soundcheck.audio.WavReader
import org.pashri.soundcheck.audio.WavWriter
import org.pashri.soundcheck.audio.resample
import org.pashri.soundcheck.warmup.ClipName

/**
 * The Sounds' recorded clips, one 16-bit mono WAV file each in [directory]. A clip is
 * written to a temporary file, synced to storage and renamed into place under a fresh name,
 * so a file with a clip's name is always whole, and a new take never overwrites an old one.
 * The library switches a Sound to a new name only once its file exists, and an old file
 * stays until [sweep] finds that the saved library no longer uses it.
 *
 * @param directory the clips folder; made when the first clip is saved.
 * @param io where file reads and writes run.
 * @param newName makes a fresh file name stem of letters, digits and dashes, e.g. a UUID.
 */
class ClipFiles(
    private val directory: File,
    private val io: CoroutineDispatcher,
    private val newName: () -> String,
) {
    /**
     * Saves a clip under a new name.
     *
     * @param pcm mono audio at [SAMPLE_RATE].
     * @return the clip's file name.
     * @throws IOException if it can't be written (storage full); nothing is left behind.
     */
    suspend fun save(pcm: FloatArray): ClipName = withContext(context = io) {
        val name = ClipName("${newName()}.wav")
        val temporary = File(directory, "${name.value}$TEMPORARY")
        try {
            directory.mkdirs()
            val bytes = WavWriter.write(frames = pcm, sampleRate = SAMPLE_RATE)
            writeSynced(file = temporary, bytes = bytes)
            Files.move(
                temporary.toPath(),
                File(directory, name.value).toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (e: IOException) {
            temporary.delete()
            throw e
        }
        name
    }

    /**
     * Reads a clip.
     *
     * @param name the clip's file name.
     * @return its audio at [SAMPLE_RATE], or null if the file is missing or not a WAV file.
     */
    suspend fun load(name: ClipName): FloatArray? = withContext(context = io) {
        try {
            val audio = WavReader.read(File(directory, name.value).readBytes())
            resample(frames = audio.frames, fromRate = audio.sampleRate, toRate = SAMPLE_RATE)
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Deletes the clip files nothing uses and any temporary file a failed save left. A file
     * changed at or after [before] is always kept, so nothing saved since the app started is
     * touched; files that are neither clips nor temporary are left alone.
     *
     * @param keep the clips the saved library uses.
     * @param before when the app started, in milliseconds since the epoch.
     * @return how many files were deleted.
     */
    suspend fun sweep(keep: Set<ClipName>, before: Long): Int = withContext(context = io) {
        val kept = keep.map { it.value }.toSet()
        directory.listFiles().orEmpty()
            .filter { it.isFile && it.lastModified() < before }
            .filter { unused(name = it.name, kept = kept) }
            .count { it.delete() }
    }

    private fun unused(name: String, kept: Set<String>): Boolean =
        name.endsWith(TEMPORARY) || (name.endsWith(CLIP_SUFFIX) && name !in kept)

    private companion object {
        const val TEMPORARY = ".tmp"
        const val CLIP_SUFFIX = ".wav"
    }
}

/**
 * Writes [bytes] to [file] and syncs them to storage before returning, so a file that exists
 * afterward is never left half-written by a crash or a sudden loss of power.
 *
 * @param file the file to write; overwritten if it exists.
 * @param bytes the file's whole contents.
 * @throws IOException if the write fails.
 */
internal fun writeSynced(file: File, bytes: ByteArray) {
    FileOutputStream(file).use { out ->
        out.write(bytes)
        out.fd.sync()
    }
}
