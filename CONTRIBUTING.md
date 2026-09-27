# Contributing

Thanks for helping a small fox carry the school bag.

This is a spare-time personal project. It is maintained when time and interest
allow, with no response-time promise, release schedule, or commitment to review
every issue or pull request. Silence usually means life happened.

## Keep it small

- Bug fixes, clear documentation, accessibility improvements, and translations
  are especially welcome.
- Open an issue before a large feature or architectural change. Please wait
  for agreement before investing substantial work.
- A useful change can still be declined if its maintenance cost is too high.
- Forks are welcome under MIT. Please distinguish independently signed builds
  from official releases, and choose a different application ID if appropriate.

## Report a bug

Include the app version, Android version, WebView version if known, steps to
reproduce, and what you expected. Use a synthetic example where possible.
Do not attach credentials, cookies, session tokens, student information, or
unredacted school screenshots. Follow [SECURITY.md](SECURITY.md) for vulnerabilities.

## Send a change

1. Fork the repository and work on a branch.
2. Follow [the development guide](docs/DEVELOPMENT.md); keep commits focused.
3. Run the build, unit tests, and lint. For gesture/menu changes, run the relevant
   QA checks locally on a separate emulator using the `.qa` app. GitHub CI does
   not build or run emulator QA because of hosted-runner storage constraints.
4. Explain the problem, the resulting behavior, and what you tested in your PR.
   For UI changes, include local QA results or explicitly state that it was not run.
   Small UI changes benefit from screenshots with fictional data.
5. Keep English/default and Czech/Slovak string pairs synchronized.
6. Do not commit APKs, signing material, local SDK paths, or emulator files.

No real school login is required for automated tests. Never run tests against
your everyday signed-in app.

Contributions are accepted under the project's MIT license. Submit only work
you have permission to contribute. No CLA is required. Be kind; disagreements
about scope are fine, personal attacks are not.
