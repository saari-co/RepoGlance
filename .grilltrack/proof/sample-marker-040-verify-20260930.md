# sample-marker-040: round, verification and review (2026-09-30)

Track `gt-20260728163459-227573`, decision `sample-marker-040`, map
`.grilltrack/maps/sample-mode.md` node 3. Base
`f2c8816b8d69967daa091776c9e0727579c1c141` (main after #45). Branch
`claude/grilltrack-040-sample-marker` in worktree
`.claude/worktrees/dazzling-jemison-935f18`.

## Round of five (sample-marker-round-1)

Slot: how sample mode is marked on the app screens, the repo and stack
widgets and the Quick Settings tile. Locked context: sample behaviour
(038/039/042/043), status colour 029 (hues not reused: every candidate used
the dynamic `tertiary` role), mono labels 030, tonal shapes 031, widget look
032/034. Manifest `.grilltrack/work/picker/sample-marker-round-1.json`
validated (`validate_picker.py`: valid).

Renderer: `SampleMarkerPickerActivity` (debug only) on the Pixel 10 Pro Fold
`59151FDCG000JA` inner display (Android 17, font scale 1.2), launched through
`bin/verify-repoglance launch MIXED sample-marker-picker "" "" <A..E>`. Left:
the production `LiveRepoGlanceScreen` fed the sample account at the XL's
width (443 dp). Right: production `CompactContent` at the Fold 2x1 cell
(148x89 dp), `TallContent` and the stack header and rows through
`GlanceRemoteViews`, and `TileTexts.sample`. Neutral chips, active
description, trace line, dark/canvas toggles and Reset; keyboard 1-5.

| candidate | app | widgets | tile | capture (16) |
| --- | --- | --- | --- | --- |
| A chip row (then production) | chip row under the header | plain `sample` | `Sample · repo · age` | 0e10994109755819 |
| B tonal banner | tertiary banner card, filled sign-in | capsule; tinted header bands | `Sample data · repo · age` | 033ea7347d6f35f8 |
| C top strip | full-bleed strip on top | SAMPLE band on top | `SAMPLE · repo` | abe1acc796e5898c |
| D header badge | SAMPLE pill + "Sign in" in the header | SAMPLE pill by the name | `repo · age · sample` | 0939a814d10794d3 |
| E bottom bar | persistent bottom bar | footer on tall and stack | `Sample data · open to sign in` | 49c5cd22b9d91eaf |

Fidelity notes given with the round:

- **Colour tokens:** on this OS the app's dynamic `tertiaryContainer` is
  vivid purple (white text), while Glance widgets render a pale pink.
- **Tall widget rows:** blank in the picker (Glance lazy lists need a
  launcher); the header and footer are real.
- **A at phone width:** A's row wraps its sentence into a narrow column at
  443 dp and font scale 1.2.
- **C's compact clip:** C's first build clipped the compact `to review` row.
  Repaired before judging, by reserving the strip height; recaptured as
  `abe1acc7`.
- **Picker build gate:** the first gate run on the picker build failed detekt
  (line wrapping, complexity, a reused content slot, and `LocalSampleMarker`
  missing from the CompositionLocal allowlist). Fixed with refactors only.
  The gate passed and the Fold was reinstalled with `266c1e10…` before the
  maintainer judged.

**Maintainer: B.** C, D and E are rejected; A is kept only as the seam's
pre-lock value.

## Implementation

- `ui/theme/SampleMarker.kt`: `SampleMarker { CHIP_ROW, BANNER }`, `Default =
  BANNER`, `LocalSampleMarker` (added to the detekt CompositionLocal
  allowlist), `sampleTone()` = dynamic tertiary / tertiaryContainer /
  onTertiaryContainer.
- `ui/SampleMarks.kt`: `SampleBanner` (16 dp tonal `StatusBanner`, SAMPLE
  section label, "These repositories are made up. Sign in to see your own
  GitHub.", tonal primary "Sign in with GitHub"; tags `repoglance:sample-bar`,
  `repoglance:sample`, `repoglance:sample-sign-in`) and the pre-lock
  `SampleChipRow`. `LiveRepoGlanceScreen.SampleModeBar` dispatches on the seam.
- `widget/SampleWidgetMarks.kt`, `RepoWidget.kt`, `StackWidget.kt`: compact
  `sample` in a tertiary capsule; tall and stack header bands on
  `tertiaryContainer`; live widgets unchanged.
- `tile/TileText.kt`: `Sample data · <repo> · <age>` by default.
- C/D/E production code removed; the picker is trimmed to A (pre-lock) and B
  (locked) with the round recorded in its KDoc.
- Tests: `SampleMarkerTest` (5: default is B, banner keeps the verify tags and
  a sign-in button, tone is tertiary not a status hue, compact capsule renders
  `sample`, live compact shows its clock), `TileTextTest` (default wording +
  pre-lock wording).
- Docs: `docs/design.md` v7 (sample marker section, rejected list),
  verify-skill `sample-mode.md`.

## Gate

- `./gradlew assembleDebug check`: BUILD SUCCESSFUL
  (`runs/build-runs/sample-marker-check-1.log`); 313 debug unit tests,
  0 failures. Debug APK
  `b9f49b5ead390a58da6e9f9bb5b018a50b49e92fc7921b270de5b23296c6baf0`.
- Mutations (`runs/build-runs/sample-marker-mutations.log`), all caught on an
  assertion: default back to the chip row; banner without the sign-in tag;
  compact never shows the capsule; banner on `errorContainer`; tile keeps the
  pre-lock prefix.

## Emulator (approved `EMULATOR37X1X11X0`, Android 16, signed out; APK `b9f49b5e`)

Run dir `runs/verify-repoglance-runs/sample-marker-040-emu/`.

- **Catalog:** `repoglance:sample-bar`, `repoglance:sample` = `SAMPLE`, `These
  repositories are made up. Sign in to see your own GitHub.`,
  `repoglance:sample-sign-in` = `Sign in with GitHub`, `@saariuslystoned ·
  7 repositories`; no `repoglance:live`. Here the banner is light lavender
  with dark text. Capture `catalog-light` `03e5e7f1e8bc5a04`.
- **Repo view (`saari-co/rocket`):** `repoglance:live-home`, the same
  banner, `Issues`. Capture `repo-light` `cd19b88cae988a15`.
- **Widgets:** placed through the fixture home (`Pin stack widget`, `Pin repo
  widget` → setup, `Sample data:` note, saved `saari-co/rocket`). Home:
  compact `rocket` with `sample` in a lavender capsule, `issues 5`, `PRs 3`;
  stack `Pinned · 1` on a lavender band, row `saari-co/rocket` `sample`
  `issues 5 · PRs 3 · review 0`. Capture `home-sample` `8be316bc1bf40df8`.
  A resize to the tall size was blocked by widgets below, which this run did
  not place and did not move; the tall band is proven on the Fold below.
- **Tile:** `add-tile`, shade `content-desc` = `RepoGlance, sample data,
  latest push to saari-co/rocket updated 25m ago` (narrow slot, icon only;
  subtitle wording from `TileTextTest`).
- **Exit:** tap `repoglance:sample-sign-in` in the banner:
  `repoglance:connect-github` and `repoglance:explore-sample`, no device code.
  Home: my repo widget reads `Tap to choose a repository`, the stack reads
  `Pinned · 0`.
- **Cleanup:** removed only the widgets this run placed (app-widget ids 7
  and 8); ids 4 and 5 predate the run and were left. Tile removed, signed
  out and not in sample mode, emulator shut down.
- **Debug picker ANR:** launching the picker on the emulator ANR'd twice
  (`No response to onStartJob`, 19:27 and 19:29). Its main-thread rendering
  starved the widget update jobs on the slow emulator. Earlier in the same
  run, the production steps (sample screens, setup, placed widgets) logged no
  ANR. The picker is debug tooling; recorded, not fixed.

## Fold, final build (`59151FDCG000JA`, Android 17, signed in; APK `b9f49b5e`)

The picker trimmed to A/B (only `picker-candidate-A` and `-B` exist), B
locked:

- The tall header band reads `saari-co/rocket` / `ISSUES 5 · PRS 3 · sample`
  on the tinted band, the stack reads `Pinned · 3` on the band, the compact
  capsule reads `sample`, and the tile wording reads
  `Sample data · saari-co/rocket · 25m`.
- Light capture: `picker-B` `b535a61b495541a8`.
- Dark preview (picker toggle, no device setting changed): the app banner
  turns bright pink with dark text, while the widget bands turn dark purple
  with light text. Capture: `picker-B-dark` `25c1212cba1dcf9f`.
- On Android 17, Compose's dynamic tertiary container runs opposite to
  Glance's in both modes. Text stays readable either way.
- The Fold was returned to the launcher.

## Not proven here

- **Tall repo widget on a launcher:** proven in the picker (production
  `TallContent`); the launcher-placed resize was blocked.
- **Tile subtitle pixels:** the tile sat in a narrow slot.
- **Physical signed-out phone:** sample mode needs a signed-out device, and
  agents never sign out. The physical Fold ran only the picker.

## Review (independent agent, exact source)

Scope: `git diff --no-color --full-index <base> <tree> -- . ':!.grilltrack'`.

| Round | Tree | Diff sha256 | Result |
| --- | --- | --- | --- |
| 1 | `ebdcf8a2022b0a634b26d416a47fb95cd7786f25` | `8af2d1afb0bded779123a60818f22440d3236f831aec952b924f7f9342b952df` (17 files, +847 −46) | 5 P3 |
| 2 | `5a46faf22485e28f47f356590bd20d669220ae07` | `ba9e412287f5f4dca8f48a82d601b4f3c158d9c7eda21391fb1189225404d9ed` (18 files, +851 −47; delta `a7d7312d…`, 2 files) | **clean** |

The identity hash matched. The reviewer also ran the unit tests in an isolated
checkout of the tree: 313 tests, 0 failures.

| # | Finding | Class | Action |
| --- | --- | --- | --- |
| 1 | `quick-settings-tile.md` still expects `Sample · <repo> · <push age>` | required_fix | Now `Sample data · …` |
| 2 | The debug picker's app canvas writes the phone's real sample pins on a thumbtack tap | defer | Gotcha added to the picker KDoc; debug only, live data never touched |
| 3 | The picker ANR: `GlanceRemoteViews.compose` and `RemoteViews.apply` run on Main next to the full screen | defer | Debug only; the production steps logged no ANR |
| 4 | The picker opens on A by default, its KDoc says `<A..E>`, and its short question still says "deciding:" | required_fix | Default B, `<A|B>`, "decided:" text; the Fold opens on `B tonal banner (locked)` with no letter (APK `1334d0b5…`) |
| 5 | The tile reads `Sample`, not `Sample data`, when there is no latest push | reject_false_positive | Unreachable with the fixed sample list (predates this change) |

The reviewer's answers:

- **(a) Band vs stale roles:** no conflict is reachable. Sample readers never
  set a rate limit and always build EXACT snapshots, and live widgets are
  unchanged.
- **(b) Guards:** none broke.
- **(c) Seam in the picker:** used correctly.

The gate after the repairs: `runs/build-runs/sample-marker-check-2.log`
green.

Round 2: clean. Both identity hashes matched. Repairs 1 and 4 are correct
and complete, and the delta touches only the debug source set and the
verify-skill docs (no production code, test or baseline).
