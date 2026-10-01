# Fixture home without widget pin buttons — proof (2026-10-01)

Claim: the debug fixture home no longer carries `Pin repo widget` /
`Pin stack widget`, no Kotlin string in `app/src/main` shows "stack" to a
user, and Settings → Widgets is the one in-app place to add a widget.

Maintainer decision (2026-10-01, in session): the fixture home is a test
screen, not a user surface, and it should not be in the main app. This
change only drops the two buttons and does not rename them; moving the
whole fixture route (fixture home + fixture navigator) into debug-only
builds is a separate follow-up.

- Base: `origin/main` `fb4afac`
- Device: Android emulator `EMULATOR37X1X11X0` (devices.tsv; AVD
  Pixel_10_Pro_Fold, API 36), signed out
- Doctor after install: installed APK sha256
  `05df3189c13874af2591da4f52c3dce5338dcda32010b201a5aa2accfdb856bf`
  equals the local debug build

## Why the fixture home ships in release

`MainActivity` (exported launcher activity, `app/src/main`) opens
`FixtureRoot` whenever an intent carries `repo_full`. No production code
sends that extra (`WidgetPinsTest` locks the widget header to
`live_repo_full`). On the main build, a plain intent without the debug
launcher reached the fixture navigator, and its `Home` showed the buttons:

```text
adb shell am start -n co.saari.repoglance/.MainActivity -a android.intent.action.VIEW --es repo_full acme/rocket
repoglance:fixture-navigator |  |  | [39,1782][2037,1899]
- | Pin repo widget |  | [384,1953][684,2002]
- | Pin stack widget |  | [1383,1953][1703,2002]
```

## After

`launch MIXED navigator acme/rocket BOTH`, `tap repoglance:fixture-home`,
`dump fixture-home`:

```text
repoglance:scenario | MIXED |  | [1623,175][2037,312]
repoglance:fixture-navigator |  |  | [39,1919][2037,2036]
- | Navigator |  | [948,1953][1128,2002]
```

No row contains `Pin `. Sample mode → menu → `Widgets`, `dump widgets`:

```text
repoglance:widgets-add-repository |  |  | [73,889][989,1006]
- |  | Add Repository widget | [73,898][989,996]
repoglance:widgets-add-pinned |  |  | [1086,889][2003,1006]
- |  | Add Pinned repos widget | [1086,898][2003,996]
```

Then **Exit** (`tap repoglance:sample-sign-in`): `connect-github` and
`explore-sample` show, there is no `Enter this code`, and the same holds
after a force-stop and relaunch. `cleanup` ran. No widget was placed.

## Guard

`SettingsGuardTest.theLauncherPickerUsesTheSameWidgetNames` now also reads
every string literal in `app/src/main/java/**/*.kt`. Any literal containing
"stack" fails unless it is tag- or key-shaped (`stack-header`). The test
asserts that the scan sees `Pinned repos widget`, so it cannot pass
vacuously.

- Red: with `HomeScreen.kt` from `fb4afac`, `:app:testDebugUnitTest` failed
  with exactly `expected:<[]> but was:<[(HomeScreen.kt, Pin stack widget)]>`.
- Green: `./gradlew assembleDebug check` exit 0: 343 debug and 337 release
  unit tests, lint, `checkFeatureMap`, `checkVerifyHelper`.

## Artifacts (ignored `runs/verify-repoglance-runs/`, not committed)

| File | SHA-256 |
|---|---|
| `fixture-home-show-20261001/fixture-home.png` (before) | `372a3d5da193efa2cdad77821bc5c44434af688cf0424a7fe76f7c46be14185e` |
| `fixture-home-show-20261001/plain-intent-home.txt` (before) | `246c0deef24c66424c24de2476a0fa3d32ac562a48cfd9c57d9722a1098ffa5a` |
| `fixture-home-after-20261001/fixture-home.txt` | `08902eac797fef7f7e1f289b3cca77a51d92bf1ab03f4a485702136d011fb567` |
| `fixture-home-after-20261001/fixture-home.xml` | `d11518038c5aeeff44d0a9a0ceb04f6a49ef8b9527104263f0a7e2b77e5dda50` |
| `fixture-home-after-20261001/fixture-home.png` | `98fd3eb4a8725396e5af2208e2a87b3d28f9d3f2ccae73df9ecbfcff13b6795e` |
| `fixture-home-after-20261001/widgets.txt` | `95ad077e94b7ed7ac2978690451e63dab99477d57f0f36c7398bf2c75e71fe85` |
| `fixture-home-after-20261001/widgets.png` | `b6b15e07924f16bd22e3f4507b3a39ba4d79a0fd69a1ed6ed88ea268b09b6594` |
| `fixture-home-after-20261001/sample-exit.txt` | `1c178174fbb89625f822f30afc6e1845d6ed588826f65c787594fb14c0a5ec80` |

Not covered: a physical Pixel. The Pixel 10 Pro XL was showing another
session's picker build, so it was left alone. The change only removes
Compose code from a debug-reached screen, and nothing about it depends on
the device.
