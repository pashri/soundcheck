package org.pashri.soundcheck.warmup

/** The eight Patterns a fresh install ships with. */
object StarterPatterns {
    /** Up and down the first five notes of the major scale. */
    val FIVE_NOTE_SCALE: Pattern = starter(
        id = "five-note-scale",
        name = "5-note scale",
        notation = "1 2 3 4 5 4 3 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A major arpeggio in quick eighth notes that repeats the octave four times. */
    val ARPEGGIO_8_HOLD: Pattern = starter(
        id = "arpeggio-8-hold",
        name = "Arpeggio 8-hold",
        notation = "1 3 5 8 8 8 8 5 3 1",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A major arpeggio up to the twelfth, then down the scale. */
    val DOUBLE_ARPEGGIO: Pattern = starter(
        id = "double-arpeggio",
        name = "Double arpeggio",
        notation = "1 3 5 8 10 12 11 9 7 5 4 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A slow slide from the root to the fifth and back, over a single note. */
    val SIREN_1_5_1: Pattern = starter(
        id = "siren-1-5-1",
        name = "1-5-1 siren",
        notation = "1 5 1w",
        defaultLength = NoteLength.HALF,
        keyChord = KeyChord.ROOT_ONLY,
    )

    /** Up and down a major triad. */
    val TRIAD: Pattern = starter(
        id = "triad",
        name = "Triad",
        notation = "1 3 5 3 1h",
        defaultLength = NoteLength.QUARTER,
        keyChord = KeyChord.MAJOR,
    )

    /** The 5-note scale in minor. */
    val MINOR_FIVE_NOTE_SCALE: Pattern = starter(
        id = "minor-five-note-scale",
        name = "Minor 5-note scale",
        notation = "1 2 ♭3 4 5 4 ♭3 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MINOR,
    )

    /** Up the major scale to the ninth and back down. */
    val NINE_NOTE_SCALE: Pattern = starter(
        id = "nine-note-scale",
        name = "9-note scale",
        notation = "1 2 3 4 5 6 7 8 9 8 7 6 5 4 3 2 1h",
        defaultLength = NoteLength.EIGHTH,
        keyChord = KeyChord.MAJOR,
    )

    /** A dominant-seventh arpeggio to the octave and back. */
    val DOMINANT_ARPEGGIO: Pattern = starter(
        id = "dominant-arpeggio",
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
    /** Six Steps from a gentle lip trill to a wide 9-note scale, as the library saves them. */
    val SAVED_WARM_UP: SavedProgramme = SavedProgramme(
        id = ProgrammeId("starter-warm-up"),
        name = "Starter warm-up",
        steps = listOf(
            starterStep(
                number = 1,
                pattern = StarterPatterns.FIVE_NOTE_SCALE,
                sound = StarterSounds.LIP_TRILL,
                bpm = 90,
                rangeOffset = RangeOffset(top = 2),
            ),
            starterStep(number = 2, pattern = StarterPatterns.TRIAD, sound = StarterSounds.HUM),
            starterStep(
                number = 3,
                pattern = StarterPatterns.ARPEGGIO_8_HOLD,
                sound = StarterSounds.MIM,
                bpm = 100,
            ),
            starterStep(
                number = 4,
                pattern = StarterPatterns.SIREN_1_5_1,
                sound = StarterSounds.OO,
                bpm = 80,
                direction = Direction.START_HIGH,
            ),
            starterStep(
                number = 5,
                pattern = StarterPatterns.DOUBLE_ARPEGGIO,
                sound = StarterSounds.NEH,
                bpm = 110,
                rangeOffset = RangeOffset(top = 10),
            ),
            starterStep(
                number = 6,
                pattern = StarterPatterns.NINE_NOTE_SCALE,
                sound = StarterSounds.MAH,
                bpm = 100,
            ),
        ),
    )

    /** The same Programme, ready to play. */
    val WARM_UP: Programme = Programme(
        name = SAVED_WARM_UP.name,
        steps = SAVED_WARM_UP.steps.map { saved ->
            saved.toStep(
                pattern = StarterPatterns.ALL.first { it.id == saved.patternId },
                soundLabel = StarterSounds.ALL.first { it.id == saved.soundId }.label,
            )
        },
    )
}

/** What the library holds on a fresh install. */
object StarterLibrary {
    /** The eight starter Patterns, the eight starter Sounds and the sample Programme. */
    val LIBRARY: Library = Library(
        patterns = StarterPatterns.ALL,
        sounds = StarterSounds.ALL,
        programmes = listOf(StarterProgrammes.SAVED_WARM_UP),
    )
}

private fun starter(
    id: String,
    name: String,
    notation: String,
    defaultLength: NoteLength,
    keyChord: KeyChord,
): Pattern = Pattern(
    id = PatternId(id),
    name = name,
    notes = PatternNotation.parse(text = notation, defaultLength = defaultLength),
    keyChord = keyChord,
)

private fun starterStep(
    number: Int,
    pattern: Pattern,
    sound: Sound,
    bpm: Int = 90,
    direction: Direction = Direction.START_LOW,
    rangeOffset: RangeOffset = RangeOffset.NONE,
): SavedStep = SavedStep(
    key = StepKey("starter-$number"),
    patternId = pattern.id,
    soundId = sound.id,
    bpm = bpm,
    direction = direction,
    rangeOffset = rangeOffset,
)
