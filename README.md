# Edoofox

A small native Android app for a user-selected school at `https://<subdomain>.edookit.net/`. Java + Android WebView, with no third-party runtime libraries.

Renamed from Edoofox. The Android application ID remains `cz.weborama.edoofox` so the renamed APK can update an existing installation and retain its app data.

## Current scope

- Android 8.0 (API 26) and later.
- First launch asks for the school subdomain (for example, `zsslovanak`). The selection is stored on the device across launches and updates. Upgrading from the fixed-school version also prompts once; existing cookies are retained.
- **More options → Switch school** changes the saved school. Cancel keeps the current page. Switching starts fresh navigation history, so Back cannot return to the previous school's pages. The current school appears below the app name.
- Enter only one DNS label: 1–63 ASCII letters/numbers, with hyphens allowed inside the name. Input is trimmed and lowercased; full URLs, dots, and invalid characters are rejected. Availability is checked by loading the school page; use Switch school again if the subdomain does not exist.
- Original purple fox vector icon, inspired by the reference's simple purple/white palette.
- Only the selected school's pages stay inside the app. Other HTTP(S) websites, including other school subdomains, open in a browser; telephone and email links open their appropriate apps.
- **Plus4U username/password sign-in only for this version.** The verified `uuidentity.plus4u.net` authentication paths stay inside the app so the login callback can return to the same session. The website may still advertise Google, Microsoft, Apple, and +4U Access; those methods are not supported by this version.
- Cookies and DOM storage are retained. Persistent cookies are flushed to disk when pages finish and the app pauses. Actual session lifetime and the “stay signed in” option are controlled by Edookit/Plus4U.
- Swipe right within the page to go Back, or left to go Forward. Android's system-edge Back gesture stays unchanged. Back/Forward also appear in the menu.
- The header hides automatically once the selected school's content has settled, without needing a scroll. Login/Plus4U, school selection, and native error screens keep the header visible. A lightweight foreground-only check recognizes public login markers and authentication routes; this is a presentation heuristic, not proof of authentication. Blank/loading pages remain visible, and site layout changes may require detection updates.
- Pulling downwards reveals the header; it stays open until scrolling away or navigating. At the top of the page, the first pull reveals a hidden header **without reloading**. Release, then pull again until “Release to refresh” appears to reload. A visible header permits refresh on the first pull. Short/cancelled pulls do nothing.
- Horizontal scroll containers and editable fields retain their own gestures. Nested vertical scrolling is respected. Custom gestures are disabled while the keyboard or school chooser is open and during touch exploration (TalkBack); the header remains accessible. Error screens reveal the controls. Loading prevents duplicate pull-refreshes. System status/navigation bars remain visible.
- Refresh button, school homepage, and open-in-browser menu.
- Page loading indicator and offline/server/certificate error screens with retry.
- Czech and English native controls, following the device language.
- Basic system file picker. Downloads are handed to the external browser and may require signing in there separately; protected downloads and camera capture are not verified.
- No notifications, analytics, JavaScript/native bridge, or app-owned backend. Invalid certificates are rejected. Cleartext HTTP cannot load in the embedded view. App data backup is disabled.

## Build

Open this directory in Android Studio and let Gradle sync. Use the bundled Java runtime and install Android SDK Platform 37 and Build Tools 36.0.0. `local.properties` holds the machine-specific SDK path and is ignored by Git.

Builds use the pinned Gradle 9.7.1 wrapper (distribution checksum verified) and Android Gradle Plugin 9.4.0. Gradle 9.7.1 supports the Java 25 runtime bundled with the installed Android Studio.

```powershell
# Set JAVA_HOME to your Android Studio bundled Java runtime before building.
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The installable development APK is `app/build/outputs/apk/debug/app-debug.apk`. It is signed with the local Android debug key. Keep it for testing; it is not the Play Store release.

The current packaged build is `artifacts/Edoofox-0.3.1-debug.apk`.

## Device smoke tests

Gesture tests use a separate `.qa` application ID and locally intercepted HTML pages, never the normal app's signed-in browser storage:

```powershell
.\gradlew.bat -Pqa :app:assembleQa :app:assembleQaAndroidTest
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5556 install -r app/build/outputs/apk/qa/app-qa.apk
& $adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
& $adb -s emulator-5556 shell am instrument -w -e phase gestures cz.weborama.edoofox.qa.test/cz.weborama.edoofox.SmokeInstrumentation
```

This checks actual touch swipes, history, automatic header hiding, pinned login/Plus4U headers, same-document login/logout transitions, manual reveal retention, two-step refresh, short/cancelled pulls, nested scrolling, horizontal widgets, and text fields. Do not combine `-Pqa` with the normal debug unit-test command.

Use an isolated emulator, without a real signed-in account. The tests navigate the public school/Plus4U pages, load test-only HTML, temporarily block WebView network loads to test retry, and create then remove a synthetic cookie. No credentials are entered.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s emulator-5556 shell am instrument -w -e phase schools cz.weborama.edoofox.test/cz.weborama.edoofox.SmokeInstrumentation
& $adb -s emulator-5556 shell am force-stop cz.weborama.edoofox
& $adb -s emulator-5556 shell am instrument -w -e phase verify-school cz.weborama.edoofox.test/cz.weborama.edoofox.SmokeInstrumentation
& $adb -s emulator-5556 shell am instrument -w cz.weborama.edoofox.test/cz.weborama.edoofox.SmokeInstrumentation
& $adb -s emulator-5556 shell am force-stop cz.weborama.edoofox
& $adb -s emulator-5556 shell am instrument -w -e phase verify-cookie cz.weborama.edoofox.test/cz.weborama.edoofox.SmokeInstrumentation
```

The custom instrumentation prints `PASS` or `FAIL`. Screenshots are written to the test app's external files directory under `smoke/`. Unit tests exercise exact-origin matching, deceptive hosts, allowed external schemes, and the narrow authentication exception.

If a school home page is already signed in, the public-login regression phase prints `SKIP` and leaves that session intact. Use a fresh emulator for the full signed-out regression suite. Automatic failure screenshots are disabled to avoid capturing account content.

The `schools` phase resets only the test emulator's saved school preference, then checks first launch, invalid input, normalization, cancel, switching, cleared history, and routing to a previous school. The `verify-school` phase checks persistence after a process restart and selects `zsslovanak` for the public-login tests. School selection survives without a network connection; it does not guarantee that an entered school exists or uses the same sign-in provider.

Account-authenticated login, logout, and the actual school's session retention still need a device test with the user's own Plus4U account. Credentials should be entered only into the Plus4U page by the user.

## Later Google Play publication

The initial application ID is `cz.weborama.edoofox`; choose the final ID before first publication. An unsigned release bundle can be built with:

```powershell
.\gradlew.bat :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`.

Before publication, use Android Studio's **Generate Signed App Bundle / APK** flow to create an upload key and signed release bundle. Back up the key securely; keys and signing configuration are ignored by Git. Then prepare the Play Console account, store listing, privacy disclosures, and required testing. Publication has not been performed, and the unsigned bundle is not ready to upload.
