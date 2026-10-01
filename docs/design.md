# Design contract: RepoGlance

- **Version:** 9 (2026-09-30): Settings and the Widgets screen
  [settings-044, widgets-entry-045, widgets-look-047]. Version 8
  (2026-09-30): in-app theme choice [theme-choice-046].
  Version 7 (2026-09-30): sample marker, tonal banner
  [sample-marker-040]. Version 6 (2026-09-30): sample widgets and the
  unconfigured widget [sample-widgets-039]. Version 5 (2026-09-29): compact widget crowding
  [compact-crowding-034]. Version 4 (2026-09-28): widget freshness and labels
  [widget-look-032]. Version 3 (2026-09-28): shape and control language
  [shape-control-031]. Version 2 (2026-09-28): label typography
  [label-typography-030]. Version 1 (2026-09-22): status colour [status-colour-029]. Version 0
  (2026-09-21) started the file with constraints only. Nothing in the
  unresolved section is decided.
- **Canonical path:** `docs/design.md`. The maintainer asked for `design.md`;
  it lives under `docs/` because `REPO_HYGIENE.md` keeps root markdown to the
  front-door set. This is the one current design language for the RepoGlance
  app, widgets and tile.
- **Decision history:** `.grilltrack/ledger.json` (ids in brackets). Where they
  disagree, the ledger owns history and this file owns the current,
  implementable design.
- **Scope:** the Android app (catalog, navigator, sign-in, checking), the
  Glance widgets, the Quick Settings tile and the launcher icon.

## Intent and constraints

- **Purpose:** a read-only, widget-first GitHub glance for Pixels. Every
  surface answers "what changed and is it healthy" in one look, and shows how
  old that answer is.
- **Audience:** the maintainer and anyone who installs the public app on a
  Pixel, phone or foldable, light or dark.
- **Ambition (maintainer, 2026-09-21):** compete with the Gemini app and
  Google's own apps on feel; read as the same dev crew as Swarm Intercom
  without borrowing Intercom's dark-only "night glass" style.
- **Hard constraints:**
  - Truth rules from `AGENTS.md` come first: Unknown never renders as zero,
    stale is labelled with its age, rate limit is visible first-class state.
  - Material 3 dynamic colour is the base on API 31+ with the M3 baseline
    scheme as the fallback (`app/src/main/java/co/saari/repoglance/ui/theme/Theme.kt`).
    Custom semantic colours must be assigned by role and harmonised with the
    dynamic scheme (Google theming guidance, queried 2026-09-21).
  - No Google brand assets: no Gemini sparkle, no four-colour sweep.
  - Picker and evaluation code lives in `app/src/debug` and never ships.
  - Comments are banned in `app/src/main` Kotlin; lint and detekt baselines
    only shrink.

## Provenance

- Swarm Intercom's contract:
  `~/Developer/side-quests/swarm-intercom/docs/design/intercom-page.md`
  (read-only from here). Family candidates: one colour per state (emerald ok,
  amber working, sky accent), Space Grotesk uppercase labels, capsule
  controls, ring pulse and the M3 spatial-default press spring.
- Google theming guidance (Developer Knowledge, 2026-09-21): tokens and roles,
  dynamic base with a static brand fallback, harmonised semantic colours,
  tonal surfaces, single-colour splash matching the first frame.

## Verified foundations

- **Colour base:** M3 dynamic light/dark colour scheme; M3 baseline below
  API 31. No static brand scheme exists yet (unresolved below).
- **Brand mark [checking-splash-018, cold-start-icon-028]:** the commit-eye
  magnifier. Launcher icon: heavy white stroke with two grey rings on a
  graphite-to-black gradient. Themed icon and Quick Settings tile: a two-ring
  monochrome glyph. In app: `CheckingMark` with ping rings while the session
  is checked; still under reduced motion.
- **App start [cold-start-icon-028]:** the start window matches the M3
  dynamic background on API 34+; API 31–33 is an approximate fallback.
- **Theme choice [theme-choice-046]:** Settings → Appearance → `Theme` shows
  the current choice; a tap opens `Choose theme` with radio buttons `Light`,
  `Dark`, `System default` and `Cancel` / `OK`, the wording and buttons of
  Google Calculator's dialog (seen on the Pixel 10 Pro XL, 2026-09-30).
  `System default` is the default. `OK` stores the choice and applies it as
  Android 12+ per-app night mode (`UiModeManager.setApplicationNightMode`;
  `System default` is `MODE_NIGHT_AUTO`, which follows the phone), so the
  app, its start window, status-bar icons, dialogs and the widget setup
  screen all follow it; `Theme.kt` still reads the configuration. Home-screen
  widgets and the Quick Settings tile stay on the phone's theme. The seam is
  `state/ThemePrefs.kt` and `ui/settings/ThemeSetting.kt`; Settings itself
  is [settings-044].
  - **Rejected (do not reintroduce without a new grill):** a link to the
    phone's Display settings (changes the whole phone; there is no public
    intent for the Dark theme page); no control (the app already followed
    the phone); a Compose-only override (start window and system bars would
    not follow); widgets following the app; inline segmented buttons.
  - **Proof:** `.grilltrack/proof/theme-choice-046-verify-20260930.md`.
- **Settings [settings-044]:** the three-dot menu (`More options`) holds
  exactly `Widgets` and `Settings` on the live and sample catalogs, and
  `Settings` alone on the Connect screen. Settings is an M3 small top app bar
  with a back arrow over plain list rows (leading outlined icon, headline,
  supporting line; a trailing open-in-new icon for links), on the background
  colour. Section heads are the SECTION mono label in `primary`, sentence
  case: `Appearance`, `GitHub`, `About`. The Widgets row comes first and is
  hidden signed out; GitHub (Manage GitHub access, Disconnect GitHub) shows
  only with a session.
- **Widgets screen [widgets-entry-045, widgets-look-047, "C preview
  tiles"]:** the user-facing names are `Repository widget` and `Pinned repos
  widget` (launcher labels `Repository` and `Pinned repos`); "stack" is never
  shown. Two tonal tiles sit side by side (24 dp, `surfaceContainer`). Each
  has a sketch of the widget's shape (rounded `surfaceContainerHighest` frame,
  a `primary` name bar, `outlineVariant` lines; 2x1 for Repository, 4x3 for
  Pinned repos), the name, one sentence and a full-width tonal `Add`
  capsule. `On your home screen` follows as list rows with icons; repository
  rows carry a chevron and open their setup. The removal hint, sample note
  and how-to are `bodySmall` in `onSurfaceVariant`.
  - **Rejected (round 1, do not reintroduce without a new grill):** A add
    cards above plain rows; B a settings list with + buttons; D placed widgets
    first with one Add button and a chooser dialog; E a card per widget kind
    holding its placed widgets.
  - **Proof:** `.grilltrack/proof/widgets-look-047-verify-20260930.md`.
- **Typography, body:** M3 default type scale on the system font for
  titles, row text and reading text.
- **Typography, labels [label-typography-030, "mono labels"]:** every label
  is set in the system monospace (`FontFamily.Monospace`), sentence case, no
  tracking, no bundled font. The seam is `ui/theme/LabelType.kt`: label sites
  call `LabelText(text, LabelRole.X)`, which merges the role's style over
  the site's base style; `LabelType.Mono` is the production default and
  `LabelType.Material` is the pre-lock M3 look.

  | role | covers | size / weight |
  | --- | --- | --- |
  | SECTION | "Issues", "PRs", "Sort", "Fixture state" heads | 12 sp / 600 |
  | CHIP | Cached · age, Unknown, Draft, Review requested, LIVE, sort chips, status pills | 12.5 sp / 400 |
  | META | data-age lines, pushed/updated lines, rate-limit lines and banners | 11 sp / 400 |
  | BUTTON | button, text-button and segmented-button labels | 14 sp / 500 |

  - Menu items, text fields, titles, row text, the sign-in code and dialog
    bodies are not labels and stay M3.
  - **Rejected (round 2, do not reintroduce without a new grill):** A M3
    default; B Intercom's uppercase tracked structure on the system font;
    C Space Grotesk bundled for labels (APK weight and an OFL file); E
    heavier, larger sentence-case "expressive" labels.
  - **Proof:** `.grilltrack/proof/family-look-round-2-captures-20260928.md`
    (picker) and `.grilltrack/proof/label-typography-030-verify-20260928.md`
    (production).
- **Shape and control language [shape-control-031, "Google tonal"]:**
  controls are tonal and borderless; there are no outlines on the seam's
  controls. The seam is `ui/theme/ControlShape.kt`: sites call
  `ControlChip`, `StatusPill`, `StatusBanner`, `PrimaryButton`,
  `SecondaryButton` and `ControlCard`, which read `LocalControlShape`;
  `ControlShape.Tonal` is the production default and `ControlShape.Material`
  is the pre-lock M3 look.

  | control | shape | fill / edge |
  | --- | --- | --- |
  | chips (Cached, Unknown, Draft, Review requested, LIVE) | 8 dp | `surfaceContainerHigh`, no edge |
  | status pills | capsule | status container, no edge |
  | primary buttons | capsule | filled tonal (`secondaryContainer`) |
  | secondary buttons (pin widgets, connect on error) | capsule | no fill, no edge, primary text |
  | rate-limit banners | 16 dp | status container, no edge |
  | repo cards (fixture and live catalog) | 24 dp | `surfaceContainer`, flat, no edge |

  - Outside the seam and still M3 (a follow-up, not decided): the sort
    FilterChips, the navigator and widget-config segmented buttons, the
    widget-config button, text buttons, and the navigator / live issue and
    PR rows (flat rows with dividers).
  - **Rejected (round 3, do not reintroduce without a new grill):** A M3
    default; B Intercom outlined capsules with state edges; D squared
    terminal (4–8 dp, hairline and state edges); E filled capsules with
    chamfered cards.
  - **Proof:** `.grilltrack/proof/shape-control-031-verify-20260928.md`.
- **Widgets [widget-look-032, hybrid "C compact, D tall/stack"]:** the
  repo and stack widgets carry the family status meanings on their
  freshness line. The seam is `widget/WidgetLook.kt` (`LocalWidgetLook`,
  `LocalWidgetTones`, `FreshnessText`, `TallLook`); `WidgetLook.Family` is
  the production default and `WidgetLook.Material` is the pre-lock look.
  Tones come from `FamilyStatus` over the dynamic light and dark schemes as
  day/night `ColorProvider`s.

  | state | role | compact widget | tall repo + stack widgets |
  | --- | --- | --- | --- |
  | fresh | none | onSurfaceVariant | onSurfaceVariant |
  | last good | working (amber) | tonal capsule, regular | ink, bold |
  | rate limited | failing (red) | tonal capsule, regular | ink, bold |
  | no data | neutral | tonal capsule, regular | ink, bold |

  - Labels: the compact widget's repo name, ledger labels and freshness
    line are mono; tall and stack widgets stay on the system face.
  - The stack header is not coloured; stack row ages ignore the rate limit
    (the header carries it), as before.
  - **Compact layout [compact-crowding-034, "merged counts"]:** fresh, the
    compact widget keeps the name, clock and issue / PR rows. Stale, the
    capsule takes its own row under the repo name and the counts merge onto
    one bold 9 sp mono line ("12 issues · 4 PRs"); "to review" keeps its own
    row when the widget is at least 84 dp tall. `CompactLayout.MERGED_COUNTS`
    on `WidgetLook.Family`.
  - **Widget floor:** `repo_widget_info.xml` `minWidth` is 140 dp and the
    compact responsive size is 140x64. With a 24-hour clock every state fits
    from 139 dp; with a 12-hour clock the capsule can truncate (see
    Unresolved). The Fold's 2x1 cell is 148x89 dp.
  - **Rejected (round 5):** A inline; B capsule own row with separate count
    rows (clips at 64 dp); C short words, name first (capsule truncates to
    "ca…"); E responsive bottom band (clips at 64 dp).
  - **Rejected (round 4):** A M3 error; B family ink + mono everywhere;
    E tinted header band. C and D survive only as the compact and
    tall/stack halves of the hybrid.
  - **Proof:** `.grilltrack/proof/widget-look-032-verify-20260928.md`.
  - **Sample widgets [sample-widgets-039]:** wherever a live widget shows
    the clock time of its data, a sample widget shows the word `sample`:
    the compact freshness slot, the tall header in place of `as of`, and
    each stack row. Sample data was never fetched and never refreshes, so
    no clock is shown. Its look is the sample marker below.
  - **Unconfigured repo widget [sample-widgets-039]:** `RepoGlance` over
    `Tap to choose a repository`; a tap opens that widget's setup. It
    replaced the stale `FIXTURE PREVIEW` label.
- **Sample marker [sample-marker-040, "B tonal banner"]:** sample mode is
  marked with the dynamic `tertiary` role, never a status hue. The seam is
  `ui/theme/SampleMarker.kt` (`LocalSampleMarker`, `sampleTone()`):
  `SampleMarker.BANNER` is the production default and `SampleMarker.CHIP_ROW`
  is the pre-lock look.

  | surface | marker |
  | --- | --- |
  | catalog and repository view | 16 dp tonal banner (`tertiaryContainer`) under the header: SAMPLE (section label), "These repositories are made up. Sign in to see your own GitHub." and a tonal primary "Sign in with GitHub" |
  | compact repo widget | `sample` in a tertiary capsule (8 dp, mono 8 sp) in the time slot |
  | tall repo widget | tertiary header band; the header line reads `ISSUES n · PRS n · sample` |
  | stack widget | tertiary header band; each row keeps the plain `sample` |
  | Quick Settings tile | subtitle `Sample data · <repo> · <push age>` |

  - On Android 16 the app's dynamic `tertiaryContainer` renders vivid purple
    while Glance widgets render the older pale-pink system token; seen and
    accepted in the picker.
  - **Rejected (round 1, do not reintroduce without a new grill):** A chip
    row (the provisional look, kept only as the seam's pre-lock value);
    C full-bleed top strip; D header badge; E persistent bottom bar.
  - **Proof:** `.grilltrack/proof/sample-marker-040-verify-20260930.md`.
- **Status colour [status-colour-029, "family tonal"]:** four meanings, one
  hue each, shared with Swarm Intercom and harmonised with dynamic colour.
  `render/CiSemanticRole.kt` maps CI to POSITIVE / NEGATIVE / IN_PROGRESS /
  NEUTRAL and `SnapshotRendering.rateLimitRole` maps rate-limit buckets to
  the same roles (LOW = working, EXHAUSTED = failing).

  | meaning | role | family hue | source |
  | --- | --- | --- | --- |
  | ok | POSITIVE | emerald `#22C55E` | Intercom `--ok` |
  | working | IN_PROGRESS | amber `#F59E0B` | Intercom `--warn` |
  | failing | NEGATIVE | red `#EF4444` | RepoGlance's own; Intercom has no failing state |
  | neutral | NEUTRAL | `outline` on `surfaceVariant` | Material, unharmonised |

  - **Harmonise:** each family hue leans towards the dynamic `primary` by
    half the hue difference, capped at 15 degrees (Google's harmonise rule),
    computed in CIELCh through `androidx.core.graphics.ColorUtils`
    (`ui/theme/StatusColors.kt`, `FamilyStatus`). This is not HCT; it needs
    no new dependency and was judged close enough on the Fold. An achromatic
    primary (monochrome theme, chroma below 5) leaves the hues unharmonised.
    Revisit if the tint ever looks off against a wallpaper.
  - **Tones:** each status is a `StatusTone` of ink, container and
    on-container. Light: ink = the harmonised hue; container L 92 / C 24;
    on-container L 30 / C 45. Dark (background luminance below 0.5): ink L
    capped at 78; container L 30 / C 30; on-container L 90 / C 20.
  - **Application:** status is a tonal pill (`CircleShape` container, 8 dp
    ink dot, CHIP-role mono on-container text) on catalog cards; rate-limit
    banners use the container/on-container pair; the live screen's
    rate-limit line uses the ink. Saturated dots are gone.
  - **Rejected (round 1, do not reintroduce without a new grill):** A dynamic
    roles (ok/working indistinct on blue wallpapers); B fixed hues
    unharmonised; C harmonised dots; D ink-only monochrome.
  - **Proof:** `.grilltrack/proof/family-look-round-1-captures-20260921.md`
    (catalog, navigator banner and live rate-limit ink all seen on the Fold).

## Unresolved (decided by GrillTrack, one slot per round)

- **Compact capsule with a 12-hour clock [compact-12h-fit-035, deferred]:**
  at 140x64 and at the Fold's 148x89, "last good <day> <h:mm AM>" (data
  older than today) and "rate limited · <h:mm AM>" truncate, hiding part
  of the data age. Shipped as is by maintainer choice; open risk against
  truth rule 3.
- **CI on widgets:** needs a live CI read first (the live store has no CI).
  Not grilled.
- **Tile state:** the system draws the tile, so only icon, active state and
  subtitle wording can change. Not grilled beyond sample mode, where the
  subtitle reads `Sample data · <repo> · <push age>` [sample-marker-040].
- **Shape on the remaining M3 controls:** sort chips, segmented buttons,
  text buttons and list rows kept M3 in round 3. Not grilled.
- **Motion signatures:** press spring, ring pulse beyond the checking mark.
  Not grilled.
- **Settings and Widgets on large screens:** on the Fold's inner display
  both screens span the full width, rows and tiles included. Not grilled.
- **Static brand fallback scheme** for devices without dynamic colour. Not
  grilled.

## Verification expectations

- Judge every visual candidate on the real app on an approved Pixel
  (`.claude/skills/verify-repoglance/devices.tsv`) in both light and dark,
  with the production dynamic scheme, the MIXED fixture and prior locks on
  the same canvas.
- Committed proof is text only under `proof/` or `.grilltrack/proof/`;
  screenshots stay in ignored `runs/`.
