# Stack widget over the live pinned set — GrillTrack `stack-widget-live-024` (2026-09-19)

Track `gt-20260728163459-227573`, frontier node 4 (the last) of
`.grilltrack/maps/live-widgets.md`. Branch `claude/stack-widget-live-024`
in an isolated worktree on main `b9b2d988f41602e91a095d0a81b25704ba303203`
(PR #22 merged; its delivery is recorded on this branch).

## Decision (sequential grill, maintainer answers)

1. Set: every live pin, most recent push first (the catalog's pinned-group
   order); no setup screen; with no pins `Pin repositories in RepoGlance`,
   which opens the app.
2. Rows: repository, `issues N · PRs N · review N`, and the row's own clock
   time under 023's label rules (`last good …`, `no data`, never zero). No
   CI column: live CI is not fetched. Header `Pinned · N`, plus
   `rate limited · resets HH:MM` while backing off.
3. Taps: a row opens that repository's live view in the app; the header
   opens the live catalog.
4. Budget: 023 unchanged. At LOW only repositories with their own repo
   widget refresh; other stack rows keep their numbers with their older
   time. The first phrasing of this question was unclear to the
   maintainer; it was restated with a 12-pin example before he answered.

Shared understanding confirmed before implementation.

## Implementation

- `widget/StackWidget.kt` rewritten: reads `AppPrefs.livePins` and
  `LiveSnapshotStore` inside `provideContent`, keyed on
  `WidgetRefresh.REDRAW_KEY` (the 023 redraw fix); `StackRows.order`,
  `stackRowCounts`, `stackRowAge`, `stackHeaderLabel`; two-line rows
  (repository and time, then counts).
- `WidgetRefresh.updateAll` redraws both widget kinds through the counter.
- The live catalog's pin toggle now redraws the widgets.
- `liveRepositoryIntent` shared by the repo widget header and stack rows.
- Header tap: `EXTRA_LIVE_CATALOG` on the catalog intent;
  `MainActivity.handleLiveIntent` returns to the repository list. Found on
  device: a plain `ACTION_MAIN` reopened the repository view the app was
  already showing.
- Removed: the fixture stack path (`SnapshotStore.stackWidgetRepos` and its
  tests), the `FIXTURE PREVIEW` header on the placed stack, and the catalog
  footer "Widgets still use preview data in this checkpoint".
- `stack_widget_info.xml` comment; feature map `stack-widget.md` (8
  features).

## JVM tests and CI floor

`StackWidgetTest`: push-recency order with unknown push last and unloaded
pins kept; empty state; row counts and clock time; `last good` from an
earlier day; `no data` without counts and never zero; header with pin count
and back-off; the stack reads only live pins and live snapshots and redraws
on the shared counter; the header intent carries the catalog extra, and
`handleLiveIntent` honours it before the repository extra. `./gradlew check`
green.

## Device proof (Pixel 11 Pro Fold 66261FDDJ002J5, inner display)

`doctor` hashes equal on each installed build. The maintainer's stack
widget (id 12) was already on his first home page.

- After the install the launcher showed the stack's loading placeholder
  (the system asked only widget 18 to update); the first redraw came from
  the next real trigger, a pin change. Recorded as a gap below.
- Pin change: pinned `saari-co/x-api` in the filtered live catalog; home
  tree `Pinned · 2`, `saari-co/RepoGlance` `8:20 AM`
  `issues 2 · PRs 0 · review 0`, `saari-co/x-api` `no data` (no counts).
  No `FIXTURE PREVIEW`. Capture stack-live
  `308b0703a6832a63ab927da0b17a73127e73e8caff53a7fd8cdea65baffe891b`.
- Row tap: `saari-co/RepoGlance` opened its live view
  (`repoglance:live-home`, `Search loaded rows`).
- Header tap, first build: from that repository view it reopened the
  repository view (`after-stack-header-tap`). Repaired with the catalog
  extra; second build: from the repository view, the header tap shows the
  live catalog (`Find a repository`, `repoglance:repo-search`, no
  `Search loaded rows`; `after-stack-header-tap2`). Not captured (the
  unfiltered catalog is never captured).
- Restore: unpinned `saari-co/x-api`; tree `Pinned · 1`.
- Empty state: unpinned `saari-co/RepoGlance`; tree `Pinned · 0`,
  `Pin repositories in RepoGlance`; capture stack-empty
  `1286eb2adb09a49d788c26a63fa5e31fba9a6db8771e5fec094ef52f2415209b`; a tap
  opened the live catalog. Re-pinned; the pin set is `saari-co/RepoGlance`
  as before; tree `Pinned · 1`, `saari-co/RepoGlance` `8:25 AM`
  `issues 2 · PRs 0 · review 0`. Capture stack-final
  `2c252fa1e7cf9e79e7fffbf167f5681ba469b2acafceea100a11a0e7cf4a1e73`.
  Home-screen captures are the maintainer's; local only, hashes cited.

## Gaps

- After an app update the stack shows the launcher's loading placeholder
  until the next redraw trigger (pin change, widget save, a refresh, or
  the 30-minute run); the system requested an update only for the repo
  widget. Not fixed in this slice.
- A newly pinned repository reads `no data` until the next background
  refresh or an in-app open; pinning does not trigger a refresh (not part
  of the lock).
- Deleting a repo widget unpins through `RepoWidgetReceiver.onDeleted`
  without redrawing the stack; it catches up at the next redraw.
- LOW/EXHAUSTED rows and header are unit-tested, not device-run (as in 023).
- Row `pushedAt` comes from the saved snapshot, which a background run
  refreshes only when it needed the repository metadata call; order can lag
  the catalog's until an in-app catalog load and repository open.
