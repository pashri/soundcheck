# Soundcheck

**Warm up your voice hands-free, on a real piano, in the car or anywhere else.** A vocal
Warm-up, a Metronome and a Tuner for Android, drawn like a page of manuscript.

[**⬇ Download Soundcheck 1.0.0 (APK)**](https://github.com/pashri/soundcheck/releases/latest)
· Android 11 or later · free, no ads, no accounts, nothing leaves your phone

<p align="center">
  <img src="docs/screenshots/warmup-playing.png" width="240" alt="The Warm-up playing a Step: the Pattern on a staff with the note being sung in vermilion, and a keyboard below">
  <img src="docs/screenshots/warmup-playing-night.png" width="240" alt="The same screen at night">
  <img src="docs/screenshots/warmup-home.png" width="240" alt="The Warm-up home: your Range, your Programmes and your library">
</p>

## About

Soundcheck runs your vocal warm-up for you. Press Start, put the phone in your pocket, and
it plays each exercise on a sampled grand piano, tells you what to sing, and walks you up
through the keys to the top of your range and back down again. You never have to look at
the screen or touch the phone: the headphone button pauses, skips and goes back.

Everything is yours to shape. Build Programmes from your own exercises, set your range from
a voice type, and record the cue for each sound in your own voice. When you want to look,
the playing screen shows the exercise on a staff in the right clef for your voice, with a
keyboard that follows the note you're singing.

It also has a Metronome and a Tuner, so it's the only practice app you need to open.

## Features

### A warm-up that runs itself

- **Programmes** are lists of Steps. Each Step sings one Pattern on one Sound (a lip trill,
  a hum, "mah") at its own tempo.
- **Round trips through your range:**
  1. Each Step starts with a short spoken cue (the Announcement) and a Demo of the Pattern.
  2. It plays the Key Chord, and you sing the Pattern.
  3. It moves up a half-step and repeats until the Pattern reaches the top of your range,
     then comes back down.
  4. A Step that can't fit your range is skipped, and the editors warn you ahead of time.
- **A real piano:** the Salamander Grand, sampled on every third key and re-pitched for the
  keys between, scheduled to the exact sample so every note lands on the beat.
- **Guide melody:** the piano can play the Pattern along with you, or leave you to sing it
  alone over the chords.
- **Starts high or low:** each Step can start at the bottom of your range and climb, or at
  the top and descend, and can be offset up or down.

### Hands-free, screen off

- It keeps playing with the screen off, from your pocket or on the car's Bluetooth.
- **Headphone button:** one press pauses or resumes, two go to the next Step, three to the
  previous one. The same button starts and stops the Metronome.
- The lock-screen card has pause, next and stop.
- Pulling out your headphones pauses it. A phone call pauses it, and it picks up again
  afterwards.
- **Play over other audio:** leave your podcast or music playing underneath if you prefer.

### See what you're singing

- **Staff:** the exercise appears in the right clef for your voice: treble for soprano and
  alto, treble with an 8 below for tenor, bass for bass. Notes are spelled properly for the
  key, with ledger lines, sharps and flats.
- **Moving note:** a vermilion note follows the piano, and the staff holds still as the keys
  change.
- **Keyboard:** it shows the Step's key, its top note, and the key you're singing right now,
  drawn as if pressed down.

### Make it yours

- **Your range:** pick Soprano, Alto, Tenor or Bass, then move the lowest and highest notes to
  fit your voice today.
- **Patterns:** write the notes to sing as scale degrees, with flats, sharps, four note
  lengths and a Key Chord (major, minor, seventh and more). Degrees can go past the octave.
  Play a Pattern to hear it.
- **Steps:** choose the Pattern and Sound, the tempo, the direction and the Range Offset.
  "Hear the Demo" plays it in its first key.
- **Sounds, in your voice:**
  - Record how each Sound's cue should sound. Silence is trimmed from both ends
    automatically, and you can re-record or undo.
  - Until you record one, the phone's voice reads the name.
- **A starter kit:** six ready-made Steps from lip trills to vowels, so it works the moment
  you install it.

### Metronome and Tuner

- **Metronome:** set the tempo with − and +, by dragging, or by tapping it out. Put an accent
  every 2 to 8 beats, or none. It keeps time to the sample, keeps going with the screen off,
  and the headphone button starts and stops it.
- **Tuner:** play or sing a note to see its name, octave and how many cents sharp or flat you
  are, on a dial and a small staff. It listens only while it's open and keeps nothing it
  hears.

### Built for everyone, day and night

- **Day and night:** warm paper by day and ink-dark by night, following your phone's dark
  theme.
- **Accessibility:** it works at the largest text and display sizes, and every control reads
  out properly with TalkBack, including sharps, flats and the notes on the staff.

### Your data stays yours

- Everything is stored on your phone: no accounts, no ads, no analytics. The app doesn't
  even ask for internet access.
- **Back up** your whole library and settings to one small file (Drive, Downloads, anywhere),
  and restore it later or on a new phone. Your recordings stay on the phone.

## Screenshots

<p align="center">
  <img src="docs/screenshots/programme-editor.png" width="200" alt="A Programme's editor, listing its Steps">
  <img src="docs/screenshots/step-editor.png" width="200" alt="A Step's editor: tempo, direction, range offset, pattern and sound">
  <img src="docs/screenshots/pattern-editor.png" width="200" alt="A Pattern's editor: its notes, lengths, accidentals and key chord">
  <img src="docs/screenshots/sounds.png" width="200" alt="The Sounds library: recorded and phone-voice cues">
</p>
<p align="center">
  <img src="docs/screenshots/tuner.png" width="200" alt="The Tuner reading a note: its name, octave, needle and cents">
  <img src="docs/screenshots/metronome.png" width="200" alt="The Metronome running">
  <img src="docs/screenshots/settings.png" width="200" alt="Settings: range, voice type, play over other audio and backup">
</p>

## Download and install

Soundcheck isn't on the Play Store. You install it straight from this page, which Android
calls sideloading. It takes about a minute.

1. **On your phone,** open the
   [latest release](https://github.com/pashri/soundcheck/releases/latest) and tap
   **soundcheck-1.0.0.apk** under *Assets* to download it. You need Android 11 or later.
2. **Open the downloaded file.** Tap the download notification, or find it in the *Files*
   app under *Downloads*.
3. **Allow the install if Android asks.** The first time, Android says your browser or file
   manager isn't allowed to install unknown apps. Tap **Settings**, switch on **Allow from
   this source**, and go back.
4. **Tap Install, then Open.** Soundcheck starts with a starter Programme ready to go. Allow
   notifications when it asks, so the lock-screen controls can appear.

**From a computer instead:** with USB debugging on, run
`adb install soundcheck-1.0.0.apk`.

**Updating:** install the newer APK the same way. It replaces the old one and keeps your
library and recordings, because every release is signed with the same key.

**Checking the download (optional):** the release notes list the APK's SHA-256 checksum.
Every release is signed by the same certificate, whose SHA-256 is
`e5344cc15ea8190e23d5dd6380798a96d56e284781ff29f7054fbffb426bf5ef`.

**Removing it:** long-press the icon and choose *Uninstall*. That deletes your library, so
back it up first if you might want it again.

---

The rest of this page is for people building Soundcheck from source.

## How it's built

- Kotlin and Jetpack Compose, one activity, one `:app` module.
- All sound goes through a small C++ mixer on Google's Oboe library, scheduled to the exact
  sample, so the Metronome never drifts or stutters and every piano note lands on its beat.
  The piano is recorded every third key; the mixer re-pitches the nearest recording for the
  keys in between.
- The Tuner listens through Android's `AudioRecord`, not the mixer, and finds the pitch in
  Kotlin with the McLeod Pitch Method. It listens only while its tab is open, and nothing
  it hears is recorded or kept. When another app takes the microphone (a call, a voice
  recorder), Android feeds the Tuner silence; the Tuner notices within half a second, says
  so, and listens again once the other app lets go.
- The Warm-up's playing screen draws the Pattern on a staff at its real pitches in each
  Iteration's key, in the clef your Range reads (treble, treble an octave lower for a
  tenor's Range, or bass), with the note being sung in vermilion, and your Range as a
  keyboard with the Iteration's key and the Pattern's top note marked.
  If the sound output fails, or another app is holding on to the sound, a paused Programme
  says so rather than looking like an ordinary pause.
- On the Sounds screen you hold a button and say a Sound; Soundcheck drops the press and
  the release, trims the silence from both ends, evens out the level and keeps up to 5
  seconds of it as a small 16-bit mono WAV file in the app's private storage, with a
  one-step Undo for a new take and "Use phone voice" to go back to text-to-speech. The
  Tuner and the recorder share the microphone and never have it open at the same time;
  each retries for about 400 ms rather than reporting the microphone unavailable while the
  other lets go of it. A Sound you haven't recorded is read by the phone's text-to-speech.
  A recording that no Sound in the library names any more is deleted the next time the app
  starts, unless a `library.json.backup-*` copy an import kept, or a
  `library.json.unreadable-*` copy set aside because it couldn't be read, still names it.
- The Warm-up runs in a foreground service whose notification carries Soundcheck's one
  media session: the lock screen shows pause, next and stop, and the headphone button
  pauses (one press), skips to the next exercise (two) and goes back (three). The
  Metronome uses the same service, so it keeps clicking with the screen off, in the
  background or on another tab; while it plays on its own the notification shows its
  tempo and accent with Pause and Close, and tapping it opens the Metronome. While a
  Programme is loaded the Warm-up's notification comes first. Off screen, a headphone
  press or Pause pauses the Metronome and the notification stays, with Play; another press
  or Play starts it again. Unplugging headphones pauses it too, as it does a Programme.
  It ends when you press Stop on its screen (or the headphone button while the screen
  shows), press Close, another tool starts (the Tuner listening, a Programme, a Demo), or
  another app takes audio focus for good. Only one tool plays, records or listens at a
  time.
- The Warm-up's library (Patterns, Sounds and Programmes) and its settings are two small
  JSON files in the app's private storage, rewritten whole, atomically, on every change. A
  fresh install starts with eight Patterns, eight Sounds and a sample Programme. Nothing
  leaves the phone unless you export it: there are no accounts and no sync.
- Settings → Backup exports the library and settings as one JSON file through Android's
  file picker (to Drive, Downloads or anywhere else), and imports one back. An import
  replaces the library and settings after asking, keeps the old files on the phone as
  timestamped backups, and leaves your recordings where they are, matched to Sounds by id
  — recordings named by those backups stay on the phone until the backups are deleted.
  Recordings are never exported. An export that fails part-way removes the file it
  started only if the file was empty before; any other file is left as it is.
- With "Play over other audio" on, the Warm-up and the Metronome play under a podcast
  instead of pausing it, and the headphone button stays with the podcast app: Soundcheck
  then has no media session at all, only its notification (pause, next and stop for the
  Warm-up, Pause or Play and Close for the Metronome). Some Bluetooth headsets only send
  "pause" while any audio is still playing, so they can pause the other app but not resume
  it. Recording always pauses a podcast, so it isn't recorded under your voice. While the
  Metronome's tab is open, or while it plays or is paused (screen off included), one press
  of the headphone button starts or stops it; the button goes to whichever of the
  Metronome and the Warm-up you started last, through Soundcheck's one media session. The
  press only reaches Soundcheck if it was the last app to play sound; otherwise Android
  sends it to that app.
- The Step and Pattern editors can play a Step's Demo or a Pattern on the piano, using the
  same timeline as the Warm-up, and the Sounds screen plays your recordings; doing either
  pauses a playing Programme.
- The look is the Manuscript design: paper, ink and vermilion by day, dark ink, cream and a
  lighter vermilion by night, following the phone's dark theme, down to the dialogs, the
  menus and the launch screen. The icon is a waveform standing on a staff, with a
  single-colour layer for Android's themed icons.

## Building

Needs JDK 17 and the Android SDK with platform 35, NDK 28.2.13676358 and CMake 3.22.1:

```bash
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "ndk;28.2.13676358" "cmake;3.22.1"
```

```bash
./gradlew :app:testDebugUnitTest   # Kotlin tests
tools/run-native-tests.sh          # C++ engine tests, on this Mac
./gradlew :app:installDebug        # install a debug build on a connected phone
```

A debug build installs as "Soundcheck debug" (`org.pashri.soundcheck.debug`), beside the
release, with its own data.

A debug build can feed the Tuner a steady synthetic tone instead of the microphone, which
is handy on the emulator (its microphone can hang it) and for screenshots. Release builds
don't contain this:

```bash
adb shell am start -n org.pashri.soundcheck.debug/org.pashri.soundcheck.MainActivity \
    --ef demoTuneHz 442.04   # the Tuner reads A4, 8 cents sharp
```

The piano samples are committed under `app/src/main/assets/piano/`.
`tools/fetch-piano-samples.sh` rebuilds them from the original archive; it needs `ffmpeg`
and downloads 742 MB.

## Release build

The release is a signed APK you install yourself; it isn't on the Play Store. Its key never
goes in this repository.

1. Once, make the key and keep a copy of it and its password somewhere safe (a password
   manager): without them, no later release can install over this one.

   ```bash
   keytool -genkeypair -v -keystore ~/.android/soundcheck-release.jks \
       -alias soundcheck -keyalg RSA -keysize 4096 -validity 10000
   ```

   `keytool` asks for the password and your name; type them at its prompts rather than on
   the command line. It makes a PKCS12 keystore, whose key password is the store password.

2. Tell the build where it is, in `keystore.properties` in the project root, which
   `.gitignore` keeps out of the repository:

   ```properties
   # Soundcheck's release signing key. This file is never committed.
   # Back up ~/.android/soundcheck-release.jks and its password: without them no later
   # release can install over this one.
   storeFile=/Users/<you>/.android/soundcheck-release.jks
   storePassword=…
   keyAlias=soundcheck
   keyPassword=…
   ```

   A path starting with `~/` works too.

3. Build and install it:

   ```bash
   ./gradlew :app:assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

A release build without `keystore.properties` stops before packaging and says what is
missing; it never produces an unsigned APK. Debug builds, and running the unit tests,
don't need the file.
`./gradlew :app:assembleRelease -Psoundcheck.signWithDebugKey=true` signs it with this
computer's debug key instead, prints a warning, and gives it versionName
`1.0.0-debugkey`, for trying the minified build on an emulator; that APK is for testing
only.

## Credits

Instrument Serif, IBM Plex Sans and IBM Plex Mono are used under the SIL Open Font License.

The piano is the [Salamander Grand Piano V3](https://freepats.zenvoid.org/Piano/acoustic-grand-piano.html)
by Alexander Holm, used under the
[Creative Commons Attribution 3.0](https://creativecommons.org/licenses/by/3.0/) licence.
Each sample was mixed to mono and shortened to 4.5 seconds, and Soundcheck retunes the keys
as it plays them. The per-key tuning comes from the "Retuned" SFZ mapping by Markus Fiedler.
