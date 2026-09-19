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

## Device proof (Pixel 11 Pro Fold 66261FDDJ002J5, cover display)

Both Folds were attached over USB; every adb call and helper ran with
`ANDROID_SERIAL=VERIFY_SERIAL=66261FDDJ002J5`. The stack widget and a repo
widget are placed on the Nexus launcher (`dumpsys appwidget`); the stack is on
home page 4. The redraw trigger is the live-catalog thumbtack, which calls
`WidgetRefresh.updateAll` and bumps `REDRAW_KEY` on every placed widget. Each
toggle pair returns the pin to its starting state (`Unpin saari-co/RepoGlance`,
i.e. pinned).

| Run | Build | Trigger | StrictMode total / DiskRead | Violation frames in `co.saari.repoglance.widget` | logcat SHA-256 |
|---|---|---|---|---|---|
| before | previously installed (`37e8853d…`, pre-fix widget code) | unpin + re-pin | 14 / 13 | 22 (`RepoWidget$provideGlance$2.invoke` 16, `StackWidget$provideGlance$2.invoke` 4, via `RepoWidgetConfigStore`, `RateLimitStore`, `LiveSnapshotStore`, `LiveRowsStore`, `CatalogNamesStore.pushedAt`, `AppPrefs`) | `14d133c8…6a39fa` |
| install | this branch (`978cc248…`) | `adb install -r`, 15 s | 0 / 0 | 0 | not retained |
| after | this branch | unpin + re-pin | 4 / 3 | 0 | `0e273033…189a0` |
| redraw | this branch | unpin, dump home, re-pin, dump home | 4 / 3 | 0 | `aece0139…96dd01` |

- `doctor` passed on the new build: `apk_local=apk_device=978cc248…30fb`,
  `scenario_launcher_present=yes`, awake and unlocked.
- The redraw re-reads the stores off the main thread. The home dump after the
  unpin read `Pinned · 0 ; Pin repositories in RepoGlance`. After the
  re-pin it read `Pinned · 1 ; saari-co/RepoGlance ; issues 2 · PRs 1 · review 0`.
  Capture `home-stack-repinned.png` sha256
  `e30d5a0fdcf330dcced66731836d836fb9b87ad85bc2bc0a0fc97ba01e376bfd` (local
  only: it shows the maintainer's home screen).
- The remaining violations after the fix are the known debug-launcher
  reads (`ScenarioLaunchActivity.onCreate` → `AppPrefs.setSelectedScenario`,
  3 `DiskReadViolation`s) and one `UntaggedSocketViolation` from
  `UrlConnectionTransport.execute` on an IO thread. Neither is widget code,
  and both appear in the before run too.
- `bin/verify-repoglance cleanup` ran afterwards (scenario `MIXED`, airplane
  mode off).
- Earlier in the session, before the Pixel 11 was attached, this debug APK
  was installed by mistake on the unregistered Pixel 10 Pro Fold
  `59151FDCG000JA`. It was a fresh install and was never driven. That phone
  is not evidence.

## Gaps and risk

- The repo widget's page was not dumped. Its absence of StrictMode frames is shown by logcat, not by a visible redraw. Its pre-fix frames (16) came from the same trigger.
- The widget pages were on the cover display (posture closed); the inner display was not exercised.
- On main, a redraw after `adb install -r` depends on the system's
  `APPWIDGET_UPDATE`; the `AppUpdateReceiver` from 025 is not merged here.
  A pin toggle in the live catalog is the reliable `REDRAW_KEY` trigger.
- `SecureTokenStore` session-file reads and the debug picker's
  `LiveSnapshotStore.load` in composition remain open debt (unchanged).
