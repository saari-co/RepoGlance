# widget-look-032 — picker and verification (2026-09-28)

- **Track:** `gt-20260728163459-227573`, family-look round 4, slot
  `widget-freshness-and-labels`.
- **Scope (maintainer, 2026-09-28):** widget freshness line and widget
  label face. Widget CI deferred (the live store hard-codes CI to UNKNOWN);
  the tile follows as a separate question.
- **Baseline:** `main` at `ee30262ece6f51c97563d9054a25f709d33bc826`.
- **Device:** Pixel 10 Pro Fold `59151FDCG000JA`, inner display 2076x2152,
  `deviceLocked=0` checked before each run.
- **Renderer:** `WidgetLookVariantPickerActivity` composes the production
  `CompactContent`, `TallContent`, `StackHeader` and `StackRow` through
  `GlanceRemoteViews` at declared sizes (compact 120x64 and 250x90, tall
  250x140, stack 250x170); dark re-renders under a night-mode configuration
  context.
- **Gates on every kept capture:** UI tree dumped before and after (equal
  after settling), only package `co.saari.repoglance`, trace text present
  ("On canvas: widgets <letter> · <canvas> · <light|dark>"). PNGs stay in
  ignored `runs/`.

## Round of five (run `widget-032-picker`, light)

Manifest `.grilltrack/work/picker/family-look-round-4.json` validated.

| candidate | compact png (16) | tall + stack png (16) |
| --- | --- | --- |
| A M3 error | 20a2d636ecdf9fce | 45f72c5d92cd3c82 |
| B family ink + mono | 5ec5ee537e912dc1 | 80f3c6171827e71b |
| C tonal capsule + mono | 80d741545644be05 | 3ac261c02a3a6ba5 |
| D family ink, system face | 5155e2744fe3408d | db7b4dd28f4a4335 |
| E tinted header + mono | 76e5ee5e960fc44e | 5a895b4de38b9d28 |

Maintainer: C for compact, leaning D for tall and stack. Per GrillTrack, the
precise hybrid replaced the five for confirmation.

## Hybrid H (runs `widget-032-hybrid` light, `widget-032-verify` dark)

| capture | seen | png (16) |
| --- | --- | --- |
| H compact, light | tonal amber "last good" capsule, red "rate limited" capsule, neutral "no data" capsule, mono labels | 67b9a4abf70c788c |
| H tall + stack, light | tall header line in amber / red family ink, bold, system face; stack "last good" ages in amber ink | 0521b6e1f28f5465 |
| H compact, dark | same capsules on dark containers | 3f7a34d249da0091 |
| H tall + stack, dark | same inks, dark-capped | 4831e532e219707b |

Maintainer confirmed H ("1; build it"). `WidgetLook.Family` is the
production default; `WidgetLookTest` asserts the default and the role
mapping (rate limited = failing, last good = working, no data = neutral,
fresh = uncoloured).

## Fidelity gaps and open risk

- The tall widget's feed list renders blank in the picker (a Glance
  `LazyColumn` needs a launcher host); only the header band was judged.
- The stack preview crops its third (no data) row at 170 dp.
- Compact at 120x64: the last-good / rate-limited capsule fills the top row
  and pushes the repo name off. Accepted as a known follow-up round.
- Stale colours on a launcher-placed widget: the live data was fresh at
  capture time, so only the fresh state was seen there (below).

## Launcher-placed widgets (run `widget-032-place`, maintainer-authorized)

Maintainer authorized placing the widgets. From the fixture home, "Pin repo
widget" → launcher "Add to home screen" → config: repository
`saari-co/RepoGlance`, feed BOTH, "Save widget" (this also pins the repo in
RepoGlance); then "Pin stack widget" → "Add to home screen".

| widget | tree text (before = after) | seen | crop png (16) |
| --- | --- | --- | --- |
| repo, compact | RepoGlance · 6:48 PM · issues 3 · PRs 0 | mono repo name, labels and clock; fresh (uncoloured); counts bold system face | a1ce73d54dda2cf7 |
| stack | Pinned · 1 · saari-co/RepoGlance · 6:48 PM · issues 3 · PRs 0 · review 0 | system face throughout; fresh age uncoloured | 204935b71a7cba7c |

- Gate: launcher dumps before and after equal for both RepoGlance host
  views; the full-screen PNG (other home-screen content) was deleted and
  only the two widget crops were kept.
- The notification shade was open at the start of this run; it was
  collapsed with `cmd statusbar collapse` (no setting changed).
- `./gradlew assembleDebug check`: green.
