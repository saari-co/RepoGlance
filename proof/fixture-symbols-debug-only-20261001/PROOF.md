# Fixture helpers in debug builds only — proof (2026-10-01)

Claim: the helpers that only the debug fixture route uses no longer ship in
release builds. The release APK has no `NavigatorScopeCodec`, no
`CiSemanticRole`, no `StatusPill`, no fixture-scenario or fixture-pin
`AppPrefs` member and no `selected_scenario` / `pinned_repos` key. Debug
builds keep them with the same behaviour, prefs file and keys.

Follow-up to #56 ("Fixture route: debug builds only"). #56's proof listed
these helpers under "Not covered and left for later".

- Base: `origin/main` `fc2ac58` (the #56 merge). Source: `20f8473` on
  `claude/fixture-symbols-debug-only`.
- No device run. Code moved between source sets and every user-visible
  path stays the same (see "Not covered").
- Run directory (ignored, not committed):
  `runs/fixture-symbols-debug-only-runs/`

## Re-verified before moving

`git grep -n -w <symbol> -- app/src/main` on `fc2ac58`. Each symbol hit
only its own definition:

```text
NavigatorScopeCodec   app/src/main/.../state/NavigatorScopeCodec.kt:6   object NavigatorScopeCodec {
rememberScenario      app/src/main/.../state/AppPrefs.kt:86             fun rememberScenario(...)
rememberPinnedRepos   app/src/main/.../state/AppPrefs.kt:90             fun rememberPinnedRepos(...)
setSelectedScenario   app/src/main/.../state/AppPrefs.kt:33             fun setSelectedScenario(...)
CiSemanticRole        app/src/main/.../render/CiSemanticRole.kt:7       object CiSemanticRole {
StatusPill            app/src/main/.../ui/theme/ControlShape.kt:118     fun StatusPill(...)
```

The same check found three findings the brief did not list:

- `CiColorRole`, which shares `CiSemanticRole.kt`, is used by release code
  (`SnapshotRendering.rateLimitRole`, `StatusColors.tone`, `StackWidget`,
  and `WidgetLookTest` in the shared tests). It **stays in main**, split
  out into `render/CiColorRole.kt`. Only the `CiSemanticRole` mapping
  moved. Main never colours CI: it only labels it (`ciLabel`), and live
  snapshots carry `CiState.UNKNOWN`.
- `AppPrefs.togglePin` is called only from debug `hooks/FixtureRoute.kt`.
  `SampleModeStore.togglePin` is a different symbol and stays.
- `AppPrefs.selectedScenario` and `AppPrefs.pinnedRepos` were read only by
  `rememberScenario`, `rememberPinnedRepos` and `togglePin`. Moving those
  three would leave them dead in release, so they moved too, with their
  keys `KEY_SCENARIO` and `KEY_PINNED`.

Fixture data (`fixtures/`, `state/SnapshotStore.kt`) stays: main still
reads it through `widget/WidgetFixtureData.kt`.

## What changed

| From `app/src/main` | To `app/src/debug` (same package) | Notes |
| --- | --- | --- |
| `state/NavigatorScopeCodec.kt` | `state/NavigatorScopeCodec.kt` | pure rename |
| `render/CiSemanticRole.kt` | `render/CiSemanticRole.kt` | `CiColorRole` split out to main `render/CiColorRole.kt` |
| `StatusPill` in `ui/theme/ControlShape.kt` | `ui/theme/StatusPill.kt` | `edgeStroke` goes from `private` to `internal` |
| six `AppPrefs` members and two keys | `state/FixturePrefs.kt` | now `AppPrefs.*` extensions, so the call sites stay `AppPrefs.x(...)`; `prefs` and `rememberPrefsState` go from `private` to `internal` |

- The debug callers gained extension imports: `hooks/FixtureRoute.kt`,
  `devpicker/NavigatorVariantPickerActivity.kt` and
  `devlaunch/ScenarioLaunchActivity.kt`.
  `ScenarioLaunchActivitySourceTest` still finds
  `AppPrefs.setSelectedScenario(` inside the IO block.
- `CiSemanticRoleTest` moved from `app/src/test` to `app/src/testDebug`.
- The extensions use the same prefs file (`repoglance`) and the same keys
  (`selected_scenario`, `pinned_repos`), so an existing debug install keeps
  its scenario and pins. No comments were added in `app/src/main`, and
  `detektDebug` enforces that.
- Docs: in `docs/INVARIANTS.md`, the fixture-route guard row now covers
  the helpers, and the migrated-rationale entries for `CiSemanticRole.kt`,
  `AppPrefs.kt` and `NavigatorScopeCodec.kt` record the move. In
  `docs/design.md`, `StatusPill` and `CiSemanticRole` are marked
  debug-only.

## Guards

| Guard | Variant | What fails it |
| --- | --- | --- |
| `FixtureRouteGuardTest.releaseSourcesNameNoFixtureOnlyHelper` (new) | both | any `.kt` under `app/src/main/java` or `app/src/release/java` matching `NavigatorScopeCodec`, `CiSemanticRole`, `StatusPill`, `selectedScenario` / `setSelectedScenario` / `rememberScenario` / `rememberPinnedRepos`, `AppPrefs.pinnedRepos` / `AppPrefs.togglePin`, or the string literals `"selected_scenario"` / `"pinned_repos"` (`live_pinned_repos` is allowed) |
| `FixtureRouteGuardTest.theFixtureHelpersLiveInTheDebugSourceSet` (new) | both | any of the four debug helper files goes missing |
| `FixtureRouteReleaseTest.releaseClassesCarryNoFixtureHomeNavigatorRouteOrHelpers` (extended `DEBUG_ONLY_CLASSES`) | release | `NavigatorScopeCodec`, `FixturePrefsKt`, `CiSemanticRole` or `StatusPillKt` loads on the release unit-test classpath (along with #56's three classes) |
| `FixtureRouteReleaseTest.releaseClassesDeclareNoFixtureOnlyMember` (new) | release | `AppPrefs` declares any of the six fixture-prefs names, or `ControlShapeKt` declares `StatusPill`. This catches a member re-added to main, which would shadow the debug extension and still compile |
| `FixtureRouteDebugTest.debugClassesCarryTheFixtureHomeNavigatorRouteAndHelpers` (extended) | debug | any of the seven classes does not load in debug, which would leave the release test checking stale names |
| `FixtureRouteDebugTest.debugClassesDeclareTheFixtureOnlyMembers` (new) | debug | `FixturePrefsKt` or `StatusPillKt` stops declaring the moved names |

### Red: base `fc2ac58` with only the new tests added

`./gradlew --continue testDebugUnitTest testReleaseUnitTest --tests 'co.saari.repoglance.hooks.FixtureRoute*'`
exited 1. In debug, 350 tests ran and 4 failed. In release, the filtered 7
ran and 4 failed. Each failure is one of the new checks:

```text
[Release] FixtureRouteReleaseTest.releaseClassesCarryNoFixtureHomeNavigatorRouteOrHelpers:
  expected:<[]> but was:<[co.saari.repoglance.state.NavigatorScopeCodec, co.saari.repoglance.render.CiSemanticRole]>
[Release] FixtureRouteReleaseTest.releaseClassesDeclareNoFixtureOnlyMember:
  expected:<[]> but was:<[co.saari.repoglance.state.AppPrefs.pinnedRepos,
  co.saari.repoglance.state.AppPrefs.rememberPinnedRepos, co.saari.repoglance.state.AppPrefs.rememberScenario,
  co.saari.repoglance.state.AppPrefs.selectedScenario, co.saari.repoglance.state.AppPrefs.setSelectedScenario,
  co.saari.repoglance.state.AppPrefs.togglePin, co.saari.repoglance.ui.theme.ControlShapeKt.StatusPill]>
[Debug+Release] FixtureRouteGuardTest.releaseSourcesNameNoFixtureOnlyHelper:
  the fixture helpers are debug-only expected:<[]> but was:<[
  app/src/main/java/co/saari/repoglance/ui/theme/ControlShape.kt: \bStatusPill\b,
  app/src/main/java/co/saari/repoglance/render/CiSemanticRole.kt: \bCiSemanticRole\b,
  app/src/main/java/co/saari/repoglance/state/NavigatorScopeCodec.kt: \bNavigatorScopeCodec\b,
  app/src/main/java/co/saari/repoglance/state/AppPrefs.kt: \b(?:selectedScenario|setSelectedScenario|rememberScenario|rememberPinnedRepos)\b,
  app/src/main/java/co/saari/repoglance/state/AppPrefs.kt: "(?:selected_scenario|pinned_repos)"]>
[Debug+Release] FixtureRouteGuardTest.theFixtureHelpersLiveInTheDebugSourceSet: state/NavigatorScopeCodec.kt
[Debug] FixtureRouteDebugTest.debugClassesCarryTheFixtureHomeNavigatorRouteAndHelpers:
  expected:<[…7 classes…]> but was:<[HomeScreenKt, NavigatorScreenKt, FixtureRouteKt, NavigatorScopeCodec, CiSemanticRole]>
[Debug] FixtureRouteDebugTest.debugClassesDeclareTheFixtureOnlyMembers:
  java.lang.ClassNotFoundException: co.saari.repoglance.state.FixturePrefsKt
```

### Green: `20f8473`

`./gradlew --no-daemon assembleDebug check assembleRelease` exited 0. That
covers `detektDebug`, `lintDebug`, `checkFeatureMap`, `checkVerifyHelper`,
`testDebugUnitTest` and `testReleaseUnitTest`:

```text
testDebugUnitTest: 350 tests, 0 failures, 0 errors, 0 skipped
testReleaseUnitTest: 335 tests, 0 failures, 0 errors, 0 skipped
```

The counts add up. On `fc2ac58`, debug ran 347 and release ran 337. This
branch adds 2 shared and 1 debug-only test, so debug is 347 + 3 = 350.
Release gains the 2 shared tests and 1 release-only test, and loses the 5
`CiSemanticRoleTest` cases now in `testDebug`, so it is 337 + 3 − 5 = 335.
Every `FixtureRoute*` test and every `CiSemanticRoleTest` case passed
(`unit-tests.txt`).

## The release APK carries none of them

`dexdump` over every `classes*.dex`, comparing the base release APK (built
on `fc2ac58` sources) with the APKs from the green gate:

```text
base-app-release-unsigned.apk sha256=9452ee9ed3b4d90a5e1b7b72e89a36a54d62aeca4c4635125ba8f1cd1ca99e7b
  state/NavigatorScopeCodec* classes: 1    render/CiSemanticRole* classes: 2
  state/FixturePrefsKt* classes: 0         ui/theme/StatusPillKt* classes: 0
  render/CiColorRole* classes: 1
  AppPrefs fixture-only methods: [pinnedRepos rememberPinnedRepos rememberScenario selectedScenario setSelectedScenario togglePin]
  ControlShapeKt fixture-only methods: [StatusPill]
  dex strings: selected_scenario=1 pinned_repos=1
head-app-release-unsigned.apk sha256=b06ec064cf9412baf44f12cd586f7edffee8c2f26f23af977fa4e36bd69e9b42
  state/NavigatorScopeCodec* classes: 0    render/CiSemanticRole* classes: 0
  state/FixturePrefsKt* classes: 0         ui/theme/StatusPillKt* classes: 0
  render/CiColorRole* classes: 1
  AppPrefs fixture-only methods: []
  ControlShapeKt fixture-only methods: []
  dex strings: selected_scenario=0 pinned_repos=0
head-app-debug.apk sha256=0363768bd834ea4f41556fbb93127073288fa834273b0e723ad2eeedd5719781
  state/NavigatorScopeCodec* classes: 1    render/CiSemanticRole* classes: 2
  state/FixturePrefsKt* classes: 3         ui/theme/StatusPillKt* classes: 2
  render/CiColorRole* classes: 1
  AppPrefs fixture-only methods: []
  ControlShapeKt fixture-only methods: []
  dex strings: selected_scenario=1 pinned_repos=1
```

The head release dex has no class whose name contains `StatusPill`,
`NavigatorScopeCodec`, `CiSemanticRole` or `FixturePrefs`. Release has
`isMinifyEnabled = false`, so before this change all of them shipped in
the release APK.

## After merging main (#54)

`origin/main` moved to `54f296e` (#54, widget previews) while this branch
was open. It merged cleanly as `755de02`, and #54 adds no line naming a
moved symbol. On the merged tree,
`./gradlew --no-daemon assembleDebug check assembleRelease` exited 0:

```text
testDebugUnitTest: 362 tests, 0 failures, 0 errors; FixtureRoute failures: []
testReleaseUnitTest: 347 tests, 0 failures, 0 errors; FixtureRoute failures: []
merged-app-release-unsigned.apk sha256=f6440aee4274dc9f51c315f413b07feb31cea9ac7d8887260badee78a67451b2
  NavigatorScopeCodec* 0, FixturePrefsKt* 0, CiSemanticRole* 0, StatusPillKt* 0, CiColorRole* 1
  AppPrefs fixture-only methods: []   ControlShapeKt fixture-only methods: []
  dex strings: selected_scenario=0 pinned_repos=0
```

#54 adds 12 tests to each variant: 350 + 12 = 362 and 335 + 12 = 347.

## Not covered and left for later

- No device run. The fixture home, fixture navigator and debug pickers
  call the same code with the same prefs file and keys, and no file under
  `app/src/main` that a release screen draws changed visibly: `StatusPill`
  had no release caller. Debug presence is proven on the classpath
  (`FixtureRouteDebugTest`) and in the debug APK dex above.
- `ControlShape.pill`, `pillEdge` and `pillFill` are now read only from
  debug (`StatusPill`, `devpicker/ShapeCandidate`). They are constructor
  fields of the production `ControlShape` data class, and removing them
  changes the seam's API and the shape picker. That is a separate,
  optional cleanup.
- `.grilltrack/maps/live-widgets.md` still says pins exist only on the
  fixture home via `AppPrefs.pinnedRepos`. The member is now a debug
  extension. The GrillTrack files were left untouched, because GrillTrack
  activates only at the maintainer's request.

## Artifacts (ignored `runs/fixture-symbols-debug-only-runs/`, not committed)

| File | SHA-256 |
| --- | --- |
| `red-tests.log` | `199bd818725f9e485dab51fbd487efeec7e0bd0cf7af70ec5f5fb0f48d19876f` |
| `red-failures.txt` | `e403f61798a9998f95be308edbf25e47ecaf688299af8b1e12bbbc3e3fe92bc4` |
| `base-assembleRelease.log` | `bc8311c10c851eb17b4f14b0d4f216f12d9b52f30bbfb25474017f4e557ee55d` |
| `gradle-check.log` | `e0dbfc35f40626d9983bde038635552bfc3129d36eadffab2121cefb42781b1a` |
| `unit-tests.txt` | `6c42e957ffe2118fe740f984d570a3351b75916306c843d389938ab4e9cc4f6d` |
| `apk-dex-scan.txt` | `0b7f1c7b008af876cedf1a5705336eb135be194814482c48f371420f391aaa04` |
| `base-app-release-unsigned.apk` | `9452ee9ed3b4d90a5e1b7b72e89a36a54d62aeca4c4635125ba8f1cd1ca99e7b` |
| `head-app-release-unsigned.apk` | `b06ec064cf9412baf44f12cd586f7edffee8c2f26f23af977fa4e36bd69e9b42` |
| `head-app-debug.apk` | `0363768bd834ea4f41556fbb93127073288fa834273b0e723ad2eeedd5719781` |
| `gradle-check-merged.log` | `ba9c64f442b58a46c7a2ea814bd5f947226ae328c53598110aeffa38f366c028` |
| `unit-tests-merged.txt` | `bf33d2ad02125eb00ab8630c92470ba872777a1d95ceb3b949dc8edac7f8fc5f` |
| `apk-dex-scan-merged.txt` | `da098e788d60da7e8cb1f82bc4435db7e76ba81b40faf64504b977a29357f9b5` |
| `merged-app-release-unsigned.apk` | `f6440aee4274dc9f51c315f413b07feb31cea9ac7d8887260badee78a67451b2` |
