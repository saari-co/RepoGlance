# Navigator detail: pane + second tap on wide, sheet when folded — GrillTrack `navigator-detail-017` (2026-09-18)

Decision `navigator-detail-017` on track `gt-20260728163459-227573`, chosen
by the maintainer as the alternative recorded at the close of
`feature-map-015` because he wanted a visual, phone-visible cycle.

## Problem (verified on device in proof/feature-map-20260918)

`NavigatorScreen` passed `openGitHubOnSelect = true` on layouts 600dp and
wider, so on the Fold's inner display a row tap opened the item in the GitHub
app beside the list and the right-hand `DetailPane` ("Select a row to see
details") could never be filled. Fixture repositories do not exist on GitHub,
so the hand-off showed "Could not resolve to a Repository".

## Picker round 1 (frontend contract: five, neutral, one slot)

Renderer: debug-only `NavigatorVariantPickerActivity` +
`NavigatorCandidates.kt` (app/src/debug), reached through
`bin/verify-repoglance launch MIXED navigator-picker acme/rocket BOTH <A..E>`.
The chrome, list and detail are the production composables (seven navigator
building blocks widened from private to internal); only the wide branch
differs per candidate; the narrow branch was production verbatim. Manifest:
`picker-round-1.json` beside this file (validated with
`validate_picker.py`). Slot: `wide-selection`. Locked slots on the canvas:
navigator chrome (navigator-scope-005), list truth state, narrow selection,
long-press.

| Candidate | Behaviour on tap (wide) | Captures (Fold inner, 2076x2152) |
| --- | --- | --- |
| A pane-fill | fills the detail pane; Open on GitHub in the pane | a-list `d5f4adf992f1625615cc224bd78b651d1e6e63d2de927ff1f504e4ca10d7c61e`, a-selected `7d90343b7efae0e6335476f029c92e8eb05c276d7b7aa3fde5ebd73f4f18c076`, a-github `faaad06d7932a6026f889992c33f690a77df1d1e0bde65ebe570575ef22e5728` |
| B list-only | no pane; tap opens GitHub beside | b-list `ecac9ebd8de9193a8c0a0ac3200df146c9106bbbf95bac7fd83602bd2790f6ee`, b-github `243e8ed69c58e98ee2a4ed7c2899b3867c82ef53631305396ee8d0be14bf7ccc` |
| C second-tap | fills the pane; second tap on the row opens GitHub beside | c-selected `610afdfa234201de134af329cea5b896ae790a923aea4b2c0e79508bb4ca0b58`, c-github `12762405b11a0283865a4fdee98b2a1adfb4fa97556eb2985f1277b943b6f0df` |
| D sheet | no pane; modal detail sheet over the full-width list | d-list `34b91cbe02c47873fc8e4b0201a7dc1aae6843246e736ed32006d10804286962`, d-sheet `c5be3dbf1a217947562de44f32674291af5a25e519f47538ece010103736ca4a` |
| E single-pane | detail replaces the list, Back returns | e-list `fa99f4fd48b61f91f6ae030098de15cc0ce046a095039bdf7ecc15ed61e16b35`, e-detail `889955aad1c6e7d5239953751dfb50174ecab04f39c029ddeee042fef04cd448` |

Every state was asserted from a `dump` before capture (pane placeholder
gone, `Author:` present, `com.github.android` ids and "Could not resolve"
for hand-offs, `Back` for E) and every PNG was inspected.

### Fidelity gap and repair

The maintainer could not tell A from E in the captures and said the picker
should make it apparent what is being decided. Stills of a behaviour look
alike. The chrome was reworked: it states the question ("deciding: what a
row TAP does on this wide layout; long-press is not being decided"), names
each chip, describes the active candidate as "tap a row → …, GitHub via …",
keeps a live "Last action" trace, and has Reset. Verified on device:
chrome-a-selected `62d01af4db46bd671df7af5ce5fd983e15f2a0b05fd26fe4eff4dc111cf39a5f`, chrome-e-detail
`ffb84d4f27bf509acb36bbf7172f4f57d5b62da23eeae1b8fd075cc242fcba4e`. The maintainer then played with the live
picker on the phone.

### Pick and hybrid

Maintainer: keep long-hold for GitHub; an opened Fold should default to
split view; B or C on wide, C gives more; likes D's slide-up sheet but wants
it when the phone is closed. That is a hybrid across two slots (wide =
C, narrow = D's sheet). Per the contract it was previewed for confirmation
as a replacement for the five, not a sixth chip: `launch … navigator-picker
… "C+sheet"` shows one chip with the folded sheet enabled (hybrid-wide-selected
`ab471e56b2ce2c3b18788ee3baa9d2b6170af82df482cee20b8291ec79eff0f4`). The narrow behaviour locked by
feature-map-015 (`nav-row-narrow`: detail replaces the list) is superseded by
this decision with the maintainer's explicit "both in this cycle".

## Lock (confirmed 2026-09-18)

- Wide (600dp+): tap fills the in-app detail pane beside the list; a second
  tap on the highlighted row, or the pane's Open on GitHub, opens the GitHub
  app in split view.
- Narrow (cover display): tap raises a modal detail sheet over the full-width
  list with Open on GitHub; dismiss restores the list.
- Long-press opens GitHub on both widths, unchanged.
- Stable ids: `repoglance:navigator-detail`, `repoglance:navigator-detail-sheet`,
  `repoglance:navigator-open-github`.

## Implementation

`NavigatorScreen.kt`: the wide branch no longer passes a tap to GitHub; the
list and controls are hoisted into one slot used by both branches; the narrow
branch renders `ModalBottomSheet` instead of swapping the list; the sheet is
suppressed once a GitHub pane has narrowed the window (a tap there goes
straight to GitHub as before). `features/navigator.md` rewritten for the new
behaviour (sub-features, both hand-off steps, cover-display sheet, gotchas).
Debug picker kept as dev tooling with `C+sheet` as the recorded hybrid.

## Deterministic verification

`ANDROID_HOME=… ./gradlew --no-daemon check assembleDebug`: exit 0 (lint and
detekt baselines, warnings as errors, comment ban, `checkFeatureMap`);
`git diff --check` clean. Debug APK SHA-256
`d916dbf8aa94f70a8f12dfea2a110bf4de23e6e22c03139a6113dc02ecbacf95`.
The floor caught, in the debug picker: ktlint wrapping, MatchingDeclarationName,
ComposableNaming, ModifierMissing/ModifierWithoutDefault, MultipleEmitters,
ContentSlotReused, past-tense lambda name, and CyclomaticComplexMethod
(twice, including the launcher's `onCreate`); each repaired, none suppressed.

## Device verification: features/navigator.md re-driven (inner display)

Pixel 11 Pro Fold, USB serial pinned by `doctor`, awake, unlocked, OPENED.
Run `runs/verify-repoglance-runs/nd017-impl-20260918/` (ignored; hashes
below). Zero assertion failures.

| Step | Observed | Capture SHA-256 |
| --- | --- | --- |
| `launch MIXED navigator acme/rocket BOTH` | `repoglance:fixture-home`, `Issues`, placeholder present | |
| mode PRS → ISSUES | `PRs` only, then `Issues` only | navigator-issues `dccc52f89e222f3f1f9ec2419955d706010044f0c72056a09d5b619e1dd94588` |
| Filters → Unknown | `Data unavailable` | navigator-unknown `46642233ef81806d9e0a77f0f8870acab95c498a98961c734b8ee0ea899ad605` |
| Filters · → Last-good | `Cached ·` chip | navigator-last-good `7bfa701cf61cc730ab7b25f30939327026534e71dd3885af30402a5b9ec7edf2` |
| tap "Fix flaky retry in sync worker" | row highlighted; `repoglance:navigator-detail` holds `Author:` and `repoglance:navigator-open-github`; placeholder absent; list still present | navigator-row-wide `69549ab12348dc9564259f1e72ca34b2d054086a9abb65f9026b0077188122a7` |
| tap the same row again | GitHub app beside the list, `com.github.android`, "Could not resolve … 'acme/rocket'" | navigator-row-github `010cc7d5cce8ea194a1e5871b226035277f00085046d477a39cf7b9c411dc850` |
| force-stop GitHub, `tap repoglance:fixture-home` | `repoglance:scenario`, `repoglance:fixture-navigator` | navigator-home `9c86dd3ca9148f0d8817bc08b94f971e6236348fd823c2ddf162138633d72a55` |

## Device verification: cover display (maintainer-gated)

The maintainer folded and unlocked the phone (posture reported `CLOSED`,
awake, unlocked; the single active physical display was the 1080x2342
cover panel). Driven with the helpers as `features/navigator.md` reads.

| Step | Observed | Capture SHA-256 |
| --- | --- | --- |
| `launch MIXED navigator acme/rocket ISSUES` | rows present, no placeholder (single pane) | navigator-cover-list `6fb9d64a2b9ddeb8cfe89605ccd0a677752f977f571ceece14b48dfe760e744f` |
| `tap "Fix flaky retry in sync worker"` | `repoglance:navigator-detail-sheet` rises over the list with the title, `Author:` and `repoglance:navigator-open-github`; rows still underneath | navigator-cover-sheet `878326a3214d1ca03d7c9f50d3e6c38d912b809974e22ddd57e1e2bff3597b55` |
| one `BACK` | sheet gone (`Author:` absent), rows present; capture byte-identical to the pre-tap list | navigator-cover-list-again `6fb9d64a2b9ddeb8cfe89605ccd0a677752f977f571ceece14b48dfe760e744f` |

Finding on the first cover pass: the sheet rendered (capture
`8702c949178d205978002844c844a7b41de866f0fb2b0850d641dfe156094eea`, inspected)
but its ids were not in the dump, because a `ModalBottomSheet` lives in its
own window and does not inherit `testTagsAsResourceId` from the activity
root. Repaired by setting that semantics property on the sheet itself;
`check` green; the pass above is on the repaired build. The same gap
explains why round 1's `d-sheet` dump held the sheet text but no id.

Final debug APK SHA-256
`ad39003194317510825f51cd8cf6a9cac07d2aaee9cbe474c52835dc38af2e5d`; device
APK equal per `doctor` before the cover pass.

## Boundary

No sign-in, token, GitHub write, Play, or account action. The only
third-party app touched was the GitHub app on the registered test phone,
stopped to break the remembered split pair. Screen kept awake over USB with
`svc power stayon usb` after the phone locked itself mid-run; `svc power
stayon false` restores it. Images stay in `runs/`; hashes above.

## Review round 1 (2026-09-18, head `9f3de52`)

- OpenClaw (`spark-openclaw-autoreview`, base `b445dc4`): scoped-clean, 0
  findings, "patch is correct (0.98)", P0-only threshold.
- ClawSweeper (PR #15 comment 5731579237): gold shrimp (3/6), proof diamond
  lobster (5/6), one P2 finding, **accepted as `required_fix`**: selection was
  keyed by issue/PR number alone, and account/org scopes aggregate rows from
  several repositories where numbers collide, so the highlight, the pane, and
  the second-tap hand-off could resolve to the wrong row. The LazyColumn item
  keys had the same flaw.

Repair: rows are now identified by `rowKey(kind, repo, number)`
(`issue:acme/rocket#100`); `selectedKey`, `findItem`, the highlight, the
second-tap comparison, and the item keys all use it. The debug picker follows.
`NavigatorChromeTest.selectionKeyDistinguishesSameNumberAcrossRepositoriesAndKinds`
pins the identity (same number, different repo or kind → different key).

Honest limit: the fixture corpus numbers rows uniquely within a scope
(issues 100+i, PRs 200+i across the repositories), so the collision cannot be
reproduced on the phone with fixtures; the JVM test is the deterministic
proof and the recipe re-drive is the regression proof. `check assembleDebug`
green (189 tests). The phone was folded at repair time, so the cover recipe was
re-driven on the repaired build (sheet `02c10ef08158405332a698591d0d4c1772e488cf179af61cfa8074aa4b5c7ef3`,
Back dismisses, list restored); the inner-display recipe re-drive waits for
the maintainer to unfold and is recorded below when done.

## Review round 2 (2026-09-18, head `e0e3b8c`) and inner-display re-drive

- OpenClaw: scoped-clean, 0 findings, "patch is correct (0.98)".
- ClawSweeper: patch quality platinum hermit (4/6), proof gold shrimp (3/6),
  overall gold shrimp; the P2 from round 1 confirmed fixed; one P1
  merge-risk, **accepted as `required_fix`**: the inner-display pane-fill and
  second-tap flow had not been re-driven on the repaired head.

Re-driven on the repaired build (device APK equals local
`cf5aca57e6a4ebb9171edce2d5aa185e88940b7b533e8ba89099f86d6abb3646`, Fold
awake, unlocked, OPENED), run `nd017-repair-20260918`, zero assertion failures:

| Step | Observed | Capture SHA-256 |
| --- | --- | --- |
| `launch MIXED navigator acme/rocket BOTH`, ISSUES | placeholder present; `Issues` only | navigator-issues `4ad72e4c1eac0d0f87286a95eb51b976c54cc8b164268e41b04fb7acc1d8284d` |
| tap "Fix flaky retry in sync worker" | `repoglance:navigator-detail` holds `Author:` and `repoglance:navigator-open-github`; placeholder gone | navigator-row-wide `b9e8eb66433209715df6f749e8ddc183ed927eab551991545d7591413bfc5f32` |
| tap a different row, "Add pagination to issue navigator" | selection moves: that title now appears twice (row and pane); no GitHub window | |
| tap the same row again | GitHub app beside the list, `com.github.android`, "Could not resolve … 'acme/rocket'" | navigator-row-github `a7f68e1ac8e9ba8f7ce0d773e386447d22dd2c3285ad2231876120137ed8463a` |
| force-stop GitHub, `tap repoglance:fixture-home` | `repoglance:scenario` present | navigator-home `873e5fc37001ae6350022429bc52cef0e2501bbc24b8c888a2bc267369bc35d7` |

Both displays are now proven on the repaired head; no source differs from
`e0e3b8c` after this proof commit.
