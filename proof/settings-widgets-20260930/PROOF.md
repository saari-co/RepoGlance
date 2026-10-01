# Settings and the Widgets screen — device proof (2026-09-30)

GrillTrack `settings-044`, `widgets-entry-045`, `widgets-look-047`. Branch
`claude/settings-widgets-entry`.

## Claims

1. The three-dot menu holds `Widgets` and `Settings` on the live and sample
   catalogs, and `Settings` alone on the signed-out Connect screen.
2. Settings shows Widgets, GitHub (Manage GitHub access, Disconnect GitHub)
   and About with a live session. In sample mode it has no GitHub section.
   Signed out it has no Widgets row either.
3. `Add` on the Widgets screen goes through the launcher's real
   `requestPinAppWidget` sheet. The sheet uses the new names `Repository`
   and `Pinned repos`. A repository widget then opens its setup; the
   Pinned repos widget lands without one.
4. The placed-widget list updates on return. A repository row opens that
   widget's setup, and changing it there updates the row and moves the pin.
5. Removing the widgets on the launcher removes their rows and releases
   their pins.
6. Settings survives a recreation, back returns the way it came, and a
   widget tap closes Settings.

## Devices and builds

| device | serial | Android | session | APK SHA-256 (debug) | source |
| --- | --- | --- | --- | --- | --- |
| Emulator, AVD Pixel_10_Pro_Fold (inner display) | `EMULATOR37X1X11X0` | 16 (API 36) | none (signed out, then sample mode) | `52ea76c2299d74cc83194816bd95b9434badb2a6d789697e51682c57d2e3a571` | `ac1637f` |
| Pixel 10 Pro XL | `63310DLCQ000RV` | 17 | maintainer's live session | `52ea76c2…` for steps L1–L2, `50a92923c328272fcb3193050b889a99e4afa4b512ec18f3fbfe5f14b4211efa` for L3–L5 | `ac1637f`, then `9bd56be` (with the theme merge) |

`bin/verify-repoglance doctor` passed on both before driving: the installed
APK equalled the local build, and the scenario launcher was present. Both
phones were checked with `dumpsys activity activities` first; the launcher
was in front and no other driver was active. Device time is
America/New_York (EDT).

## Emulator: signed out and sample mode

- **E1 Connect screen.** `launch MIXED live`.
  - The dump has `repoglance:menu` (`More options`),
    `repoglance:connect-github` and `repoglance:explore-sample`.
  - After `tap repoglance:menu`, the popup holds only
    `repoglance:menu-settings` (`Settings`).
- **E2 Settings, signed out.** `tap repoglance:menu-settings`.
  - The dump has `repoglance:settings`, `About`,
    `repoglance:settings-version` (`RepoGlance 0.0.0-dev`),
    `repoglance:settings-privacy` and `repoglance:settings-source`
    (`saari-co/RepoGlance`).
  - It has no `repoglance:settings-widgets` and no GitHub section.
  - Capture `settings-signedout.png`:
    `bdaaaffe6e6f67d3de3c39cf7f8e71f3c324439f705fb4dceea50fd08b2cd5bd`.
- **E3 Sample menu and Settings.** `settings-back`, then `explore-sample`.
  - The catalog has `repoglance:sample-bar` and `@saariuslystoned · 7
    repositories`.
  - The menu has `repoglance:menu-widgets` and `repoglance:menu-settings`.
  - Settings has `repoglance:settings-widgets` and `About`, and no
    `repoglance:settings-manage-access`.
- **E4 Widgets screen.** `tap repoglance:settings-widgets`.
  - The dump has `Sample mode: widgets you add show the sample
    repositories.`, `Repository widget` with `repoglance:widgets-add-repository`,
    `Pinned repos widget` with `repoglance:widgets-add-pinned`, and `On your
    home screen`.
  - Two widgets were already placed by earlier runs and match `dumpsys
    appwidget` ids 5 and 4: `repoglance:widgets-placed-5` (`Repository
    widget` / `Not set up · tap to choose a repository`) and
    `repoglance:widgets-placed-4` (`Pinned repos widget` / `No pins yet · pin
    repositories in the list`).
  - Capture `widgets-sample-before.png`:
    `6c1f2c64276aa8d7107a6a046146a90a1df127359280e45acbfb00869a26596b`.
- **E5 Add a repository widget.** `tap repoglance:widgets-add-repository`.
  - The top activity is `com.google.android.apps.nexuslauncher/com.android.launcher3.dragndrop.AddItemActivity`.
  - Its sheet shows `widget_name` `Repository`, `2 × 1`, the description
    `One repo's issues, PRs, and recent activity at a glance`, and `Add to
    home screen`.
  - Capture `pin-dialog-repo.png`:
    `e8745173f95e935313257f1a8ff3ef9b4852cd5e5043e20598bb365d31f25335`.
  - `tap "Add to home screen"`: the top activity is
    `co.saari.repoglance/.widget.RepoWidgetConfigActivity`, in RepoGlance's
    task. It shows `Configure widget`, `saari-co/rocket`, and the note
    `Sample data: saving pins this sample repository; …`.
  - `tap "Save widget"`: the app is back on `MainActivity` (Widgets). The
    new `repoglance:widgets-placed-7` reads `saari-co/rocket` / `Repository
    widget · Issues and PRs`, and widget 4 now reads `1 pinned repository`.
  - Capture `widgets-after-repo.png`:
    `5fdbbea7b64a7586915678d74790ad6d0c819f515eab887db5d0c90df87a3eaf`.
- **E6 Add the Pinned repos widget.** `tap repoglance:widgets-add-pinned`.
  - The launcher sheet shows `Pinned repos` / `Your pinned repos, newest push
    first, at a glance`.
  - `Add to home screen` returns straight to Widgets, with no setup. The new
    `repoglance:widgets-placed-8` reads `Pinned repos widget` / `1 pinned
    repository`.
- **E7 Change it.** `tap repoglance:widgets-placed-7`.
  - Its setup opens on `saari-co/rocket`. The dropdown lists exactly the
    seven sample repositories.
  - Choose `saari-co/api-server` and `PRS`, then `Save widget`. Row 7 reads
    `saari-co/api-server` / `Repository widget · PRs`, and the pin count
    stays `1`.
  - Capture `widgets-after-change.png`:
    `b844ff7c9c12cb464a6057f84cf5e5c8f67530f266533aaf6a3024c00cceabb6`.
- **E8 Home screen.** `widget-bounds`:
  - `repo … 190x91dp api-server | sample | issues | 3`
  - the stack `Pinned · 1 | saari-co/api-server | sample | issues 3 · PRs 2
    · review 0`
  - the pre-existing `RepoGlance | Tap to choose a repository`
  - On the next page, the new Pinned repos widget showed the same sample
    row.
- **E9 Remove both.** Each was dragged to the launcher's `Remove` target
  with `input motionevent`. `dumpsys appwidget` lost id 8 and then id 7;
  ids 5 and 4, placed earlier by someone else, stayed.
  - The launcher reads `Pinned · 0` / `Pin repositories in RepoGlance`.
  - Widgets reads only rows 5 and 4, with `No pins yet · pin repositories in
    the list`.
- **E10 Back and recreation.**
  - From Widgets (opened via Settings), `BACK` gives `repoglance:settings`.
  - `cmd uimode night yes` recreates the activity, and it is still on
    `repoglance:settings`. `cmd uimode night no` restored the phone (`Night
    mode: no` before and after).
  - `BACK` gives the sample catalog.
- **E11 A widget tap closes Settings.** With Settings open, `HOME`, then tap
  the stack's `Pinned · 0`. The top activity is `MainActivity`, and the dump
  shows the sample catalog with no `repoglance:settings`.
- **E12 Exit.** `tap repoglance:sample-sign-in` gives
  `repoglance:connect-github` and `repoglance:explore-sample`, with no
  device code. `bin/verify-repoglance cleanup` ran.

## Pixel 10 Pro XL: live session

- **L1 Live menu and Settings.**
  - The catalog was filtered to `saari-co/RepoGlance` (the only
    repository-shaped label) before any capture.
  - The menu has `repoglance:menu-widgets` and `repoglance:menu-settings`.
  - Settings has `Widgets`, `GitHub`, `repoglance:settings-manage-access`
    (`Manage GitHub access`, `Opens GitHub`),
    `repoglance:settings-disconnect` (`Disconnect GitHub`) and `About`.
    Neither GitHub row was tapped.
  - Capture `xl-settings-live.png`:
    `8e6d6ca7b4ea4b352f88455292a6fd4a606540056b1a446f78b7962da5e598c0`.
- **L2 Widgets, nothing placed.** The dump reads `No RepoGlance widgets on
  your home screen yet.`; `dumpsys appwidget` held no RepoGlance widget.
  - Capture `xl-widgets-empty.png`:
    `0410312304c87a782b11f71d48795d6fb97fb690ad870ce972a37c817cc04da2`.
  - `Add` → `QuickstepAddItemActivity` → `Add to home screen` →
    `RepoWidgetConfigActivity`, with the live note `Saving pins this
    repository in RepoGlance; …`.
  - The first attempt ended without a save. Two back gestures (`[Gesture
    Monitor] edge-swipe`, then `startBackNavigation … TYPE_CROSS_ACTIVITY`)
    closed the dropdown and then the setup, at 21:07:17 in the device's
    local time. None of this run's commands sent them. The widget stayed
    placed as `Not set up`, which is the designed unconfigured state.
  - Capture `xl-widgets-after.png`:
    `26ba714f0b1f4cb0c8ee9535ddbf5e43e9d8dc372e1d5ec1791147e5b18ffc5d`.
- **L3 Finish setup from the list.** On `9bd56be`, `tap
  repoglance:widgets-placed-6` opens its setup. Choose `saari-co/RepoGlance`,
  `BOTH`, then `Save widget`. Row 6 reads `saari-co/RepoGlance` / `Repository
  widget · Issues and PRs`.
  - Capture `xl-widgets-live-configured.png`:
    `0fa418b10093c1cc5bf2891f981960d52b9bd403bfdacd38d6ca4ee9e3e9e9b4`.
  - The save ran one background refresh: `PinnedRefreshWorker … Worker
    result SUCCESS` at 21:10:48. The home screen reads `RepoGlance | 9:10
    PM | issues | 3`.
- **L4 Remove.** On the XL, long-pressing the widget opens the launcher's
  menu (`Remove`, `Settings`); `Remove` dropped id 6. Two drags onto the
  `Remove` target did nothing there.
- **L5 Pins restored.** `saari-co/RepoGlance` was not pinned before L3.
  Saving pinned it and the removal released it: the filtered catalog reads
  `Pin saari-co/RepoGlance` again. No RepoGlance widget is left on the XL.
  `cleanup` ran.

## Other observations

- **ANR on the first emulator cold start.** The first emulator cold start
  after `adb install`: `ANR in co.saari.repoglance`, `Reason: No response to
  onStartJob` (WorkManager's `SystemJobService`).
  - `Displayed … +18s588ms`, and verification ran at 172 bytecodes/s
    (`base.apk has no usable artifacts`).
  - Tapping `Wait` let it continue, and later starts were normal.
  - This is attributed to an unoptimized fresh install on a loaded emulator,
    not to this change, but it was seen once and is recorded here.
- **StrictMode.** No `StrictMode` lines on the emulator for the whole run.
  On the XL, two `UntaggedSocketViolation` lines appeared: one at catalog
  load and one in `PinnedRefreshWorker`, both from the existing HTTP client.
  There was no disk or network work on the main thread.
- **Not seen on a device:** the how-to that replaces `Add` when a launcher
  cannot pin (Pixel Launcher always can). It is covered by
  `SettingsGuardTest` only.

## Checks

- `./gradlew check` passed at `ac1637f`: Lint "no new issues", detekt, unit
  tests, the feature-map and verify-helper checks.
- Run directories (ignored, not committed):
  `runs/verify-repoglance-runs/20261001T0052*`–`20261001T0114*`.
- The feature map is updated: `features/settings-widgets.md` is new, and
  `widget-setup.md`, `stack-widget.md`, `sample-mode.md` and `sign-in.md`
  name the in-app paths.
