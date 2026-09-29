# compact-crowding-034 — picker and verification (2026-09-29)

- **Track:** `gt-20260728163459-227573`, family-look round 5, slot
  `compact-widget-crowding`.
- **Scope (maintainer):** levers layout, shorter wording, merged count
  rows, size-responsive. Capsule colours (widget-look-032) and the tall and
  stack widgets unchanged.
- **Baseline:** `main` at `580f6ad16a6b89e11ea499b1be2b418fc353c85e`.
- **Device:** Pixel 10 Pro Fold `59151FDCG000JA`, inner display 2076x2152,
  390 dpi (2.4375 px/dp), `deviceLocked=0` checked before each run.
- **Renderer:** `CompactVariantPickerActivity` composes the production
  `CompactContent` through `GlanceRemoteViews` in four data states (fresh,
  last good 3d, rate limited, no data).
- **Gates on every kept capture:** UI tree before and after equal after
  settling, only package `co.saari.repoglance`, trace text present
  ("On canvas: compact <letter> · <canvas> · <light|dark>"). PNGs stay in
  ignored `runs/`.

## Round of five (run `compact-034-picker`, light; floor then 120x64)

Manifest `.grilltrack/work/picker/family-look-round-5.json` validated.

| candidate | floor png (16) | mid + wide png (16) | seen at 120x64 |
| --- | --- | --- | --- |
| A inline (shipped) | e0f8dc7eb621b26c | 8903cc879d55f6d0 | stale capsule pushes the name to "…" |
| B capsule own row | 1a7ab418ece31cdc | 654e7128a5bcecee | PRs row clipped at 64 dp tall |
| C short words, name first | 764c4e7978ffb43b | 36cef6618ae40a03 | capsule truncates to "ca…" (clock hidden) |
| D merged counts | da22805f8907a59e | 20a6e728cb54f1bf | name kept; capsule clock and PR count truncated |
| E responsive bottom band | 8c69ca81a80dc31c | 952fc50c674e5258 | bottom capsule clipped at 64 dp tall |

- D was re-captured after fixing a self-inflicted defect (merged counts
  applied when fresh at 11 sp and overflowed); the hashes above are the
  fixed D.
- Maintainer named C, then chose D, and chose to raise the floor.

## Floor sizing

- 136x64 (run `compact-034-floor`, f43cdbc6ee751071): every D state fits
  except the rate-limited clock ("rate limited · 09:…").
- 139 dp and wider: every D state fits.
- The Fold's placed 2x1 cell measured 362x217 px = 148x89 dp (an earlier
  note said 139x83 using the wrong density). Maintainer approved
  `android:minWidth` 110 -> 140 dp and the compact render size 120x64 ->
  140x64; the Fold keeps 2x1.

## Production D (run `compact-034-verify`, `WidgetLook.Family`)

| capture | seen | png (16) |
| --- | --- | --- |
| floor + placed, light | 140x64 and 148x89: name on top, full capsule ("last good Sat 10:17", "rate limited · 10:07", "no data"), "12 issues · 4 PRs"; at 148x89 "to review 1" on its own row | 263d897f1a8374fd |
| floor + placed, dark | same, dark containers | 64090e81490e0c69 |
| mid + wide, light | 180x64 and 250x90 fit | 1d3104cfe0603212 |

- A second defect was found and fixed during verification: at 148x89 the
  merged line appended "· 1 to review" and truncated; "to review" now keeps
  its own ledger row when the widget is tall enough.
- `WidgetLookTest` asserts the default layout and that the capsule takes
  its own row only when stale. `./gradlew assembleDebug check`: green.

## Launcher-placed widget (run `compact-034-placed`, maintainer-authorized)

- The placed repo widget had been resized to ~148x191 dp outside this
  session; with Bobby's authorization it was resized back to 2x1 by
  `bin/verify-repoglance widget-resize repo -255` (long-press, drag the
  bottom handle): bounds [657,1182][1018,1399] = 148x89 dp.
- States were seeded with the debug-only `WidgetStateSeedActivity`
  (`bin/verify-repoglance widget-state`), which stashes the real records and
  restores them. Counts and observedAt are the real stored values; only the
  basis / rate-limit state is seeded.
- Gate: launcher dumps before and after equal for the RepoGlance host view,
  needle text present; only the widget crop is kept, the full-screen PNG is
  deleted.

| state | widget tree text | crop png (16) |
| --- | --- | --- |
| fresh (restored) | RepoGlance · 8:36 AM · issues 3 · PRs 2 | 2f1d0358e0ad252a |
| last good (seeded) | RepoGlance · last good 8:36 AM · 3 issues · 2 PRs | 204b2501ef96ead7 |
| rate limited (seeded) | RepoGlance · rate limited · 8:36 AM · 3 issues · 2 PRs | 9671f412d9320f17 |
| no data (seeded) | RepoGlance · no data · — issues · — PRs | 69a5dd3b36254176 |

- After `restore`, the real rate-limit prefs were identical to before
  (`bucket OK, resetsAt 2026-09-29T13:28:13Z`), and the fresh capture hash
  matched the first restore.
- **Finding:** on the launcher the rate-limited capsule renders
  "rate limited · 8:36 …": the AM is cut.

## Fidelity gap and re-measure (run `compact-034-12h`)

- The round-5 picker hard-coded a 24-hour clock; the Fold uses 12-hour
  time, so every floor measurement above was optimistic. The picker now
  uses the production `widgetClock(context)`.
- Re-captured D (1dd584eef0d4d9e5) with the device clock: at both 140x64
  and 148x89, "last good Sat 10:36 …" (older than today) and
  "rate limited · 10:26 …" truncate. Fresh, same-day last good, and no data fit.
- The lock premise "every D state fits from 139 dp" holds only for 24-hour
  clocks.
- Maintainer decision 2026-09-29: ship D as is; the 12-hour truncation is
  logged as deferred decision `compact-12h-fit-035` with its risk.

## Not proven / open

- The stack widget reads "Pinned · 0"; that pin change was not made in
  this session and was not touched.
- 12-hour clocks: stale capsule text truncates at the placed size (above);
  deferred as `compact-12h-fit-035`.
- Launcher cell sizes on other phones are untested; a narrow grid may
  default the widget to 3 cells now that the minimum is 140 dp.
