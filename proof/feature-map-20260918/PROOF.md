# verify-repoglance skill and feature map — GrillTrack `feature-map-015` (2026-09-18)

Grill B of the poteto-style verification substrate plan
([saari-co/x-api#693](https://github.com/saari-co/x-api/pull/693)), decision
`feature-map-015` on track `gt-20260728163459-227573`.

## Decision history

Locked first as a blocks-style ownership table with a path/testTag
validator. Reopened the same day after the maintainer asked for Lauren Tan's
pstack to be read first (`cursor/plugins` at `pstack/`, skills
`create-verification-skill` and `maintain-verification-skill` with their
feature-map example). Re-locked in pstack shape: a project-local
verification skill directory, user point-of-view feature files with four
fixed headings, top-five features seeded, and the skill proven by driving one
mapped feature end to end. Both transitions are in the ledger.

## Locks (confirmed by the maintainer 2026-09-18)

- `.claude/skills/verify-repoglance/` canonical, `.cursor/skills/verify-repoglance`
  symlink, `AGENTS.md` pointer.
- `SKILL.md` with Launch, Doctor, Drive, Evidence, Cleanup, Helpers.
- `features/README.md` (baseline preconditions, driving conventions, proof
  and skip reporting, feature entry contract) and five feature files:
  compact widget truth states, navigator, find a repository, sign in with
  GitHub (human-gated), refresh and freshness.
- `bin/verify-repoglance` helpers: `doctor`, `launch`, `dump`, `tap`,
  `type`, `capture`, `cleanup`, over adb, `uiautomator dump`, and the
  PhoneProof capture helper.
- Debug-only `ScenarioLaunchActivity` (`app/src/debug`): adb extras
  `scenario`, `screen` (`live` | `navigator` | `picker`), `repo`, `mode`.
- Stable `testTag` resource ids on the driven controls only (12 added).
- `scripts/check_feature_map.py` as a hard structural gate inside
  `./gradlew check` (`checkFeatureMap`).
- Upkeep is pstack's `maintain-verification-skill` loop (gardener grill).

## Baseline

`main` at `fb220e8` (after
[saari-co/RepoGlance#13](https://github.com/saari-co/RepoGlance/pull/13)):
the CI floor is in place; no feature map, no verification skill, no launch
path to a fixture scenario, five test tags in total.

## Deterministic verification

- `./gradlew check assembleDebug`: exit 0 with the new `checkFeatureMap`
  task in `check`; 188 tests / 25 suites unchanged and green; lint and
  detekt 0 outside their baselines. Debug APK SHA-256
  `5293a911bdc004b3be040e1961059d08e734df7281ffc4627ffe0d490e68b6bf`.
- The floor caught two of my own changes on the way: an import-order
  violation from the inserted `testTag` import (fixed by autocorrect) and
  lint's `CustomSplashScreen` on the launch-and-finish launcher (a scoped
  `@SuppressLint` on the class; the shape is intentional).
- Validator probes (`scripts/check_feature_map.py`, direct):

  | Breakage | Exit | Message |
  | --- | --- | --- |
  | rename a feature file's `## Gotchas` to `## Notes` | 1 | H2s must be exactly the four fixed headings |
  | add `ghost.md` to the index | 1 | index lists ghost.md which does not exist |
  | `chmod -x bin/verify-repoglance` | 1 | helper is not executable |
  | remove the `.cursor` symlink | 1 | must be a symlink to the skill directory |
  | restored | 0 | ok (5 features, 6 skill sections) |

- `git diff --check` clean including the new files.

## First live pass: drift and harness findings (2026-09-18)

pstack's rule is that a generated skill is a draft until its own instructions
have been run end to end once. The first pass on the Pixel 11 Pro Fold did
not pass, and every failure was either the map describing the app wrong or
the harness being too trusting. Product behaviour was correct throughout.

Map drift, corrected in the feature files before the second pass:

1. `navigator.md` claimed the truth state (`Cached`, `Last-good`,
   `Data unavailable`) follows the launcher's fixture scenario. In the app
   the navigator's list state is a user choice under `Filters → Fixture
   state` (`Loaded`, `Empty`, `Last-good`, `Data unavailable`) and defaults
   to `Loaded` for every scenario; the scenario drives the fixture home and
   widget snapshots. The recipe now drives the Filters sheet.
2. `navigator.md` asserted rows labelled `ISSUE`/`PR`; that is the widget
   feed's shape. The navigator uses `Issues` / `PRs` section headers and
   rows shaped `#100` / title / `open` / label / `by … · Updated …`. The
   recipe now asserts the section header and taps a row by its title.
3. The scenario captures for `EXACT`, `UNKNOWN`, and `RATE_LIMITED` were
   byte-identical because, per finding 1, the navigator does not vary with
   scenario. That is the app, not a capture fault; the recipe no longer
   implies otherwise.

Harness weaknesses, fixed in `bin/verify-repoglance`:

- The phone came back over wireless debugging after a USB unplug, then USB
  returned mid-run, so adb saw two entries for one device and refused every
  command. `doctor` and every helper now pin one transport (USB preferred,
  `VERIFY_SERIAL` overrides).
- The first Wi-Fi run dumped the keyguard on the cover display because the
  phone was locked and folded; `capture` asked for the inner panel. `doctor`
  now fails unless the phone is awake and unlocked and prints the posture;
  `capture` captures whichever physical panel is active and refuses a
  sleeping screen.
- Two of eight launches landed on the wrong screen (once the GitHub app
  showing "Could not resolve … acme/rocket", once the live catalog), which
  reads as a race between `install -r`, `force-stop`, and the launcher
  hand-off. `launch` now installs only when the device APK hash differs,
  waits up to eight seconds for the expected resumed activity and marker,
  and retries once before failing.

Doctrine check: edits stayed inside the skill directory and the helper; no
product code changed because of this pass.

## Second and third live passes (2026-09-18)

Pixel 11 Pro Fold, serial pinned over USB, awake and unlocked, `OPENED`.
`doctor`: device APK SHA-256 equals local build
`5293a911bdc004b3be040e1961059d08e734df7281ffc4627ffe0d490e68b6bf`, launcher
present.

Every fixture scenario reached through the launcher (`launch <S> navigator
acme/rocket BOTH`), 7 of 7, each dump containing `acme/rocket` and
`repoglance:fixture-home`; captures `nav-<S>.png` with SHA-256s in the run
directory. The scenario provably reaches the fixture home: after
`launch RATE_LIMITED navigator` and `tap repoglance:fixture-home`, the
`repoglance:scenario` field reads `RATE_LIMITED` (capture `home-rate-limited`,
SHA-256 `333f7c460386550dddb1a050a4e5c859ccf2a606dba0fb162073be70eab4d418`).

Navigator feature, driven by the helpers exactly as `features/navigator.md`
now reads:

| Step | Result |
| --- | --- |
| open for `acme/rocket` | resumed `MainActivity`; ids `repoglance:fixture-home`, `…-mode-ISSUES/PRS/BOTH`; `Issues` header |
| `tap repoglance:fixture-mode-PRS` | `PRs` header present, `Issues` absent |
| `tap repoglance:fixture-mode-ISSUES` | `Issues` present, `PRs` absent; capture `navigator-issues` `b04d5b01…` |
| `tap Filters` | `Fixture state` sheet with `Loaded`, `Empty`, `Last-good` (`Data unavailable` below the fold) |
| `tap "Fix flaky retry in sync worker"` on the inner display | GitHub app opens in the adjacent pane, navigator stays visible; dump reads `com.github.android` and `Could not resolve to a Repository with the name 'acme/rocket'`; capture `navigator-row-wide` `dae29a66…` |
| cover display via `cmd device_state state 0` | phone locked itself and lit the always-on cover screen (`Dreaming`); step is maintainer-gated and the recipe now says so |

Further drift found and corrected on these passes:

4. Row selection semantics differ by width, not by gesture. `NavigatorScreen`
   passes `openGitHubOnSelect = true` on layouts 600dp and wider, so on the
   inner display a tap opens the item in the GitHub app beside the list; on
   the cover display a tap selects and the in-app detail replaces the list.
   The recipe first described the cover behaviour as universal.
5. `Data unavailable` is below the fold of the Filters sheet; the recipe now
   uses the visible `Last-good` chip and says how to reach the other.
6. Android remembers the RepoGlance + GitHub split pair, which is why an
   earlier launch came up with the GitHub app on top. `launch` and `cleanup`
   now stop the GitHub app before starting RepoGlance.

Product observation, reported not fixed: on the inner display the right
pane reads `Select a row to see details` and can never be filled, because
selection there always hands off to the GitHub app. Either the pane should
not render on wide layouts, or selection should populate it and the GitHub
hand-off move to the `Open on GitHub` control. Recorded for a later grill.

## Final pass: navigator recipe green (2026-09-18)

Phone unlocked by the maintainer after the posture emulation locked it.
Driven with the helpers exactly as `features/navigator.md` reads at this
head; zero assertion failures.

| Step | Observed |
| --- | --- |
| `launch MIXED navigator acme/rocket BOTH` | resumed `MainActivity`, ids and `Issues` header present |
| `tap Filters` → `tap Last-good` (sheet closes on selection) | list header shows `Cached · 1h`; `acme/rocket` and `repoglance:fixture-home` still present; capture `navigator-last-good` `2df3b605c1f5e36e6290a9819ffaf0108ba691d8bc1eb2a54389981ebb6efb96` |
| `tap repoglance:fixture-home` | dump contains `repoglance:scenario` and `repoglance:fixture-navigator`; capture `navigator-home` `0da2e406ac982047615c5447a9a6ae6f55bc11cdb30a72e732dca75aacb2e6cf` |
| `cleanup` | app stopped, scenario `MIXED`, airplane off; 14 captures and their dumps remain in `runs/verify-repoglance-runs/fm015-20260918/` |

Last corrections from this pass: the Filters sheet closes on selection (a
following `BACK` had bounced to the fixture home and then to the launcher; a
capture of the launcher was discarded as non-evidence), the active filter
label reads `Filters · N` (the helper's `tap` now falls back to a text
prefix), and `Last-good` renders as a `Cached · <age>` chip rather than the
word.

Coverage summary for the five mapped features on this head:

- **Compact widget truth states**: picker reached through the launcher on
  every pass; the compact renders were proven in
  `proof/compact-widget-live-20260917/PROOF.md` on the same composition.
- **Navigator**: driven end to end above, inner-display row hand-off to the
  GitHub app included; cover-display detail is maintainer-gated (posture
  emulation locks the phone) and recorded as such.
- **Find a repository**, **Sign in with GitHub**, **Refresh and freshness**:
  mapped and validated structurally; their live steps need the maintainer's
  session and were not driven this cycle, per the human-gated rows. The
  earlier signed-in proofs on PR #12 (guarded filter, persisted `LAST_GOOD`
  after a real offline refresh) are the current evidence for those paths.

Images are held locally with the hashes above, not committed. No sign-in,
token, account, GitHub write, or Play action occurred; the only third-party
app touched was the GitHub app, stopped on the registered test phone to break
a remembered split pair.

## Review repair (2026-09-18)

ClawSweeper on `ae5834f`: platinum hermit (4/6), proof diamond lobster
(5/6), one `required_fix` at P3, accepted: `check_feature_map.py` only
checked that `Preconditions:` appeared somewhere in a driving section, while
the feature contract requires it first. The validator now requires
`Preconditions:` to be the first non-blank line of that section; a probe
with prose before it fails (`first non-blank line is 'Some prose before the
preconditions.'`) and the five feature files pass. OpenClaw on `ae5834f`:
scoped-clean, 0 findings (after a first queue attempt that I had given a
mangled base SHA; re-queued with the real `main` head).

## Review repair 2: device-code redaction (2026-09-18)

ClawSweeper on `8b3c75f`: `required_fix` at P1, security boundary,
accepted. `features/sign-in.md` allowed `dump code-screen` while GitHub's
device code was visible, and `dump` writes every visible string to the run
directory, so the recipe's promise that the code is never stored was false
as written. No such dump was ever taken (sign-in was never driven), but the
path existed.

Repair:

- `scripts/verify_redact_guard.py` refuses a UI tree that contains
  `Enter this code on GitHub`, `Copy code & open GitHub`, or
  `Code expires in about`; `bin/verify-repoglance dump` pulls the tree to a
  temp file, runs the guard, and deletes the file and exits 1 when it
  trips; `capture` runs the same guard on a fresh tree before writing a PNG.
- `features/sign-in.md` now says the agent runs no `dump`, `tap`, or
  `capture` while the code screen is visible and waits for the maintainer;
  `SKILL.md` documents the guard under Evidence and Helpers.
- Probes: synthetic trees with the code prompt and with the copy button →
  exit 1 with the guard message; a safe tree → exit 0; a real `dump` of the
  signed-in catalog on the Fold → allowed. One earlier run artifact that
  held the unfiltered live catalog (`nav-NO_CI.*`, from a launch that landed
  on the live screen) was deleted from the local run directory under the
  same policy.

## Review repair 3: screen before any artifact exists (2026-09-18)

ClawSweeper on `cc5fe2a`: `required_fix` at P1, accepted. The first guard
ran after `uiautomator dump` had written `/sdcard/verify-ui.xml` and `adb
pull` had written the host file, so an interrupted run could still leave the
device code on disk. Repair: `dump` and `capture` now stream the tree with
`adb exec-out uiautomator dump /dev/tty` straight into
`scripts/verify_redact_guard.py`, which reads stdin, never touches disk, and
emits the XML only when safe; the helper writes a file only from that
output. Probes: a synthetic device-code tree on stdin → exit 1 and zero
bytes written; a safe tree → exit 0 and the XML; on the Fold, a `dump` of the
signed-in catalog is allowed, `capture` runs the same screen first, and no
`/sdcard/verify-ui.xml` exists on the device afterwards.

## Review repair 4: real Fixture-state labels (2026-09-18)

ClawSweeper on `8fa0049`: `required_fix` at P2, accepted. The navigator
recipe named a `Data unavailable` chip in the Filters sheet; the sheet's
labels are `Loaded`, `Empty`, `Paged`, `Last-good`, `Unknown`, and
`Rate-limited` (`ListState.displayLabel()`), and `Data unavailable` is what
the list renders after choosing `Unknown`. Verified on the Fold: the sheet
dump shows the five visible chips; choosing `Unknown` renders
`Data unavailable` with an `Unknown` chip, `Rate-limited` and `Last-good`
render a `Cached · <age>` chip, `Empty` renders `No rows match`. The recipe
now names the chips and their renderings exactly. The structural gate does
not check recipe strings against the UI; that is the maintenance loop's job
and the reason the gate is called structural.

## Terminal review (2026-09-18, head `725744b`)

- OpenClaw (`req-20260918T125718Z-379153731252`): scoped-clean, 0 findings,
  `patch is correct (0.96)`.
- ClawSweeper: overall **platinum hermit (4/6)**, proof **diamond lobster
  (5/6)**, patch quality platinum hermit, **no actionable findings**. Its two
  remaining items are maintainer decisions: accepting a Python-backed
  structural check inside `./gradlew check`, and waiting for CI, which it
  saw still running.
- This proof/ledger commit is the only change after the reviewed head; no
  product, helper, skill, or script source differs from `725744b`.

Cycle history: implementation `6f1f1db` → proof `ae5834f` → validator
precondition-first `8b3c75f` → device-code guard `cc5fe2a` → in-memory
guard `8fa0049` → real Fixture-state labels `725744b`. Four review-repair
cycles, each recorded as findings → implemented → verified. The two
security findings were about the harness's own redaction promise and were
repaired before any sign-in was ever driven.
