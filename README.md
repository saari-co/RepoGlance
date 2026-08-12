# RepoGlance

**Your GitHub repos, at a glance, on your Pixel home screen.**

RepoGlance is a read-only, widget-first Android app optimized for Google
Pixels: Material You home-screen widgets showing each repository's PR/issue
pressure, default-branch CI state, latest release, and data age — paired
with a fast issue navigator — without ever mutating GitHub.

Inspired by the excellent [RepoBar](https://github.com/steipete/RepoBar) by
Peter Steinberger (macOS menu bar). RepoGlance is the Pixel-native analog,
not a port: no local git, no menu bar — Glance widgets, an owner switcher,
and deep links into the GitHub mobile app.

## Status

Slice 1 available: typed snapshot model, fixture corpus, and truth-rule tests.

Slice 2 available: fixture-first Glance widgets (per-repo + stack) and an
in-app fixture mode (pinning, navigator, cached search), plus per-widget
repository/mode configuration and a Fold-first navigator shell.

The prototype live-data slice on this branch adds user-present GitHub App
sign-in, a verified `repoglance.ztoned.com` Android App Link, Keystore-backed
rotating tokens, live repository discovery, and repo-scoped open issue/PR
lists. It has been exercised on the maintainer's Pixel 10 Pro Fold. Widgets
remain fixture-backed in this checkpoint; background refresh, live widget
content, CI/release pressure, and general distribution remain planned.

## Planned

- **Widgets first.** Per-repo and multi-repo stack widgets in Material You
  dynamic color, light/dark, with the data age always visible. Pixel Fold
  cover- and inner-display sizes are first-class.
- **Account-first discovery.** Sign in once; switch between your account
  and your orgs; every repo you can see shows its open PRs, issues, CI
  state, and latest release.
- **Pin what matters.** Pin any repo in place (the pin toggle lives on the
  row, like pinning a session in the Claude Code app). Pinned repos float
  to the top, earn widget slots, and are the only background-refresh set —
  that is what keeps battery and API rate limits honest. No preset pins.
- **Issue navigator.** Scope account-wide, org-wide, or to one repo; show
  issues only, PRs only, or both; filter by Open / Mine / Mentions /
  Recently updated / Awaiting my review; instant search over cached rows.
  Full threads and every action deep-link to the installed GitHub app.
- **Pressure surfaces.** A Quick Settings attention tile and a single
  watched-CI-run live notification. No notification firehose.
- **Honest by contract.** Unknown never renders as zero; stale data is
  labelled with its age; rate-limited is a visible state, never silently
  masked with old numbers.

## Auth (prototype)

RepoGlance signs in through its public GitHub App in an Android Custom Tab.
State and PKCE bind the verified HTTPS return; short-lived user tokens refresh
and remain encrypted behind Android Keystore. RepoGlance never reads or reuses
browser cookies, and read-only permissions are the ceiling — the app performs
no GitHub writes.

GitHub's current GitHub App web flow still requires a client secret during code
exchange. A native APK cannot keep that value confidential, so the personal
prototype treats it as public, revocable build configuration. Default and CI
builds contain no credential and remain signed out; credential-bearing APKs
must never be published. General distribution requires either GitHub device
flow or a narrowly scoped confidential auth broker. See
[docs/AUTH_ARCHITECTURE.md](docs/AUTH_ARCHITECTURE.md).

## How this is being built

Product decisions are made in focused GrillTrack cycles and recorded
durably: the human-readable index is
[docs/PRODUCT_DECISIONS.md](docs/PRODUCT_DECISIONS.md); the canonical
ledger with full lifecycle history lives in `.grilltrack/`. Development
happens in the open on branches and PRs; `main` only ever claims what is
actually proven.

## License

[MIT](LICENSE)
