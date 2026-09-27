# Signing and releases

## Local builds

No signing key is needed for debug builds or unsigned release builds.
The optional local file is `keystore.properties`; copy
`keystore.properties.example` and fill it with your own key details.
Keep the real file and key out of Git and back both up securely.

Modes:

- Default `auto`: use a complete local signing configuration if present;
  otherwise produce an explicitly named `-release-unsigned.apk`.
- `-PreleaseSigning=disabled`: never read local signing credentials; used by CI.
- `-PreleaseSigning=required`: fail if signing configuration is absent.

For an installable official release:

```sh
./gradlew -PreleaseSigning=required :app:assembleRelease
```

Use the Weborama release key created on 2026-09-27 for future releases.
Its public identity is `CN=Edoofox, OU=Android, O=Weborama, C=CZ`, with alias
`edoofox-release`, 3072-bit RSA, and certificate expiry on 2054-02-12.
Future updates require this key and application ID `cz.weborama.edoofox`.

This is a replacement key, not a signing-key rotation lineage. Existing
development APKs, including the previously distributed 0.3.11 APK, used the
old key. They cannot be updated in place with this key: uninstall first (which
removes local settings and sessions), then install and sign in again. Keep
the old key backed up. Fork maintainers should use their own keys.

The new certificate's verified public SHA-256 fingerprint is:

```text
ce6292a8755206e8d0a32c23e86c8e3b955788e911b9915485145baade0bb779
```

A certificate fingerprint is public, not a secret. It is different from an
APK file checksum, which changes with each build.

## How secrets work in a public GitHub repository

Public source does not make Actions secrets public. GitHub stores secrets
encrypted and supplies them only to eligible jobs. A workflow that receives a
secret can still deliberately leak it: masking logs is not an access boundary.
Anyone able to change code that runs with the key must be trusted.

Use a protected environment, not ordinary repository-wide secrets, for the
production key. PR validation must not receive production credentials.
Never build fork code with secrets using `pull_request_target`. Prefer ephemeral
GitHub-hosted runners rather than a personal/self-hosted machine for public PRs.

## One-time GitHub setup

These settings and secrets are NOT provisioned by committing the workflow.

1. Open `mancze/edoofox → Settings → Environments`; create `release-signing`.
2. Restrict deployment branches to **main only**. Do not allow PR refs.
3. Add yourself as a required reviewer. For a solo maintainer, leave
   **Prevent self-review** off so you can approve your own manual release.
   With another trusted reviewer, enable it. Disable administrator bypass
   if you want the approval gate to apply to admins too.
4. Add the following **environment secrets**, not plain variables or files:

| Name | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the existing release keystore |
| `ANDROID_STORE_PASSWORD` | Existing keystore password |
| `ANDROID_KEY_ALIAS` | Exact alias from your private signing configuration; do not change it just to match the app name |
| `ANDROID_KEY_PASSWORD` | Existing key password |

5. Add environment **variable** `ANDROID_SIGNING_CERT_SHA256` with the fingerprint above.
6. Protect `main` and release tags against deletion/force pushes and restrict
   who can create `v*` tags. Require the Build and checks CI job once it has run.
   A solo maintainer need not require someone else's PR approval for every change.
7. Enable private vulnerability reporting and GitHub secret scanning/push
   protection where available in repository security settings.
8. Keep an encrypted offline backup of the key and passwords. GitHub secrets
   are not a backup system and cannot be read back through the UI.

To encode the key on Windows without printing it in a terminal, use PowerShell
on your own machine (enter the full path when prompted):

```powershell
$keyPath = Read-Host 'Signing keystore path'
[Convert]::ToBase64String([IO.File]::ReadAllBytes($keyPath)) | Set-Clipboard
```

Paste into the secret field, then clear the clipboard with `Set-Clipboard -Value ''`.
Clipboard managers/history may retain it; handle that as sensitive too.
For CLI setup, `gh secret set NAME --repo mancze/edoofox --env release-signing`
accepts a value through stdin. Do not pass passwords as command-line literals.
Base64 is only binary-to-text encoding, **not encryption**. GitHub's secret
storage provides the encryption.

## Prepared workflows

- `ci.yml`: credential-free builds, JVM unit tests, lint, and unsigned release
  on PRs/pushes; no signing, publication, or instrumentation QA.
- `release.yml`: manual, main-only, builds an unsigned APK, signs on a separate
  runner after environment approval, then creates a **draft** GitHub Release.

Emulator QA is local-only because of hosted-runner storage constraints; there
is no GitHub emulator workflow. See [local QA instructions](DEVELOPMENT.md#local-only-isolated-qa).

The signing job does not check out the repository, execute Gradle, restore a
Gradle cache, or receive a write-capable repository token. It downloads only the
unsigned APK produced in the same workflow run and signs using Android SDK
tools. Secrets exist only in that step; a temporary key file is removed on exit.
Only the draft-publication job receives `contents: write`; it receives no
signing secrets. All third-party actions are pinned to commit SHAs.

A compromised signing job or Android SDK tool could still steal the key.
Review workflow/action changes before approving releases. Do not approve an
unexpected environment request. Local signing remains a supported alternative.

## Make a GitHub release

1. Merge reviewed changes into `main`, increment `versionCode` and `versionName`,
   and check CI. Run relevant local QA for UI/gesture changes; CI does not cover
   emulator behavior. Use SSH for Git:
   `git remote set-url origin git@github.com:mancze/edoofox.git`.
2. Tag the exact current main commit, for example `v0.3.12`, and push that tag.
   The workflow checks that the tag points to its selected main commit and
   matches the app's version.
3. Run **Signed release (draft)** from the Actions tab, select **main**, and
   enter the existing tag. Approve the signing environment when prompted.
4. Inspect the draft APK, its `SHA256SUMS.txt`, certificate fingerprint, and
   generated notes. Test installation/update on a suitable device.
5. Publish the draft manually when satisfied. Do not replace an already
   published version's APK silently; release a new version.

Existing drafts/releases are not overwritten by this workflow. Re-running a
successful release creation requires handling the existing draft explicitly.
There is no automatic Play Store publication.

## Sources

- [Using secrets in Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows/use-secrets)
- [Environment protection](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments)
- [Untrusted PR workflows](https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
