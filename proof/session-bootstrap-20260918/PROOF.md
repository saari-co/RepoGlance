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

## Proof: device cold start (PENDING)

Planned: `bin/verify-repoglance doctor`, `launch MIXED live`, `launch MIXED
picker` on the registered Pixel 11 Pro Fold, logcat filtered to `StrictMode`
lines with `co.saari.repoglance` frames plus process-start lines, expecting
zero. Not run: at 16:45Z the phone dropped off adb (no USB serial, no
`adb mdns services` entry) after being present at the start of the session.
Blocked on the maintainer reconnecting the phone.

## Remaining risk

- `GitHubSession.accessToken()` may refresh and write the token; it already
  runs on `Dispatchers.IO` inside `GitHubApiClient`, unchanged.
- Ordering of a queued `signOut()` against a later device-token write relies on
  the single-thread dispatcher's FIFO order; both go through it.
