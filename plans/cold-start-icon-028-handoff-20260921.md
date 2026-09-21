# cold-start-icon-028 — build handoff (2026-09-21)

GrillTrack decision `cold-start-icon-028` on track `gt-20260728163459-227573`,
**locked** after the maintainer's shared-understanding confirmation on
2026-09-21. The grill ran in the session that owned branch
`claude/cold-start-mark`; the maintainer asked for the build to run in a fresh
session. This file is the handoff: everything the builder needs, with no
dependence on that conversation.

## Confirmed decision

1. **Launcher icon = round 3 B.** Foreground: the locked commit-eye mark
   (checking-splash-018) drawn as a heavy white stroke with two solid grey
   rings ("R2 A"). Background: vertical gradient, graphite (`#2E3238`) at the
   top to near-black (`#040506`) at the bottom.
2. **Monochrome / themed-icon glyph = round 2 A silhouette**: the mark with its
   two solid rings.
3. **Quick Settings tile switches to that glyph** (maintainer-approved change to
   the icon locked in `quick-settings-tile-019`; record it in 028, do not
   reopen the rest of 019).
4. **Start window background follows the system.** Dark mode:
   `@android:color/system_neutral1_900` (the app's own dynamic dark background;
   verified seamless on device in the preview). Light mode:
   `@android:color/system_neutral1_10` (best match for the app's dynamic light
   background; not verified on device — see Human checks).

**Out of scope, unchanged:** the in-app loading spinner and refresh spinner;
the in-app checking animation (`CheckingMark`, still the plain mark per 018).
Next grill queued by the maintainer: open straight onto the last catalog,
labelled with its age.

## Build

- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`: adaptive icon with
  background, foreground and monochrome layers (resources
  `ic_launcher_background`, `ic_launcher_foreground`,
  `ic_launcher_monochrome` in `res/drawable/`). No `-v26` qualifier: minSdk is
  31 and lint flags obsolete SDK qualifiers.
- `AndroidManifest.xml` `<application>`: add `android:icon="@mipmap/ic_launcher"`
  and `android:roundIcon="@mipmap/ic_launcher"` (the app has no icon today).
- `res/drawable/ic_repoglance_glyph.xml`: the glyph below; the tile service's
  `android:icon` and `RepoGlanceTileService` (`tile.icon =
  Icon.createWithResource(this, R.drawable.ic_repoglance_mark)`) switch to it.
- Window theme: `res/values/themes.xml` (parent
  `@android:style/Theme.Material.Light.NoActionBar`, `windowBackground`
  `@android:color/system_neutral1_10`) and `res/values-night/themes.xml`
  (parent `@android:style/Theme.Material.NoActionBar`, `windowBackground`
  `@android:color/system_neutral1_900`); `<application android:theme>` points
  at it. No `core-splashscreen` dependency: the platform SplashScreen on API
  31+ takes its background from `windowBackground` and its icon from the
  launcher icon.
- Guard tests (repo idiom: source-inspection tests under `app/src/test`):
  manifest icon + roundIcon wiring; adaptive icon has a monochrome layer;
  `values-night` window background is `system_neutral1_900`; the tile uses the
  glyph in both manifest and service.
- Comment ban: detekt `ForbiddenComment` covers Kotlin in `app/src/main`;
  `app/src/main` XML already carries short rationale comments, so XML comments
  are allowed but keep them to that style.
- Lint `VectorPath`: any single path over ~800 characters fails lint (hit
  during the grill); split long paths.

## Exact artwork (verbatim from the chosen candidates)

The foreground and monochrome are drawn in the mark's own 24-unit coordinates
inside a group scaled by 2.35 and translated by 25.212 into the 108 dp
adaptive-icon canvas, which keeps the handle cap inside the 66 dp safe circle.

### `ic_launcher_background` (round 3 B)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0 H108 V108 H0 Z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear" android:startX="54" android:startY="0" android:endX="54" android:endY="108">
                <item android:offset="0.0" android:color="#FF2E3238" />
                <item android:offset="1.0" android:color="#FF040506" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
```

### `ic_launcher_foreground` (round 2 A mark)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="2.35" android:scaleY="2.35" android:translateX="25.212" android:translateY="25.212">
        <path android:pathData="M1.10,10.1 a9.0,9.0 0 1,1 18.00,0 a9.0,9.0 0 1,1 -18.00,0 Z" android:strokeColor="#FF8B949E" android:strokeWidth="1.0" android:fillColor="#00000000" />
        <path android:pathData="M-0.90,10.1 a11.0,11.0 0 1,1 22.00,0 a11.0,11.0 0 1,1 -22.00,0 Z" android:strokeColor="#FF8B949E" android:strokeWidth="0.8" android:fillColor="#00000000" android:strokeAlpha="0.6" />
        <path android:pathData="M10.1,10.1 m-6.7,0 a6.7,6.7 0 1,1 13.4,0 a6.7,6.7 0 1,1 -13.4,0" android:strokeColor="#FFFFFFFF" android:strokeWidth="2.5" android:fillColor="#00000000" />
        <path android:pathData="M15.6,15.6 L21.1,21.1" android:strokeColor="#FFFFFFFF" android:strokeWidth="3.0" android:fillColor="#00000000" android:strokeLineCap="round" />
        <path android:pathData="M10.1,4.3 L10.1,5.7 M10.1,14.5 L10.1,15.9" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.6" android:fillColor="#00000000" android:strokeLineCap="round" />
        <path android:pathData="M5.7,10.1 Q10.1,4.9 14.5,10.1 Q10.1,15.3 5.7,10.1 Z" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.6" android:fillColor="#00000000" android:strokeLineJoin="round" />
        <path android:pathData="M10.1,10.1 m-1.6,0 a1.6,1.6 0 1,1 3.2,0 a1.6,1.6 0 1,1 -3.2,0" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.6" android:fillColor="#00000000" />
    </group>
</vector>
```

### `ic_launcher_monochrome` (round 2 A silhouette)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="2.35" android:scaleY="2.35" android:translateX="25.212" android:translateY="25.212">
        <path android:pathData="M1.10,10.1 a9.0,9.0 0 1,1 18.00,0 a9.0,9.0 0 1,1 -18.00,0 Z" android:strokeColor="#FF000000" android:strokeWidth="1.0" android:fillColor="#00000000" />
        <path android:pathData="M-0.90,10.1 a11.0,11.0 0 1,1 22.00,0 a11.0,11.0 0 1,1 -22.00,0 Z" android:strokeColor="#FF000000" android:strokeWidth="0.8" android:fillColor="#00000000" />
        <path android:pathData="M10.1,10.1 m-6.7,0 a6.7,6.7 0 1,1 13.4,0 a6.7,6.7 0 1,1 -13.4,0" android:strokeColor="#FF000000" android:strokeWidth="2.5" android:fillColor="#00000000" />
        <path android:pathData="M15.6,15.6 L21.1,21.1" android:strokeColor="#FF000000" android:strokeWidth="3.0" android:fillColor="#00000000" android:strokeLineCap="round" />
        <path android:pathData="M10.1,4.3 L10.1,5.7 M10.1,14.5 L10.1,15.9" android:strokeColor="#FF000000" android:strokeWidth="1.6" android:fillColor="#00000000" android:strokeLineCap="round" />
        <path android:pathData="M5.7,10.1 Q10.1,4.9 14.5,10.1 Q10.1,15.3 5.7,10.1 Z" android:strokeColor="#FF000000" android:strokeWidth="1.6" android:fillColor="#00000000" android:strokeLineJoin="round" />
        <path android:pathData="M10.1,10.1 m-1.6,0 a1.6,1.6 0 1,1 3.2,0 a1.6,1.6 0 1,1 -3.2,0" android:strokeColor="#FF000000" android:strokeWidth="1.6" android:fillColor="#00000000" />
    </group>
</vector>
```

### `ic_repoglance_glyph` (24 dp, for the tile and any future status-bar icon)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <group android:scaleX="0.92" android:scaleY="0.92" android:translateX="1.3" android:translateY="1.3">
        <path android:pathData="M1.10,10.1 a9.0,9.0 0 1,1 18.00,0 a9.0,9.0 0 1,1 -18.00,0 Z" android:strokeColor="#FF000000" android:strokeWidth="1.0" android:fillColor="#00000000" />
        <path android:pathData="M-0.90,10.1 a11.0,11.0 0 1,1 22.00,0 a11.0,11.0 0 1,1 -22.00,0 Z" android:strokeColor="#FF000000" android:strokeWidth="0.8" android:fillColor="#00000000" />
        <path android:pathData="M10.1,10.1 m-6.7,0 a6.7,6.7 0 1,1 13.4,0 a6.7,6.7 0 1,1 -13.4,0" android:strokeColor="#FF000000" android:strokeWidth="2.5" android:fillColor="#00000000" />
        <path android:pathData="M15.6,15.6 L21.1,21.1" android:strokeColor="#FF000000" android:strokeWidth="3.0" android:fillColor="#00000000" android:strokeLineCap="round" />
        <path android:pathData="M10.1,4.3 L10.1,5.7 M10.1,14.5 L10.1,15.9" android:strokeColor="#FF000000" android:strokeWidth="1.6" android:fillColor="#00000000" android:strokeLineCap="round" />
        <path android:pathData="M5.7,10.1 Q10.1,4.9 14.5,10.1 Q10.1,15.3 5.7,10.1 Z" android:strokeColor="#FF000000" android:strokeWidth="1.6" android:fillColor="#00000000" android:strokeLineJoin="round" />
        <path android:pathData="M10.1,10.1 m-1.6,0 a1.6,1.6 0 1,1 3.2,0 a1.6,1.6 0 1,1 -3.2,0" android:strokeColor="#FF000000" android:strokeWidth="1.6" android:fillColor="#00000000" />
    </group>
</vector>
```

## Verify

- `./gradlew check` green; lint/detekt baselines unchanged.
- Device: approved phones in `.claude/skills/verify-repoglance/devices.tsv`;
  pin `VERIFY_SERIAL`. On 2026-09-21 only the Pixel 10 Pro Fold
  (`59151FDCG000JA`) was attached; it is signed in (maintainer's session) and
  in dark mode.
  - Real app drawer showing RepoGlance with the new icon. **Crop to the
    RepoGlance entry only** and fail closed: take no screenshot unless the
    entry's bounds were found in the UI dump (the grill captured two
    uncropped home-screen frames by mistake and deleted them).
  - Dark-mode app start: `screenrecord --display-id 4619827677550801152` for
    ~2 s from a cold start; the splash shows the white mark and grey rings on
    navy with no disc and runs into the app with no white flash. Crop to the
    icon region; the first frames of a recording show the home screen.
  - `screencap` on this Fold needs `-d 4619827677550801152` or it writes a
    text warning instead of a PNG.
  - Before driving, check `dumpsys trust` for `deviceLocked=0`. If locked,
    stop and ask; never type onto the lock screen.
  - `bin/verify-repoglance launch` force-stops the app; never run it while the
    maintainer is signing in.
- The phone still has the grill's candidate build installed (five extra
  launcher entries, "R3 A".."R3 E"); the first install of the real build
  replaces it and removes them.

## Human checks (agent must not change system settings)

- Light mode start screen: maintainer switches the phone to light mode, cold
  starts RepoGlance, and confirms no visible shift from start screen to app.
- Themed icons: maintainer turns on Wallpaper & style → Themed icons and
  confirms the RepoGlance glyph looks right.
- Quick Settings tile: not added on the 10 Pro Fold; maintainer adds it (or
  checks the 11 Pro Fold) and confirms it shows the ringed glyph.

## Evidence from the grill (local, gitignored, on Bobby's MacBook)

- `/Users/bobbybones/Developer/side-quests/RepoGlance/.claude/worktrees/zen-kowalevski-fed0f6/runs/cold-start-icon-20260921/drawer-candidates-dark.png`, `splash-candidates-white.png` (round 1)
- `/Users/bobbybones/Developer/side-quests/RepoGlance/.claude/worktrees/zen-kowalevski-fed0f6/runs/cold-start-icon-20260921/r2/drawer-r2.png`, `r2/splash-r2-white-vs-dark.png`,
  `r2/silhouettes-under-statusbar.png` (round 2; the glyph choice)
- `/Users/bobbybones/Developer/side-quests/RepoGlance/.claude/worktrees/zen-kowalevski-fed0f6/runs/cold-start-icon-20260921/r3/drawer-r3.png`, `r3/splash-r3-white-vs-dark.png` (round 3; the
  background choice)

## Picker record

```json
{
  "schema_version": "grilltrack-picker/v0.1",
  "round_id": "cold-start-icon-r3",
  "canvas_ref": "device:Pixel 10 Pro Fold 59151FDCG000JA inner display; Pixel Launcher app drawer (circle mask) and the Android 17 system splash, via debug activity-aliases of MainActivity",
  "active_slot": "icon-background",
  "slots": [
    {
      "id": "mark",
      "status": "locked",
      "selection_ref": "decision:checking-splash-018 (A+B commit-eye magnifier)"
    },
    {
      "id": "icon-foreground",
      "status": "locked",
      "selection_ref": "maintainer: round 2 candidate A (heavy white stroke mark, two solid grey rings); background flagged as the gap"
    },
    {
      "id": "icon-background",
      "status": "unresolved"
    }
  ],
  "candidates": [
    {
      "id": "A",
      "label": "Flat charcoal",
      "delta_ref": "app/src/debug/res/drawable/pick_icon_a_background.xml"
    },
    {
      "id": "B",
      "label": "Vertical gradient, graphite top to black",
      "delta_ref": "app/src/debug/res/drawable/pick_icon_b_background.xml"
    },
    {
      "id": "C",
      "label": "Faint grey radial glow behind the lens, fading to black",
      "delta_ref": "app/src/debug/res/drawable/pick_icon_c_background.xml"
    },
    {
      "id": "D",
      "label": "Diagonal gradient along the handle",
      "delta_ref": "app/src/debug/res/drawable/pick_icon_d_background.xml"
    },
    {
      "id": "E",
      "label": "Black with a faint fine dot grid",
      "delta_ref": "app/src/debug/res/drawable/pick_icon_e_background.xml"
    }
  ],
  "controls": {
    "keyboard": true,
    "pointer": true,
    "mobile": true
  },
  "production": false,
  "history": [
    {
      "round_id": "cold-start-icon-r1",
      "outcome": "maintainer leaned D, asked for a Tailscale-like black/grey/white direction and a solid-white status-bar glyph; replaced by a new round of five"
    },
    {
      "round_id": "cold-start-icon-r2",
      "outcome": "maintainer favourite A but not loved; only flat black background flagged; foreground kept, new round of five backgrounds"
    }
  ]
}
```

## Delivery

Commit on the builder's own branch. A PR needs the maintainer's request;
merging needs his request plus CI green on the head, OpenClaw autoreview
(`bin/smoky lane run spark-openclaw-materialize-worktree` then
`spark-openclaw-autoreview --queue --operator-id bobby --mode branch --base
<fork-point-sha> --remote-worktree <path>`, run from
`~/Developer/side-quests/x-api`), and a ClawSweeper review of that exact head
with `proof: sufficient`, `status: ready for maintainer look` and no
unresolved P0/P1. The ledger's delivery record for `signin-return-027`
(PR #31, merged as `7b59139`) is still to be added; do it in this branch.
