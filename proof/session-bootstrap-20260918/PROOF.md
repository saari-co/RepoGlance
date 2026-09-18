# session-bootstrap-016: session store I/O off the main thread

GrillTrack decision `session-bootstrap-016` (track `gt-20260728163459-227573`),
branch `claude/session-bootstrap-016`, base `main` at
`bd5ea67` (after saari-co/RepoGlance#15).

## Problem (from proof/ci-floor-20260918/PROOF.md)

- `RepoGlanceViewModel.<init>` called `GitHubSession.hasSavedSession()` on the
  main thread: six `DiskReadViolation`s per cold start from
  `SecureTokenStore.<init>` and `SecureTokenStore.read`.
- The debug picker's PERSISTED candidate called `LiveSnapshotStore.load`
  inside the composable handed to `GlanceRemoteViews.compose`.

## Confirmed slice

1. `SecureTokenStore.tokenFile` is `lazy`, so construction touches no disk.
2. The ViewModel owns a single-thread `sessionDispatcher`. `hasSavedSession()`
   runs there from `viewModelScope` in `init`; `liveState` stays
   `LiveUiState.Checking` ("Checking your GitHub session…", text only) until
   the answer, then `refreshCatalog()` or `SignedOut`. Unknown never renders as
   signed-out or as a count.
3. The two sign-in failure catches take the same hop and re-check the commit
   generation after suspending.
4. Every session write leaves the main thread on the same dispatcher: the
   device-token commit, and `signOut()` from cancel, sign-out, catalog 401 and
   repository-content 401 (maintainer widened the slice to include these).
5. The picker loads the persisted snapshot once on `Dispatchers.IO` before
   composing and passes the value into the candidate.
6. Deferred: `checking-splash-018` (logo with an animation while checking);
   no logo asset exists in the repo.

Feature map unchanged: no user-visible behaviour changed, the Checking state
already existed.

## Proof: CI floor (local, 2026-09-18)

```
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon -q check assembleDebug
EXIT=0
feature-map: ok (5 features, 6 skill sections)
jvm tests=189 failures=0
app-debug.apk sha256=1b85c80cff878a93f29fe857d0e5ea0a7fe851e0e8594f3731a28d7788dfd315
```

## Proof: device cold start (2026-09-18, Pixel 11 Pro Fold)

Run directory (ignored by git): `runs/verify-repoglance-runs/20260918T164500Z-session-bootstrap/`.
`bin/verify-repoglance doctor` before install reported the APK mismatch;
`launch MIXED live` installed the build; `doctor` after passed:
`apk_device=1b85c80cff878a93f29fe857d0e5ea0a7fe851e0e8594f3731a28d7788dfd315`,
`scenario_launcher_present=yes`, `posture=C wakefulness=Awake device_locked=0`.

Each launch force-stopped the process first; `adb logcat -c` before, `-d`
after, then filtered to `ActivityManager Start proc` lines for the package and
`StrictMode policy violation` blocks whose frames include `co.saari.repoglance`.
No token, header, URL or private content is in the filtered files.

| Cold start | Thread-policy violations with app frames (disk/network) | Other app-frame lines | File (sha256 prefix) |
| --- | --- | --- | --- |
| `am start -n …/.MainActivity` (the user path) | **0** | 1 `UntaggedSocketViolation` (VM policy, IO thread, `UrlConnectionTransport.execute`) | `logcat-main-direct-strictmode.txt` (5bfb066ef9506a8a) |
| `am start -n …/.devpicker.WidgetVariantPickerActivity` | **0** | 0 | `logcat-picker-direct-strictmode.txt` (a9e18408eb9f9b7d) |
| `launch MIXED live` via `ScenarioLaunchActivity` | 3 `DiskReadViolation`, all `AppPrefs.setSelectedScenario` ← `ScenarioLaunchActivity.onCreate` (debug launcher) | 1 `UntaggedSocketViolation` as above | `logcat-live-strictmode.txt` (0112f3b93bff8fd9) |
| `launch MIXED picker` via `ScenarioLaunchActivity` | 3, same launcher frames | 0 | `logcat-picker-strictmode.txt` (f858d0b8db6d296c) |

Before this slice the MainActivity cold start logged six `DiskReadViolation`s
from `SecureTokenStore` via `RepoGlanceViewModel.<init>`, and the picker logged
one from `LiveSnapshotStore.load` in composition. Neither frame appears in any
of the four captures.

UI after the direct MainActivity cold start (`main-direct.txt`,
a1d243116fd785da): `repoglance:live`, `repoglance:owner-filter`,
`repoglance:rate-limit`, `repoglance:refresh-repositories`,
`repoglance:repo-search`; no "Checking your GitHub session" text and no
`repoglance:connect-github`. The saved session on the phone was found on the
session dispatcher and the catalog loaded. Cleanup restored MIXED.

## Remaining risk and findings outside the lock

- Debug `ScenarioLaunchActivity.onCreate` writes `AppPrefs` on the main thread
  (three DiskReads per launcher-driven start). Debug source set only, never on
  the user path, but the verifier tier fails on StrictMode lines, so it is a
  bounded follow-up for the verification skill's launcher, not this slice.
- `UntaggedSocketViolation` from `UrlConnectionTransport` is the VM policy's
  `detectAll()` on the IO thread, pre-existing, unrelated to main-thread I/O.

- `GitHubSession.accessToken()` may refresh and write the token; it already
  runs on `Dispatchers.IO` inside `GitHubApiClient`, unchanged.
- Ordering of a queued `signOut()` against a later device-token write relies on
  the single-thread dispatcher's FIFO order; both go through it.
