# Changelog

All notable changes to RepoGlance. Each `## X.Y.Z` section here is the exact
release-notes body used for that tag's GitHub Release — see
[RELEASING.md](RELEASING.md).

## 0.4.0-beta.1

First Google Play candidate (closed testing). Everything merged since
`v0.3.0-beta.1`, #6 through #47. RepoGlance stays read-only: it never
changes anything on GitHub.

**Sign-in**
- GitHub App device flow. The code stays on screen, and **Copy code & open
  GitHub** opens GitHub's page in a Custom Tab. Once GitHub authorizes,
  RepoGlance comes back over the tab and shows its mark until the catalog
  loads. This was proven on a Pixel Fold running Android 17; elsewhere the
  code screen tells you to close the tab (#31).
- Session start-up and token storage run off the main thread (#16, #28).
- **Manage GitHub access** is in the header menu (#6).

**Catalog and repositories**
- The live catalog can be filtered by account or organization and searched
  (#6). It sorts by recent push or A to Z (#19).
- Pin a repository in place from its row. Pinned repositories sort first
  (#20).
- The repository view lists open issues and pull requests, shows Issues, PRs
  or both, and searches the loaded rows. A tap opens the item in the GitHub
  app.

**Widgets and Quick Settings tile**
- The per-repository widget is set up from the live catalog and shows that
  repository's saved counts with their age: `last good` when stale, never a
  made-up zero (#12, #21). The compact layout merges counts and has a 140 dp
  minimum width (#41).
- The stack widget lists every pinned repository, most recent push first
  (#23). Tapping a widget opens that repository or the catalog in
  RepoGlance.
- Pinned repositories refresh about every 30 minutes while a network is
  available, through WorkManager, within a visible GitHub rate-limit budget.
  There is no wake lock and no foreground service (#22, #29).
- Disconnecting GitHub or cancelling sign-in now always redraws placed
  widgets, so they never keep the previous live counts after sign-out (#46).
- Widgets redraw from saved data after an app update and read their stores
  off the main thread (#24, #26). Their look now matches the app (#39), and
  the widget picker describes only what the widget shows (#40).
- A Quick Settings tile shows the latest push and opens the catalog (#19).

**Sample mode**
- **Explore with sample data** on the sign-in screen opens the real catalog,
  repository view and navigator on seven made-up repositories. It is marked
  `SAMPLE` on every screen, needs no GitHub account and makes no network
  call (#43).
- In sample mode, the repo and stack widgets are set up from the sample
  repositories and show `sample` where the time would be. The Quick Settings
  tile reads `Sample · <repo> · <age>`. Their taps open RepoGlance, and
  sample mode never refreshes in the background. Signing in clears the
  sample widget setups. An unconfigured repo widget now says `Tap to choose
  a repository` and opens its setup (#45).

**Look**
- Ringed commit-eye launcher icon, a themed monochrome glyph, and a start
  window that matches the app (#34). The mark also shows while RepoGlance
  checks for a saved session (#18).
- Tonal status pills, mono label type, and Google-style shapes and controls
  (#36, #37, #38).

**Platform and build**
- Targets and compiles against Android 16 (API 36) on AGP 8.10.1. The
  minimum is Android 12 (API 31) (#42).
- CI is a hard gate: lint, detekt with Compose rules, and warnings as errors
  (#13).
- A `verify-repoglance` skill and feature map drive the real app for proof
  (#14, #25, #30).
- The privacy policy now lists every permission the installed app declares,
  including the four that WorkManager adds, and describes sample mode and
  **Disconnect GitHub**.

**Known limits**
- The widgets have no CI column, and there is no latest-release field and no
  watched-CI notification yet.
- The return over the GitHub tab can need a manual switch back after a slow
  authorization (#32).
- The start-window colour on Android 12 and 13 is approximate and unverified.

## 0.3.0

- Switch to semver tag-driven GitHub Releases (`vX.Y.Z`, `-beta.N`), retiring
  the rolling `canary` prerelease model. `versionName`/`versionCode` are now
  derived from the pushed git tag at build time instead of being
  hand-committed.
- Add `.github/workflows/release.yml`: builds a release APK and AAB on
  `v*` tag push, signs conditionally when `ANDROID_KEYSTORE_B64` is
  provisioned (falls back to an unsigned build otherwise), and publishes a
  GitHub Release with the APK, the AAB, and a `SHA256SUMS` checksum file.
  Play Store upload is stubbed (`play-internal-upload`, disabled) pending
  Play Console provisioning.
- Add `docs/RELEASING.md` documenting the tag convention, the versionCode
  formula, the cut procedure, and the signing model.
