# Fixture route in debug builds only — proof (2026-10-01)

Claim: the fixture home and fixture navigator no longer ship in release
builds. A release build ignores the `repo_full` extra and opens the normal
live entry. Debug builds keep the route, and
`bin/verify-repoglance launch MIXED navigator acme/rocket BOTH` still works.

Maintainer decision (2026-10-01, on #55): the fixture home is a test screen,
not a user surface, and it "shouldn't be in the main app". Don't repurpose
it.

- Base: `origin/main` `fb4afac`. Source: `a976283` on
  `claude/fixture-route-debug-only`.
- Device: Android emulator `EMULATOR37X1X11X0` (devices.tsv; AVD
  Pixel_10_Pro_Fold, API 36, inner display), signed out. It was idle on the
  launcher before the run. The Pixel 10 Pro XL was also attached and was not
  touched (`VERIFY_SERIAL` chose the emulator).
- Run directory (ignored, not committed):
  `runs/verify-repoglance-runs/fixture-route-debug-only-20261001/`

## What changed

- `ui/HomeScreen.kt` and `ui/NavigatorScreen.kt` moved from `app/src/main`
  to `app/src/debug`. They are pure renames in the same package, so the
  debug pickers did not change.
- `FixtureRoot` and the route state (scope, mode, route token) left
  `MainActivity` for `hooks/FixtureRoute`. The debug flavour holds them.
  The release flavour is a no-op: `isOpen` is `false`, and `onCreate`,
  `onNewIntent`, `close` and `Content` do nothing.
- `MainActivity` keeps one field and four calls:
  `fixtureRoute.onCreate(intent)`, `fixtureRoute.onNewIntent(intent)`,
  `fixtureRoute.close()` (two live-intent branches), and
  `if (fixtureRoute.isOpen) fixtureRoute.Content() else LiveRoot()`.
- `EXTRA_REPO_FULL` and `EXTRA_NAVIGATOR_MODE` moved from
  `widget/WidgetActions.kt` to the debug hook. `navigatorModeFromExtra`
  stays in main because `RepoWidgetConfigStore` uses it.
- `NavigatorChromeTest` moved to `app/src/testDebug`.

## Guards

| Guard | Variant | What fails it |
| --- | --- | --- |
| `FixtureRouteGuardTest.releaseSourcesNeitherReadTheFixtureExtrasNorNameTheFixtureScreens` | both | any `.kt` under `app/src/main/java` or `app/src/release/java` matching `repo_full` (not `live_repo_full`), `navigator_mode`, `EXTRA_REPO_FULL`, `EXTRA_NAVIGATOR_MODE`, `HomeScreen`, `NavigatorScreen` or `FixtureRoot` |
| `FixtureRouteGuardTest.theFixtureRouteAndItsScreensLiveInTheDebugSourceSet` | both | the debug hook or the two debug screens go missing |
| `FixtureRouteGuardTest.theReleaseRouteIsANoOpThatNeverReadsAnIntent` | both | the release hook gains an import, reads an extra, or any of its four functions stops being `= Unit` |
| `FixtureRouteReleaseTest` | release | `HomeScreenKt`, `NavigatorScreenKt` or `FixtureRouteKt` loads on the release unit-test classpath |
| `FixtureRouteDebugTest` | debug | any of those three does not load in debug, which would leave the release test checking stale names |

Red, on base `fb4afac` with only the new tests added:

```text
FixtureRouteReleaseTest: expected:<[]> but was:<[co.saari.repoglance.ui.HomeScreenKt, co.saari.repoglance.ui.NavigatorScreenKt]>
```

The scan test, with an empty stub release `FixtureRoute` so its
preconditions pass:

```text
the fixture route is debug-only expected:<[]> but was:<[
  app/src/main/java/co/saari/repoglance/ui/HomeScreen.kt: \bHomeScreen\b,
  app/src/main/java/co/saari/repoglance/ui/NavigatorScreen.kt: \bNavigatorScreen\b,
  app/src/main/java/co/saari/repoglance/MainActivity.kt: \bEXTRA_REPO_FULL\b,
  app/src/main/java/co/saari/repoglance/MainActivity.kt: \bEXTRA_NAVIGATOR_MODE\b,
  app/src/main/java/co/saari/repoglance/MainActivity.kt: \bHomeScreen\b,
  app/src/main/java/co/saari/repoglance/MainActivity.kt: \bNavigatorScreen\b,
  app/src/main/java/co/saari/repoglance/MainActivity.kt: \bFixtureRoot\b,
  app/src/main/java/co/saari/repoglance/widget/WidgetActions.kt: (?<!live_)repo_full,
  app/src/main/java/co/saari/repoglance/widget/WidgetActions.kt: navigator_mode,
  app/src/main/java/co/saari/repoglance/widget/WidgetActions.kt: \bEXTRA_REPO_FULL\b,
  app/src/main/java/co/saari/repoglance/widget/WidgetActions.kt: \bEXTRA_NAVIGATOR_MODE\b]>
```

The other two base tests failed on missing files, as expected.

Green: `./gradlew --no-daemon assembleDebug check assembleRelease` exited 0.
That covers 347 debug unit tests and 337 release unit tests (0 failures),
`detektDebug`, `lintDebug`, `checkFeatureMap` and `checkVerifyHelper`.

## The release APK has no fixture route

`dexdump` over every `classes*.dex` in the two APKs from that gate run:

```text
app-debug sha256=67719f37aefcc69d592e284df16bde3032b20a1e39e94c9e7758131ef01a14b4
  co/saari/repoglance/ui/HomeScreenKt* classes: 29
  co/saari/repoglance/ui/NavigatorScreenKt* classes: 106
  co/saari/repoglance/hooks/FixtureRouteKt* classes: 12
  co/saari/repoglance/hooks/FixtureRoute$* classes: 3
  co/saari/repoglance/devlaunch/ScenarioLaunchActivity$* classes: 4
  dex strings:  2 live_repo_full; 3 repo_full;
app-release-unsigned sha256=49141f69bb42b6dfb8ca07497e018f7c33858990291f67b6f355e93f4abc493b
  co/saari/repoglance/ui/HomeScreenKt* classes: 0
  co/saari/repoglance/ui/NavigatorScreenKt* classes: 0
  co/saari/repoglance/hooks/FixtureRouteKt* classes: 0
  co/saari/repoglance/hooks/FixtureRoute$* classes: 2
  co/saari/repoglance/devlaunch/ScenarioLaunchActivity$* classes: 0
  dex strings:  1 live_repo_full;
```

The two release `FixtureRoute` classes are the no-op class and one
compiler-generated lambda. The release dex has no standalone `repo_full`
string.

## Debug build: the verify route still works

Doctor passed with the device APK equal to the local debug build
(`67719f37…`). `launch MIXED navigator acme/rocket BOTH` reached
`co.saari.repoglance/.MainActivity` with `repoglance:fixture-home` in the
dump. Then `dump debug-navigator`:

```text
repoglance:fixture-home |  |  | [39,166][246,283]
- | acme/rocket |  | [256,156][1038,293]
repoglance:fixture-mode-ISSUES |  |  | [39,441][372,558]
repoglance:fixture-mode-PRS |  |  | [372,441][704,558]
repoglance:fixture-mode-BOTH |  |  | [704,441][1038,558]
- | Issues |  | [39,587][1038,636]
```

The capture shows the two-pane form, with the list on the left and
`Select a row to see details` on the right. `tap repoglance:fixture-mode-PRS`,
then `dump debug-prs-only`: `PRs` is present and there is no `Issues`
header. `tap repoglance:fixture-home`, then `dump debug-home`:

```text
repoglance:scenario | MIXED |  | [1623,175][2037,312]
repoglance:fixture-navigator |  |  | [39,1782][2037,1899]
```

The fixture home still shows `Pin repo widget` / `Pin stack widget`. #55
removes them, and it has not merged yet.

On debug, a plain intent still reaches the route:
`am start -n co.saari.repoglance/.MainActivity -a android.intent.action.VIEW --es repo_full acme/rocket`
dumps the same fixture navigator (`debug-plain-intent.txt` hashes equal to
`debug-navigator.txt`).

## Release variant: `repo_full` opens the live entry

Local release builds are unsigned (no keystore here). For this device check
only, the same release variant was packaged with Gradle's standard debug
signing config through a scratch init script. The repository was not
changed, and no keystore was handled directly. The signed APK's three
`classes*.dex` files are byte-identical to the gate's
`app-release-unsigned.apk` (`release-signed-dex-vs-unsigned.txt`). Its
signer is `CN=Android Debug`, the same certificate as the debug APK, so
`adb install -r` replaced the debug install and kept the data.

```text
serial=EMULATOR37X1X11X0
Success
device_apk_sha256=35cd3c6137d628cc6aea11d8bbc3b7aeaa15d5ee92e63998c3c8bcb352649871
pkgFlags: pkgFlags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ]
resolve ScenarioLaunchActivity:
No activity found
```

The build is not debuggable, and the debug scenario launcher does not
exist. Next, a cold start with the plain intent:

```text
adb shell am force-stop co.saari.repoglance
adb shell am start -W -n co.saari.repoglance/.MainActivity -a android.intent.action.VIEW --es repo_full acme/rocket
LaunchState: COLD
topResumedActivity=ActivityRecord{… u0 co.saari.repoglance/.MainActivity t246}
fixture handles: 0
repoglance:menu |  |  | [1901,196][2018,313]
repoglance:connect-github |  |  | [840,1131][1238,1248]
repoglance:explore-sample |  |  | [770,1268][1308,1385]
```

A warm start then sent `--es repo_full acme/rocket --es navigator_mode PRS`
to the running activity (`onNewIntent`). The result was the same: 0
`repoglance:fixture-*` handles, with the Connect screen and its menu. The
capture shows the normal signed-out Connect screen (`Connect GitHub`,
`Explore with sample data`). The crash buffer held 0 lines for
`co.saari.repoglance`.

## Restore

`launch MIXED live` reinstalled the debug build. `doctor` passed again
(`apk_device=67719f37…`), `cleanup` ran, and the emulator was left on the
launcher. No widget was placed, nothing signed in, and no GitHub call was
made.

## Artifacts (ignored `runs/verify-repoglance-runs/fixture-route-debug-only-20261001/`, not committed)

| File | SHA-256 |
| --- | --- |
| `debug-navigator.txt` | `b29b882c6ba6d2b0ca567112b4d1aca7fa1ee613d3674071da69ebb164e7a2f9` |
| `debug-navigator.png` | `2b792aaf7028219381b4393551894b9523a5a6c25bba5e172b2cde8325be6cc3` |
| `debug-prs-only.txt` | `68202b64b5953088a26ee7ac332dd00b6d12230012dd96d4c8aa4e6d76590419` |
| `debug-home.txt` | `246c0deef24c66424c24de2476a0fa3d32ac562a48cfd9c57d9722a1098ffa5a` |
| `debug-home.png` | `73271356e3f7e462347f414818a452fda41aaaec31a3de15b311ffd9ed639c5b` |
| `debug-plain-intent.txt` | `b29b882c6ba6d2b0ca567112b4d1aca7fa1ee613d3674071da69ebb164e7a2f9` |
| `release-install.txt` | `01211ee989190a92ac35f197bb6d8d15a7f3e7a7c67a9b2be60a16319d7ffec8` |
| `release-plain-intent-cold.txt` | `1c178174fbb89625f822f30afc6e1845d6ed588826f65c787594fb14c0a5ec80` |
| `release-plain-intent-warm.txt` | `1c178174fbb89625f822f30afc6e1845d6ed588826f65c787594fb14c0a5ec80` |
| `release-plain-intent.png` | `5aaedbf36fe917a364d54245663ed23ef1d788df9715863fa7576a1579c62a8c` |
| `apk-dex-classes.txt` | `7bee0a129d0e52b2b26f67310e1004577e497218ca324d923da9932c3ecb5a53` |
| `release-signed-dex-vs-unsigned.txt` | `ab036f1bb5119188f9ebcdbf39e72b0b4725b15b0c4e5740b2ad9d2538950610` |
| `red-base-release-test.log` | `5dfb1fa0c6dca2cf02c6665471860f7bfcc9b9f508f369aa4c4314320cc486e3` |
| `red-base-scan.log` | `ccefb5ec58420e42fc9343aa89801ae56c82b12f579641b286f34a016e898c95` |
| `gradle-check.log` | `152d4a79b5109fc18d89613bf9030b561ac3cfec2f08d72575888b0a848e1fc3` |
| `unit-tests.txt` | `c4a32e7e8580f11fe38252edd8dcf31389545bbfcf4a594b5b6ce7feae71159f` |

The signed-out Connect dump hashes the same as #55's `sample-exit.txt`
(`1c178174…`). The debug fixture-home dump hashes the same as #55's "before"
`plain-intent-home.txt` (`246c0dee…`). Those dumps are deterministic for
the same screen.

## Not covered and left for later

- No physical Pixel was used. The change moves code between source sets,
  and nothing in it depends on the device. Release signing in CI
  (`release.yml`) was not exercised: it builds the same release variant
  with the real keystore.
- Main-source helpers now used only from debug, and inert in release with
  no entry point: `state/NavigatorScopeCodec`,
  `AppPrefs.rememberScenario` / `rememberPinnedRepos` /
  `setSelectedScenario`, `render/CiSemanticRole`, `ui/theme/StatusPill`.
  The fixture data (`fixtures/`, `SnapshotStore`) is still read in main by
  `widget/WidgetFixtureData.kt`, so it stays. Moving the helpers is a
  separate follow-up.
- Overlaps: #55 deletes the pin row and #54 edits it in `HomeScreen.kt`.
  Here that file is a pure rename, so git carries either edit across. #54
  also edits the `ScenarioLaunchActivity` imports beside the ones this
  change touches, and whichever merges second may need a one-line import
  merge. `.grilltrack/maps/sample-mode.md` says "A release build has no way
  into fixture mode". That was not true on `fb4afac`, and it is true after
  this change. The line references are stale, and the GrillTrack files were
  left untouched.
