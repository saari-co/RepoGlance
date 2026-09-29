# targetSdk 36 — proof

Google Play has required API 36 for new apps and app updates since
2026-08-31, so a RepoGlance build targeting 35 can no longer be uploaded
(tracker #7). Branch `claude/targetsdk-36`, cut from `origin/main` at
`432be33` (after #41).

## Change

- `compileSdk` and `targetSdk` 35 → 36.
- AGP 8.7.3 → 8.10.1. AGP 8.10 is the first line whose documented maximum
  API level is 36. Gradle 8.14.4 and JDK 17 already meet its minimums.
- AGP 8.10's lint adds `UseKtx`. Under the repo's warnings-as-errors gate it
  flagged 31 sites. All of them were `SharedPreferences.edit()…apply()` or
  `Uri.parse(…)`, and they now use `edit { … }` and `toUri()`. `edit { }`
  defaults to `apply()`, and no site used `commit()`, so write behaviour is
  unchanged.
- `androidx.core:core-ktx` is now declared directly at 1.13.1, the version
  already resolved through the transitive dependency. The code calls it
  directly now.
- `lint-baseline.xml` shrinks by two entries that no longer fire:
  `OldTargetApi` (fixed by this change) and `MissingApplicationIcon`.

## Android 16 behaviour changes for apps targeting 36

| Change | Exposure in RepoGlance | Result |
| --- | --- | --- |
| Edge-to-edge opt-out removed | Every activity already calls `enableEdgeToEdge()`; nothing opts out | Content starts below the status bar on both phones |
| Predictive back on by default (`onBackPressed` and `KEYCODE_BACK` no longer delivered) | Back goes through Compose `BackHandler`, which uses `OnBackPressedDispatcher`; there are no `onBackPressed` overrides | Back dismisses the sheet, then returns Home, then leaves the app |
| Orientation, aspect-ratio and resizability limits ignored at sw ≥ 600 dp | The manifest sets none | Not applicable |
| `elegantTextHeight` ignored | Not used | Not applicable |
| At most one missed fixed-rate run replays | Refresh uses WorkManager periodic work, not `scheduleAtFixedRate` | Not applicable |

## Build gate

```
./gradlew assembleDebug assembleRelease check
BUILD SUCCESSFUL
Lint found no new issues (and 1 error filtered by baseline lint-baseline.xml)
testDebugUnitTest   273 tests, 0 failures
testReleaseUnitTest 267 tests, 0 failures
aapt2 dump badging: app-debug.apk and app-release-unsigned.apk targetSdkVersion '36'
```

## Device runs

Device: Pixel 10 Pro XL, serial `63310DLCQ000RV`, Android 17 (SDK 37). RepoGlance
was not installed before this run. The installed debug build reports
`targetSdk=36`, and `bin/verify-repoglance doctor` passed (device APK equals
the local build).

A first run on Pixel 10 Pro Fold `59151FDCG000JA` was abandoned because another
agent was driving that phone at the same time. Its dumps failed after the
first BACK, and Chrome took the foreground. Only the pre-BACK Fold capture is
kept below; no Fold result after BACK is claimed.

| Step | Command | Observed |
| --- | --- | --- |
| Navigator, Issues mode | `launch MIXED navigator acme/rocket ISSUES`, `dump list` | `repoglance:fixture-mode-ISSUES` and the `#100` row present; first control top at y=191, status bar frame `[0,0][1080,161]` |
| Row opens the phone sheet | `tap "Fix flaky retry in sync worker"`, `dump sheet-open` | `repoglance:navigator-detail-sheet` present |
| BACK 1 | `input keyevent KEYCODE_BACK`, `dump back-1` | sheet gone, list present, `MainActivity` still resumed |
| BACK 2 | `input keyevent KEYCODE_BACK`, `dump back-2` | fixture home (`repoglance:fixture-navigator`) |
| BACK 3 | `input keyevent KEYCODE_BACK` | top resumed moves to the previous task (Chrome); the app leaves |
| Sign-in entry, no session | `launch MIXED live`, `dump signin` | `repoglance:connect-github` present, not tapped |
| Post-token screen | `launch MIXED signin-finishing`, `dump signin-finishing` | `repoglance:signin-mark` and `Finishing sign-in…` |

Captures (images stay in ignored `runs/verify-repoglance-runs/`):

| Capture | SHA-256 |
| --- | --- |
| Fold `navigator-issues` (inner display, before the conflict) | `bd51bec0b1ad2806b23ec51058f6f01b145a617573a53f7c990f91f9a0666c11` |
| XL `xl-navigator-issues` | `abd669305d78fb2f64c219d169f64fe914c9aaacf33d5eb4b86376b16c12589e` |
| XL `xl-navigator-sheet` | `ed9f0031999ad92a61050aa51e6701132af2e1e681e343663df3aec4bd752063` |
| XL `xl-home` | `815447f6741b31eb75dd8133589eb3039d2249a59acb3acb3c4f578228766a52` |
| XL `xl-signin-entry` | `e9d0a02ea3a1bdc195caa7879986828052bf68fb48145ced9ce327d8db1f7d8a` |
| XL `xl-signin-finishing` | `61284365fb0e50c4352c4fe089c1f2fcabab154cec83073981b57e553ffe2c52` |

## Sign-in return under targetSdk 36 (BAL probe)

The riskiest path is sign-in return (`signin-return-027`): once the token is
committed, `MainActivity` starts itself over the GitHub Custom Tab. This probe
re-ran the method in `proof/signin-return-20260921/BAL_PROBE.md` at targetSdk
36, with no GitHub, no account and no code. A throwaway debug-only `singleTask`
`BalProbeActivity` opened `https://example.com/` in a Custom Tab. Eight seconds
later it started itself with `CLEAR_TOP|SINGLE_TOP` from the activity context,
the same call `MainActivity.returnFromGitHubVerification` makes. The probe was
never committed and was deleted after the run, and the clean build was
reinstalled. `pm resolve-activity` for the probe now reports `No activity
found`.

```
RESULT created targetSdk=36                 11:38:32.115
RESULT attemptA start                       11:38:41.353
RESULT onNewIntent                          11:38:41.430
RESULT onResume #2                          11:38:41.430
```

- At +5s the top-resumed activity was `customtabs.CustomTabActivity` in task 63.
- After the self-start, task 63 held only `BalProbeActivity`. The Custom Tab
  record was finished (`t-1 f`), removed rather than covered.
- Background-activity-launch block lines in logcat: 0.

## Not proven here

- **Real end-to-end sign-in at targetSdk 36.** Human-gated: the maintainer
  signs in. The probe proves the launch is allowed, not the full flow.
- **Fold inner display after BACK, and the Fold two-pane detail.** Not re-run
  because the Fold was in use by another agent. The XL covers the phone
  layout.
- **Widgets and the Quick Settings tile under 36.** Not re-driven. Neither
  depends on the changed platform behaviours above.
- **#32.** Leaving the GitHub tab for another app before the token lands is
  still deferred.
