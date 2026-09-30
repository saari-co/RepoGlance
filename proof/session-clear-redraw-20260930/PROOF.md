# Session-clear widget redraw survives `onCleared` (2026-09-30)

Branch `claude/session-clear-redraw-on-io` off main `8713d0f` (after #44).
Guard row added to `docs/INVARIANTS.md` (layer 2).

## Finding

Source: the independent review of GrillTrack node `sample-widgets-039`
(PR #45, `.grilltrack/proof/sample-widgets-039-verify-20260930.md`, "Round 2
adjudication").

`RepoGlanceViewModel.clearSessionAndTileRecord()` does two things, in order:

1. clears the session and every widget-visible store inside `session.signOut { … }`
2. redraws placed widgets with `WidgetRefresh.updateAll(context)`

Every session clear runs this helper on `sessionDispatcher + NonCancellable`,
and `onCleared()` closes `sessionDispatcher`.

The store clears are all synchronous (`Session.signOut` is `@Synchronized`,
not `suspend`), so they finish before the first suspension. The redraw does
suspend, inside Glance `updateAppWidgetState`/`update`. If the ViewModel is
cleared at that moment:

- The redraw resumes onto the closed executor, which rejects it.
- kotlinx.coroutines 1.7.3 (the resolved version) handles the rejection in
  `ExecutorCoroutineDispatcherImpl.dispatch`: it calls
  `cancelJobOnRejection`, then re-dispatches on `Dispatchers.IO` (checked
  with `javap`).
- Because the job is now cancelled, the redraw throws
  `CancellationException` and stops.
- Placed widgets keep showing the previous live counts after sign-out
  (truth rules 2-3).

## Fix

```diff
-        WidgetRefresh.updateAll(context)
+        withContext(Dispatchers.IO) { WidgetRefresh.updateAll(context) }
```

This is the same pattern PR #45 uses for the sample-mode redraws.

- The store clears stay on `sessionDispatcher + NonCancellable`, under the
  session lock.
- Every suspension inside the redraw now resumes on `Dispatchers.IO`.
- The only resume onto `sessionDispatcher` is the return from `withContext`,
  at the end of the helper. By then the redraw has already finished, so a
  rejection there loses nothing.

Callers checked. All four session-clear paths go through the one helper
(`LatestPushRecordTest` pins a single `session.signOut`):

| Path | Entry | Runs the helper via |
|---|---|---|
| Disconnect | `signOut()` → `authorizationCommitGate.invalidate(::clearSavedSession)` | `viewModelScope.launch(sessionDispatcher + NonCancellable)` |
| Cancel sign-in | `cancelGitHubAuthorization()` → same | same |
| Catalog load says sign in again | `loadCatalog` → `clearSavedSessionNow()` | `withContext(sessionDispatcher + NonCancellable)` |
| Repository load finds the session invalid | `refreshSelectedRepository` → `clearSavedSessionNow()` | same |

The two `clearSavedSessionNow()` callers still wait for the redraw before
they update the UI state. That ordering is unchanged.

The one remaining bare redraw is in `refreshSelectedRepository`. It runs on
`viewModelScope`'s main dispatcher, which `onCleared` never closes, so it is
outside this finding.

## Guard

`app/src/test/java/co/saari/repoglance/SessionClearRedrawGuardTest.kt` is a
source guard in the style of `SampleModeGuardTest`. Its three tests:

- **`theSignOutRedrawSurvivesOnClearedClosingTheSessionDispatcher`:** checks
  that `onCleared` closes `sessionDispatcher` (the premise). Then checks that
  the helper redraws exactly once, as
  `withContext(Dispatchers.IO) { WidgetRefresh.updateAll(context) }`, after
  the `session.signOut { … }` block.
- **`everySessionClearRunsTheHelperOnTheSessionDispatcherPastCancellation`:**
  checks that both callers use `sessionDispatcher + NonCancellable` and that
  nothing else calls the helper.
- **`noViewModelRedrawRunsDirectlyOnTheSessionDispatcher`:** strips the
  IO-wrapped redraws and requires that exactly one bare `WidgetRefresh.`
  call remains, the main-dispatcher one in `refreshSelectedRepository`. This
  still holds after PR #45 merges, because its three new redraws use the IO
  form.

Mutation checks (source edited, the guard run, the fix restored, the diff
re-checked):

| Mutation | Result |
|---|---|
| Redraw back to bare `WidgetRefresh.updateAll(context)` (byte-identical to `origin/main`) | 2 of 3 fail on assertions. `theSignOut…`: "the sign-out redraw runs on Dispatchers.IO, so its suspensions never resume onto the closed session dispatcher". `noViewModelRedraw…`: "… never directly on sessionDispatcher expected:<1> but was:<2>" |
| IO redraw moved above `session.signOut {` | 1 of 3 fails on an assertion. `theSignOut…`: "widgets redraw after the stores are cleared, outside the session lock" |

## Runtime reproduction

Added after the ClawSweeper review of `2015dde` asked for real behaviour
proof.

`app/src/test/java/co/saari/repoglance/SessionDispatcherCloseTest.kt` runs
the race for real on the JVM. It uses kotlinx.coroutines 1.7.3 (the version
the app resolves) and needs no test-only dispatcher or new dependency. The
setup matches the ViewModel's:

- `sessionDispatcher` is a real `Executors.newSingleThreadExecutor().asCoroutineDispatcher()`.
- The session clear is launched as `launch(sessionDispatcher + NonCancellable)`
  on a `SupervisorJob` scope that stands in for `viewModelScope`.
- The redraw stand-in suspends twice, like Glance: once awaiting a pending
  widget-state write (`updateAppWidgetState`), then again at `yield()`
  (`update`).

Each run then:

1. waits until the redraw is suspended
2. does what `ViewModel.clear()` does: cancels the scope, then closes the
   dispatcher (`onCleared`)
3. waits for the session thread to go idle, which guarantees the redraw is
   parked inside its suspension
4. completes the Glance write

| Test | Redraw runs | Steps recorded | Session-clear job |
|---|---|---|---|
| `aRedrawSuspendedOnTheSessionDispatcherIsLostWhenOnClearedClosesIt` | directly on `sessionDispatcher` (the `main` code) | `stores cleared`, `redraw started`, then nothing | cancelled: the redraw is lost |
| `aRedrawOnIoFinishesWhenOnClearedClosesTheSessionDispatcher` | inside `withContext(Dispatchers.IO)` (this PR) | `stores cleared`, `redraw started`, `widget state written`, `widgets updated` | cancelled only by the final return to the closed dispatcher, after the redraw |

The first row is the negative control: it reproduces the reported loss on
the real coroutines runtime. The only difference between the two rows is
the `withContext(Dispatchers.IO)` wrapper, and `SessionClearRedrawGuardTest`
pins that the real helper uses the wrapped form.

Stability: 30 of 30 consecutive `--rerun` runs of the class passed (2/2 each
time).

## Verification

`ANDROID_HOME=~/Library/Android/sdk ./gradlew assembleDebug check` finished
with BUILD SUCCESSFUL, run again after adding the runtime test:

| Unit tests | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `testDebugUnitTest` | 292 | 0 | 0 | 0 |
| `testReleaseUnitTest` | 286 | 0 | 0 | 0 |

In both variants, `SessionClearRedrawGuardTest` ran 3/3 green and
`SessionDispatcherCloseTest` ran 2/2 green. `LatestPushRecordTest` is still
green, 4/4.

**Not run in the app on a device.** The runtime test reproduces the
coroutine race, not the app. The project has no Robolectric, and
`RepoGlanceViewModel` builds the real GitHub services, so the test cannot
construct it. A device run would have to catch the ViewModel being cleared
inside the redraw window, which is timing-dependent. It would also sign the
test phone's real session out, and signing back in needs maintainer approval
(`AGENTS.md` hard gates).

## Report only: `RepoWidgetReceiver.onDeleted` main-thread prefs reads

This is not changed on this branch.

On `main`, `onDeleted` makes these prefs calls on the main thread before
`super.onDeleted`:

- `RepoWidgetConfigStore.load`
- `RepoWidgetConfigStore.remove`
- `RepoWidgetConfigStore.configuredRepos`
- `AppPrefs.removeLivePins`

PR #45 adds the sample equivalents and a fire-and-forget
`CoroutineScope(SupervisorJob() + Dispatchers.IO)` stack redraw. It records
the main-thread reads as debt in the INVARIANTS sample row. That redraw runs
outside any `goAsync` window, so the process can die before it runs.

Candidate fix for a follow-up slice after #45 merges: move the cleanup into
`override suspend fun onDelete(context, glanceId)` on `RepoWidget`. `javap`
of glance-appwidget 1.1.1 shows the path:

- `GlanceAppWidgetReceiver.onDeleted` already runs
  `goAsync(coroutineContext)` on `Dispatchers.Default`.
- For each id, it calls `GlanceAppWidget.deleted`, which then calls
  `onDelete`.

In that hook, resolve the id with `GlanceAppWidgetManager.getAppWidgetId`,
clear the live and sample setups, and release the pins that no remaining
setup uses. The stack redraw can then run inside Glance's `goAsync` window.

The receiver cannot take its own `goAsync()`, because `super.onDeleted`
already consumes the broadcast's pending result.

Two things change and need their own proof: pins are released one id at a
time instead of as one batch, and the cleanup now runs after Glance closes
the widget session.
