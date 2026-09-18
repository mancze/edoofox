# Validation

## Edoofox 0.3.1 — 2026-09-18

- Header automatically hides on settled school content, while recognized public login/Plus4U pages keep it pinned. Detection uses a presentation-only, foreground-only boolean check, not credentials or an authentication guarantee. Public login selectors were verified against the school's signed-out HTML.
- Local-fixture touch tests passed for automatic hiding without scrolling, pinned login and Plus4U headers, same-document login/logout transitions, and keeping a manually revealed header open between checks. Existing history, two-step refresh, cancellation, nested-scroll, and editable-widget checks passed.
- Seven JVM tests passed; lint reported no issues. Debug APK and unsigned release AAB built; APK signature verified.
- Fresh dedicated API 36 QA emulator passed school selection/switching (including pinned headers), the complete header/gesture suite, and blank-page fallback. An initial school-menu run on the pre-existing emulator failed its window-focus check; rerunning in the dedicated instance passed. The pre-existing emulator was left running.
- Actual authenticated school layouts and physical-device usability still need the user's check. No sign-in data was cleared or inspected.
- Deliverables: `artifacts/Edoofox-0.3.1-debug.apk` and `artifacts/Edoofox-0.3.1-unsigned.aab`.
- APK SHA-256: `a3775798d13b996775e5580abe3a57358ed72c934113112dd2ee9a0f9cf5d1d2`.

## Edoofox 0.3.0 — 2026-09-16

- Debug APK and unsigned release AAB built. Seven JVM tests passed; Android lint reported no issues. APK signature, app label, application ID, and version verified.
- Android 16 / API 36 emulator checks use the separate `cz.weborama.edoofox.qa` app and local HTML fixtures. Existing signed-in app storage was not accessed or changed.
- Real touch input verified right-swipe Back, left-swipe Forward, automatic header hiding, full page expansion without a leftover header gap, pull-to-reveal without reload, and a separate pull-to-refresh exactly once.
- Short and cancelled pulls did not reload. Nested vertical content scrolled without refreshing. Horizontal widgets and text inputs did not trigger history navigation. Header-visible and header-hidden fixture screenshots were visually checked.
- Gestures have menu/button alternatives. Edge swipes remain Android-owned. TalkBack, keyboard handling, multi-touch, and actual authenticated Edookit layouts still need physical-device usability checks; they were not exhaustively exercised by this fixture suite.
- Deliverables: `artifacts/Edoofox-0.3.0-debug.apk` and `artifacts/Edoofox-0.3.0-unsigned.aab`.
- APK SHA-256: `20daf51520f70c6f87975e35c53e2d1cc2f5fb60bfa9c320d2c7900ff3760994`.

## Edoofox 0.2.0 — 2026-09-16

- Debug APK and unsigned release AAB built; seven JVM tests passed; Android lint reported no issues.
- Device checks passed: first-run school prompt without loading a default school, invalid input rejection, lowercase/whitespace normalization, cancel, switching, new navigation history, selected-school header, and external routing for links to the former school.
- A forced process restart retained the selected school and skipped the first-run prompt. Switching back to the original school restored an existing signed-in session.
- The historical signed-out login regression could not run against that existing session. No logout, cookie clearing, or account changes were performed. The regression test now skips when the expected public login is not shown.
- Setup and school-menu screens were visually checked.
- Deliverables: `artifacts/Edoofox-0.2.0-debug.apk` and `artifacts/Edoofox-0.2.0-unsigned.aab`.
- APK SHA-256: `aea6d4ed58303f1b5676eef4613219187b7f8dc5f2f3e009329ee50185024cfd`.

## Original build — 2026-09-15

## Build

- Debug APK, device-test APK, and unsigned release AAB built successfully.
- Four JVM link-policy tests passed.
- Android lint: no issues found.
- APK signature verified with Android SDK `apksigner` (v2 signing).

## Device checks

Tested on the isolated `Edoofox_Test` Android 16 / API 36 emulator:

- Public school login page renders.
- Automatic Plus4U session-restoration redirect completes.
- Interactive Plus4U sign-in form opens inside Edoofox.
- Internal link stays in the WebView.
- System Back navigates history and keeps the app open.
- External HTTPS link dispatches a browser intent and preserves the app page.
- Blocked network request shows the native error screen; Retry recovers after restoring network access.
- A synthetic persistent cookie survives a forced app stop and new process. The test cookie was removed afterward.
- Sign-in and error screenshots were visually reviewed.

No real credentials were entered. Completing account login, signing out, and retaining the actual Edookit session require a user's device test. Google, Microsoft, Apple, and +4U Access sign-in are outside this version's scope. File uploads, protected downloads, and physical-device behavior have not been verified.

## Deliverables

- `artifacts/Edoofox-0.1.0-debug.apk` — installable development build, 903,445 bytes.
- APK SHA-256: `652a3101737c44164c803b4d9f4d8f1bdd41f40ea064623d57d85b000508a80b`.
- `artifacts/Edoofox-0.1.0-unsigned.aab` — unsigned bundle for later release preparation, not ready for Play upload.
- `artifacts/screenshots/` — public login, Plus4U sign-in, and offline screens.

Artifacts are ignored by Git and can be regenerated. No publication or account creation was performed.
