# Rate-limit device proof — GrillTrack `rate-limit-device-proof-026` (2026-09-19)

Track `gt-20260728163459-227573`, the recommendation recorded after the
live-widgets map closed (`.grilltrack/maps/live-widgets.md`). The goal is to
prove truth rule 4 (rate limit is first-class state; back off visibly) on the
placed repo and stack widgets instead of only in unit tests. Branch
`claude/amazing-tharp-89c945`, worked in the isolated worktree
`.claude/worktrees/amazing-tharp-89c945`, based on main
`948c159343f884c45eaa921c651d9b9b2432657f` (PRs #24–#27 merged).

## Decision (sequential grill, confirmed before implementation)

1. **Fault seam:** a debug-only `hooks.TransportFault` rewrites rate-limit
   headers on real calls. LOW makes the real GitHub call and rewrites only
   the `X-RateLimit-*` headers (250 of 5000 remaining, reset in the future).
   EXHAUSTED answers a synthetic 403 (0 remaining, reset about 3 minutes
   out) and never calls GitHub.
2. **Surfaces:** the stack header and rows; the repo widget in both layouts
   (the maintainer resizes it mid-proof); the in-app catalog rate-limit line.
3. **Assertions:** the 023/024 locks as they are. At LOW the widgets show no
   rate-limit text by design. The maintainer kept that; a visible LOW marker
   would be a separate grill.
4. **LOW skip of pins without their own repo widget:** not device-proven.
   The phone had one pin, and it had its own repo widget. It stays
   unit-tested (`RefreshPlanTest`); pins were not touched for it.
5. **Trigger and recovery:** a debug `refreshNow` extra calls the production
   `BackgroundRefresh.refreshNow`. Recovery is a dump after the reset time,
   then one triggered run.

Prior locks kept: read-only (no GitHub mutation; the fault changes only the
responses the app receives); auth rules (no token access, and the fault
never logs a URL, request header or body); 023/024 display rules unchanged;
CI floor.

## Implementation (`git:c5cf58bdb11b9520a4e2a698b94e1d1276461b5b`)

- `app/src/debug/.../hooks/TransportFault.kt`: `arm(kind|OFF, resetSeconds)`
  persists to `repoglance_debug_fault` prefs so the fault survives process
  death, and expires by itself at its reset time. `wrap()` routes each
  request through `respond()`. The only evidence it records is per-kind
  served counts with the last status and time.
- `app/src/release/.../hooks/TransportFault.kt`: `wrap()` returns the
  transport unchanged.
- `app/src/main/.../data/LiveGitHub.kt`: the one wiring change,
  `GitHubApiClient(session, TransportFault.wrap(context, UrlConnectionTransport()))`.
  No comments in main.
- `ScenarioLaunchActivity` (debug): new extras `rateFault`,
  `rateFaultResetSeconds` (default 180) and `refreshNow`, plus
  `screen=none`, which applies the extras and stays on the current screen.
- Feature map: `refresh-freshness.md` gains `fresh-rate-limit-widgets` and
  the drive recipe, and replaces the old "cannot be forced without real quota
  use" gotcha. The device-learned gotchas are added in the follow-up docs
  commit (see Findings).

## JVM tests and CI floor

`app/src/testDebug/.../hooks/TransportFaultTest.kt`, 6 tests, 0 failures:
- disarmed passes the real response through;
- LOW makes the real call, keeps status, body and other headers, and
  replaces the `X-RateLimit-*` headers case-insensitively (no duplicates);
- EXHAUSTED never calls the delegate;
- the fault expires exactly at its reset time;
- through the real `GitHubApiClient`, LOW parses as `LOW` (250 remaining,
  reset carried) with real counts, and EXHAUSTED as `EXHAUSTED` with
  "GitHub's rate limit is exhausted".

`./gradlew --no-daemon check` green (`BUILD SUCCESSFUL`, 61 tasks).
`app/lint-baseline.xml` and the detekt baselines are unchanged. The first
check run failed detekt on `android.util.Log`; the fault now records to
debug prefs, and `Log` stays banned.

## Device proof (Pixel 11 Pro Fold `66261FDDJ002J5`)

`VERIFY_SERIAL=66261FDDJ002J5`. A Pixel 10 Pro Fold and an emulator were
also attached. Doctor passed on the installed build (APK SHA-256
`6fe9b37172cf992920737b33ac482667fa210ca172dfb1bb0681be0b763d1c6c` both
local and on the device). Posture read `CLOSED` (cover, 1080x2342) for the
tall phase and `OPENED` (inner, 2076x2152) for the compact phase. Run
directory `runs/verify-repoglance-runs/rl026-20260919/`, which is ignored.

Starting state: stack id 16 and repo widget id 18 (`saari-co/RepoGlance`,
ISSUES); one live pin, `saari-co/RepoGlance`. On the cover display the repo
widget rendered **tall** (page 2) and the stack was on page 3. The catalog
read `GitHub rate limit: 4908 remaining`.

| Phase | Widget / surface | Observed (dump) | Capture SHA-256 |
| --- | --- | --- | --- |
| LOW, run 1 (15:59:49Z) | store | `bucket=LOW`, 2 faulted real calls (`served.LOW=2`, status 200) | — |
| LOW, run 2 (16:01:18Z) | tall repo | `ISSUES 2 · as of 12:01 PM` (was 11:51); no `rate limited` | `f24f5910…a7b3ec` |
| LOW, run 2 | stack | `Pinned · 1`; row `12:01 PM`, `issues 2 · PRs 0 · review 0`; no `rate limited` | `40f3da48…c6f1a0` |
| LOW | catalog | `GitHub rate limit is low: 250 remaining` (dump only; the unfiltered catalog is not captured) | — |
| EXHAUSTED (16:02:46Z) | store | `bucket=EXHAUSTED`, `resetsAt 16:05:46Z`; one synthetic 403 served, run stopped | — |
| EXHAUSTED | tall repo | `rate limited · resets 12:05 PM · last good · ISSUES 2 · as of 12:01 PM` | `5f223eaf…2bb6d` |
| EXHAUSTED | stack | `Pinned · 1 · rate limited · resets 12:05 PM`; row `last good 12:01 PM`, counts kept | `f8c6c937…82d5` |
| after reset, before a run (16:06:02Z) | tall + stack | still `rate limited · resets 12:05 PM` (locked 023: redraw only when a run finishes) | — |
| recovery run | tall repo | `ISSUES 2 · as of 12:06 PM`; `bucket=OK`; no fault served | `ee914f11…c5db532` |
| recovery run | stack | `Pinned · 1`; row `12:06 PM`, real counts | `ad9987d3…7c961e` |
| EXHAUSTED (16:13:38Z, inner) | compact repo | `rate limited · 12:06 PM` (error colour), `issues 2`, `PRs 0` | `17c52456…82d89` |
| EXHAUSTED (inner) | stack (resized) | `Pinned · 1 · rate limited · resets 12:16 PM`; row `last good 12:06 PM` | same capture |
| after reset (16:16:54Z) | compact + stack | still `rate limited` | — |
| recovery run | compact + stack | compact `12:17 PM`, `issues 2`, `PRs 0`; stack `Pinned · 1`, row `12:17 PM`; no `rate limited` anywhere; `bucket=OK` | `00cb4aa6…3c155c` |

Counts never rendered as `0` in place of a known value: every rate-limited
render kept the last saved counts and marked them `last good` or
`rate limited`. The fault served 9 LOW responses (real calls) and 2
EXHAUSTED responses (no GitHub call). It was disarmed at the end with
`rateFault OFF`, leaving the prefs empty.

## StrictMode (whole run, host-side `adb logcat -v time`)

`logcat-full.txt` SHA-256 `85cf57fe…07f862e`. Violations attributed to
their app frames (`strictmode-callers.txt` `29778029…81d44d0`):

- **0** violations with a `co.saari.repoglance.widget` frame, so PR #26's
  fix holds under redraw, rate-limit and recovery runs.
- Main-thread `DiskReadViolation`s occur only in the debug-only
  `ScenarioLaunchActivity.onCreate`: the existing
  `AppPrefs.setSelectedScenario` and the new `TransportFault.arm`. Both are
  in the launcher and never ship.
- 7 `UntaggedSocketViolation`s (VM policy) come from real calls in
  `UrlConnectionTransport.execute` on the worker thread. The fault wrapper is
  in the stack only because it delegates; it opens no socket.

## Findings (recorded, not fixed in this cycle)

1. **The tall header hides the data age while rate limited.** The dump holds
   the full `rate limited · resets 12:05 PM · last good · ISSUES 2 · as of
   12:01 PM`, but the single-line header renders
   `rate limited · resets 12:05 P…`. The counts, `last good` and `as of`
   time are cut off, so on screen truth rule 3 fails for the tall widget
   while rate limited. The rows below still show.
2. **The stack header cuts the reset time.** It rendered
   `Pinned · 1 · rate limited · resets 12:0…` on the cover and `… 12:1…` at
   the inner size. The rows still show `last good <time>` and the counts.
3. **The debug launcher needs `--activity-clear-task`.** Without it, Android
   brings the existing task forward (`result code=2/3`) and the extras are
   never read. The recipe now includes the flag.
4. **The compact repository name truncates** to `Rep…` at the floor size.
   This is the known carried-forward compact-floor item, seen again.

## Disclosures

- **Another adb client** drove the phone once at about 12:11:05–20 local:
  `logcat -c`, `am start MainActivity`, `logcat -d`, `uiautomator dump
  /sdcard/u.xml`, then `am force-stop co.saari.repoglance`. None of these
  came from this run. After that force-stop, the widgets the maintainer had
  just resized on the inner display sat on the launcher's loading
  placeholder (`inner-before`, `f06960d2…61f07e`) until this run's
  EXHAUSTED refresh redrew them. The package wasn't reinstalled
  (`lastUpdateTime` stayed 11:57:39).
- **Pins and placed widgets changed during the maintainer's restore.** After
  the proof, the maintainer resized the widgets back by hand. Afterwards the
  placed widgets were new ids (stack 20, repo 21), and the repo widget and
  the only live pin were a different saari-co repository in BOTH mode
  (`inner-restored`, `7adc5cda…59137e`). This run's last command was the
  12:17:50 disarm, so the change came from the maintainer's hands, not this
  run. It is reported, not reverted.
- `bin/verify-repoglance cleanup` was not run: its force-stop is what
  stranded the resized widgets. The scenario was already `MIXED` from this
  run's last `launch`, and airplane mode was never used.

## Gaps

- LOW's skip of pins without a repo widget is unit-tested only (one pin on
  the device).
- The fault was proven on the background refresh and the catalog load, not
  on the in-app repository view's per-section rate-limit lines.
