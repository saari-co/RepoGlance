# Hard CI floor — GrillTrack `ci-floor-014` (2026-09-18)

Grill A of the poteto-style verification substrate plan
([saari-co/x-api#693](https://github.com/saari-co/x-api/pull/693)). Decision
`ci-floor-014` on track `gt-20260728163459-227573`.

## Locks (confirmed by the maintainer 2026-09-18)

- Android Lint as a gate: `abortOnError`, `warningsAsErrors`, existing
  warnings frozen in `app/lint-baseline.xml`, `ContentDescription` at error.
- detekt 1.23.8 + Compose rules 0.4.22 + detekt-formatting as the single
  Kotlin analyser, with type resolution (`detektDebug`).
- Kotlin `allWarningsAsErrors`.
- Full comment ban in `app/src/main`; rationale migrated to
  `docs/INVARIANTS.md`.
- `android.util.Log` and `println` banned outside one designated logger file;
  `java.util.ArrayList`/`HashMap` imports banned.
- StrictMode in debug builds: network on main kills, disk on main logs.
- CI runs `./gradlew check assembleDebug`.

## Baseline

`main` at `f40a916b46acad51194064fea9098629710aedda` (after
[saari-co/RepoGlance#12](https://github.com/saari-co/RepoGlance/pull/12)):
CI ran `testDebugUnitTest assembleDebug` only; lint by hand (0 errors,
30 warnings); no static analysis; 115 comment blocks across 38 source files.

## What changed

- `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`:
  detekt plugin, `lint {}` block, `allWarningsAsErrors`, `detektDebug` wired
  into `check`, plain `detekt` task disabled.
- `config/detekt/detekt.yml`: rule configuration (see `docs/INVARIANTS.md`
  for every decision and its reason).
- `app/lint-baseline.xml`: 3 frozen entries (`OldTargetApi`,
  `ObsoleteSdkInt`, `MissingApplicationIcon`). History: the first generation
  held 33; the `mutableIntStateOf` fix retired three `AutoboxingStateCreation`
  entries; then the first CI run failed with 27 un-baselined
  `GradleDependency` / `AndroidGradlePluginVersion` errors because those
  checks report whatever newer versions the network shows at run time, so
  their messages differ between a laptop and a CI runner. They are disabled
  in the gate (`disable += GradleDependency, AndroidGradlePluginVersion,
  NewerVersionAvailable`) and belong to the gardener (plan grill G). The
  baseline was regenerated and shrank from 30 to 3.
- `app/detekt-baseline-debug.xml`: 20 frozen entries, complexity and size
  only (`ModifierMissing` 6, `LongParameterList` 4, `CyclomaticComplexMethod`
  3, `LongMethod` 2, `ComplexCondition` 1, `NestedBlockDepth` 1,
  `ReturnCount` 1, `ThrowsCount` 1, `TooManyFunctions` 1). Formatting and
  every other category was fixed, not frozen.
- `RepoGlanceApplication` + `DebugHooks` (debug: StrictMode; release: no-op);
  manifest points at the Application.
- `.github/workflows/ci.yml`: `check assembleDebug`, reports uploaded on
  failure, `claude/**` branches added to push triggers.
- Comment ban applied: 115 comment blocks removed from 38 files; verbatim
  text preserved under "Migrated rationale" in `docs/INVARIANTS.md`.
- Hand fixes surfaced by the new rules: three deprecated `menuAnchor()` calls
  (Kotlin warnings as errors), `mutableIntStateOf` for three `Int` states and
  their `intValue` assignments, `error()` for two `IllegalStateException`
  throws, `requireNotNull` x2, `orEmpty()`, an unused `now` parameter in
  fixtures and four in the debug picker, a redundant rethrow of
  `CancellationException`, `Ages.format` rewritten as a `when`, braces on
  five multi-line `if`/`else`, six over-length lines wrapped, and the
  `DisposableEffect` in `AppPrefs` keyed on its lambda. Formatting was applied
  by detekt's autocorrect; three passes converged with no further change.

## Deterministic verification on this tree

- `./gradlew check assembleDebug`: exit 0 (green), twice, with the probe
  files removed between runs.
- 188 JVM tests across 25 suites; 0 failures, 0 errors, 0 skips (unchanged
  from `main`: this slice changes no behaviour).
- Lint outside the baseline: 0 errors, 0 warnings, 2 informational (after
  the dependency-age checks were disabled; see above).
- detekt outside the baseline: 0.
- Kotlin compiler: 0 warnings.
- Debug APK SHA-256
  `01a8b38b9a491580f3620c41644cc59cb0f7e3a212b4410de0e9884dbc206ffe`.

## Deliberate-violation probes

Each probe writes one violating file, runs only the task that owns the rule,
records the exit code and the rule that fired, deletes the file, and the
tree is proven green again afterwards. Local runs; CI executes the same
`check` task. See the table appended below.

## Honest boundaries

- `ContentDescription` at error only covers View/XML. A Compose
  `contentDescription = null` was probed and lint did not flag it; the
  Compose guard is the accessibility check in the Grill C emulator tier.
  Recorded as partial in `docs/INVARIANTS.md`.
- A Kotlin unused-local-variable probe did **not** fail the build under K2;
  the deprecation probe did. `allWarningsAsErrors` is proven for the warnings
  the compiler actually emits.
- `InjectDispatcher` is disabled by configuration (no DI framework), and
  `MagicNumber` is off. `FunctionNaming`, `LongMethod`, `LongParameterList`,
  and `TooManyFunctions` ignore `@Composable` functions by configuration.
  Each is a documented decision, not an accident.
- The 20 baselined detekt findings and 3 baselined lint warnings are ratchet
  debt: the files only shrink, and burning them down is later bounded work.
- The single permitted logger file does not exist yet; RepoGlance logs
  nothing today. The exclusion pre-blesses its path.
- The red probes ran locally, not as CI runs. CI proof for this head is the
  green `check` on the pull request; a CI-red proof would require pushing
  violating commits, which was not done.
- Device proof for the runtime hook is in the section below. The slice still
  changes no rendering, storage, or network behaviour; the StrictMode catches
  it recorded are findings for later bounded work, not fixes in this PR.

## Probe table (local, tree green before and after)

| Probe | Task | Exit | Rule that fired |
| --- | --- | --- | --- |
| `// a forbidden comment` | `detektDebug` | 1 | `ForbiddenComment` (config reason text shown) |
| `/** kdoc */` | `detektDebug` | 1 | `ForbiddenComment` |
| `import android.util.Log` + `Log.d` | `detektDebug` | 1 | `ForbiddenImport` with the redaction reason |
| `println("m")` | `detektDebug` | 1 | `ForbiddenMethodCall` (`kotlin.io.println`) |
| `import java.util.ArrayList` | `detektDebug` | 1 | `ForbiddenImport` with the immutable-collections reason |
| 2-space indent, no spaces around `:`/`=` | `detektDebug` | 1 | `Indentation` (formatting rule set) |
| emitting `@Composable` without `modifier` | `detektDebug` | 1 | `ModifierMissing` (Compose rule set) |
| 7-parameter function | `detektDebug` | 1 | `LongParameterList`, not shielded by the baseline |
| call to a `@Deprecated` function | `compileDebugKotlin` | 1 | `-Werror`: warnings found |
| layout with `android:text="Probe hardcoded"` | `lintDebug` | 1 | lint: 2 errors (`HardcodedText` promoted by `warningsAsErrors`; not in baseline) |
| Compose `Image(contentDescription = null)` | `lintDebug` | 0 | **not caught** — recorded as the Compose gap |
| unused local `val` | `compileDebugKotlin` | 0 | **not caught** under K2 — recorded |

After the last probe: `./gradlew check` exit 0; `Probe.kt` and
`probe_layout.xml` absent; `git status` shows no tracked file altered by the
probes.

## CI finding and repair (2026-09-18)

First CI run on `e3a755d` (GitHub runs 35307732058 / 35307729075) failed in
`lintDebug`: 27 errors, all `GradleDependency` / `AndroidGradlePluginVersion`,
because the runner's network view of "newer version available" differs from
the laptop's and lint baselines match on message text. Repair: those checks
are disabled in the gate and the baseline regenerated (3 entries). Local
`check assembleDebug` green again. This is the floor catching its own
non-determinism, which is the kind of finding the gate exists to surface.

## Device proof: Application hook and StrictMode (2026-09-18)

ClawSweeper on `50158a0` asked for real-behaviour evidence that the new
`RepoGlanceApplication` / `DebugHooks` path runs. Pixel 11 Pro Fold, inner
display `4619827677550801152`; debug APK SHA-256
`01a8b38b9a491580f3620c41644cc59cb0f7e3a212b4410de0e9884dbc206ffe` (the
build from this tree) installed with `adb install -r`, process force-stopped
before each launch so the hook ran on a cold start.

Redacted logcat excerpts (only `ActivityManager` process-start lines and
`StrictMode` lines whose frames are in `co.saari.repoglance`; no token,
header, URL, or private content is present in the filter):

1. Debug picker launch (`WidgetVariantPickerActivity`), captured after start:
   `Start proc … co.saari.repoglance`, then
   `StrictMode policy violation … DiskReadViolation` at
   `LiveSnapshotStore.prefs(LiveSnapshotStore.kt:19)` ←
   `LiveSnapshotStore.load(…:26)` ← the picker's PERSISTED candidate
   (`WidgetVariantPickerActivity.kt:87`). The hook is installed and logging;
   the violation is in debug-only tooling that reads the store synchronously
   in composition.
2. Production cold start (`MainActivity`): six `DiskReadViolation`s, all on
   the main thread, from `SecureTokenStore.<init>` (`SecureTokenStore.kt:26`)
   and `SecureTokenStore.read` (`:29`, `:31`) via
   `GitHubSession.hasSavedSession` ← `RepoGlanceViewModel.bootstrapSessionState`
   (`RepoGlanceViewModel.kt:246`) ← `RepoGlanceViewModel.<init>` ←
   `MainActivity.onCreate(MainActivity.kt:64)`. No network violation, so the
   process was not killed; the app reached the picker and the main screen
   normally.

Files (held locally, not committed):

- `runs/ci-floor-proof-20260918-runs/launch-logcat-redacted.txt`
- `runs/ci-floor-proof-20260918-runs/mainactivity-logcat-redacted.txt`
- `runs/ci-floor-proof-20260918-runs/picker-after-hook.png`, SHA-256
  `9ffc8688847d4071e5203c91ac44c8cba590cd71b810a1cb056740a9d7fa8cf0`,
  inspected: the picker renders all four candidate rows after the hook, no
  crash, no black or clipped region.

What this proves: the manifest points at the new Application, `DebugHooks`
installed the thread policy before the first Activity, and the disk penalty
is `penaltyLog` as locked (the app keeps running). What it found: the
session-file read at startup happens on the main thread. That is the
"no disk or network I/O on the main thread" invariant catching a pre-existing
behaviour on the guard's first run. It is recorded in `docs/INVARIANTS.md`
as open ratchet debt; moving session bootstrap off the main thread is a
behaviour change and belongs to its own bounded slice, not this CI-floor PR.
