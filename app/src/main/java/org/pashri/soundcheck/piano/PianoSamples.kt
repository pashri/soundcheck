package org.pashri.soundcheck.piano

/**
 * One recorded key of the bundled piano.
 *
 * @property midi the key it was recorded on.
 * @property tuneCents how far to retune it so it sounds in tune at A4 = 440 Hz.
 */
data class PianoSample(val midi: Int, val tuneCents: Int) {
    /** Where the recording lives in the app's assets, e.g. "piano/midi-060.wav". */
    val assetPath: String
        get() = "piano/midi-${midi.toString().padStart(3, '0')}.wav"
}

// Cents per recording, lowest first: the tune= of each v8 region in the "Retuned" SFZ.
private val TUNE_CENTS = listOf(
    10, 13, 11, -3, -9, -9, -11, -7, -4, 0, -6, -3, -3, -6, -3,
    0, -4, -8, -8, -5, -7, -8, -12, -13, -12, -17, -17, -27, -38, -38,
)

/**
 * The bundled piano: velocity layer 8 of the Salamander Grand Piano V3 (Alexander Holm,
 * CC BY 3.0), one key every minor third from A0 to C8.
 */
object PianoSamples {
    /** The lowest recorded key, A0. */
    const val LOWEST_MIDI: Int = 21

    /** The highest recorded key, C8. */
    const val HIGHEST_MIDI: Int = 108

    /** Half-steps between recorded keys. */
    const val SPACING: Int = 3

    /** Every recording, lowest first, with the retuning from the "Retuned" SFZ. */
    val ALL: List<PianoSample> = (LOWEST_MIDI..HIGHEST_MIDI step SPACING)
        .zip(TUNE_CENTS) { midi, cents -> PianoSample(midi = midi, tuneCents = cents) }
}
