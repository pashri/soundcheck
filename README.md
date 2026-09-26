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
  it hears is recorded or kept.
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
- The Warm-up runs in a foreground service with a media session: the lock screen shows
  pause, next and stop, and the headphone button pauses (one press), skips to the next
  exercise (two) and goes back (three). Only one tool plays, records or listens at a time.
- The Warm-up's library (Patterns, Sounds and Programmes) and its settings are two small
  JSON files in the app's private storage, rewritten whole, atomically, on every change. A
  fresh install starts with eight Patterns, eight Sounds and a sample Programme. Nothing
  leaves the phone unless you export it: there are no accounts and no sync.
- Settings → Backup exports the library and settings as one JSON file through Android's
  file picker (to Drive, Downloads or anywhere else), and imports one back. An import
  replaces the library and settings after asking, keeps the old files on the phone as
  timestamped backups, and leaves your recordings where they are, matched to Sounds by id
  — recordings named by those backups stay on the phone until the backups are deleted.
  Recordings are never exported.
- With "Play over other audio" on, the Warm-up and the Metronome play under a podcast
  instead of pausing it, and the headphone button stays with the podcast app: the Warm-up
  then has no media session at all, only a notification with pause, next and stop. Some
  Bluetooth headsets only send "pause" while any audio is still playing, so they can pause
  the other app but not resume it. Recording always pauses a podcast, so it isn't recorded
  under your voice.
- The Step and Pattern editors can play a Step's Demo or a Pattern on the piano, using the
  same timeline as the Warm-up, and the Sounds screen plays your recordings; doing either
  pauses a playing Programme.

## Building

Needs JDK 17 and the Android SDK with platform 35, NDK 28.2.13676358 and CMake 3.22.1:

```bash
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "ndk;28.2.13676358" "cmake;3.22.1"
```

```bash
./gradlew :app:testDebugUnitTest   # Kotlin tests
tools/run-native-tests.sh          # C++ engine tests, on this Mac
./gradlew :app:installDebug        # install on a connected phone
```

The piano samples are committed under `app/src/main/assets/piano/`.
`tools/fetch-piano-samples.sh` rebuilds them from the original archive; it needs `ffmpeg`
and downloads 742 MB.

## Credits

Instrument Serif, IBM Plex Sans and IBM Plex Mono are used under the SIL Open Font License.

The piano is the [Salamander Grand Piano V3](https://freepats.zenvoid.org/Piano/acoustic-grand-piano.html)
by Alexander Holm, used under the
[Creative Commons Attribution 3.0](https://creativecommons.org/licenses/by/3.0/) licence.
Each sample was mixed to mono and shortened to 4.5 seconds, and Soundcheck retunes the keys
as it plays them. The per-key tuning comes from the "Retuned" SFZ mapping by Markus Fiedler.
</content>
</invoke>
