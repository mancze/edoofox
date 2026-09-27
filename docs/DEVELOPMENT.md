# Development

## Toolchain

- Android SDK Platform 37 and Build Tools 36.0.0; SDK Platform Tools for device tests.
- JDK 26 (Temurin/Adoptium) matches the checked-in Gradle daemon JVM criteria.
  Gradle can provision that daemon JVM; install it explicitly for CI.
- Gradle 9.7.1 and Android Gradle Plugin 9.4.0 are pinned in the project.
- Java application source/target compatibility is 17.

Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK, or let Android Studio
create an ignored `local.properties` with `sdk.dir`. No private keys are needed.

```sh
git clone git@github.com:mancze/edoofox.git
cd edoofox
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew -PreleaseSigning=disabled :app:assembleRelease
```

On Windows use `.\gradlew.bat` in place of `./gradlew`.
Debug APKs are signed with your local Android debug key. Without local signing
configuration, release APKs end in `-release-unsigned.apk` and cannot be installed
until signed. Build outputs are under `app/build/outputs/apk/<variant>/`.
See [signing](SIGNING.md) for optional signed releases.

The production application ID and Java namespace are `cz.weborama.edoofox`.
Keep this ID for future updates. Earlier development builds used a different
ID; this branded app installs separately and does not migrate their data.

## Local-only isolated QA

GitHub CI runs builds, JVM unit tests, and lint. It does not build or run the
instrumentation QA suite. Hosted-runner storage constraints prevented reliable
emulator startup, so emulator QA is deliberately excluded from GitHub Actions.
A green CI result does not verify gestures or other on-device UI behavior.

Use a separate emulator and the `.qa` variant. No production app data or real
school credentials should be used. SDK API 36 is the tested emulator target.

```sh
./gradlew -Pqa -PreleaseSigning=disabled :app:assembleQa :app:assembleQaAndroidTest
adb -s YOUR_QA_SERIAL install -r app/build/outputs/apk/qa/edoofox-VERSION-qa.apk
adb -s YOUR_QA_SERIAL install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
adb -s YOUR_QA_SERIAL shell am instrument -w -e phase gestures cz.weborama.edoofox.qa.test/cz.weborama.edoofox.SmokeInstrumentation
adb -s YOUR_QA_SERIAL shell am instrument -w -e phase locales cz.weborama.edoofox.qa.test/cz.weborama.edoofox.SmokeInstrumentation
adb -s YOUR_QA_SERIAL shell am instrument -w -e phase about cz.weborama.edoofox.qa.test/cz.weborama.edoofox.SmokeInstrumentation
```

Substitute the actual version and emulator serial. `-Pqa` selects QA as the
instrumentation-test target and changes which unit-test tasks exist; run the
debug unit-test command separately without it. The gesture suite uses local
HTML fixtures; these tests do not log in to a real school.

The custom instrumentation reports `PASS:`, `FAIL:`, or a process crash.
Do not rely only on adb's exit code: the local `scripts/run-qa.sh` helper requires
an explicit PASS and rejects failures. With a dedicated emulator already booted,
`adb` on PATH, and exactly one current QA APK in the output directory, run:

```sh
ANDROID_SERIAL=YOUR_QA_SERIAL bash scripts/run-qa.sh
```

Use Bash (Git Bash on Windows) for this helper. Other historical smoke phases
contact live school pages and must not run against a real session.

## Code and translations

The app is Java plus platform Android UI/WebView, with no third-party runtime
libraries. Keep changes focused. Use vector assets for UI icons.
Strings live in `values`, `values-en`, `values-cs`, and `values-sk`.
English/default and Czech/Slovak pairs should match. Slovak intentionally uses
Czech; it is not advertised as a separate native app-language option.

The Gradle wrapper checksum is pinned. Update toolchain versions intentionally
and verify a clean clone before proposing upgrades. CI is defined in
`.github/workflows/`; no production signing secret is available to normal checks.
