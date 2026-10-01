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
