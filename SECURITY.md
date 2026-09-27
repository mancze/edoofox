# Security

## Supported versions

Only the latest published release is considered for fixes. There are no
backported security releases or guaranteed response times. This is a
best-effort personal project, not a monitored security service.

## Report privately

Use [Report a vulnerability](https://github.com/mancze/edoofox/security/advisories/new)
when private vulnerability reporting has been enabled for this repository.

If that option is unavailable, open an issue containing only a request for a
private contact channel, without exploit details or sensitive data. A maintainer
can arrange private reporting. Do not post a vulnerability publicly merely
because the private-reporting setting is not enabled.

Include the affected version, Android/WebView version, impact, and minimal
reproduction using a test page or fictional data. Do not send real credentials,
session cookies, student records, or the release signing key.

Examples in scope: unintended in-app navigation to an untrusted origin,
certificate handling mistakes, exposure of school session data, or a compromised
release/build process. Edookit and Plus4U service vulnerabilities should be
reported to their respective operators.

## Limits

Edoofox renders remote school content with JavaScript enabled. Keep Android and
Android System WebView updated. The app is not a content filter, a sandbox for
arbitrary websites, or a replacement for Family Link. External links can open
other applications.

See [privacy](docs/PRIVACY.md) and [release signing](docs/SIGNING.md).
