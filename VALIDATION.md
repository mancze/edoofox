# Validation

## Edoofox 0.3.5 — 2026-09-18

- Added Czech/English login guidance below recognized school-login and Plus4U pages: use Plus4U email/password; Google, Microsoft, Apple and +4U Access are not currently supported in Edoofox. The native notice does not cover the form, modify provider buttons, or change login routing.
- Dedicated API 36 QA tests passed notice visibility on school login and Plus4U, disappearance on content/blank pages, return after same-document logout, and existing gesture/mascot regressions. The test explicitly waits for asynchronous page classification. The notice screenshot was visually reviewed on a local fixture.
- All translation-resource checks and seven JVM tests passed; lint reported no issues; APK signature verified. Real credentials were not used. Physical-device keyboard/large-font layouts were not tested.
- Deliverables: `artifacts/Edoofox-0.3.5-debug.apk` and `artifacts/Edoofox-0.3.5-unsigned.aab`.
- APK SHA-256: `df5464fe0f898596e561712bc1c9c4d89062b79fbd94689b57a01b3de2679a2d`.

## Edoofox 0.3.4 — 2026-09-18

- Replaced the direct GitHub menu entry with About / O aplikaci: fox icon, Edoofox, an honest website-wrapper slogan, current build version, author Michal Novák, and the external GitHub link. The URL remains the requested `https://github.com/` placeholder.
- Added a native floating fox shortcut only while the document is at the top and the header is fully hidden. Tapping reveals the header without navigation or reload. It is absent while the document is scrolled, the header is visible, or loading/setup/login/error UI is active. Website scroll-to-top behavior is not modified.
- Dedicated API 36 QA tests passed About menu activation, all displayed fields, external GitHub routing, and Close. The initial About test raced the menu-opening animation; it now waits for the dialog to appear before checking content. The About screenshot was visually reviewed.
- Touch tests passed fox visibility, native tap-to-reveal, no reload, disappearance while scrolling/header-visible, and reappearance on returning to the top, alongside existing gesture regressions. Locale checks passed for all app strings and ordered language preferences. Mascot placement was visually reviewed on a local fixture; actual authenticated website layouts remain unverified.
- Seven JVM tests passed; lint reported no issues; APK signature verified. No real account data was used or changed.
- Deliverables: `artifacts/Edoofox-0.3.4-debug.apk` and `artifacts/Edoofox-0.3.4-unsigned.aab`.
- APK SHA-256: `5da4d58ad4d5669babdb1478103a82f769749a87ca552227ec32e0cfdbaf9bbb`.

## Edoofox 0.3.3 — 2026-09-18

- Added Project on GitHub / Projekt na GitHubu to the menu. It uses the existing external-browser handler and temporarily points to `https://github.com/`; the repository URL is not yet supplied.
- Seven JVM tests passed; lint reported no issues; English/default and Czech/Slovak translation pairs match. Debug APK and unsigned release AAB built; APK signature verified. No new emulator tap-through was performed for this small menu addition.
- Deliverables: `artifacts/Edoofox-0.3.3-debug.apk` and `artifacts/Edoofox-0.3.3-unsigned.aab`.
- APK SHA-256: `edea78661b8985b32c525842774bf909899a786d7662caf25fd9af38521dd0d7`.

## Edoofox 0.3.2 — 2026-09-18

- Uses Android's native ordered locale/resource matching, with no custom locale override. English/default and Czech/Slovak resource pairs provide the two UI translations. The standard API 33+ per-app language configuration lists Czech and English.
- Dedicated API 36 QA emulator passed ten language-list configurations: Czech, Slovak, English, German, Arabic, and mixed preference lists. All app strings matched the expected translation, including secondary Czech/Slovak preferences and English taking precedence when ordered earlier. The packaged locale configuration was also checked.
- Menu text now says Edookit dashboard / Nástěnka Edookitu and still opens the selected Edookit root. No unverified external school website entry was added.
- Seven JVM tests passed; lint reported no issues. Two intentional manifest lint exceptions are documented inline: API 33-only language settings and Czech wording supplied under Slovak resources without advertising a third translation.
- Debug APK and unsigned release AAB built; APK signature verified. No system-language settings or signed-in account data were changed. Full visual testing in every locale and language-split installation through Google Play remain untested.
- Deliverables: `artifacts/Edoofox-0.3.2-debug.apk` and `artifacts/Edoofox-0.3.2-unsigned.aab`.
- APK SHA-256: `fe6a32ab80e3ecca5e720151d4d04cb46d16c9d80c5ab1a9de1102ebfe5a1157`.

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
