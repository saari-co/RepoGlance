# RepoGlance

Read-only, widget-first GitHub glance and issue navigator for Google Pixels.

> RepoGlance puts each repository's PR/issue pressure, default-branch CI
> state, latest release, and data age on a Material You Pixel home-screen
> widget, pairs it with a fast read-only issue navigator, and stays honestly
> labelled without ever mutating GitHub.

Inspired by [steipete/RepoBar](https://github.com/steipete/RepoBar) (macOS).
This is the Pixel-native analog, not a port: no local git, no menu bar, no
Swift — Jetpack Glance widgets, an owner switcher, and deep links into the
installed GitHub app.

## Status

**Contract-only bootstrap. No Android project exists yet.**

The product contract lives in the x-api plan
`plans/repobar-pixel-companion-plan-20260728.md`
([saari-co/x-api#522](https://github.com/saari-co/x-api/pull/522), pinned at
commit `d944fdd2b` plus the GrillTrack cycle-1 amendment on the same PR).
Locked product decisions live in [docs/PRODUCT_DECISIONS.md](docs/PRODUCT_DECISIONS.md)
with the canonical GrillTrack ledger in `.grilltrack/`.

## Access model (locked 2026-07-28)

- Sign in once as `saariuslystoned` via the RepoGlance GitHub App (Chrome
  Custom Tab; no browser-cookie scraping, ever).
- Owner switcher: `saariuslystoned` · `saari-co` · `dinkuskit` — all repos
  with PR/issue pressure per owner.
- Pin/hide curation: pinned repos earn widget slots and background refresh;
  everything else fetches on demand.
- Issue navigator scopes account-wide, org-wide, or single-repo, each
  showing issues only, PRs only, or both.

## Boundaries

Read-only against `api.github.com` in v0.x. No CP-1/broker/swarm dependency.
No GitHub writes. Not a session roster (that is SwarmBar) and never a full
GitHub client — full threads and all actions deep-link to the installed
GitHub app.

Private repository; no license chosen yet (distribution decision open).
