# Main-thread I/O debt: token store and debug launcher (2026-09-19)

Branch `claude/token-store-off-main` off main `948c159` (after #25).

## Token-store read: already fixed, INVARIANTS corrected

`docs/INVARIANTS.md` still listed two 2026-09-18 catches as open:

- the `SecureTokenStore` session-file read during `RepoGlanceViewModel`
  construction
- the debug picker's `LiveSnapshotStore.load` in composition

`session-bootstrap-016` (`proof/session-bootstrap-20260918/PROOF.md`,
reviewed clean) fixed both. The token file is `lazy`, session reads and
writes run on the ViewModel's `sessionDispatcher`, and the picker loads on
`Dispatchers.IO`.

Re-check on current code, on the Pixel 11 Pro Fold `66261FDDJ002J5`, with a
signed-in session:

| Run | StrictMode lines | `DiskReadViolation` | `SecureTokenStore` | logcat SHA-256 prefix |
|---|---|---|---|---|
| Direct `am start -n …/.MainActivity` cold start | 1 (`UntaggedSocketViolation`, `UrlConnectionTransport.execute`, IO thread) | 0 | 0 | `63fb60c7bb3786d0` |

The two earlier pin-toggle cold starts the same day
(`proof/widget-reads-off-main-20260919/PROOF.md`) also show no
`SecureTokenStore` or `RepoGlanceViewModel` frames.

## Debug launcher write: fixed

`devlaunch.ScenarioLaunchActivity` (debug source set only) wrote
`AppPrefs.setSelectedScenario` and `RefreshProbe.arm` on the main thread,
logging three `DiskReadViolation`s on every `bin/verify-repoglance launch`.
Now it:

- is a `ComponentActivity`
- performs both writes in `lifecycleScope` on `Dispatchers.IO`
- starts the next screen only after the writes return

The next screen still sees the scenario: the loaded `SharedPreferences`
instance is process-wide, and `apply()` updates it in memory at once.

- `ScenarioLaunchActivitySourceTest` checks three things: both writes sit
  inside `withContext(Dispatchers.IO)`, no `AppPrefs.`/`RefreshProbe.` call
  is outside it, and `startActivity(next)` follows it.
- `./gradlew check assembleDebug` is green.

### Device proof: Pixel 9 Pro `4C061FDAP000AD` (aiworker-02)

- **Access:** reached through a local ssh tunnel to aiworker-02's adb server
  (`ADB_SERVER_SOCKET=tcp:127.0.0.1:5047`, `VERIFY_SERIAL=4C061FDAP000AD`).
- **Why not the Folds:** both were in use by other sessions. The Pixel 11 had
  a newer RepoGlance build installed, and the Pixel 10 Fold was running
  OpenClaw.
- **Starting state:** the phone had no RepoGlance installed.
- **Clean-up:** it was uninstalled afterwards, leaving the phone on the
  launcher.

| Build | Launch | StrictMode / DiskRead | Launcher frames in violations | Stored `selected_scenario` | logcat SHA-256 prefix |
|---|---|---|---|---|---|
| main `948c159` (`bd06bfaf…`) | `launch RATE_LIMITED navigator` | 5 / 3 | 11 (`ScenarioLaunchActivity`, `AppPrefs.setSelectedScenario`) | n/a | `0d78ea3980ee423f` |
| branch (`7561d269…`) | `launch RATE_LIMITED navigator` | 0 / 0 | 0 | `RATE_LIMITED` | `fc384bea20ce55d7` |
| branch | `launch EXACT navigator` | 0 / 0 | 0 | `EXACT` | `d47bd88f2370b4a6` |
| branch | `launch LAST_GOOD navigator` | – / 0 | 0 | `LAST_GOOD` | not retained |
| branch | `am start … --el probeCommitDelaySeconds 1` | – / 0 | 0 | probe prefs record `armed` | not retained |

- `doctor` passed on the branch build: `apk_local=apk_device=7561d269…`,
  `scenario_launcher_present=yes`, `posture=DEFAULT`.
- Each launch reached `co.saari.repoglance/.MainActivity` with
  `repoglance:fixture-home`.
- The stored scenario was read with `run-as co.saari.repoglance cat
  shared_prefs/repoglance.xml`.

## Found, not fixed here

- On main, `bin/verify-repoglance doctor` and `launch` exit 1 without a
  message when RepoGlance is not installed on the chosen phone. Under
  `set -euo pipefail`, the package lookups fail.
