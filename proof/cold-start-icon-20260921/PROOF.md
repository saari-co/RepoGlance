# cold-start-icon-028 — build proof (2026-09-21)

GrillTrack track `gt-20260728163459-227573`, decision `cold-start-icon-028`
(locked 2026-09-21). Built from `plans/cold-start-icon-028-handoff-20260921.md`
on branch `claude/cold-start-mark`.

**Result:** implemented and verified on the device. The first device run found
F1: the dark start window was lighter than the app. The maintainer chose
option 2. The rebuilt start is seamless in dark mode (see "Re-verification").

## What was built

| Lock item | Where |
| --- | --- |
| Launcher icon round 3 B (gradient background, R2 A foreground) | `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `res/drawable/ic_launcher_background.xml`, `res/drawable/ic_launcher_foreground.xml` |
| Monochrome / themed glyph (R2 A silhouette) | `res/drawable/ic_launcher_monochrome.xml` (adaptive icon `monochrome` layer) |
| Quick Settings tile uses the glyph | `res/drawable/ic_repoglance_glyph.xml`; manifest `RepoGlanceTileService android:icon`; `RepoGlanceTileService.render` |
| Start window follows the system | `res/values/themes.xml` (`system_neutral1_10`), `res/values-night/themes.xml` (`system_neutral1_900`); `<application android:theme="@style/Theme.RepoGlance">` |
| App icon wiring | `<application android:icon/roundIcon="@mipmap/ic_launcher">` |

Artwork XML is taken verbatim from the handoff. The in-app mark
(`ic_repoglance_mark`, used by `RepoGlanceMark`), `CheckingMark`, and both
spinners are unchanged.

## Checks

- `./gradlew check`: BUILD SUCCESSFUL (lint, detekt, unit tests, verifier
  structure). `app/lint-baseline.xml` and `app/detekt-baseline-debug.xml`
  are unchanged.
- `ColdStartIconGuardTest` (4 tests): manifest icon, roundIcon and theme;
  adaptive icon has background, foreground and monochrome layers; the
  window background is `system_neutral1_10` / `system_neutral1_900`; the tile
  uses `ic_repoglance_glyph` in the manifest and the service.
  Mutation check: setting `values-night` to `system_neutral1_10` fails
  1 of 4 tests; the change was reverted.

## Device (Pixel 10 Pro Fold `59151FDCG000JA`, `VERIFY_SERIAL` pinned)

- Before driving: `dumpsys trust` `deviceLocked=0`, awake, dark mode on
  (`cmd uimode night` read only; not changed). Posture `OPENED`.
- Installed the debug APK with `adb install -r` (session kept). Doctor then
  passed: `apk_device` = local build SHA-256
  `f4b3e192e756815aff8a64c888b1501b69fadd6d79cec51e563aa620b8e27e1f`.
- The launcher now resolves exactly one RepoGlance entry,
  `co.saari.repoglance/.MainActivity`. The grill's five `R3 A..E` aliases are
  gone.
- Cold start: `am start -W` reported `LaunchState: COLD`, `TotalTime: 491`,
  recorded with `screenrecord --display-id 4619827677550801152` (383
  frames, 120 fps). Full frames stayed in a temp dir and were deleted. Only
  frames whose centre 600 px region had uniform RepoGlance-window corners were
  kept (frames 22–381). The first 22 frames show the home screen and were never
  saved.
  - The system splash shows the round 3 B icon: the white mark with grey
    rings on the graphite-to-black gradient disc. This matches candidate B in
    the grill's `r3/splash-r3-white-vs-dark.png`, where B also shows its disc on the dark splash.
    The handoff's "no disc" line does not describe B.
  - **No white flash.** Full-frame mean brightness after the home-screen
    frames never exceeds 28.7/255 (the frame 0 home screen is 54.8).
  - Kept crop: `runs/cold-start-icon-028-runs/cold-start-dark-strip-frames-24-40-70-85-100-200.png`
    (local, ignored; SHA-256 `f7d5433d…d876`). It has six centre crops:
    splash, splash, fade, loading, loading, loading.

### F1: the dark start window is lighter than the app (decided: option 2)

The splash background measures RGB (16,23,60), which is `system_neutral1_900`
and the same colour as the grill's round 3 dark splash. The app's first
frames measure (3,6,45). The window crossfades between them over about
250 ms (frames ~70–100). The result is a visible darkening, not a flash.

Cause: the handoff assumed `system_neutral1_900` is the app's dynamic dark
background. In Compose Material 3 1.3.0 (the resolved version),
`dynamicDarkColorScheme` on API 34+ uses
`android.R.color.system_background_dark`. The light scheme uses
`system_background_light`. On API 31–33 it uses a computed neutral-variant
tone 6, which has no system resource.

Options put to the maintainer:

1. Keep the lock as built (`system_neutral1_900` / `system_neutral1_10`) and
   accept the short darkening.
2. Match the app on API 34+: add `values-v34` (`system_background_light`) and
   `values-night-v34` (`system_background_dark`), keeping the current
   values as the API 31–33 fallback. This is a change to the locked resource.
   Seamlessness would still need to be proven on the device.

**Decision (maintainer, 2026-09-21): option 2.** It follows Google's
SplashScreen guidance (the splash is one colour matching the app's first
frame, per a Developer Knowledge query). Built as `res/values-v34/themes.xml`
(`system_background_light`) and `res/values-night-v34/themes.xml`
(`system_background_dark`). API 31–33 keep `system_neutral1_10` /
`system_neutral1_900`, which is the closest resource: M3 computes that
background with no system resource there. `ColdStartIconGuardTest` asserts
all four values. `./gradlew check` is green with unchanged baselines.

### Re-verification (option 2, source `9d2940e`)

- The Fold's system update was still downloading in the background. The phone
  still reported Android 16 (API 36), build `BD3A.250808.001`, patch
  2025-09-05. `deviceLocked=0`, awake, dark mode.
- `adb install -r` the debug APK. Doctor passed with `apk_device` =
  `89e0f09d0e856531e695d7661ca665cd39ce551272eabb86a6a51e503085b0ca`.
  `aapt2 dump resources` shows `Theme.RepoGlance` in the default, `night`,
  `v34` and `night-v34` configurations.
- Cold start: `LaunchState: COLD`, `TotalTime: 443`. Recorded 347 frames
  with the same fail-closed crop filter. The RepoGlance window covers frames
  24–345 without a break. **Every one of those frames has the same
  background, RGB (3,6,45)**, from the first splash frame through the
  loading screen. The earlier (16,23,60) → (3,6,45) step is gone. Full-frame
  mean brightness after the splash starts is at most 19.4/255: no flash.
- Kept crop: `runs/cold-start-icon-028-runs/cold-start-dark-v34-strip-frames-26-39-69-89-114-204.png`
  (local, ignored). It has six centre crops: splash, splash, fade, loading,
  loading, loading. Full frames and the recording were deleted after
  extraction.

## Launcher drawer shot: not captured (fail-closed)

The uiautomator bounds of the drawer's `RepoGlance` label did not line up
with the inner-display frame from `screencap -d 4619827677550801152`. Two
crops at those bounds showed other apps' icons and labels. Both crops were
deleted and no drawer image was kept. The splash frames above show the launcher
icon in RepoGlance's own window.

## Human checks (not done by the agent)

- Light mode start screen (no visible shift from splash to app).
- Themed icons on: the glyph looks right.
- Quick Settings tile shows the ringed glyph (not added on the 10 Pro Fold).
