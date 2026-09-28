# shape-control-031 — picker and production verification (2026-09-28)

- **Track:** `gt-20260728163459-227573`, family-look round 3, slot
  `shape-and-control-language`.
- **Scope (maintainer, 2026-09-28):** chips + status pills, buttons,
  banners, cards/rows.
- **Baseline:** stacked on PR #37 head `c821c979f8b4f0e13828eb4b353a4c516967633e`.
- **Device:** Pixel 10 Pro Fold `59151FDCG000JA`, inner display 2076x2152,
  light, `deviceLocked=0` checked before each run.
- **Gates on every kept capture:** UI tree dumped before and after (traces
  equal after settling), only package `co.saari.repoglance`, required text
  present; crops rows 150..2100. PNGs stay in ignored `runs/`.

## Picker (run `shape-031-picker`, `ShapeVariantPickerActivity`, fixture MIXED)

Manifest `.grilltrack/work/picker/family-look-round-3.json` validated
(`validate_picker.py`: valid).

| candidate | trace text | png sha256 (16) |
| --- | --- | --- |
| A M3 default | On canvas: shape A | b521c8434e21497e |
| B outlined capsules | On canvas: shape B | f58cdf32dacff237 |
| C Google tonal | On canvas: shape C | d2aeb456d720b0e3 |
| D squared terminal | On canvas: shape D | d515a4025721cb2b |
| E filled capsules + chamfer | On canvas: shape E | 812d330849aed706 |

- B and C failed the gate on the first pass (tree still changing, and a
  stale trace for C); those PNGs were deleted and re-captured after the tree
  settled.
- Fidelity gap: A was captured in light mode and B–E in dark (the dark chip
  was toggled during the run). The maintainer judged on the live picker,
  not on the sheet.
- Maintainer picked **C** on the device.

## Production (run `shape-031-verify`, `ControlShape.Default = Tonal`)

| capture | screen | seen | png sha256 (16) |
| --- | --- | --- | --- |
| live | live catalog filtered to `saari-co/RepoGlance` (only repo row in tree) | 24 dp flat tonal card, tonal borderless LIVE chip | 6cbd4230b3ed1a32 |
| nav-prs | fixture navigator, PRS mode | tonal borderless Draft chip | a6f0eb44903fc027 |
|- An earlier live-catalog capture showed unfiltered repositories; it was
  deleted with its dumps and not used. A first fixture-home capture failed
  its text gate (wrong needle) and was deleted.
- The tonal Navigator button renders saturated indigo with light text: this
  device's Android 17 dynamic `secondaryContainer` is saturated. The selected
  sort FilterChip (also `secondaryContainer`) shows the same colour in the
  live capture, so this is the scheme, not a fill bug.
ture catalog, which the signed-in app does not open.
  They go through the same `StatusPill` / `StatusBanner` seam. The status
  pill was seen under candidate C in the picker; a rate-limit banner was
  not in any kept crop (open risk).
- `./gradlew assembleDebug check`: green.
