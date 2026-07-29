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

## Status: contracts only — no code yet

This repository is in its bootstrap phase. What exists today is the product
contract, the decision ledger, and proof of how decisions were made. The
Android project has not started. This README distinguishes **planned** from
**available**; right now, everything below is planned.

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

## Auth (planned)

A RepoGlance GitHub App: sign in via the browser's authorization flow in a
Custom Tab, short-lived user tokens refresh silently, stored behind Android
Keystore. RepoGlance never reads or reuses browser cookies, and read-only
scopes are the ceiling — the app performs no GitHub writes.

## How this is being built

Product decisions are made in focused GrillTrack cycles and recorded
durably: the human-readable index is
[docs/PRODUCT_DECISIONS.md](docs/PRODUCT_DECISIONS.md); the canonical
ledger with full lifecycle history lives in `.grilltrack/`. Development
happens in the open on branches and PRs; `main` only ever claims what is
actually proven.

## License

[MIT](LICENSE)
