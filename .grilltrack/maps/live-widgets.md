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
