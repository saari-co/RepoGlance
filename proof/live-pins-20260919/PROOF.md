# Live pins — GrillTrack `live-pins-021` (2026-09-19)

Track `gt-20260728163459-227573`, frontier node 1 of
`.grilltrack/maps/live-widgets.md`. Branch `claude/next-grill-021` in an
isolated worktree on main `3c651701cf39366c8a8d2861fbd7ef49e2ec4192`.

## Decision (sequential grill, maintainer answers)

- Pinned repositories always stay at the top with a thumbtack and take
  priority over the recency sort; among pins, most recent push first; the
  rest follow the active sort.
- Pins clear on every session-clear path (option 1), like the tile record.

Shared understanding confirmed before implementation.

## Implementation

- `data/CatalogSort.kt`: `orderCatalog(repositories, sort, pinned)` =
  pins in recency order + rest in the active sort.
- `LiveRepoGlanceScreen.kt`: `PinToggle` on each row (`Icons.Filled.PushPin`
  / `Icons.Outlined.PushPin`, content description `Pin owner/name` /
  `Unpin owner/name`, testTag `repoglance:pin-<owner/name>`);
  `visibleRepositories` takes the pin set.
- `AppPrefs`: `live_pinned_repos` (own key, fixture-home pins untouched),
  `toggleLivePin`, `clearLivePins`, `rememberLivePins`.
- `RepoGlanceViewModel.clearSessionAndTileRecord` clears live pins before
  the token; a source test enforces it beside the tile-record clear.
- Feature map: `find-pin` and a drive step in `find-repository.md`.

## JVM tests

`CatalogOrderingTest`: pins lead in recency order then the rest follow the
active sort (both sorts); no pins leaves the sort untouched.
`LatestPushRecordTest`: the single session-clear helper clears live pins.
`./gradlew check` green.

## Device proof (Fold inner display, run `runs/verify-repoglance-runs/live-pins`)

- Filtered to `saari-co/RepoGlance` (only repository-shaped label):
  `before-pin` shows `Pin saari-co/RepoGlance`; after the tap,
  `after-pin-filtered` shows `Unpin saari-co/RepoGlance`. Guarded capture
  pinned-filtered
  `1a4393e1b252a77c74ec8584869461d546b4c6c10afa050eb3f643ba1ef42353`
  (thumbtack filled on the row), inspected.
- Search cleared, `pinned` dump: first label `saari-co/RepoGlance` although
  `saari-co/swarm-intercom` had the newer push; the row's control reads
  `Unpin saari-co/RepoGlance`. Not captured (unfiltered).
- `Unpin` tapped, `unpinned` dump: order restores to recency
  (`saari-co/swarm-intercom` first) and no `Unpin` control remains.

## Gaps

- Sign-out clearing is unit-tested only; disconnecting is the maintainer's
  gated action.
- The fixture home keeps its own star pins; the two pin sets are separate
  by design until widget configuration moves to the live set (map node 2).

## Review (source identity git:93f53cd9a7c980579e9401774e5329142dab24fe, PR #20)

- OpenClaw `req-20260919T013033Z-128441618462`: correct (0.99), 0 findings.
- ClawSweeper: platinum hermit overall, proof and patch; no findings, no
  security items, no maintainer decision required beyond merge authority
  (PR #20 comment 5738298240). Clean at the target. Merge is the maintainer's.
