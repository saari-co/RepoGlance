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
| Start window matches the app background | `res/values-v34` / `res/values-night-v34` (`system_background_light` / `_dark`), fallback `res/values` / `res/values-night` (`system_neutral1_10` / `_900`); `<application android:theme="@style/Theme.RepoGlance">` |
| App icon wiring | `<application android:icon/roundIcon="@mipmap/ic_launcher">` |

Artwork XML is taken verbatim from the handoff. The in-app mark
(`ic_repoglance_mark`, used by `RepoGlanceMark`), `CheckingMark`, and both
spinners are unchanged.

## Checks

- `./gradlew check`: BUILD SUCCESSFUL (lint, detekt, unit tests, verifier
  structure). `app/lint-baseline.xml` and `app/detekt-baseline-debug.xml`
  are unchanged.
- `ColdStartIconGuardTest` (5 tests):
  - the manifest's icon, roundIcon and theme;
  - the adaptive icon has background, foreground and monochrome layers;
  - the start-window colour in all four folders: `values` `system_neutral1_10`,
    `values-night` `system_neutral1_900`, `values-v34` `system_background_light`,
    `values-night-v34` `system_background_dark`;
  - each folder uses the light or dark parent theme for its mode (added after
    review R4);
  - the tile uses `ic_repoglance_glyph` in the manifest and the service.
- Mutation checks, each reverted afterwards:
  - `values-night` set to `system_neutral1_10`;
  - `values-v34` set to `system_neutral1_10`;
  - `values-night-v34` set to `system_neutral1_900`;
  - `values-v34` given the dark parent.

  Each fails exactly one test.
- API 31–33 is approximate and unverified (review R1). Material 3 draws the
  app there on a computed neutral-variant tone 6, and no API 31–33 device was
  run.

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
`system_neutral1_900` as an approximate fallback. Material 3 computes the app background there from a neutral-variant tone 6 that has no system resource, and API 31–33 is unverified (review R1). `ColdStartIconGuardTest` asserts
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

## Launcher drawer shot: first attempt failed closed; cause found

On the first run, the uiautomator bounds of the drawer's `RepoGlance` label
did not match the inner-display frame, and two crops showed other apps. Both
were deleted. Cause: RepoGlance was not on the drawer's first screen. The only
node labelled RepoGlance was the hidden home-screen dock prediction behind the
drawer (no launcher id, bounds `[1462,1915][1584,2054]`). The later captures
accept only a node whose label and description are both exactly `RepoGlance`
inside the drawer's search results, or the home-dock node while no drawer is
showing, with matching dumps before and after the capture.

## Maintainer-gated checks (2026-09-21)

Bobby switched on light mode and themed icons and added the tile. The agent
then captured each result, cropped to RepoGlance's own UI only, with a dump
before and after each capture that had to match (fail closed). The phone
still reported build `BD3A.250808.001` (API 36), with the update downloading.
`deviceLocked=0` was checked before driving.

1. **Light-mode start:** seamless. `cmd uimode night` = `no`. Cold start
   `LaunchState: COLD`, `TotalTime: 498`, 375 frames. The RepoGlance window
   covers frames 23–374 without a break, all on background RGB (246,243,253)
   (±1, which is video-encoding noise), from the first splash frame through
   the loading screen. Crop:
   `runs/cold-start-icon-028-runs/cold-start-light-v34-strip-frames-25-38-68-88-113-203.png`.
2. **Themed icon:** correct. With themed icons on, the home-dock prediction
   shows the monochrome layer: the two-ring commit-eye glyph tinted in the
   system accent on the light tonal disc. The drawer's search result stays
   full colour, because Pixel themes home-screen icons only. It shows the
   round 3 B icon and the "RepoGl…" label. Crops:
   `home-dock-themed-light.png`, `drawer-search-themed-light.png`.
3. **Quick Settings tile:** it shows the ringed glyph, active state, small
   tile. The tile's description reads "RepoGlance, latest push to <repo>
   updated 5m ago" (019 behaviour unchanged). Crop: `qs-tile-light.png`.

The agent did not change any system setting. Switching dark mode and themed
icons back is the maintainer's call.
