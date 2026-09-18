# Checking splash: RepoGlance mark and motion — GrillTrack `checking-splash-018` (2026-09-18)

Decision `checking-splash-018` on track `gt-20260728163459-227573`, reopened
from deferred after `session-bootstrap-016` merged (PR #16, main
`12c506842ac003e73dd0a2f2e912e54de88ad641`). Branch
`claude/checking-splash-018` in an isolated worktree.

## Question

What the live screen shows while the GitHub session is unknown
(`LiveUiState.Checking`, tens of milliseconds while the saved session is
read on IO). Maintainer direction: a RepoGlance logo with a magnifying glass,
low-key, a fun animation with a clever hook, legible as the small Pixel
status-bar/notification icon. Quick Settings tile is a separate feature
decision, raised as a follow-up grill.

## Picker round 1 (frontend contract: five, neutral, one slot at a time)

Renderer: debug-only `SplashVariantPickerActivity` + `SplashCandidates.kt`
(app/src/debug), reached through
`bin/verify-repoglance launch MIXED splash-picker "" "" <mark>/<motion>`.
Canvas: the production Checking canvas (locked message, no spinner, no
counts, no sign-in control; Unknown never renders as zero or signed-out).
Every mark is one monochrome shape and the canvas carries a 20dp
white-on-dark and dark-on-light strip: the honest status-bar fidelity test,
since Android draws notification and tile icons from the alpha channel.
Manifest: `picker-round-1.json` beside this file (validated with
`validate_picker.py`). Slot 1 `mark` grilled first; slot 2 `motion`
depends on it. Chrome states the question, names each chip, describes the
active candidate, shows what is on canvas, offers Replay (active motion
only) and Reset; keyboard 1–5, DPAD, Tab, R.

Captures: Pixel 11 Pro Fold cover display (CLOSED, 1080x2342), run
`runs/verify-repoglance-runs/checking-splash-round1`, each state asserted from a `dump` (`repoglance:picker-active`,
`repoglance:picker-trace`) before capture and each PNG inspected.

| Slot 1 mark | Form language | Capture SHA-256 |
| --- | --- | --- |
| mark-A | see picker-round-1.json | `e241f7db95f31ef11563f23891692f2b1b30e44feaffab7d2d5bd86a5b29aa78` |
| mark-B | see picker-round-1.json | `67e7a1fa679f73c21def6238d60445de20228619b1206c8598a001a650661918` |
| mark-C | see picker-round-1.json | `e7cea5895e97674e3cf21da36f77f3b010c047cb9e7560d3df8dce331284f1df` |
| mark-D | see picker-round-1.json | `d6ba49d511367fe090f61ed98ae35a40f48954e4780dfd4d6ef2987aeb8b6786` |
| mark-E | see picker-round-1.json | `0323f65a8d720bdbc0bdfd2c6babfb878ef8114e7e860ffffeeed3fcda095cf5` |

Slot 2 motion candidates (A sweep across three dots, B clockwise draw-in
then settle, C double-take tilt on the handle, D sonar ping rings, E hold
with the message fading in after 400 ms) are live on the phone; stills of
motion are not proof and were not used as such.

### Fidelity repair

The cover display clipped chip E in a horizontally scrolling row, so a tap
on E landed on the wrong chip (first round of captures discarded). The
chrome now wraps chips with FlowRow so all candidates are visible and
tappable on the cover display.

### Maintainer feedback and hybrid preview

Maintainer, with the live picker on the phone: slot 1 should combine A and
B; he likes B and wants it to also read as the GitHub commit glyph (screenshot
of the commit node and the GitHub eye provided). Slot 2 is between A and D,
leaning A. Per the contract the precise hybrid was previewed as a
replacement for the five, launched with candidate `A+B/A`: one chip
"A+B Commit eye", a stroke magnifier whose lens holds the GitHub-style almond
eye, the pupil a hollow ring sitting on a vertical commit line.
Capture hybrid-AB `785e2e59582d749f0601ad6eb3d3425bd17b3e591e958a19a1b335408d75eec5`. At 20dp
the mark reads as a magnifier with a ringed centre; the eyelids merge into
the lens, which is the honest limit of the strip.

### Pick

Maintainer: the A+B Commit eye is the logo; motion D Ping. Locked in the
ledger (`checking-splash-018`). Phone left on `A+B/D`; capture locked-AB-D
`16721422cb891d477cd0b05c0d499e2a08a38ff0aaab50b5104e6837214ad5a5`.
Production implementation waits for shared-understanding confirmation.

## CI floor

`./gradlew check` green after the picker (lint, detektDebug with the Compose
rule set, checkFeatureMap). Feature map unchanged: no user-visible behaviour
has changed yet.

## Production implementation (after shared-understanding confirmation)

- `app/src/main/res/drawable/ic_repoglance_mark.xml`: the commit-eye mark as
  one monochrome 24dp vector (stroke lens, handle, commit ticks, almond eye,
  hollow pupil ring). It is the single source for the splash and, later, the
  status-bar/notification/tile icon; no notification or tile is built here.
- `app/src/main/java/co/saari/repoglance/ui/brand/RepoGlanceMark.kt`:
  `RepoGlanceMark` (the vector, tinted onSurface) and `CheckingMark` (motion
  D: two staggered rings, 1600 ms period, 550 ms offset, pulsing out of the
  lens). Reduced motion: when `Settings.Global.ANIMATOR_DURATION_SCALE` is 0
  the rings are not composed and the mark holds still.
- `LiveRepoGlanceScreen.kt`: `LiveUiState.Checking` renders `CheckingScreen`
  (mark 96dp + the unchanged message). No spinner, no counts, no sign-in
  control: Unknown never renders as zero or signed-out.
- Debug holder `CheckingPreviewActivity` (`launch MIXED checking`) keeps the
  production composable open, because the real state lasts tens of
  milliseconds. Feature map `sign-in.md` gained `signin-checking` and the
  drive step. `checkFeatureMap` ok.

Device proof, run `runs/verify-repoglance-runs/checking-splash-prod`, Fold
inner display (the maintainer opened the phone; 2076x2152):

- `dump checking-inner`: `repoglance:checking-mark` and
  `repoglance:checking-message` present, no `repoglance:connect-github`.
- capture checking-inner
  `69faad286175110a2b0517e28ccb14378883b67926ab3ebbf6c0761bca6355b0`,
  inspected: mark with both rings mid-pulse above the message.
- live cold start after install: `launch MIXED live` reached MainActivity,
  the dump shows the session/sign-in state, no `AndroidRuntime` crash. The
  only StrictMode lines are the known `ScenarioLaunchActivity` AppPrefs
  write (recorded debt, `launcher-prefs`).

Gaps: no cover-display capture of the production screen (forcing CLOSED
posture put the phone to sleep and the helper refuses a sleeping screen;
the layout is the same centred column). Reduced-motion was implemented, not
device-proven: toggling the animator scale is a device setting change and
stays with the maintainer.

## CI floor

`./gradlew check` green on the final tree (lint, detektDebug + Compose rules,
ktlint, checkFeatureMap, warnings as errors, no comments in app/src/main).

## Follow-up grill raised

Quick Settings tile for quick access (maintainer direction 2026-09-18): a
separate feature decision; the mark vector is ready to serve as its icon.

## Review (source identity git:5f793f75acd4b86d97ae214667872b41384bf4a7, PR #18)

- OpenClaw `req-20260918T230653Z-9496833793`: correct (0.98), 0 findings.
- ClawSweeper: platinum hermit 4/6 overall, patch diamond lobster 5/6,
  proof platinum hermit with media bonus, no code or security findings
  (PR #18 comment 5737299348). One merge-readiness item: the reduced-motion
  branch is source-evident, not device-proven.

Adjudication: no required fixes. The reduced-motion item is classified
`human_gate`: proving it means changing the phone's animator duration scale,
a device setting that stays with the maintainer; the branch is a single
`if` on `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` that skips composing
the rings. Merge is the maintainer's.
