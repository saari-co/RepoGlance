# Widget store reads off the main thread (2026-09-19)

Branch `claude/serene-jackson-006b49` in an isolated worktree on main
`948d662`. Follow-up to the StrictMode gap recorded by
`widget-update-redraw-025` (commit `84a8665`, `proof/widget-update-redraw-20260919/PROOF.md`,
"Gaps and risk"): 26 `DiskReadViolation`s from `RepoWidget` and `StackWidget`
reading their stores inside `provideContent` on every redraw.

## Cause

Glance 1.1.1 `androidx.glance.session.SessionWorker` declares
`override val coroutineContext = Dispatchers.Main`, and `AppWidgetSession`
calls `widget.runGlance` from inside its composition. Both `provideGlance`
and the `provideContent` composition therefore run on the main thread;
moving the reads above `provideContent` alone would not have removed the
violations.

## Change

- `widget/WidgetRefresh.kt`: `readWidgetStores` runs a read on
  `Dispatchers.IO`; `redrawnWidgetData(initial, read)` holds the last read
  and re-reads through `readWidgetStores` in a `LaunchedEffect` keyed on
  `currentState(WidgetRefresh.REDRAW_KEY)`, so a redraw bump still re-reads
  every store.
- `widget/RepoWidget.kt`: `readRepoWidgetData` (config, live snapshot, rows,
  freshness incl. `RateLimitStore` and the clock) is read once off-main in
  `provideGlance` before `provideContent`; the composition only renders
  `RepoWidgetData`.
- `widget/StackWidget.kt`: same shape with `readStackWidgetData` (live pins,
  catalog push times, live snapshots, freshness).
- On a redraw the previous data stays on screen until the IO read returns;
  there is no placeholder frame.
- `docs/INVARIANTS.md`: the "No disk or network I/O on the main thread" row
  records this catch as fixed and names the guarding tests.

## JVM proof

- `WidgetStoreReadsTest` (new, 3 tests): `readWidgetStores` runs on a
  `DefaultDispatcher-worker` thread, not the caller; the redraw helper keys
  its `LaunchedEffect` on `REDRAW_KEY` and reads through
  `readWidgetStores`; the `RepoWidget` composition contains no store,
  clock, or `Instant.now()` call and `provideGlance` reads through
  `readRepoWidgetData`.
- `StackWidgetTest` (+1 test, 10 total): the same composition check for
  `StackWidget` via `readStackWidgetData`; the existing live-pins test now
  asserts the redraw path through `redrawnWidgetData(`.
- Negative control: with `RepoWidget.kt`/`StackWidget.kt` restored to
  `948d662` and the new tests kept, `testDebugUnitTest --tests '*Widget*'`
  reported `27 tests completed, 3 failed`.
- `./gradlew check`: green (exit 0), 251 unit tests, 0 failures; lint and
  detekt baselines untouched. Two intermediate findings were fixed in code,
  not baselined: detekt `LambdaParameterInRestartableEffect`
  (`rememberUpdatedState`) and lint `ProduceStateDoesNotAssignValue` (a
  false positive on a generic `produceState`; replaced with
  `remember` + `LaunchedEffect`).
- Debug APK SHA-256 `978cc2484d417181907cf5d57f403fa7c5a674de9265ce3304a16398635930fb`.

## Device proof: BLOCKED

- The registered phone (Pixel 11 Pro Fold `66261FDDJ002J5`) was not
  attached: `adb devices` listed only `59151FDCG000JA` (Pixel 10 Pro Fold)
  and `adb mdns services` was empty.
- Before checking the serial against the registered one, this session ran
  `adb install -r` of the debug APK on `59151FDCG000JA`. That was a fresh
  install (`firstInstallTime=lastUpdateTime=2026-09-19 09:57:16`, never
  launched, no RepoGlance widget placed). It is not the registered phone,
  so it was not driven further, and it is not evidence. `doctor` on it
  reported `scenario_launcher_present=no` for a debuggable build whose
  manifest declares `devlaunch.ScenarioLaunchActivity`. The install is
  still on that phone and is left for the maintainer to keep or uninstall.
- Remaining acceptance step, on the registered phone with a placed repo
  widget and stack widget: `bin/verify-repoglance doctor`, then
  `adb logcat -c`, `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  (or a pin toggle / refresh, which bumps `REDRAW_KEY`), wait ~10 s, and
  `adb logcat -d | grep -A12 'StrictMode policy violation'` must show no
  block with frames in `co.saari.repoglance.widget`.

## Gaps and risk

- On-device StrictMode absence is unproven until the step above runs.
- On main, a redraw after `adb install -r` depends on the system's
  `APPWIDGET_UPDATE`; the `AppUpdateReceiver` from 025 is not merged here.
  A pin toggle in the live catalog is the reliable `REDRAW_KEY` trigger.
- `SecureTokenStore` session-file reads and the debug picker's
  `LiveSnapshotStore.load` in composition remain open debt (unchanged).
