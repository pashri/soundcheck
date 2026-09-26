package org.pashri.soundcheck.audio

/** The tools that make sound or listen. */
enum class Tool {
    /** Listens to the microphone. */
    TUNER,

    /** Clicks. */
    METRONOME,

    /** Plays a Programme. */
    WARM_UP,

    /** Plays a Step's Demo or a Pattern once, from an editor. */
    AUDITION,
}

/**
 * Keeps one tool making sound or listening at a time: whichever claims the slot last gets it,
 * and the tool that held it is told to stop. Call from the main thread.
 */
class ToolArbiter {
    private var holder: Tool? = null
    private var evict: (() -> Unit)? = null

    /** The tool holding the slot, or null. */
    val current: Tool?
        get() = holder

    /**
     * Takes the slot for [tool], evicting any other tool that holds it. The new holder is
     * recorded before the old one is told, so the old one freeing the slot as it stops
     * changes nothing.
     *
     * @param tool the tool starting.
     * @param onEvicted called if another tool later takes the slot.
     */
    fun claim(tool: Tool, onEvicted: () -> Unit) {
        val previous = evict.takeIf { holder != tool }
        holder = tool
        evict = onEvicted
        previous?.invoke()
    }

    /**
     * Frees the slot if [tool] holds it.
     *
     * @param tool the tool stopping.
     */
    fun release(tool: Tool) {
        if (holder != tool) return
        holder = null
        evict = null
    }
}
