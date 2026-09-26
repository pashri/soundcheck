package org.pashri.soundcheck.data

/** [SharedFiles] held in memory, by address. */
class FakeSharedFiles : SharedFiles {
    /** Every file, by address; a test puts a file here to have it picked. */
    val files: MutableMap<String, String> = mutableMapOf()

    /** Set false to act as if the picked place can't be written to. */
    var writes: Boolean = true

    /**
     * Sizes to report instead of the file's real size, by address; a null value acts as a
     * provider that can't say how big the file is.
     */
    val reportedSizes: MutableMap<String, Long?> = mutableMapOf()

    override suspend fun write(uri: String, text: String, deleteOnFailure: Boolean): Boolean {
        if (!writes) {
            if (deleteOnFailure) files.remove(uri)
            return false
        }
        files[uri] = text
        return true
    }

    override suspend fun sizeOf(uri: String): Long? =
        if (uri in reportedSizes) {
            reportedSizes[uri]
        } else {
            files[uri]?.toByteArray(Charsets.UTF_8)?.size?.toLong()
        }

    override suspend fun read(uri: String): String? = files[uri]
}
