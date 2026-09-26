package org.pashri.soundcheck.warmup

/** The eight Patterns a fresh install ships with. */
object StarterPatterns {
    /** Up and down the first five notes of the major scale. */
    val FIVE_NOTE_SCALE: Pattern = starter(
        name = "5-note scale",
        notation = "1 2 3 4 5 4 3 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A major arpeggio that holds the octave over four quick notes. */
    val ARPEGGIO_8_HOLD: Pattern = starter(
        name = "Arpeggio 8-hold",
        notation = "1 3 5 8e 8e 8e 8e 5 3 1h",
        defaultLength = NoteLength.QUARTER,
        keyChord = KeyChord.MAJOR,
    )

    /** A major arpeggio up to the twelfth, then down the scale. */
    val DOUBLE_ARPEGGIO: Pattern = starter(
        name = "Double arpeggio",
        notation = "1 3 5 8 10 12 11 9 7 5 4 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A slow slide from the root to the fifth and back, over a single note. */
    val SIREN_1_5_1: Pattern = starter(
        name = "1-5-1 siren",
        notation = "1 5 1w",
        defaultLength = NoteLength.HALF,
        keyChord = KeyChord.ROOT_ONLY,
    )

    /** Up and down a major triad. */
    val TRIAD: Pattern = starter(
        name = "Triad",
        notation = "1 3 5 3 1h",
        defaultLength = NoteLength.QUARTER,
        keyChord = KeyChord.MAJOR,
    )

    /** The 5-note scale in minor. */
    val MINOR_FIVE_NOTE_SCALE: Pattern = starter(
        name = "Minor 5-note scale",
        notation = "1 2 ♭3 4 5 4 ♭3 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MINOR,
    )

    /** Up the major scale to the ninth and back down. */
    val NINE_NOTE_SCALE: Pattern = starter(
        name = "9-note scale",
        notation = "1 2 3 4 5 6 7 8 9 8 7 6 5 4 3 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A dominant-seventh arpeggio to the octave and back. */
    val DOMINANT_ARPEGGIO: Pattern = starter(
        name = "Dominant arpeggio",
        notation = "1 3 5 ♭7 8 ♭7 5 3 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.SEVENTH,
    )

    /** All eight, in library order. */
    val ALL: List<Pattern> = listOf(
        FIVE_NOTE_SCALE,
        ARPEGGIO_8_HOLD,
        DOUBLE_ARPEGGIO,
        SIREN_1_5_1,
        TRIAD,
        MINOR_FIVE_NOTE_SCALE,
        NINE_NOTE_SCALE,
        DOMINANT_ARPEGGIO,
    )
}

/** The eight Sounds a fresh install ships with, none recorded yet. */
object StarterSounds {
    /** A lip trill. */
    val LIP_TRILL: Sound = Sound(id = SoundId("lip-trill"), label = "lip trill")

    /** "mim". */
    val MIM: Sound = Sound(id = SoundId("mim"), label = "mim")

    /** "neh". */
    val NEH: Sound = Sound(id = SoundId("neh"), label = "neh")

    /** "mah". */
    val MAH: Sound = Sound(id = SoundId("mah"), label = "mah")

    /** "ng". */
    val NG: Sound = Sound(id = SoundId("ng"), label = "ng")

    /** "oo". */
    val OO: Sound = Sound(id = SoundId("oo"), label = "oo")

    /** "ee". */
    val EE: Sound = Sound(id = SoundId("ee"), label = "ee")

    /** A hum. */
    val HUM: Sound = Sound(id = SoundId("hum"), label = "hum")

    /** All eight, in library order. */
    val ALL: List<Sound> = listOf(LIP_TRILL, MIM, NEH, MAH, NG, OO, EE, HUM)
}

/** The sample Programme a fresh install ships with. */
object StarterProgrammes {
    /** Six Steps from a gentle lip trill to a wide 9-note scale. */
    val WARM_UP: Programme = Programme(
        name = "Starter warm-up",
        steps = listOf(
            Step(
                pattern = StarterPatterns.FIVE_NOTE_SCALE,
                soundId = StarterSounds.LIP_TRILL.id,
                bpm = 90,
                direction = Direction.START_LOW,
                rangeOffset = RangeOffset(top = 2),
            ),
            Step(
                pattern = StarterPatterns.TRIAD,
                soundId = StarterSounds.HUM.id,
                bpm = 90,
                direction = Direction.START_LOW,
            ),
            Step(
                pattern = StarterPatterns.ARPEGGIO_8_HOLD,
                soundId = StarterSounds.MIM.id,
                bpm = 100,
                direction = Direction.START_LOW,
            ),
            Step(
                pattern = StarterPatterns.SIREN_1_5_1,
                soundId = StarterSounds.OO.id,
                bpm = 80,
                direction = Direction.START_HIGH,
            ),
            Step(
                pattern = StarterPatterns.DOUBLE_ARPEGGIO,
                soundId = StarterSounds.NEH.id,
                bpm = 110,
                direction = Direction.START_LOW,
                rangeOffset = RangeOffset(top = 10),
            ),
            Step(
                pattern = StarterPatterns.NINE_NOTE_SCALE,
                soundId = StarterSounds.MAH.id,
                bpm = 100,
                direction = Direction.START_LOW,
            ),
        ),
    )
}

private fun starter(
    name: String,
    notation: String,
    defaultLength: NoteLength,
    keyChord: KeyChord,
): Pattern = Pattern(
    name = name,
    notes = PatternNotation.parse(notation, defaultLength = defaultLength),
    keyChord = keyChord,
)
