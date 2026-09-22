# Design contract: RepoGlance

- **Version:** 1 (2026-09-22): status colour [status-colour-029]. Version 0
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
- **Typography:** M3 default type scale, system font. Section heads
  ("Issues", "PRs") are `titleSmall`; ages and rate-limit lines are
  `labelSmall`.
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
    ink dot, `labelLarge` on-container text) on catalog cards; rate-limit
    banners use the container/on-container pair; the live screen's
    rate-limit line uses the ink. Saturated dots are gone.
  - **Rejected (round 1, do not reintroduce without a new grill):** A dynamic
    roles (ok/working indistinct on blue wallpapers); B fixed hues
    unharmonised; C harmonised dots; D ink-only monochrome.
  - **Proof:** `.grilltrack/proof/family-look-round-1-captures-20260921.md`.

## Unresolved (decided by GrillTrack, one slot per round)

- **Status colour on widgets and the tile:** the Glance widgets and the
  Quick Settings tile still show status as text only; whether they carry the
  family tones is not grilled.
- **Label typography:** whether RepoGlance adopts a shared label face or
  style with Intercom (uppercase section heads, chips). Not grilled.
- **Shape and control language:** capsules and outlined state edges versus
  M3 defaults. Not grilled.
- **Motion signatures:** press spring, ring pulse beyond the checking mark.
  Not grilled.
- **Static brand fallback scheme** for devices without dynamic colour. Not
  grilled.

## Verification expectations

- Judge every visual candidate on the real app on an approved Pixel
  (`.claude/skills/verify-repoglance/devices.tsv`) in both light and dark,
  with the production dynamic scheme, the MIXED fixture and prior locks on
  the same canvas.
- Committed proof is text only under `proof/` or `.grilltrack/proof/`;
  screenshots stay in ignored `runs/`.
