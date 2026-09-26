# Soundcheck

A Tuner and a Metronome for any instrument, and a hands-free vocal Warm-up, for Android.
The Warm-up plays your Programmes on a sampled grand piano, with the phone's voice
announcing each exercise, and keeps going with the screen off. You build Programmes from
Patterns (the notes to sing) and Sounds (what to sing them on), and set your Range from a
voice type. Recording your own announcements is on the way.

## How it's built

- Kotlin and Jetpack Compose, one activity, one `:app` module.
- All sound goes through a small C++ mixer on Google's Oboe library, scheduled to the exact
  sample, so the Metronome never drifts or stutters and every piano note lands on its beat.
  The piano is recorded every third key; the mixer re-pitches the nearest recording for the
  keys in between.
- The Tuner listens through Android's `AudioRecord`, not the mixer, and finds the pitch in
  Kotlin with the McLeod Pitch Method. It listens only while its tab is open, and nothing
  it hears is recorded or kept.
- The Warm-up runs in a foreground service with a media session: the lock screen shows
  pause, next and stop, and the headphone button pauses (one press), skips to the next
  exercise (two) and goes back (three). Only one tool plays or listens at a time.
- The Warm-up's library (Patterns, Sounds and Programmes) and its settings are two small
  JSON files in the app's private storage, rewritten whole, atomically, on every change. A
  fresh install starts with eight Patterns, eight Sounds and a sample Programme. Nothing
  leaves the phone: there are no accounts and no sync.
- With "Play over other audio" on, the Warm-up and the Metronome play under a podcast
  instead of pausing it, and the headphone button stays with the podcast app: the Warm-up
  then has no media session at all, only a notification with pause, next and stop. Some
  Bluetooth headsets only send "pause" while any audio is still playing, so they can pause
  the other app but not resume it.
- The Step and Pattern editors can play a Step's Demo or a Pattern on the piano, using the
  same timeline as the Warm-up; doing so pauses a playing Programme.

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
