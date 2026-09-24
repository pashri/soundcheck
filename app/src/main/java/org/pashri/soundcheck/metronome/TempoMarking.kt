package org.pashri.soundcheck.metronome

/** The Italian tempo marking shown above the BPM. */
object TempoMarking {
    private val MARKINGS = listOf(
        200 to "Prestissimo",
        168 to "Presto",
        120 to "Allegro",
        108 to "Moderato",
        76 to "Andante",
        66 to "Adagio",
        60 to "Larghetto",
        40 to "Largo",
    )

    /**
     * The marking for a tempo.
     *
     * @param bpm beats per minute.
     * @return e.g. "Andante" for 96.
     */
    fun forBpm(bpm: Int): String =
        MARKINGS.firstOrNull { bpm >= it.first }?.second ?: "Grave"
}
