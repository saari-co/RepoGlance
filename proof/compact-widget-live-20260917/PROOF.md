# Compact widget: live counts + right-aligned ledger

GrillTrack decision `widget-content-priority-008`, track
`gt-20260728163459-227573`. Deferred at the bootstrap seed, reopened
2026-09-17 because the widget still rendered `FIXTURE PREVIEW` while the app
was live on the Play internal track.

## Source identity

- Implementation + review subject: `cd737e2a5b219b82080bbc8e3b0d5d931f8c2e20`
- First implementation (review found three fixes): `d2463aabb2d7f561fc5e55dc684f64704c4b3b1c`

## Decision

Right-aligned ledger: repository name, then label-left/number-right rows —
`issues N`, `PRs N`, and `to review N` as progressive disclosure above 84dp.
Selected in picker round `compact-widget-content-round-2`. Round 1 was
rejected by the maintainer: five wordings of a single column layout read as
the same widget, which the rendered canvas confirmed.

## Deterministic verification

- 169 JVM tests across 23 suites; 0 failures, 0 errors, 0 skips
- Lint: 0 errors. The diff introduces no new warnings; the 33 remaining are
  pre-existing dependency-age, `AutoboxingStateCreation`, and
  `MissingApplicationIcon` warnings on `main`
- 9 truth-rule tests pin `LiveSnapshotFactory`: PR-page truncation, GitHub's
  `open_issues_count` including PRs, LAST_GOOD aging, UNKNOWN-not-zero,
  negative-count rejection, and both exact count sources
- 2 Glance composable tests via `glance-appwidget-testing` (JVM only, no
  device, no UI Automator). **Both were mutation-checked**: each fails when the
  rendered value is wrong and passes when restored, so
  `unitTests.isReturnDefaultValues = true` has not hollowed them out

## Device verification

Pixel 11 Pro Fold, `66261FDDJ002J5`.

Display inventory before capture, per the PhoneProof Android display contract:

| Display | Physical ID | Size | State |
| --- | --- | --- | --- |
| Inner | `4619827677550801152` | 2076x2152 | ON (captured) |
| Outer | `4619827677550801153` | 1080x2342 | OFF (excluded) |
| `studio.screen.sharing:0` | virtual | 1552x1610 | excluded as virtual |

The real production `CompactContent` was rendered through the actual Glance
composition pipeline (`GlanceRemoteViews.compose`) at 120x64, 180x64 and
250x90dp, in both an exact-count state and a no-observation state.

- Floor and mid: two rows, no truncation
- Wide: third row `to review` appears — progressive disclosure confirmed
- No observation: every value renders an em dash at every size, never `0`

Stability: two consecutive captures, byte-identical.

- `prod-stable-1.png` sha256 `efd8bc087c66446fa49e9d7b8a5e6289cb50cbaef9d62ac4b833f2f9f19feb41`
- `prod-stable-2.png` sha256 `efd8bc087c66446fa49e9d7b8a5e6289cb50cbaef9d62ac4b833f2f9f19feb41`
- picker round 2 `picker-round2.png` sha256 `14048fb1a2c3b44ca4210ecabb6729661cbeac19e89760a46d977b2478ab2dad`

Images are held locally and not committed as Git blobs, per `REPO_HYGIENE.md`.

### Disclosed overlay event

The first production capture caught the notification shade over the app. It was
logged as a failure, discarded without being retained or transmitted (it
contained personal notification content), a single reversible BACK was issued,
and two stable captures were then taken and compared — the PhoneProof
transient-overlay rubric.

## Review findings (adjudicated)

Reviewed `d2463aa` on both axes. Three `required_fix`, all repaired in
`cd737e2` and re-verified:

1. `LiveSnapshotFactory.build` accepted an `issues` page and never read it.
   That page is already fetched and already excludes pull requests, so an
   untruncated page is exact on its own. It is now the primary source.
2. `LiveSnapshotStore.load` ran on the main thread while its matching save
   was dispatched to IO.
3. The metadata request fired before the request-generation and session
   checks, burning quota on responses that were then discarded.

## Honest boundary

- **Scope is the compact slot.** The tall row feed still renders fixture rows
  and retains its `FIXTURE PREVIEW` label. The widget is live at one size and
  labelled fake at another, deliberately, until a follow-up cycle.
- **No live-GitHub-data-on-device proof.** Rendering real account data in the
  widget requires an account-bound sign-in on the phone, which is a
  maintainer-gated step. Everything above uses constructed snapshots through
  the real rendering and derivation paths.
- Finding 1 makes the extra API request conditional rather than
  per-refresh. This is a disclosed deviation from the confirmed summary, which
  specified one `GET /repos/{owner}/{name}` per refresh. Exactness is unchanged
  or better in every case; quota use is strictly lower.
- No release, merge, tag, account, or Play Console action occurred.

## Merged-head re-verification (2026-09-18)

After [saari-co/RepoGlance#6](https://github.com/saari-co/RepoGlance/pull/6)
merged, `origin/main` (`91ef720a4aa22ce35b0cc01a04851c3913cc03e1`) was merged
into this branch, not rebased, so the source identities above remain
ancestors. Merge commit: `2d19847e19054608e91d6e57ac4b6ca1e1fc1567`, no
conflicts.

Deterministic verification on `2d19847`:

- `clean testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL
- 179 JVM tests across 24 suites (10 tests / 1 suite added by #6); 0 failures,
  0 errors, 0 skips
- Lint: 0 errors, 30 warnings, 3 informational; all pre-existing on `main`
- Debug APK: 60,985,304 bytes, SHA-256
  `6bc5c3d6160667458bebab7562794b0a495e60b30cf5bc8fe224d801a5e8707d`
- GrillTrack ledger: `validate` passes on this head (it fails on `main` only
  because the bootstrap seed left decision 008 without a choice; this branch
  supplies it)

Independent review on exact head `2d19847` via the canonical Spark-2 OpenClaw
rail (`req-20260918T031246Z-25096762431`, engine codex, model gpt-5.6-sol,
thinking high): `scoped-clean`, overall `patch is correct (0.99)`, 0 findings.
The reviewer states its threshold explicitly: only P0 defects and credential
exposure were reportable; lower-priority correctness, freshness, debug-surface,
and test concerns were not surfaced. The widget-freshness gap is therefore
still open and is recorded in the ledger as the next grill, not resolved by
this verdict. Proof: x-api
`runs/spark-openclaw-autoreview-runs/spark-openclaw-autoreview-20260918T031312Z-11965/PROOF.md`.

Device claims were not re-run on the merged APK: #6 changed only the in-app
GitHub-access UI and its test, and no widget, snapshot, or Glance source
differs between `cd737e2` and `2d19847`. The Fold captures above remain the
widget proof for this PR.

## Compact freshness repair (2026-09-18)

ClawSweeper's exact-head advisory review of `2b6fc1b` (comment
`5724629634`) reported one `required_fix` at P1: `LiveSnapshotStore` can
persist a `LAST_GOOD` snapshot and `CompactContent` rendered bare counts with
no age, violating truth rules 2-3. Accepted. The independent read-only OpenClaw
pass on `2d19847` had not surfaced it because it reported at P0 threshold only.

Repair, commit `19a3d1d0995fd0a05c7c88a64364e2c0899e2b80`: the compact slot
carries a freshness element beside the repository name — bare age for `EXACT`
(`12m`), `last good <age>` in the theme error colour and bold for `LAST_GOOD`,
and `no data` when there is no observation. The debug picker gains a
"last good, 3 days old" production render and a "PERSISTED" candidate that
renders whatever `LiveSnapshotStore` actually holds for the public
`saari-co/RepoGlance` repository against the wall clock.

Deterministic verification on `19a3d1d`:

- 185 JVM tests across 25 suites; 0 failures, 0 errors, 0 skips (6 new)
- Lint: 0 errors, 30 warnings, 3 informational; unchanged
- The `LAST_GOOD` Glance assertion was mutation-checked: `"last good 3x"` →
  1 failed of 6; restored → green
- Debug APK on device SHA-256
  `de8ed1a0472b756f6e4c31a5e020a88a8da79baa41d98189c474ffd76a559d04`

Device verification, Pixel 11 Pro Fold, inner display `4619827677550801152`
(outer `4619827677550801153` OFF, virtual display excluded), production
`CompactContent` through the real `GlanceRemoteViews` pipeline at 120x64,
180x64, and 250x90dp:

| State | Path | Rendered | Capture SHA-256 |
| --- | --- | --- | --- |
| EXACT, constructed 2h old | picker | `x-api · 2h · 128 / 23 / 7` | `dfac55618cd906049a63a538d89cb173cb1634666afbcbe16cd7780b29bfc57c` |
| LAST_GOOD, constructed 3d old | picker | `x-api · last good 3d` in error colour | same capture |
| UNKNOWN, no snapshot | picker | `x-api · no data · — / — / —` | same capture |
| **Persisted live, online** | real `LiveSnapshotStore` after opening the repo signed in | `RepoGlance · just now · issues 2 · PRs 1 · to review 0` — matches the repository's open items | `05230e68001b757d9a70cc4db86be95fe45d85ac2f96d7a55695984470711264` |
| **Persisted live, after real refresh failure** | airplane mode on, repo reopened, navigator showed `Could not refresh GitHub right now`; `LiveSnapshotFactory` aged the stored snapshot to `LAST_GOOD` and the store persisted it | `RepoGlance · last good 3m · 2 / 1 / 0`, label in error colour | `e0466e6e9a56090bb75d9ed324b013e3e46f936d456157bf250c71b416237ebc` (two byte-identical captures) |

Source-blind guard before each live step: the catalog was filtered to the
exact public repository and a UI dump proved `saari-co/RepoGlance` was the
only repository-shaped label on screen before the row was opened. One earlier
capture that showed the unfiltered catalog was discarded unread beyond
confirming that fact and is not retained. Airplane mode was restored to off
afterwards. Images are held locally with the hashes above, not committed.

Fidelity note: at the 120dp floor the freshness label takes precedence and
the repository name truncates (`Repo…`). The label is the truth-rule surface,
so it wins; a later cycle may revisit the name treatment.

The account-bound session on the test device was already present from the
maintainer's earlier sign-in; no sign-in, token, or account action was
performed, and no non-public repository content was captured.

## Metadata-quota repair (2026-09-18)

OpenClaw on `5fd1643` (`req-20260918T033615Z-255670529466`): scoped-clean,
0 findings, P0 threshold. ClawSweeper re-review on the same head: proof
`diamond lobster` (sufficient), P1 closed, one new `required_fix` at P2 —
the repository-metadata request was spent whenever the issues page was
truncated, even when the PR page was missing or truncated and the factory had
to fall back regardless. Accepted; this is repair cycle 2 of the authorized
slice.

Repair, commit `a49449707bab7d6ea33c8e510aa6b470b530e670`: the decision moves into
`LiveSnapshotFactory.needsRepositoryMetadata`, gated on a whole PR page; the
view model calls it. Three regression tests: truncated PR page, failed PR page,
and the only case that requests metadata (truncated issues + whole PRs).

- 188 JVM tests across 25 suites; 0 failures, 0 errors, 0 skips
- Lint: 0 errors, 30 warnings, 3 informational; unchanged
- No widget, rendering, or store change since the device proof above, so the
  Fold captures at `19a3d1d` remain the widget proof for this head.
