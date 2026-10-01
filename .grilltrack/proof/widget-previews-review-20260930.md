# widget-previews-048/049 — exact-source review (2026-09-30)

## Round 1 — `git:54b95f6` (implementation commit)

Reviewer: an independent general-purpose agent, read-only (no file edits, no
device), over `git diff fb4afac 54b95f6`, on two axes: repository standards
(AGENTS.md, INVARIANTS.md) and the confirmed lock. It disassembled Glance
1.2.0 from the Gradle cache and read the platform `AppWidgetManager` source.
No P0 or P1.

| # | finding | severity | class | action |
| --- | --- | --- | --- | --- |
| 1 | `WidgetPinning.request` now runs in a coroutine; `requestPinAppWidget` throws `IllegalStateException` when the app is no longer in the foreground, and a Glance composition error would also escape an unhandled `rememberCoroutineScope` launch and kill the process | P2 | required_fix | catch, rethrow cancellation, return false; guard test |
| 2 | `publishIfNeeded` runs from `Application.onCreate` on every process start with no handler; a repeating throw (system server, provider race, composition) would crash every start, stopping widget redraws and background refresh | P2 | required_fix | per-receiver `runCatching` (rethrow cancellation), no stamp on failure |
| 3 | `WidgetPreviewTest` renders at the 349×455 dp default, where the compact preview gains a `to review` row; the sheet and picker compose at the minimum size (140×40, 250×180) | P3 | required_fix | set the minimum sizes, assert no `to review` |
| 4 | the "reads no store" guard scans only `WidgetPreview.kt`, not the shared composables the preview draws; the rate-limit retry claim is a string check | P3 | required_fix | scan the composables' bodies; pure `isDue` decision with unit tests |
| 5 | dev builds use versionCode 1, so a changed preview keeps stamp `1.1` unless `LOOK_VERSION` moves | P3 | required_fix | document "bump LOOK_VERSION with the preview" in INVARIANTS and the feature file |
| 6 | Glance 1.2.0 raises Compose runtime to 1.7.8 and adds core-remoteviews 1.1.0; the tall Repository widget on 1.2.0 was not resized on a device | P3 | required_fix | record the dependency diff in proof; resize a tall widget on the emulator |
| 7 | README and CHANGELOG say Android 12–14 uses the static copy; that path was only seen on API 36 | P3 | required_fix | say it was not yet seen on Android 12–14 |
| — | `ui/HomeScreen.kt` fixture home still labels a button "Pin stack widget" (pre-existing, adjacent lines) | — | defer | outside this slice; follow-up task |

Checked clean by the reviewer: comment/log bans, debug-only picker, no
"stack" in new strings, no clock or user data in any preview path, publish
gating (API guard, stamp only on success, home-screen only, no loop),
reflection-based receiver construction in `setWidgetPreviews`,
`requestPinGlanceAppWidget` internals, `WidgetPinning` support check and
setup callback, fallback colours equal to Glance 1.2.0's `glance_color*`
resources.

## Round 2 — `git:8e8cdcb` (round 1 fixes)

Same reviewer, read-only. Nothing new above P3. Round 1: findings 1, 2, 3, 5
and 7 resolved in `8e8cdcb`; 6 resolved by the re-verification proof; 4
partially resolved.

| # | finding | severity | class | action |
| --- | --- | --- | --- | --- |
| 4b | the store guard misses helpers the preview runs (`TallLook`, `labelFamily`, `freshnessRole`, `sampleMarker`, `StackRows`, `SampleAccount`); the "every system call is caught" check only counts two `attempt {` | P3 | required_fix | scan `WidgetLook.kt`, `SampleWidgetMarks.kt`, `SampleWidgetData.kt`, `sample/SampleAccount.kt` whole, add `object StackRows {`, reword the count check |
| N1a | a failing sheet preview or lost foreground now makes `Add` silently do nothing | P3 | required_fix | on a non-cancellation failure, retry the plain `requestPinAppWidget` (the launcher then shows the static sample copy), itself caught |
| N1b | the debug picker reports any failure as "refused: launcher cannot pin" | P3 | defer | debug-only wording; release behaviour unaffected |
| N2 | `kotlinx.coroutines.CancellationException` vs the repo's `kotlin.coroutines.cancellation.CancellationException` | nit | required_fix | use the repo's import |

Checked clean by the reviewer: catch ordering (cancellation before
`Exception`), foreground-loss `IllegalStateException` caught, `attempt {}`
inside inline `filter`/`forEach`, `setAppWidgetSize` semantics in
glance-appwidget-testing 1.2.0 (the `to review` assertion is not vacuous),
string-anchored extraction fails loudly on a missing anchor, pin-support
check first, setup callback flags, single pin caller, home-screen-only
stamped publish.

## Round 3 — `git:d3f6a76` (round 2 fixes)

Same reviewer, read-only. "Round 3 clean": nothing new above P3. 4b, N1a and
N2 resolved; N1b deferred as stated. The fallback cannot open a second
sheet (Glance's system pin call is its last step; a throw there means no
sheet), foreground loss is caught twice without a crash, the setup
`PendingIntent` is the same on both paths, and the guards are not vacuous.

| # | finding | severity | class | action |
| --- | --- | --- | --- | --- |
| R3-1 | design.md and INVARIANTS describe only the Glance sheet path, not the preview-less fallback | P3 | required_fix | name the fallback (launcher's own preview, same sample) |
| R3-2 | the proof says the fallback "was not triggered on a device", which `request` cannot show (both paths return true and look alike) | P3 | required_fix | reword: not distinguishable on the device |
| R3-3 | the fallback's `runCatching` also swallows `Error`s | nit | required_fix | `try { … } catch (_: Exception) { false }` |

## Round 4 — `git:a36c46b` (round 3 fixes)

Same reviewer, read-only. "Round 4 clean": no findings at any severity.
R3-1 (docs name the fallback), R3-2 (proof says the path is
indistinguishable on the device) and R3-3 (`withoutPreview` catches
`Exception` only) resolved; the pin-support check is still first,
`WidgetPinning.kt` is still the single pin caller, and the new guard fails
loudly if `withoutPreview` is renamed.

Deferred across rounds: N1b (debug picker labels any failure "refused:
launcher cannot pin") and the fixture home's pre-existing "Pin stack widget"
label (follow-up task).
