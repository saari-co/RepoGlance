# Decision map: live widgets (2026-09-19)

Destination: the home-screen widgets show real GitHub data for the pinned
set and refresh in the background, honouring truth rules 2-4 and 6.

Facts (source-linked, main 3c651701cf39366c8a8d2861fbd7ef49e2ec4192):

- Pins exist only on the fixture home (`HomeScreen.kt`, `AppPrefs.pinnedRepos`);
  the live catalog (`LiveRepoGlanceScreen.kt`) has no pin control.
- Widget configuration lists fixture repositories only
  (`RepoWidgetConfigActivity.kt:74`); the compact widget reads
  `LiveSnapshotStore` only when a config names a repository.
- `LiveSnapshotStore.save` runs only when a repository is opened in the app
  (`RepoGlanceViewModel.kt:240`); there is no background refresh, no
  WorkManager dependency, `updatePeriodMillis=0`.
- The live catalog says "Widgets still use preview data in this checkpoint".

Frontier nodes (one per GrillTrack cycle):

1. `live-pins-021` — pins on the live catalog: control, placement beside
   the recency sort (`catalog-sort-020`) and `pin-model-010`'s "pinned
   sort first", persistence, sign-out behaviour. **Current frontier.**
2. `widget-live-config-022` — widget configuration picks from the pinned
   live set; compact widget shows the live snapshot or `no data`.
3. `widget-background-refresh-023` — trigger and cadence for refreshing the
   pinned set (WorkManager periodic vs on-unlock vs manual), rate-limit
   budget, back-off, and the age label between refreshes.
4. `stack-widget-live-024` — the stack widget over the live pinned set.

Dependencies: 2 needs 1; 3 needs 2; 4 needs 2 and 3.

## Closeout (2026-09-19, main a21fb7e2c650f9da3497db85f504c0797fca8482)

Status: **closed** at the maintainer's request. The destination is reached:
the home-screen widgets show real GitHub data for the pinned set and
refresh in the background. Every node was verified on the Pixel 11 Pro Fold,
reviewed clean (OpenClaw 0 findings, ClawSweeper platinum hermit or better
with no findings) and merged by the maintainer.

| Node | Result | Proof | Reviewed source | Delivery |
| --- | --- | --- | --- | --- |
| `live-pins-021` | In-place pin toggle on live catalog rows; pinned group first, most recent push first | `proof/live-pins-20260919/PROOF.md` | `93f53cd9` | PR #20, `5b73ac7a` |
| `widget-live-config-022` | Widget configuration picks from the last catalog load, pinned first; the compact widget shows the live snapshot or `no data` | `proof/widget-live-config-20260919/PROOF.md` | `a7d9cea8` | PR #21, `06cd6cf2` |
| `widget-background-refresh-023` | WorkManager periodic refresh every 30 minutes on a network connection, one-time run on widget save, rate-limit budget and back-off, clock-time age labels | `proof/widget-background-refresh-20260919/PROOF.md` | `b7d1f699` | PR #22, `b9b2d988` |
| `stack-widget-live-024` | Stack lists every live pin by push recency with per-row counts and time; row and header taps; empty state | `proof/stack-widget-live-20260919/PROOF.md` | `d1c5d5f6` | PR #23, `948d6627` |
| `widget-update-redraw-025` (follow-up) | Both widgets redraw from saved data after an app update (`MY_PACKAGE_REPLACED`) | `proof/widget-update-redraw-20260919/PROOF.md` | `84a8665e` | PR #24, `a21fb7e2` |

Superseded along the way: the fixture stack path and its `FIXTURE PREVIEW`
header, and the catalog footer "Widgets still use preview data in this
checkpoint" (both removed in 024). `README.md` status now describes the live
widgets instead of fixture-backed ones.

Carried forward (open, not blockers for this map):

- The compact widget's `last good` label doesn't leave room for the
  repository name at the 120dp floor.
- ~~LOW/EXHAUSTED rate-limit rows and headers are unit-tested only, not
  device-run (023, 024).~~ Struck 2026-09-19: device-proven on the Pixel 11
  Pro Fold for tall, compact, stack and catalog, with recovery
  (`rate-limit-device-proof-026`,
  `proof/rate-limit-device-proof-20260919/PROOF.md`). The LOW skip of pins
  without a repo widget stays unit-tested only.
- While rate limited, the one-line tall header renders
  `rate limited · resets HH:MM P…`, hiding the counts and `as of` time, and
  the stack header cuts the reset time (found in 026).
- A newly pinned repository reads `no data` until the next background
  refresh or an in-app open; pinning doesn't trigger a refresh.
- Deleting a repo widget unpins without redrawing the stack; it catches up
  at the next redraw.
- ~~StrictMode logs main-thread disk reads from both widgets' `provideContent`
  (found in 025).~~ Struck 2026-09-19: fixed by PR #26 (`2998c7c1`), store
  reads moved off the main thread; guarded by `WidgetStoreReadsTest`.
- ~~`bin/verify-repoglance doctor` misreads the Fold posture as `C` on this
  Android build.~~ Struck 2026-09-19: fixed by PR #25 (`948c1593`); doctor
  prints `posture=CLOSED` on the Pixel 11 Pro Fold.
- Live CI is not fetched, so the widgets have no CI column.
