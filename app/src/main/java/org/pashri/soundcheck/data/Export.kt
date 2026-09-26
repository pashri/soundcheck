package org.pashri.soundcheck.data

import kotlinx.serialization.Serializable
import org.pashri.soundcheck.warmup.Library
import org.pashri.soundcheck.warmup.WarmupSettings

/**
 * Everything a backup file holds: the library (Patterns, Sounds with their ids and labels,
 * and Programmes) and the Warm-up settings. Recordings are never part of it.
 *
 * @property library the library, with no Sound holding a clip.
 * @property settings the settings.
 */
data class Backup(val library: Library, val settings: WarmupSettings)

/**
 * Writes a backup as one JSON file: `format` "soundcheck-export", `version` 1, then the
 * settings and the library in the same records their own files use (each with its own
 * version). Every Sound's clip is left out.
 */
object ExportCodec {
    /** The `format` every backup file names. */
    const val FORMAT: String = "soundcheck-export"

    /** The backup format this build writes; a newer one is refused as too new. */
    const val VERSION: Int = 1

    /**
     * Writes [backup] as text.
     *
     * @param backup the library and settings; any clips are dropped.
     * @return the file's text.
     */
    fun encode(backup: Backup): String {
        val sounds = backup.library.sounds.map { it.copy(clip = null) }
        val library = backup.library.copy(sounds = sounds)
        val file = ExportFile(
            format = FORMAT,
            version = VERSION,
            settings = backup.settings.toFile(),
            library = library.toFile(),
        )
        return DocumentJson.encodeToString(serializer = ExportFile.serializer(), value = file)
    }
}

/** What a chosen file turned out to be. */
sealed interface ExportRead {
    /**
     * A backup this build can import.
     *
     * @property backup what it holds.
     */
    data class Valid(val backup: Backup) : ExportRead

    /** Not a backup: another file, broken JSON, or a library that breaks the rules. */
    data object NotAnExport : ExportRead

    /** A backup from a newer version of Soundcheck. */
    data object TooNew : ExportRead
}

/**
 * Reads a backup file, checking everything before anything is changed: the format, the
 * version, and the library and settings as strictly as their own files are read (unknown
 * fields, dangling ids, duplicate ids, a Range past the piano). A backup that names a
 * recording is refused, since backups never hold them.
 *
 * @param text the file's text.
 * @return the backup, or why it can't be imported.
 */
fun readExport(text: String): ExportRead {
    val file = try {
        DocumentJson.decodeFromString(deserializer = ExportFile.serializer(), string = text)
    } catch (e: IllegalArgumentException) {
        return ExportRead.NotAnExport
    }
    if (file.format != ExportCodec.FORMAT) return ExportRead.NotAnExport
    if (file.version > ExportCodec.VERSION) return ExportRead.TooNew
    return try {
        backupOf(file)
    } catch (e: IllegalArgumentException) {
        ExportRead.NotAnExport
    }
}

private fun backupOf(file: ExportFile): ExportRead {
    require(value = file.version >= 1) { "Backup format ${file.version} doesn't exist" }
    require(value = file.library.sounds.all { it.clip == null }) { "A backup names a clip" }
    val library = file.library.readLibrary()
    return ExportRead.Valid(Backup(library = library, settings = file.settings.readSettings()))
}

@Serializable
internal data class ExportFile(
    val format: String,
    val version: Int,
    val settings: SettingsFile,
    val library: LibraryFile,
)
