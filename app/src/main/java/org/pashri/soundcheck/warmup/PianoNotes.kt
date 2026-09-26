package org.pashri.soundcheck.warmup

import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.piano.Piano

/**
 * Hands one piano note to the engine, on the recording and rate that sound its pitch, at its
 * part's loudness: the one way a Programme and an audition play a note.
 *
 * @param piano the loaded piano.
 * @param event the note.
 * @param origin the output frame the note's own frames count from.
 */
fun SoundOutput.schedulePianoNote(piano: Piano, event: PianoNoteEvent, origin: Long) {
    val key = piano.keyFor(event.pitch)
    schedule(
        id = key.id,
        frame = origin + event.startFrame,
        gain = pianoGain(event.part),
        rate = key.rate,
        lengthFrames = event.lengthFrames,
    )
}

/**
 * How loud a piano note plays.
 *
 * @param part what the note is for.
 * @return [ProgrammePlayer.CHORD_GAIN] for a Key Chord note, [ProgrammePlayer.MELODY_GAIN]
 *     for a Demo or Guide Melody note.
 */
fun pianoGain(part: PianoPart): Float = when (part) {
    PianoPart.KEY_CHORD -> ProgrammePlayer.CHORD_GAIN
    PianoPart.DEMO, PianoPart.GUIDE_MELODY -> ProgrammePlayer.MELODY_GAIN
}
