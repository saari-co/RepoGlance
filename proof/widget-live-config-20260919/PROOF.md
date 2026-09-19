# Widget configuration from the live catalog — GrillTrack `widget-live-config-022` (2026-09-19)

Track `gt-20260728163459-227573`, frontier node 2 of
`.grilltrack/maps/live-widgets.md`. Branch `claude/widget-live-config-022`
in an isolated worktree on main `5b73ac7a478c73f1bcdaf90491ca128367196a73`.

## Decision (sequential grill, maintainer answers, mockups for Q3)

1. Any repository from the last catalog load can be configured, pinned
   ones first.
2. Choosing a repository for a widget pins it; when no widget uses it any
   more (deleted or reconfigured) it is unpinned, even a hand-set pin.
3. Tall sizes show the rows saved the last time the repository was opened
   in the app, each with its age, plus an `as of` age in the header.

Shared understanding confirmed before implementation.

## Implementation

- `state/CatalogNamesStore.kt`: repository names from each catalog load
  (recency order), cleared on every session-clear path.
- `state/LiveRowsStore.kt`: up to 10 rows per repository (kind, number,
  title, updatedAt, url) saved in `persistLiveSnapshot` from the loaded
  issue and PR pages, versioned, cleared with the session.
- `widget/WidgetPins.kt`: `configurationList` (pins first, then catalog
  order, unknown pins kept) and `releasedRepositories`.
- `RepoWidgetConfigActivity`: lists the live set; empty state
  "No repositories to choose from yet" with `Open RepoGlance`; saving pins
  the repository and unpins a replaced one no other widget uses; the note
  says so. `RepoWidgetReceiver.onDeleted` unpins released repositories.
- `RepoWidget`: tall sizes render `LiveRowsStore` rows (kind, number, age
  on one line, title below) under a header `<counts> · as of <age>`,
  `last good ·` prefix when the snapshot is last-good,
  `no data · open RepoGlance to load` and `No saved rows · open RepoGlance
  to load` otherwise. No fixture rows on live widgets. Compact unchanged.
- Feature map: new `widget-setup.md`; README indexed (7 features).

## JVM tests

`WidgetPinsTest` (pins first, malformed names dropped, released set),
`LiveRowsStoreTest` (round trip, merge newest-first capped at 10, unknown
version decodes to nothing), `LatestPushRecordTest` still guards the
single session-clear helper. `./gradlew check` green.

## Device proof (Fold inner display, run `runs/verify-repoglance-runs/widget-live`)

Placement: the maintainer dragged the compact widget (id 14) and the stack
widget onto the home screen; the setup screen was opened for id 14 through
`APPWIDGET_CONFIGURE` over adb.

- `widget-setup` dump: `Configure widget`, `Repository` = `saari-co/RepoGlance`
  (first in recency order with no pins), note `Saving pins this repository
  in RepoGlance; removing the widget unpins it…`, `Save widget`. Capture
  widget-setup `6405a9bed5d96ce74eda8275318ea3846fe1ae7b5fca6f197d3fa029e67953ff`.
- Pin on save: after `Save widget`, live catalog filtered to
  `saari-co/RepoGlance` (only repository-shaped label in `filtered3`), the
  row control reads `Unpin saari-co/RepoGlance`. Capture pinned-by-widget
  `8397f62528092c9e21d6fc68dd33aba7dd83b2194c401b04479f541695c8d92f`.
  An earlier capture taken before the filter had applied was discarded.
- Rows: after opening the repository in the app, the home-screen tree
  (`home-widgets-final.xml`) holds `saari-co/RepoGlance`,
  `ISSUES 2 · PRS 0 · as of 5m`, `ISSUE #11 · 4w`, `ISSUE #7 · 5w` with
  titles beneath. Capture home-widget-rows
  `c4987f2231ff1c2cfe8615d6efb0cc8191884ebae940f0bf202bef5a8c94d268`
  (contains the maintainer's home screen; local only, hash cited). Before
  the open, the same widget showed `last good · ISSUES 2 · PRS 1 · as of 23h`
  and `No saved rows · open RepoGlance to load`: the honest pre-open state.
- Fidelity repair: the first row layout put kind, title and age on one line
  and the title collapsed to an ellipsis at the placed width; the row is now
  two lines (kind · age, then title) and the titles read.
- Unpin on delete: the maintainer removed the widget (the appwidget dump
  no longer lists an instance for `RepoWidgetReceiver`); live catalog
  filtered to `saari-co/RepoGlance` (only repository-shaped label in
  `after-delete`), the row control reads `Pin saari-co/RepoGlance`. Capture
  unpinned-after-delete
  `016a159b92415a2bd0f3e8aed020b93dc1d72f1390b61ee0713f3ee2a5341a32`.
- `cleanup` run afterwards; the stack widget (fixture, map node 4) stays on
  the maintainer's home screen by his choice.

## Gaps

- Reconfiguring a widget to a different repository (unpin of the replaced
  one) is unit-tested through `releasedRepositories`, not device-proven.
- The empty setup state (`No repositories to choose from yet`) is
  source-evident; proving it needs a sign-out, the maintainer's gated action.
- The stack widget still renders fixture data and says so (`FIXTURE
  PREVIEW`); it is map node 4.
- Rows and counts refresh only on an in-app open until map node 3 adds
  background refresh; the widget's ages say so.

## Review round 1 (source identity git:2451aa9dc5dfe24e01b3e5eedfc4bc4e73ecda59, PR #21)

- OpenClaw `req-20260919T031141Z-14753179313`: correct (0.98), 0 findings.
- ClawSweeper: gold shrimp 3/6, one P1 accepted as `required_fix`: a
  transient issue or PR fetch failure after rows were saved replaced the
  row cache with an empty list while counts stayed last-good, so the tall
  widget could pair stale counts with a false "No saved rows"
  (PR #21 comment 5738925554).

### Repair

- `LiveRowsStore.replacementRows(issues, pullRequests)` returns null unless
  both pages loaded; the view model saves rows only through it, so a
  partial or failed refresh keeps the previous rows beside the last-good
  counts. An empty but complete result still clears the rows honestly.
- `LiveRowsStoreTest`: partial/failed fetches never replace saved rows;
  source check that the view model uses only `replacementRows`.
- `./gradlew check` green; installed on the Fold and the live catalog still
  loads (`runs/verify-repoglance-runs/widget-live-fix`). A device
  reproduction of a transient fetch failure with a placed widget was not
  run; the guard is a pure function covered by tests.

## Review round 2 (source identity git:0e5a1e731b9b31357713af442905cef5bbc08156, PR #21)

- OpenClaw `req-20260919T032129Z-14992405731`: correct (0.98), 0 findings.
- ClawSweeper: round-1 blocker confirmed repaired; one late P1 accepted as
  `required_fix`: the tall widget rendered the mixed row cache without
  applying the widget's feed mode, so an ISSUES widget could list PR rows
  under an ISSUES header (PR #21 comment 5738925554).

### Repair

- `rowsForMode(rows, mode)` filters saved rows by kind (ISSUES, PRS, BOTH)
  and `RepoWidget` applies it before rendering.
- `WidgetPinsTest.savedRowsAreFilteredByTheWidgetFeedMode`. `./gradlew
  check` green; build installed on the Fold. The maintainer's placed
  widget was BOTH, so the mode filter is covered by the pure function and
  its test, not a device run.
