# Widget previews

Before a widget is placed, the launcher shows a preview of it: in the `Add to home screen` sheet that the in-app `Add` opens, and in the launcher's own widget picker. Both previews are the sample widgets, never the app icon and never the user's data. Repository is the compact widget for `saari-co/rocket` (`issues 5`, `PRs 3`) with `sample` in a tinted capsule where a live widget shows its time. Pinned repos is a tinted `Pinned · 3` band over `saari-co/rocket`, `saari-co/api-server` and `dinkuskit/infra`, each row reading `sample` and its counts. No preview shows a clock time. Colours follow the wallpaper and light/dark.

## Sub-features

- `preview-sheet`: the in-app `Add` on the Widgets screen opens the launcher's sheet with the sample preview, signed in, signed out or in sample mode.
- `preview-picker`: the launcher's widget picker (long-press the home screen, `Widgets`, search `RepoGlance`) shows the same two previews under `Repository` and `Pinned repos`.
- `preview-generated`: on Android 15+ the picker preview is generated from the widgets and published when the app starts, once per app version; a rate-limited publish retries on a later start.
- `preview-fallback`: without a generated preview (Android 12–14, before the first publish, or after a rate-limited one) the picker and a sheet opened without a preview show a static copy that looks the same.
- `preview-theme`: both previews follow light and dark and the dynamic colours.

## How to get to it (user POV)

- In RepoGlance, three-dot menu → `Widgets` → `Add` on either tile.
- On the home screen, long-press an empty spot → `Widgets` → search `RepoGlance`, then tap the RepoGlance row.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes. Viewing a sheet or the picker places nothing; tapping `Add to home screen` does, and needs the placement approval in [Settings and Widgets](./settings-widgets.md).
- The debug picker is `bin/verify-repoglance launch MIXED widget-preview-picker "" "" <0|A>`. Its `repoglance:picker-generated` line reads whether each widget has a generated preview (from `getWidgetPreview`), the publish stamp, and every placed RepoGlance widget id with its size.

- **Sheet.** `launch MIXED live` (or sample mode), `tap repoglance:menu`, `tap repoglance:menu-widgets`, `tap repoglance:widgets-add-repository`: the top activity is the launcher's `AddItemActivity` and the dump shows `Repository`, `2 × 1`, `Cancel` and `Add to home screen`. The preview itself is not in the accessibility tree: `capture` it and check the rocket widget with `sample`, `issues 5`, `PRs 3`. `BACK`, then the same with `repoglance:widgets-add-pinned` (`Pinned · 3` band, three rows reading `sample`).
- **Picker.** Go home, long-press an empty spot, `tap "Widgets"`, `tap "Search"`, `type "RepoGlance"`, tap `Repository, Pinned repos`: the dump shows `Repository`, `2 × 1`, `Pinned repos` and its span. `capture` it.
- **Generated.** In the debug picker, `repoglance:picker-generated` reads `Repository: generated preview published · Pinned repos: generated preview published (stamp <versionCode>.<look>)`. `adb shell run-as co.saari.repoglance cat shared_prefs/widget_previews.xml` holds the same stamp for both receivers; it is written only after a successful publish.
- **Fallback.** In the debug picker `tap repoglance:picker-remove-generated`: the line reads `no generated preview (previewLayout)` for both. Pick `0` (`repoglance:picker-candidate-0`) and `tap repoglance:picker-sheet-repository`: the sheet shows the static copy. Open the launcher picker again: it shows the static copy. A later app start republishes when the rate limit allows (`adb logcat -s GlanceAppWidgetManager` prints `was rate-limited` when it does not).
- **Theme.** `adb shell cmd uimode night yes`, open the sheet and the picker again, capture; `adb shell cmd uimode night no` afterwards.
- **Proof.** Capture both sheets and the picker in light and dark, the fallback in at least one theme, and record each SHA-256.

## Gotchas

- `uiautomator dump` cannot see inside a preview; assert the sheet and picker text from the dump and the preview content from a capture.
- The system rate-limits generated previews to about two publishes per hour per widget. `launch` force-stops the app, and every start may spend one; after removing previews, expect the fallback to stay until the window passes.
- The sheet and picker preview are the same sample on a live session; they never show the maintainer's repositories, so a capture of them is safe to keep.
- `Add to home screen` on a sheet the debug picker opened places a real widget (entry `A` also opens the Repository setup); swipe the sheet away instead.
- On the Pixel 10 Pro XL a 2 × 1 preview leaves blank space under `PRs`, as a placed compact widget does at that cell height.
- Local debug builds all carry versionCode 1, so the publish stamp reads `1.<look>` on every build. After changing the preview, bump `WidgetPreviews.LOOK_VERSION` or `Remove generated previews` in the debug picker, or the launcher picker can keep an older generated preview.
- The static copy was seen only on API 36 after removing the generated previews; no Android 12–14 device has been run.
