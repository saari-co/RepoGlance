# RepoGlance verification map

This directory is the maintained source for verifying RepoGlance's
user-facing behaviour on the registered Pixel test phone. Read this index
before driving, then use the matching feature file as the recipe.

## Baseline preconditions

- The registered test phone over adb (USB or wireless debugging), awake,
  unlocked, and open on the inner display unless a recipe names another
  posture. `bin/verify-repoglance doctor` passes: device APK SHA-256 equals
  the local debug build, debug launcher present, phone awake and unlocked.
- The app is launched only through `bin/verify-repoglance launch`, which sets
  the fixture scenario before opening a screen. Never drive an install this
  run did not put there.
- Fixture repositories are `acme/rocket`, `acme/api-server`, `octoco/infra`,
  `dinkuskit/blocks`; the public live repository used for signed-in proof is
  `saari-co/RepoGlance`.
- The signed-in session on the phone belongs to the maintainer. Rows that
  need it are marked human-gated; an agent never signs in, out, or touches
  GitHub access settings.

## Driving conventions

- Start every recipe from `doctor` and a fresh `launch`.
- Address controls by `resource-id` (`repoglance:…`), `content-desc`, or
  exact visible text from the last `dump`; never by coordinates from memory.
- Treat every command as literal; keep quoted names unchanged.
- One reversible `BACK` is allowed to clear a transient overlay; then dump
  twice and compare before trusting the screen.
- Restore state after a mutation (scenario, airplane mode, posture).

## Proof and skip reporting

- Assert the observable strings from a `dump` first, then `capture`.
- Every capture records its SHA-256 in the proof packet; images stay in the
  run directory.
- Report a feature you could not reach as `verified-unreachable` with the
  command attempted and the unmet precondition; never mark it verified via a
  different path.
- On the live screen, prove from the dump that `saari-co/RepoGlance` is the
  only repository-shaped label before capturing; discard anything else.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the
user-visible behaviour, then exactly four H2 sections in this order:

1. `Sub-features` — short IDs, one line each.
2. `How to get to it (user POV)` — every user entry point.
3. `Driving it with verify-repoglance` — starts with `Preconditions:`, then
   labelled bullets pairing each user action with an exact command and the
   observable result.
4. `Gotchas` — traps that waste or invalidate a run.

Keep implementation details out. Name only user paths, stable handles,
required state, commands, and observable proof.

## Features

- [Compact widget truth states](./compact-widget.md) covers exact, last-good,
  and no-data rendering of the small home-screen widget at three sizes.
- [Navigator](./navigator.md) covers the fixture issue/PR navigator: scope,
  mode, filters, row detail, and the Home return.
- [Find a repository](./find-repository.md) covers the live catalog's owner
  filter and repository search, including the source-blind guard.
- [Sign in with GitHub](./sign-in.md) covers the device-flow sign-in screen and
  the human-gated live session.
- [Widget setup](./widget-setup.md) covers configuring a widget from the live
  catalog, pin-on-save, unpin-on-delete, and the tall size's saved rows.
- [Quick Settings tile](./quick-settings-tile.md) covers the shade tile: label,
  latest-push subtitle, and the tap that opens the live catalog.
- [Refresh and freshness](./refresh-freshness.md) covers refresh controls,
  data-age labels, rate-limit state, and a real failed refresh.
- [Stack widget](./stack-widget.md) covers the large widget over the live
  pinned set: order, per-row clock times, empty state, and taps.
