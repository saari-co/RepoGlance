# RepoGlance

**Your GitHub repos, at a glance, on your Pixel home screen.**

RepoGlance is a read-only, widget-first Android app optimized for Google
Pixels: Material You home-screen widgets showing each repository's PR/issue
pressure and data age (default-branch CI state and latest release are
planned) — paired with a fast issue navigator — without ever mutating
GitHub.

Inspired by the excellent [RepoBar](https://github.com/steipete/RepoBar) by
Peter Steinberger (macOS menu bar). RepoGlance is the Pixel-native analog,
not a port: no local git, no menu bar — Glance widgets, an owner switcher,
and deep links into the GitHub mobile app.

## Status

RepoGlance is in beta. `main` is the source for **0.4.0-beta.1**, the first
Google Play candidate; it is not on Google Play yet, and the launch is
tracked in [#7](https://github.com/saari-co/RepoGlance/issues/7). Version
tags build signed APKs and AABs on
[GitHub Releases](https://github.com/saari-co/RepoGlance/releases)
([docs/RELEASING.md](docs/RELEASING.md)).

Available on `main`, with device proof in [`proof/`](proof/) and
[`.grilltrack/proof/`](.grilltrack/proof/):

- **Sign in with GitHub.** A GitHub App device flow that the user completes
  on GitHub's own page. The token is encrypted with Android Keystore and
  rotated before it expires. Proven on a Pixel 10 Pro Fold, and again on a
  Pixel 10 Pro XL after the move to API 36. See [Auth](#auth) below.
- **Live catalog.** Every repository the GitHub App installation shares,
  filtered by account or organization, searched, and sorted by recent push
  or A to Z. Pin a repository in place from its row; pinned repositories
  sort first.
- **Repository view.** Open issues and pull requests (Issues, PRs or both)
  with search over the loaded rows. A tap opens the item in the GitHub app.
- **Widgets.** A Repository widget set up from the live catalog, and a
  Pinned repos widget over the pinned set (named so in the launcher's widget
  picker too). Every value shows its age; a stale value
  says `last good`, a missing one `no data`, never zero. Pinned repositories
  refresh about every 30 minutes on a network connection (WorkManager, no
  wakelock or foreground service) within a visible rate-limit budget, and
  both widgets redraw from saved data after an app update.
- **Quick Settings tile.** It shows the latest push, and a tap opens the
  catalog.
- **Sample mode.** **Explore with sample data** on the sign-in screen opens
  the real catalog, repository view and navigator on seven made-up
  repositories under RepoGlance's own accounts, marked `SAMPLE` on every
  screen, with no GitHub account and no network call.
  - The Repository and Pinned repos widgets can be set up from the sample
    repositories and show `sample` where a live widget shows its time. The
    Quick Settings tile reads `Sample data · <repo> · <age>`. Their taps open
    RepoGlance, never GitHub.
  - Sample mode stays until the user chooses **Sign in with GitHub**, which
    also clears the sample widget setups.
  - Verified on the emulator (Android 16), with a signed-in Fold run for the
    widgets. A Pixel 10 Pro XL (Android 17) ran an earlier owner set.
- **Theme.** Settings → Theme chooses `Light`, `Dark` or `System default`
  (the default) for RepoGlance alone, including its start window and status
  bar; widgets and the Quick Settings tile keep the phone's theme. Verified
  on the emulator (Android 16) with the phone light and on a Pixel 10 Pro
  XL (Android 17) with the phone dark.
- **Settings and Widgets.** The three-dot menu holds **Widgets** and
  **Settings** on the live and sample catalogs, and **Settings** alone on the
  Connect screen. **Widgets** adds either widget through the launcher's own
  *Add to home screen* sheet (`requestPinGlanceAppWidget`), lists the widgets
  already placed, and opens a repository widget's setup to change it.
  Settings holds Widgets, Appearance (theme), GitHub access (**Manage GitHub
  access** and **Disconnect GitHub**, moved here from the menu, with a
  session only) and About (version, privacy policy, source). Verified on the
  emulator (Android 16) signed out and in sample mode, adding, changing and
  removing both widgets, and on a Pixel 10 Pro XL (Android 17) with a live
  session for a repository widget.
- **Widget previews.** The launcher's *Add to home screen* sheet and its
  widget picker show each widget as its sample: Repository with `sample` in
  its time slot, Pinned repos with three sample pins under a `Pinned · 3`
  band. They show no clock time and none of the user's data. Android 15+
  gets a preview generated from the widgets; without one the launcher falls
  back to a static copy. Verified on a Pixel 10 Pro XL (Android 17, dark,
  live session) and on the emulator (Android 16, light and dark, sample
  mode), including the static copy after removing the generated preview.
  Android 12–14, where only the static copy applies, has not been run.
- **Read-only.** RepoGlance performs no GitHub writes of any kind.

Not available yet:

- CI state, latest release, and a watched-CI live notification. Live CI is
  not fetched, so the widgets have no CI column.
- Account-wide and organization-wide issue navigation with the Mine,
  Mentions and Awaiting-my-review filters. On live data the navigator is per
  repository today.
- Google Play distribution ([#7](https://github.com/saari-co/RepoGlance/issues/7)).
  Revoking the earlier prototype client secret remains maintainer-gated.

## Planned

The product direction is in [VISION.md](VISION.md). Next up, beyond the
"not available yet" list above: a CI column and pressure fields from the
read-only Checks, Commit statuses and Contents permissions, and the single
watched-CI-run notification. Honest by contract throughout: unknown never
renders as zero, stale data is labelled with its age, and rate-limited is a
visible state, never masked with old numbers.

## Auth

RepoGlance requests a short user code from its public GitHub App, keeps that
code visible, and waits for an explicit **Copy code & open GitHub** tap. That
action copies the code and opens GitHub's exact verification page in an Android
Custom Tab. Once GitHub authorizes and the token is saved, RepoGlance brings
itself back over the tab and shows its mark with "Finishing sign-in…" and
"Loading your repositories…" until the catalog appears (proven on a Pixel
Fold, Android 17; elsewhere the code screen tells the user to close the tab).
Returning to RepoGlance by hand before that leaves the code visible. GitHub currently
uses segmented one-character fields on Android, so normal clipboard paste may
not fill the complete code and manual entry can still be required.
Returning also wakes a paused pending check, which still rechecks GitHub's
minimum interval before any request. The ViewModel honors slowdown and expiry
responses, retries transient poll I/O only until code expiry, and stops
immediately when sign-in is cancelled. Authorized tokens are committed with
Android's atomic-file sync and encrypted behind Android Keystore; a save failure
is cleared and reported without exposing its exception or token. The APK
contains only the public client ID, including for silent refresh rotation when
GitHub issues expiring user tokens.

Current authentication has no callback-host dependency, embedded confidential
client credential, browser-cookie reuse, or auth broker. Read-only permissions
are the ceiling and the app performs no GitHub writes. The GitHub App must have
Device Flow enabled before sign-in can succeed; an operator-approved Codex
browser action enabled it on 2026-08-12. See
[docs/AUTH_ARCHITECTURE.md](docs/AUTH_ARCHITECTURE.md).

Exact repaired-source proof on the registered Pixel 10 Pro Fold completed the
device authorization, loaded the public live RepoGlance navigator, and retained
the encrypted session across a force-stop/cold launch. Selected sanitized
evidence is linked from
[proof/github-auth-live-20260812/PROOF.md](proof/github-auth-live-20260812/PROOF.md).

## How this is being built

Product decisions are made in focused GrillTrack cycles and recorded
durably: the human-readable index is
[docs/PRODUCT_DECISIONS.md](docs/PRODUCT_DECISIONS.md); the canonical
ledger with full lifecycle history lives in `.grilltrack/`. Development
happens in the open on branches and PRs; `main` only ever claims what is
actually proven.

## License

[MIT](LICENSE)
