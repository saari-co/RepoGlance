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

## Closeout (2026-09-19, maintainer request)

The destination is reached: the home-screen widgets show real GitHub data
for the pinned set and refresh it in the background, honouring truth rules
2-4 and 6. Every frontier node is verified, reviewed clean, and merged.

| Node | Decision | Delivered | Proof |
| --- | --- | --- | --- |
| 1 | `live-pins-021` | PR #20, merge `5b73ac7a478c73f1bcdaf90491ca128367196a73` | `proof/live-pins-20260919/PROOF.md` |
| 2 | `widget-live-config-022` | PR #21, merge `06cd6cf237d84a924ecf5b9edbac75b1cacf35b0` | `proof/widget-live-config-20260919/PROOF.md` |
| 3 | `widget-background-refresh-023` | PR #22, merge `b9b2d988f41602e91a095d0a81b25704ba303203`, ClawSweeper platinum hermit (round 3) | `proof/widget-background-refresh-20260919/PROOF.md` |
| 4 | `stack-widget-live-024` | PR #23, merge `948d6627a3e4a59b4d7612cef537aeaeba71cc3b`, ClawSweeper platinum hermit (round 2) | `proof/stack-widget-live-20260919/PROOF.md` |

The delivery of node 4 is recorded in the ledger on branch
`claude/widget-update-redraw-025` (commit
`4442f2493515f62610d69c67a82bb8a8e8eaa7f4`).

Closing the map does not close the GrillTrack track: the track spans the
whole product and still holds open decisions outside this map
(`public-mit-009`, `pin-model-010`, `app-public-011`, `release-model-013`)
and the follow-up below that is in progress.

Follow-ups outside the map:

- `widget-update-redraw-025` (in progress, confirmed): redraw both widgets
  from saved data after an app update, so the stack never sits on the
  launcher's loading placeholder. Handed to a fresh session. Time zone,
  clock, locale and boot redraws were considered and not chosen.
- Compact 120dp fit: a last-good label from an earlier day
  (`last good Mon 19:00`) squeezes the repository name to an ellipsis.
- LOW and EXHAUSTED rate-limit behaviour is unit-tested, not device-run.
- A newly pinned repository reads `no data` until the next background
  refresh or an in-app open; pinning does not trigger a refresh.
- Deleting a repo widget unpins through the receiver without redrawing the
  stack until the next redraw.
- Live CI is not fetched, so neither widget shows CI.
