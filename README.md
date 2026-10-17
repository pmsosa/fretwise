![Guitarlele Tuner](docs/banner.jpg)

A simple, pretty Android tuner for the guitarlele. It listens through the microphone, finds which
string you played and shows how many cents sharp or flat it is.

A guitarlele is a guitar tuned up with a capo, so the tuner works the same way: pick a guitar tuning,
then pick the capo fret (0–7).

- Default: Standard + capo 3 → G C F Bb D G
- Capo 5 → A D G C E A (the common guitarlele tuning)
- Capo 6 → Bb Eb Ab Db F Bb
- Other base tunings: Drop D, Half step down, DADGAD, Open G
- Auto-detects the string, or tap a string to lock onto it
- Chords tab: a chord wheel. Pick a start chord, tap where it could go next, and each tap plays the chord and shows the guitar fingering
- Every build is signed with the same key (`signing/`, not in git, backed up nightly) so a new APK installs over the old one
- Your tuning and capo are remembered

## Build

    docker run --rm -v "$PWD:/workspace" -w /workspace -v android-gradle:/root/.gradle \
      android-build:35 ./gradlew assembleRelease

The APK lands in `app/build/outputs/apk/release/app-release.apk`. Pitch detection is YIN (`PitchEngine.java`).
