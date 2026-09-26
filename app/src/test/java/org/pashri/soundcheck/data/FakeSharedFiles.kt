package org.pashri.soundcheck.data

/** [SharedFiles] held in memory, by address. */
class FakeSharedFiles : SharedFiles {
    /** Every file, by address; a test puts a file here to have it picked. */
    val files: MutableMap<String, String> = mutableMapOf()

    /** Set false to act as if the picked place can't be written to. */
    var writes: Boolean = true

    override suspend fun write(uri: String, text: String): Boolean {
        if (!writes) return false
        files[uri] = text
        return true
    }

    override suspend fun read(uri: String): String? = files[uri]
}
