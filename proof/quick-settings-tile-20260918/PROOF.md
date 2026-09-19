# Catalog sort and Quick Settings tile — GrillTrack `catalog-sort-020` + `quick-settings-tile-019` (2026-09-18)

Track `gt-20260728163459-227573`. Branch `claude/quick-settings-tile-019`
in an isolated worktree on main `c7a504d8e136269fede1178866618e05aad6a536`
(after PR #18).

## Decisions (sequential grill, maintainer answers 2026-09-18)

1. Tile tap opens the app on the live catalog.
2. The catalog's canonical order is recency (most recent push first; unknown
   push time last, still labelled); an alphabetical toggle is remembered.
   Recorded as `catalog-sort-020`, the tile depends on it.
3. After three shade mockups the maintainer chose the subtitle that names the
   most recently pushed repository with its push age.

Shared understanding confirmed before implementation.

## Implementation

- `data/CatalogSort.kt`: `CatalogSort`, `orderRepositories` (recency:
  pushedAt desc, null last, then name; alphabetical: name), `mostRecentlyPushed`.
- `LiveRepoGlanceScreen.kt`: `visibleRepositories` takes the sort;
  `CatalogSortToggle` chips `repoglance:catalog-sort-recent` /
  `repoglance:catalog-sort-alpha` under Find a repository; `AppPrefs`
  remembers `catalog_sort` (default RECENT).
- `state/LatestPushStore.kt`: one record {repo, pushedAt, observedAt} in its
  own prefs file, written on the session dispatcher after every successful
  catalog load (`RepoGlanceViewModel.recordLatestPush`), cleared on sign-out.
- `tile/TileTexts.kt`: pure subtitle rule. `tile/RepoGlanceTileService.kt`:
  reads the record off the main thread, sets label/icon/subtitle/state
  (ACTIVE with a record, INACTIVE without), tap = unlockAndRun if locked,
  then `startActivityAndCollapse(PendingIntent)` on 34+, the Intent overload
  on 31–33 (lint suppression `StartActivityAndCollapseDeprecated`, the only
  API those releases offer). Manifest: exported service with
  `BIND_QUICK_SETTINGS_TILE`, `QS_TILE` filter, `TOGGLEABLE_TILE=false`,
  icon `ic_repoglance_mark` (theme tint removed so SystemUI can render it).
- No network from the tile, no polling. Read-only preserved.

## JVM tests

`CatalogOrderingTest` (recency order, unknown last, alphabetical,
mostRecentlyPushed skips unknown), `TileTextTest` (no record inactive
"Open to connect"; fresh "repo · 3h"; observed >24h "open to refresh";
name sanitised). `./gradlew check` green (lint, detektDebug, ktlint,
checkFeatureMap with 6 features).

## Device proof (Fold inner display, run `runs/verify-repoglance-runs/tile-sort-prod`)

Sort, from dumps only (the unfiltered catalog is never captured):
- `live-recent`: sort chips present; first repository labels
  `saari-co/swarm-intercom` ("Updated just now"), then 50m, 56m, 1h ago.
- `live-alpha` after tapping A to Z: labels start `dinkuskit/.github`,
  `dinkuskit/blocks`, `dinkuskit/bundles`, `dinkuskit/clawsweeper`.
- Guarded capture with the search filtered to `saari-co/RepoGlance` (the
  only repository-shaped label in `filtered`), Recent push selected:
  filtered-sort `50c0ac2f0e223a03e91553aa14e99c9654f569264b5ccf55c3a3a12aecaf9996`.

Tile (maintainer approved adding it over adb in the confirmed plan):
- `cmd statusbar add-tile` then `expand-settings`; `dump shade` holds a
  node with content-desc `RepoGlance, latest push to saari-co/swarm-intercom
  updated 1m ago`. Capture shade
  `4e4dc24334e910d10718578a067612a2c36bd2410f1266a8ca323addd07fd775` shows
  the tile active with the commit-eye icon in the first small slot. The
  image contains other apps' notifications and stays local in runs/.
- `click-tile` after a force-stop: top resumed activity
  `co.saari.repoglance/.MainActivity`, `after-tile-tap` dump contains
  `repoglance:live` and the sort chips and no tile node: the shade
  collapsed into the catalog.
- Shade collapsed and `cleanup` run afterwards. The tile was left in the
  shade for the maintainer to try.

## Gaps and notes

- The Fold's wide shade puts a newly added tile in a small icon-only slot,
  so the subtitle is proven from the content description, not seen in the
  capture. The cover-display shade and a wide slot show it; not captured.
- "Open to connect" (signed-out tile) and "open to refresh" (record older
  than a day) are unit-tested, not device-proven: both need a sign-out or a
  day's wait.
- Locked-phone tap (`unlockAndRun`) not device-proven.
- Pre-existing wording: the catalog row labels the push time "Updated Nm
  ago"; the tile subtitle says "latest push". Left as is; a copy grill if
  wanted.
- Feature map: `find-repository.md` gained `find-sort`; new
  `quick-settings-tile.md`; README indexed.

## Review round 1 (source identity git:a3d9b3cb408f6a4ab8a5679edbf1013535474509, PR #19)

- OpenClaw `req-20260919T002048Z-110778117861`: correct (0.98), 0 findings.
- ClawSweeper: gold shrimp 3/6, patch incorrect (0.86), needs-human
  (PR #19 comment 5737818227). Findings, both accepted as `required_fix`:
  1. P2: a successful catalog with no pushed repository left the previous
     latest-push record in place, so the tile could name a repository no
     longer in the catalog.
  2. Security, medium: a 401 that invalidates the session cleared only the
     token, not the tile record, so the shade could keep showing a
     repository name after the session was gone.

### Repair

- `latestPushRecordFor(repositories, observedAt)` is a pure function; the
  view model now calls `LatestPushStore.replace(record)`, which clears the
  store when the record is null (empty or unknown-only catalog).
- Every session-clear path (`clearSavedSession`, `clearSavedSessionNow`,
  used by cancel, sign-out, catalog 401 and content 401) goes through one
  helper that clears the tile record before the token. The separate
  sign-out clear was removed so there is exactly one path.
- Regression: `LatestPushRecordTest` (empty and unknown-only catalogs yield
  no record; newest repository wins; a source-structure check that the
  single `session.signOut()` call site clears the tile record first and
  that the catalog path uses `replace`). `./gradlew check` green.
- Device re-check on the Fold (run `runs/verify-repoglance-runs/tile-sort-fix`,
  registered serial pinned because a second device was on adb): catalog
  loads with the sort chips, shade dump still carries
  `RepoGlance, latest push to saari-co/swarm-intercom updated 1m ago`.
