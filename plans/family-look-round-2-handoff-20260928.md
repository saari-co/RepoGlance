# family-look round 2 — label typography — grill handoff (2026-09-28)

Next GrillTrack grill on track `gt-20260728163459-227573`, chosen by the
maintainer on 2026-09-28 right after round 1 (`status-colour-029`) merged as
PR #36 (`e9d4647`). Run it in a fresh session. **Nothing about this slot is
decided.** The grill decides.

## The ask (unchanged from round 1)

RepoGlance competes with the Gemini app and Google's own apps on feel, and
reads as the same dev crew as Swarm Intercom without borrowing Intercom's
dark-only "night glass". One slot per round; keep it bounded.

## Slot for this round: label typography

How RepoGlance sets its labels: section heads ("Issues", "PRs", owner and
scope labels), chips (Cached · age, sort, mode), the rate-limit and age
lines, and button labels. Body text stays M3 default unless the maintainer
says otherwise.

Facts:

- Today: M3 default type scale on the system font. Section heads are
  `titleSmall`; ages and rate-limit lines `labelSmall`; chips `labelMedium`.
  No fonts are bundled (`app/src/main/res/font` does not exist).
- Intercom's label system (`~/Developer/side-quests/swarm-intercom/docs/design/intercom-page.md`,
  read-only): Space Grotesk, section heads 11 px / 600 / uppercase /
  0.12 em tracking; chips 13.5 px / 500; button labels 17–20 px / 600;
  reading text Inter; code Space Mono. Self-hosted under SIL OFL.
- Google guidance (round 1 query): dynamic colour base, roles and tokens.
  For type, M3 recommends the type scale roles; a brand face is fine when
  mapped onto the roles and kept legible at small sizes.
- Bundling a font adds APK weight and a licence file; a system-face
  treatment (uppercase + tracking on the default font) is free. Both are
  legitimate candidates.

Starting candidates (a starting point, not the five): keep M3 defaults;
uppercase-tracked section heads on the system font; Space Grotesk bundled
for labels only; a Google Sans-like feel via the system font's weight and
size; labels as small caps chips. Exactly five, materially distinct, on the
production catalog and navigator.

## Locks to keep on the canvas

- `status-colour-029` (E family tonal): tonal status pills and rate-limit
  banners from `FamilyStatus` in `ui/theme/StatusColors.kt`. See
  `docs/design.md` v1 (canonical design contract; update it in the same
  bounded implementation after the lock, per the grilltrack design-contract
  rule).
- Brand mark, start window, dynamic colour base, truth rules.

## Process

- Read `.grilltrack/ledger.json` first and resume:
  `python3 ~/.claude/skills/grilltrack/scripts/grilltrack_ledger.py --project . resume --activation implicit`.
- Frontend pack: `picker.md` plus `typography.md`. Picker in
  `app/src/debug/.../devpicker/` (copy the `StatusColourVariantPickerActivity`
  harness: states the question, chips 1–5, DPAD, light/dark preview, collapse,
  Reset, testTags `repoglance:picker-*`), a `type-picker` route in
  `ScenarioLaunchActivity` and `bin/verify-repoglance launch`.
- Only two cards fit under the expanded chrome on the Fold; collapse the
  chrome and swipe inside the list (start above the Navigator button) for a
  second frame. EXACT shows Passing/Failing then Running; MIXED shows the
  rate-limited banner, Unknown and No CI when scrolled.
- Every capture: dump before and after, traces must match, only package
  `co.saari.repoglance`, needed text present, else delete the PNG. Text
  proof under `.grilltrack/proof/`, PNGs stay in ignored `runs/`.
- `./gradlew check` green; no comments in `app/src/main` Kotlin; imports in
  ktlint order (`java.*`, `kotlin.*` last); baselines only shrink.

## Device

- Pixel 10 Pro Fold `59151FDCG000JA`, Android 17, light mode, themed icons
  on. Always `VERIFY_SERIAL=59151FDCG000JA`. Check `dumpsys trust` shows
  `deviceLocked=0`; it relocks after a few minutes idle, so ask the
  maintainer rather than typing on the lock screen. Never change settings,
  never sign in or out.
- `screencap`/`screenrecord` need display `4619827677550801152`; the
  verify helper handles that.
- Production fixture home: `launch <SCENARIO> navigator` then tap
  `repoglance:fixture-home`. Live catalog: `launch MIXED live`, filter to
  `saari-co/RepoGlance` before any capture.

## Delivery rails

- Branch: `claude/grilltrack-029-delivery` (local, unpushed, one commit above
  main with the 029 delivery record). Continue on it; it ships with this
  round's PR.
- PR on the maintainer's request. Merge needs his request plus CI green on
  the head, the OpenClaw autoreview (from `~/Developer/side-quests/x-api`:
  `bin/smoky lane run spark-openclaw-materialize-worktree --repo <worktree> --ref <head> --base <fork-point> --pr-url <pr>`,
  then `bin/smoky lane run spark-openclaw-autoreview --queue --operator-id bobby --mode branch --base <fork-point> --remote-worktree <path> --pr-url <pr>`;
  result in `~/.local/state/openclaw-review/runs/<req>/result.json` on
  spark-2), and a ClawSweeper review of the exact head (`gh pr comment <n> --body "@clawsweeper review"`
  works from the maintainer's gh account) with `proof: sufficient`,
  `status: ready for maintainer look` and no unresolved P0/P1. Any push
  after a review makes it stale: re-run both rails.
