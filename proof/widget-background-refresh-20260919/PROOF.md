# Background refresh of the pinned set — GrillTrack `widget-background-refresh-023` (2026-09-19)

Track `gt-20260728163459-227573`, frontier node 3 of
`.grilltrack/maps/live-widgets.md`. Remote branch
`claude/widget-background-refresh-023` (local branch
`gt/widget-background-refresh-023`, because the remote branch name is still
checked out in the handoff worktree) in an isolated worktree on main
`06cd6cf237d84a924ecf5b9edbac75b1cacf35b0`.

## Decision (sequential grill, maintainer answers)

1. Trigger: WorkManager unique periodic work every 30 minutes with a
   network constraint, plus one immediate one-time run when a widget is
   saved. No wakelock, no foreground service, no on-unlock trigger (a
   manifest `USER_PRESENT` receiver is not delivered since Android 8).
2. Budget: a run refreshes live pins least recently observed first, at most
   20. At `LOW` only pins with a placed widget are refreshed; at `EXHAUSTED`
   the run stops, nothing is overwritten, and the reset time is saved so
   widgets say `rate limited · resets HH:MM`. The maintainer asked what a
   "run" and its "spend" were; the question was restated in plain terms
   (a run is one wake-up over the pins, about 2-3 core requests per repo
   against the 5,000/hour user budget) before he answered.
3. Label: an interactive picker
   (artifact `https://claude.ai/artifact/Q2GNAJzeimKDHxrmkqi2PZ`) showed that a
   relative age frozen in the widget picture understates the age between
   runs and that a `refreshing…` state can stick if the job is killed. He
   chose A: clock time `as of 1:31 AM` (`as of Tue 14:05` from an earlier
   day, `as of 12 Sep` after a week), redrawn only when a run finishes.

Shared understanding confirmed before implementation.

## Implementation

- `data/LiveGitHub.kt`: one process-wide `GitHubSession` and
  `GitHubApiClient`, shared by the view model and the worker, so the
  synchronized token refresh and the rate-limit guard cannot race between
  two instances (a refresh-token rotation race would otherwise sign the
  maintainer out).
- `data/LiveRefresh.persist`: the one path that turns loaded repository
  content into a saved snapshot and rows (moved out of
  `RepoGlanceViewModel.persistLiveSnapshot`), and records the latest known
  rate-limit bucket.
- `state/RateLimitStore.kt`: persisted bucket and reset time; an expired
  reset reads as `UNKNOWN`; `EXHAUSTED` without a reset header backs off
  60 seconds, like the client's own guard.
- `refresh/RefreshPlan.kt` (order, 20 cap, LOW filter, EXHAUSTED stop),
  `refresh/PinnedRefreshWorker.kt`, `refresh/BackgroundRefresh.kt`
  (schedule on catalog load, one-time run on widget save, cancel on every
  session clear).
- `CatalogNamesStore` also saves the viewer login (the worker needs it for
  the `to review` count and skips the run without it).
- `render/ClockLabel.kt`; `RepoWidget` compact and tall labels use it with
  the device zone and 12/24-hour setting, `rate limited ·` on compact,
  `rate limited · resets <time> ·` on the tall header.
- Redraw fix found on device: `WidgetRefresh` bumps a counter in each
  widget's Glance state before `update`, and `RepoWidget` keys its content on
  it. Without it a live Glance session re-showed the previous picture: after
  the worker wrote `05:25:58Z` the widget still read `as of 1:24 AM`.
- Setup note now says counts and rows refresh in the background about every
  30 minutes and whenever the repository is opened in the app.
- Dependency: `androidx.work:work-runtime-ktx` 2.9.1 (Glance already pulled
  WorkManager 2.7.1 transitively for its session worker).
- Session clear (`clearSessionAndTileRecord`) also clears `RateLimitStore`
  and cancels both unique works before the token is dropped.

## JVM tests and CI floor

`ClockLabelTest`, `RefreshPlanTest`, `RateLimitStoreTest`,
`CompactFreshnessUnitTest` (clock labels, rate-limited compact and tall
header, label unchanged as time passes), `LiveRowsStoreTest` (only
`LiveRefresh` saves rows and snapshots; the view model and worker call it),
`LatestPushRecordTest` (session clear drops the rate limit and cancels the
work before sign-out). `./gradlew check` green (lint, detekt, comment ban,
warnings as errors, feature map).

## Device proof (Pixel 11 Pro Fold 66261FDDJ002J5, inner display)

`doctor` passed on each installed build (APK hash equal, launcher present,
awake, unlocked). The maintainer's ISSUES widget (id 18,
`saari-co/RepoGlance`) was already on his second home page.

- Before: the pre-branch build's widget read `ISSUES 2 · as of 22m`
  (`home-next.txt`). The same data was observed at 12:04 AM and read
  `as of 12:04 AM` once the new build redrew it at 01:24: the frozen
  relative label had been understating the age by about an hour.
- Scheduling: after the catalog load, `dumpsys jobscheduler` lists the
  periodic job with `Minimum latency: +29m59s983ms` and an
  `INTERNET&…&VALIDATED` network request (`jobs-after-install.txt`).
- Forced periodic job: `cmd jobscheduler run -f` did not refresh (WorkManager
  reschedules a periodic worker that is not yet due); recorded as a gotcha in
  the feature map, not as proof.
- Save → one-time run, repository never opened in the app: setup note dump
  and capture setup-note
  `058878a58d44858d1c9b5f38e8da46b42ce0c8b5ce1b9dea1370a6093077a8a2`;
  `Save widget` at 01:31:51; `WM-WorkerWrapper … Worker result SUCCESS …
  PinnedRefreshWorker` at 01:31:52.499; stored `observedAt`
  `2026-09-19T05:31:52Z`; home tree `ISSUES 2 · as of 1:31 AM`,
  `ISSUE #11 · 4w`, `ISSUE #7 · 5w`. Capture home-widget-after-save
  `e20be30ac32610afcc99951af36f02bc1336a64d17f29d5b291da183aa7049ce`
  (the maintainer's home screen; local only, hash cited).
- Natural 30-minute run (read-only observation; another agent held the
  phone at the time, so no widget dump): `dumpsys jobscheduler` history shows
  the periodic job `#u0a383/45` (queued at 01:31 with a 30-minute latency)
  `STOP … app called jobFinished` at about 01:57:58, followed by the Glance
  session workers that redraw the widget; the stored `saari-co/RepoGlance`
  snapshot reads `observedAt 2026-09-19T05:57:54Z`; the next periodic job
  `#54` is queued with `Minimum latency: +29m59s948ms`. The only live pin is
  `saari-co/RepoGlance`; the other stored snapshot (`dinkuskit/.github`,
  2026-09-18) is unpinned and correctly not refreshed. The watcher missed the
  `WorkerWrapper` log line because the log buffer no longer held any
  RepoGlance lines. It cannot be excluded that the other agent opened
  RepoGlance near 01:57.

## Gaps

- `LOW` and `EXHAUSTED` are not reproducible on the phone without burning
  real quota; they are covered by `RefreshPlanTest`, `RateLimitStoreTest`
  and the label tests only.
- Doze stretching the interval is expected and not measured.
- The live catalog footer still says "Widgets still use preview data in
  this checkpoint", which has been partly false since node 2 (only the stack
  widget is still fixture data). Left for map node 4.
- StrictMode `UntaggedSocketViolation` lines appear for the worker's
  requests, as for every in-app request; they are not main-thread I/O.

## Review round 1 (source identity git:728d09641883e9211aa2f4741bed54ec073b989d, PR #22)

- OpenClaw `req-20260919T053708Z-183956310196`: correct (0.97), 0 findings.
- ClawSweeper: unranked krab 1/6 (proof gold shrimp 3/6), one P1 plus a
  matching medium security note, accepted as `required_fix`: the worker's
  saved-session check was not atomic with `LiveRefresh.persist`, so a
  disconnect landing in between could leave repository data on the device
  after sign-out (PR #22 comment 5739724936).

### Repair

- `GitHubSession` keeps a session generation, bumped on every sign-in and
  sign-out. `commitIfCurrent(generation) { … }` runs a write only while that
  generation is current and a token exists, under the session lock.
  `signOut { … }` bumps the generation and clears local data under the same
  lock before dropping the token.
- `LiveRefresh.persist` does its network and build work first, then writes
  the snapshot, rows and rate limit only inside `commitIfCurrent`. The view
  model captures the generation before loading a repository; the worker
  captures it at run start and stops the run when a commit is refused.
- `clearSessionAndTileRecord` performs every local clear (and cancels the
  work) inside `session.signOut { … }`.
- Tests: `GitHubSessionTest` (a write started before sign-out, or under an
  earlier sign-in, is dropped; a write racing a sign-out blocks until the
  clear finishes and is then dropped), `LiveRowsStoreTest` (every store
  write in `LiveRefresh` sits inside the session commit),
  `LatestPushRecordTest` (all clears inside the `signOut` block). `./gradlew
  check` green.
- The final-effect proof ClawSweeper asked for (disconnect while a worker is
  in flight on the phone) needs a sign-out, which is the maintainer's gated
  action; the race is covered by the concurrent unit test instead.

## Review round 2 (source identity git:e6ed55a916d0d31cb2a1ded02badb4417a302c5b, PR #22)

- OpenClaw `req-20260919T061403Z-19305683503`: correct (0.96), 0 findings.
- ClawSweeper: gold shrimp 3/6. Round-1 race confirmed repaired in source.
  One medium finding, classified `human_gate`: no device proof that an
  in-flight refresh is refused when a sign-out lands before its save
  (`LiveRefresh.kt:42`). Also listed: the natural periodic run had no widget
  dump (`defer` until the phone was free), and LOW/EXHAUSTED are unit-tested
  only (`defer`, unchanged gap).

### Repair and proof (maintainer chose a debug-only probe plus his sign-out)

- `hooks/RefreshProbe` exists in both flavours: the release flavour is a
  no-op; the debug flavour, armed once through `ScenarioLaunchActivity`
  (`--el probeCommitDelaySeconds N`), holds the next `LiveRefresh.persist`
  after its network work and before `commitIfCurrent`, and records only its
  own `armed`/`holding`/`released` timestamps.
- Natural periodic run with a widget dump (phone free again, nobody opened
  the app): the job history shows periodic `#u0a383/54` START at 02:31:28 and
  STOP at 02:31:30 (`job-history.txt`); stored `observedAt
  2026-09-19T06:31:30Z`; the home tree reads `ISSUES 2 · as of 2:31 AM`,
  unchanged at 02:37. Capture home-widget-periodic
  `1b65cb07f74b319a96eb445a74720a8daa43633950910b7e203715b2281f9446`
  (maintainer's home screen; local only).
- Picker (`launch MIXED picker`, debug build with the probe, doctor hashes
  equal `1833aa00…`): exact `17:00`, last-good `last good Mon 19:00`,
  no-data `no data`/`—`, persisted `02:31`. Capture compact-states
  `bb3929ee867cd72db1ca87f33c4bdb4598e8160eb10e46527a27cfcd890109f0`.
  Fit gap: at the 120dp floor `last good Mon 19:00` squeezes the repository
  name to `…` (the feature map already allows `Repo…` there; a bare ellipsis
  is worse, raised with the maintainer rather than changed).
- In-flight sign-out (run `runs/verify-repoglance-runs/signout-proof`):
  probe armed 06:39:34Z (300 s); `Save widget` on widget 18 at 02:39:55; the
  worker fetched and held at 06:39:55Z with the store still at 06:39:18Z
  from the preceding refresh. The maintainer tapped Disconnect GitHub; the
  WorkManager log records the work `was cancelled` at 02:42:31 (the clear's
  `BackgroundRefresh.cancel`), but the held thread kept running because
  cancellation is cooperative. The probe released at 06:44:55Z; at 02:45:13
  `repoglance_live_snapshots`, `repoglance_live_rows`,
  `repoglance_rate_limit`, `repo_widget_configs` and
  `repoglance_catalog_names` were all `<map />`: the save was refused by the
  session generation. The home tree reads `FIXTURE PREVIEW` / `Choose a
  repository`. Capture widget-after-signout
  `e5ff5ba7ae061cb04a0a05bbf32cfc6320b2c2c25d5dd4b7cf0658bf977a58f0`.
  The maintainer then signed in again himself; the device-code screen was
  never dumped or captured.
