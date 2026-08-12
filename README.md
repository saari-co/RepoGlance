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
device-flow sign-in, Keystore-backed rotating tokens, live repository
discovery, and repo-scoped open issue/PR lists. The earlier confidential-flow
checkpoint was exercised on the maintainer's Pixel 10 Pro Fold; the current
public-client device-flow repair is source- and JVM-verified only and does not
claim new device proof. Widgets remain fixture-backed and visibly say
`FIXTURE PREVIEW`; background refresh, live widget content, CI/release
pressure, and general distribution remain planned.

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

RepoGlance requests a short user code from its public GitHub App, keeps that
code visible, and opens GitHub's verification page in an Android Custom Tab.
The ViewModel polls no faster than GitHub's interval, honors slowdown and
expiry responses, and stops immediately when sign-in is cancelled. The APK
contains only the public client ID. Device-flow access and refresh tokens stay
encrypted behind Android Keystore, including silent refresh rotation when
GitHub issues expiring user tokens.

Current authentication has no callback-host dependency, embedded confidential
client credential, browser-cookie reuse, or auth broker. Read-only permissions
are the ceiling and the app performs no GitHub writes. The GitHub App must have
Device Flow enabled before sign-in can succeed; an operator-approved Codex
browser action enabled it on 2026-08-12. See
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
