# BAL feasibility probe — can RepoGlance dismiss its own Custom Tab?

Research spike for the device-flow return grill. Not a feature, not a claim
about RepoGlance's shipped behaviour. It answers one question so the grill's
first decision rests on a fact instead of a recollection.

## Question

GitHub's device flow has no redirect, AGENTS.md Boundaries forbid an auth
callback and any server-side component, and the Custom Tabs protocol exposes no
programmatic close. The only remaining lever is the app starting itself when the
poll returns a token. Android restricts background activity launches, and the
current Activity-security page does **not** list "has an activity in the back
stack of the foreground task" among its exceptions.

So: on this device, may a backgrounded app start its own `singleTask` activity
and clear a Custom Tab it launched into its own task?

## Method

A throwaway debug-only `BalProbeActivity` (declared `singleTask` in the debug
manifest). It was never committed and was removed before the slice's first
commit; its core is reproduced below so the run can be repeated. It mirrors the
real path: a `singleTask` activity launches a Custom Tab into its own task, is
backgrounded by it, and a coroutine that survives the stop (`lifecycleScope`,
the same shape as the `viewModelScope` poll job) later starts the same activity.

- Attempt A at +8s: `startActivity` from the activity context, `CLEAR_TOP|SINGLE_TOP`.
- Attempt B at +14s: `startActivity` from the application context, `NEW_TASK|CLEAR_TOP|SINGLE_TOP`.

No GitHub, no account, no device code: the URL is `https://example.com/`, and
nothing in the probe reads, writes or logs a token.

```kotlin
lifecycleScope.launch {
    delay(1_200L)
    CustomTabsIntent.Builder().setShowTitle(true).build()
        .launchUrl(this@BalProbeActivity, Uri.parse("https://example.com/"))
    delay(8_000L)
    startActivity(
        Intent(this@BalProbeActivity, BalProbeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    )
}
```

`onNewIntent` and a second `onResume` logged `RESULT` lines under the tag
`RepoGlanceBalProbe`.

## Environment

| | |
| --- | --- |
| Device | Pixel 11 Pro Fold, serial `66261FDDJ002J5`, inner display, OPENED |
| OS | Android 17, `ro.build.version.sdk=37` |
| App | `co.saari.repoglance` debug, `targetSdk=35`, `minSdk=31` |
| Browser | Chrome `org.chromium.chrome.browser.customtabs.CustomTabActivity` |
| Date | 2026-09-20 |

## Commands

```
adb -s 66261FDDJ002J5 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s 66261FDDJ002J5 logcat -c
adb -s 66261FDDJ002J5 shell am force-stop co.saari.repoglance
adb -s 66261FDDJ002J5 shell am start -n co.saari.repoglance/.devlaunch.BalProbeActivity
adb -s 66261FDDJ002J5 shell dumpsys activity activities
adb -s 66261FDDJ002J5 exec-out screencap -d 4619827677550801152 -p
adb -s 66261FDDJ002J5 logcat -d -s RepoGlanceBalProbe
```

`screencap` needs the explicit inner-display id on this Fold; without `-d` it
emits a "Multiple displays were found" warning instead of a PNG.

## Result — allowed

Attempt A succeeded. The app returned to the foreground **58 ms** after the
self-start, and the Custom Tab was removed from the task rather than merely
covered.

Task `t1256` while the tab was on top:

```
* Hist  #1: ActivityRecord{... com.android.chrome/...customtabs.CustomTabActivity t1256}
* Hist  #0: ActivityRecord{... co.saari.repoglance/.devlaunch.BalProbeActivity t1256}
ResumedActivity: ActivityRecord{... com.android.chrome/...customtabs.CustomTabActivity t1256}
```

Task `t1256` after attempt A:

```
* Hist  #0: ActivityRecord{... co.saari.repoglance/.devlaunch.BalProbeActivity t1256}
topResumedActivity=ActivityRecord{... co.saari.repoglance/.devlaunch.BalProbeActivity t1256}
```

`grep -i "background activity launch blocked"` over the full logcat: **0 hits**
(`bal-blocked.txt` is empty). Attempt B also landed, so the application-context
variant is available too; A is the one the feature would use.

## Artifacts

Images and logs stay in the gitignored run directory
`runs/bal-probe-20260920/` on Bobby's MacBook; the hashes below pin them.

| File | SHA-256 |
| --- | --- |
| `01-tab-on-top.png` | `420c3c468de7f1d07b79ffc305ea6f79da24118e5765451be2d7d583d1b80e62` |
| `02-app-forward.png` | `ee41511bfb78db4f9f05da1486e2e8f2be1c2cf611726ec856ca08d9b3bc9169` |

`probe-logcat.txt` is the probe's own trace. `bal-blocked.txt` is empty by
design: it is the negative evidence.

## Scope and limits

- One device, one OS build, one browser. It shows the launch is permitted here;
  it is not a guarantee for every Android version or every Custom Tabs provider.
  Any feature built on it must degrade to the user closing the tab by hand.
- The probe does not prove the *product* decision, only that the mechanism is
  available.
- Authorizing on a second machine leaves no tab on this phone to dismiss; the
  self-start must be a no-op when the app is already foreground.
- The probe's first run captured screenshots with `screencap` and no `-d`; on
  this Fold that writes a text warning instead of a PNG. The run above was
  repeated with the inner-display id, and only those captures are recorded.
