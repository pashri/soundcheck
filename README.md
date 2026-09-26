# Soundcheck

A Tuner and a Metronome for any instrument, and a hands-free vocal Warm-up, for Android.
The Warm-up plays your Programmes on a sampled grand piano, announcing each exercise in
your own recorded voice (or the phone's voice until you record one), and keeps going with
the screen off. You build Programmes from Patterns (the notes to sing) and Sounds (what to
sing them on), and set your Range from a voice type.

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
  pauses (one press), skips to the next exercise (two) and goes back (three). Only one
  tool plays, records or listens at a time.
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
  started.
- With "Play over other audio" on, the Warm-up and the Metronome play under a podcast
  instead of pausing it, and the headphone button stays with the podcast app: the Warm-up
  then has no media session at all, only a notification with pause, next and stop. Some
  Bluetooth headsets only send "pause" while any audio is still playing, so they can pause
  the other app but not resume it. Recording always pauses a podcast, so it isn't recorded
  under your voice. While the Metronome's tab is open, one press of the headphone button
  starts or stops it; the button goes to whichever of the Metronome and the Warm-up you
  started last, through Soundcheck's one media session.
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
