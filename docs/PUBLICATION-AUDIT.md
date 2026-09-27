# Pre-publication review

Reviewed locally on 2026-09-27, starting from `8c320c2` (16 reachable commits).
This is a repository-readiness review, not a penetration test or a guarantee
that no unknown secret exists.

## Checks and findings

- Gitleaks 8.30.1 scanned all refs with `git . --log-opts="--all" --redact`:
  no detected leaks (the tool reported 15 scanned commits).
- A separate content comparison covered all 114 reachable historical blobs
  after the initial build-preparation commit, including the initial tree:
  no match for the actual local signing passwords, keystore bytes, or
  base64-encoded keystore. No secret values were included in reports.
- Historical filenames contained no APKs, AABs, keystores, local signing
  properties, or machine-specific SDK properties.
- Commit identity uses the author's GitHub no-reply email, not a private
  email address. The author's name is intentionally public in the app.
- Existing documentation/history contains a Windows username/path and a
  real school's publicly accessible subdomain used during development.
  These are personal context, not credentials. Current onboarding docs use
  generic paths. No history rewrite was performed.
- Historical documentation names local screenshots, but no screenshots or
  student records were found among the tracked historical files.
- The new README screenshot was captured from the isolated QA app with the
  fictional school label `about-test`; it contains no school account data.
- The Gradle wrapper JAR is the expected tracked binary. Signing keys, build
  outputs, emulator files, audit tools, and reports remain untracked/ignored.
- A clean local clone without `keystore.properties` or `local.properties`
  passed debug build, unit tests, lint, and unsigned release build.
- Explicit signing without credentials failed as intended; the clone's release
  APK was verified to be unsigned and zip-aligned. A disposable test key signed
  it successfully using the same password-via-environment approach as CI.
- The actual CI QA script passed gestures, locales, and About checks on an
  isolated API 36 emulator. The release APK verified against the existing key.
- Actionlint 1.7.12 accepted all three workflows; the QA Bash script passed syntax
  checking. GitHub-hosted execution has not yet been exercised.

## Changes prompted by the review

Optional signing; explicit unsigned artifact names; broader key/artifact ignore
patterns; MIT license and third-party notice; public-oriented README; security,
privacy, contribution, and signing documentation; actual GitHub project link.

No signing material has been uploaded to GitHub. Repository settings, environment
protection, private vulnerability reporting, and CI execution still need GitHub
setup after the branch is published. The workflow files do not configure these
settings by themselves.

## Before publication

Review the remaining historical personal context if it matters to you. If a
real secret is ever found in history, revoke/rotate it before considering
history cleanup; deleting the latest file alone is not sufficient.
