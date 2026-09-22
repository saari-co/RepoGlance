# family-look — grill handoff (2026-09-21)

Next GrillTrack grill on track `gt-20260728163459-227573`. The maintainer
chose it on 2026-09-21, right after `cold-start-icon-028` was delivered (PR #34,
merged as `05c21fd`), and asked for it to run in a fresh session. This file
has everything that session needs, with no dependence on the conversation
that wrote it. **Nothing about this grill is decided yet.** The grill itself
decides.

## The ask, in the maintainer's words (paraphrased)

- RepoGlance should be a cutting-edge app whose UI/UX competes directly with
  the Gemini app and other Google apps.
- RepoGlance and Swarm Intercom should feel like they come from the same dev
  crew, but each stays unique. **Do not steal Swarm Intercom's "night glass"
  style.** Find the features that tie the two together.
- "I don't want to veer too far off course": keep the grill bounded.

## Facts to start from

- **RepoGlance's look today:** Compose Material 3 with **dynamic colour**
  (`ui/theme/Theme.kt`: `dynamicLightColorScheme`/`dynamicDarkColorScheme`
  on API 31+, M3 baseline fallback), light and dark. The start window now
  matches the M3 dynamic background on API 34+ (028). There is no
  `design.md` or other design contract in the repo.
- **Brand mark (locked, 018 and 028):** the commit-eye magnifier. White heavy
  stroke with two grey rings on a graphite-to-black gradient launcher icon; a
  two-ring monochrome glyph for themed icons and the Quick Settings tile;
  `CheckingMark` with ping rings in the app.
- **Swarm Intercom's design contract:**
  `~/Developer/side-quests/swarm-intercom/docs/design/intercom-page.md` (read
  it, but do not edit that repo). It is a web page, dark only, "night glass":
  - background `#070b14` with a navy radial gradient, and translucent blurred
    glass surfaces;
  - one colour per state: emerald `#22c55e` for listening/ok, amber `#f59e0b`
    for working/warn, sky `#38bdf8` for speaking/accent;
  - fonts: Inter (reading), Space Grotesk (labels: uppercase 11 px section
    heads, chips, button labels) and Space Mono (code);
  - capsule controls with glowing outlines, a state edge glow, and motion
    tuned to the Gemini app, including the M3 "spatial default" spring for
    press and release;
  - its own rules forbid Google brand assets (no Gemini sparkle, no
    four-colour sweep).
- **Google guidance** (Developer Knowledge query, 2026-09-21):
  - assign colours by token and role;
  - keep dynamic colour as the base with a static brand fallback scheme;
  - harmonise custom semantic colours (status or error colours) with the
    dynamic scheme;
  - use tonal surfaces;
  - the splash is one colour matching the first frame.
- **Starting candidates for shared signatures** (a starting point, not a
  shortlist):
  - the same state-colour meanings (ok/working/active), harmonised with
    dynamic colour;
  - a shared label typeface or label style (such as Space Grotesk uppercase
    section heads);
  - the black/grey/white mark language and ring motif;
  - shared motion signatures (ring pulse, press spring);
  - the capsule shape language.

  Keep dynamic colour as RepoGlance's base unless the maintainer decides
  otherwise.

## Process notes for the grill

- Follow the grilltrack skill: read `.grilltrack/ledger.json` first and resume
  (`python3 ~/.claude/skills/grilltrack/scripts/grilltrack_ledger.py --project . resume --activation implicit`).
  This is a visual decision, so load the frontend pack (`picker.md`, plus
  `layout.md` or `typography.md` for the chosen slot). Present exactly five
  materially distinct candidates, one active slot at a time, on the real app,
  with earlier locks kept.
- Scope first: agree with the maintainer on which slot(s) this grill covers
  before building candidates. Examples: the state-colour system, the label
  typography, or the section-head style. It is probably one slot per round.
- The picker must say what is being decided, per the maintainer's feedback on
  earlier pickers. It must stay development-only (`app/src/debug`) and never
  ship.
- Consider whether RepoGlance should start a canonical design contract
  (`docs/design/…`) that the locks feed. Swarm Intercom keeps one; ask the
  maintainer before creating it.

## Device and proof rules (learned in 028)

- Approved phones: `.claude/skills/verify-repoglance/devices.tsv`. Always pin
  `VERIFY_SERIAL`.
- The Pixel 10 Pro Fold `59151FDCG000JA` now runs **Android 17 (API 37)**,
  build `CP2A.260805.005`. On 2026-09-21 the maintainer left it in **light mode
  with themed icons on** and the RepoGlance Quick Settings tile added.
- Never change system settings (dark mode, themed icons) and never sign in or
  out. Ask the maintainer.
- Check that `dumpsys trust` shows `deviceLocked=0` before driving. Never type
  onto a lock screen.
- Crop every capture to RepoGlance's own UI and fail closed. Use a
  uiautomator dump before and after the capture; they must agree, or save
  nothing.
  - `screencap`/`screenrecord` need `-d 4619827677550801152` /
    `--display-id 4619827677550801152` on this Fold.
  - **Launcher trap:** a hidden home-screen "Predicted app: RepoGlance" node
    stays in the dump behind the app drawer. Filter launcher cells on the
    exact label plus description, or crop the wrong app.
- Screenshots stay in ignored `runs/`. Committed proof is text only.

## Delivery rails

- Commit on the builder's own branch; this handoff sits on
  `claude/grilltrack-028-delivery`, and continuing on it is fine.
- That branch also carries the unpushed 028 delivery record (`2b51cc7`);
  deliver it with the grill's PR.
- A PR needs the maintainer's request. Merging needs his request plus:
  - CI green on the head;
  - OpenClaw autoreview via x-api
    (`bin/smoky lane run spark-openclaw-materialize-worktree --repo <worktree> --ref <head> --base <fork-point> --pr-url <pr>`,
    then `spark-openclaw-autoreview --queue --operator-id bobby --mode branch --base <fork-point> --remote-worktree <path>`);
  - ClawSweeper (`@clawsweeper review` from the maintainer's account now works
    through the spark-dgx relay) on that exact head, with `proof: sufficient`,
    `status: ready for maintainer look`, and no unresolved P0/P1.
