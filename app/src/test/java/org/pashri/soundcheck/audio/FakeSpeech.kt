package org.pashri.soundcheck.audio

/**
 * A [SpeechSynth] that returns the same audio for any text.
 *
 * @property pcm what every [speak] returns; null acts like a phone with no working voice.
 */
class FakeSpeech(var pcm: FloatArray? = null) : SpeechSynth {
    private val _spoken = mutableListOf<String>()

    /** Every text asked for, in order. */
    val spoken: List<String> get() = _spoken

    override suspend fun speak(text: String): FloatArray? {
        _spoken += text
        return pcm
    }
}
