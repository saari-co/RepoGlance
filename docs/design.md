# Design contract: RepoGlance

- **Version:** 0 (2026-09-21), started by the `family-look` grill. Nothing in
  the unresolved section is decided.
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
- **Status semantics (code, not yet colour):** `render/CiSemanticRole.kt`
  maps CI to POSITIVE / NEGATIVE / IN_PROGRESS / NEUTRAL; rate-limit buckets
  map to the same roles (`SnapshotRendering.rateLimitRole`). Today the
  catalog paints POSITIVE `primary`, NEGATIVE `error`, IN_PROGRESS
  `tertiary`, NEUTRAL `outline`, and rate-limit banners `errorContainer`.

## Unresolved (decided by GrillTrack, one slot per round)

- **Status colour [status-colour-029, round `family-look-round-1`]:** how the
  four status roles get their colour. Candidates A–E are in
  `.grilltrack/work/picker/family-look-round-1.json`; nothing is chosen.
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
