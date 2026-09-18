# GitHub Access Menu And Owner Filter Proof — 2026-08-13, re-run 2026-09-17

## Scope

This packet binds the repository-home access-control cleanup and dynamic owner
filter to exact source, automated checks, and privacy-safe Pixel 10 Pro Fold
evidence. It does not claim a GitHub App configuration change, sign-out,
re-authorization, release, or public distribution.

## Exact identity

- Proven application source: `a089e50ffc3568b7fb72f31acb7eb2fb3cad6f74`
- Branch head at this run: `393579d` — a merge of `origin/main` into the
  branch. The merge changes no file under `app/src`, so the proven
  application source above is unaffected by it; `a089e50` remains an ancestor
  of the head.
- Base: RepoGlance `main` at
  `08d8545f0e94878fe3031a1f5812bd8c1ac0960d`, brought current by the merge
- APK SHA-256:
  `191a32d014e7c8f6c0fc03cd86f41f5eb66c908845983d33165e0980d5be940e`
- APK size: 60,919,688 bytes
- Installed package: `co.saari.repoglance`, **`0.0.0-dev` / version code 1**
- Device: registered **Pixel 11 Pro Fold**, Android 17 / API 37, 2076×2152
  inner display, physical id `4619827677550801152` (outer display
  `4619827677550801153` was OFF and excluded)

The fresh local APK and the device's installed `base.apk` had the same SHA-256.
Installation used replacement mode only; app data was preserved.

The version is no longer `0.3.0-auth-live` / code 3. Merging `main` brought in
the tag-driven versioning from the release pipeline, which derives the version
from an exact Git tag and otherwise falls back to a development identity. This
head is ten commits past `v0.3.0-beta.1`, so it reports `0.0.0-dev` / code 1.
That is the designed behavior for a non-tagged build, not a regression, and it
means any branch build now carries a development version.

Debug APKs are **not byte-reproducible** — the same commit built twice in one
session yields different hashes, because the build embeds timestamps. The hash
above identifies the exact artifact installed for this run and pins the
local-build/device-install match; it is not re-derivable from the commit alone.
The 60,919,696-byte size is the stable signal across builds.

### Prior run

An earlier packet for this branch proved `6a45a50eb964a36c4031a60528d6dbeca0ed8bff`
on a Pixel 10 Pro Fold (Android 16 / API 36, APK SHA-256
`329dda255896f7b107b276c03196f8e5297859969cfd74def7a43a41e955e423`). Its
sanitized media remains valid for that commit and is retained below. This packet
supersedes it and covers the current branch head, which adds unique
accessibility identifiers to the two editable controls on the loaded home.

### Session

The device's August authorization had expired. The maintainer re-authorized the
device flow on the phone on 2026-09-17 before this run, and the session
remained live across the subsequent merge and reinstall. The agent did not
complete, observe, or retain any part of that authorization. No credential,
token, or authorization value entered agent context.

## Implemented behavior

- Removes the permanent bottom `Repositories` / `Disconnect` bar.
- Adds header-menu actions named `Manage GitHub access` and
  `Disconnect GitHub`.
- Keeps `Choose repositories on GitHub` in the true empty-catalog state.
- Refreshes the live repository catalog once after returning from GitHub access
  management, without replacing the existing device-flow resume call.
- Adds an `All` / account / organization selector derived from the owners of
  repositories actually visible to RepoGlance.
- Composes the owner selection with repository search, preserves a valid owner
  across catalog refresh, and falls back to `All` if that owner disappears.

## Automated verification

- Focused `GitHubAccessUiSourceWiringTest`: **10** tests, 0 failures/errors/skips.
- Full suite: **168 tests across 22 suites**, 0 failures/errors/skips.
- Full `clean testDebugUnitTest assembleDebug lintDebug`: PASS, 54 tasks executed.
- Android lint: **0 errors; 27 warnings** retained. The rise from the prior
  run's 18 is dependency-age warnings accrued since August plus three
  `AutoboxingStateCreation` warnings that a clean run surfaces on pre-existing
  `main` code. This branch's diff contributes no warnings.
- `git diff --check`: PASS.
- Independent read-only review: CLEAN at `6a45a50`; not re-run for `a089e50`.

## Physical Fold verification

The source-blind validator installed the exact APK with `adb install -r` and
cold-launched RepoGlance without clearing data. Clauses proven on-device:

1. The existing authorized `LIVE` session remained available.
2. Exact footer labels `Repositories` and `Disconnect` were absent.
3. The header menu contained exactly `Manage GitHub access` and
   `Disconnect GitHub`; neither action was invoked.
4. The owner selector contained `All`, `saariuslystoned`, `dinkuskit`, and
   `saari-co` exactly once each.
5. After selecting `saari-co`, the on-device predicate counted five visible
   repository-shaped labels: five owned by `saari-co`, zero by another owner.
   No repository label left the device.

### Clauses newly proven at `a089e50`

The prior run stopped before typing into search because Android accessibility
exposed two editable controls without a unique allowlisted search label.
`a089e50` publishes Compose `testTag` values as accessibility resource ids and
tags both controls, which closes that gap. Re-run on the Pixel 11 Pro Fold with
a live session:

6. Both controls are uniquely addressable through accessibility:
   `repoglance:repo-search` and `repoglance:owner-filter` appear as
   `resource-id` values on the loaded home. Two editable controls are present,
   as before, but each is now individually selectable.
7. **Search entry.** Addressing the field by its resource id, typing `glance`
   left the field reading exactly `glance` and reduced the visible repository
   list from five rows to one, and that row matched the query.
8. **Owner menu.** The selector offered `All` plus exactly three owner choices:
   `dinkuskit`, `saari-co`, `saariuslystoned`.
9. **`dinkuskit` result list.** Selecting `dinkuskit` set the selector to
   `dinkuskit` and produced five repository rows, five owned by `dinkuskit` and
   zero owned by anyone else.
10. **Cold-launch behavior.** After `am force-stop` and a cold relaunch the
    owner filter read `All` and the search field was empty. The filter is
    therefore session-scoped and does **not** survive process death. This is
    consistent with the branch's claim, which is retention across catalog
    refresh rather than across cold launch, and it is now a verified result
    rather than an open question.

### Re-verified after merging `main`

Clauses 6 and 7 were re-run against the merged head's APK
(`191a32d014e7c8f6c0fc03cd86f41f5eb66c908845983d33165e0980d5be940e`, matching
the device's installed `base.apk` exactly). Both controls still expose
`repoglance:repo-search` and `repoglance:owner-filter`, and typing `glance`
again left the field reading `glance` with the list reduced from five rows to
one. Clauses 8 to 10 rest on application source the merge does not touch.

The live session remained intact throughout. No repository name, URL,
authentication material, or clipboard content was captured or retained; the
counts above were derived from accessibility structure and the raw uiautomator
dumps were discarded after measurement.

### Known repository condition, not caused by this branch

`grilltrack_ledger.py validate` fails on this branch with
`widget-content-priority-008.choice must be a non-empty string`. That is a
pre-existing defect in the bootstrap seed on `main`, which this branch merely
inherits through the merge; `main`'s own ledger carries the same null value. It
is repaired separately in the compact-widget branch and is out of scope here.

## Sanitized media

Immutable release:
[repoglance-access-owner-filter-6a45a50](https://github.com/saari-co/swarm-pr-assets/releases/tag/repoglance-access-owner-filter-6a45a50)

| asset | bytes | SHA-256 | claim |
| --- | ---: | --- | --- |
| [account-access-menu.png](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-access-owner-filter-6a45a50/account-access-menu.png) | 16,256 | `40e81fb533976d68286d1382b0d8fc60ea030e84c804307cf2fbb4dbddb924e3` | Header menu contains only the two intended access actions. |
| [owner-selector-options-safe.png](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-access-owner-filter-6a45a50/owner-selector-options-safe.png) | 14,633 | `c77fc029597e9f464984ee1f2c48f702292ca34c343d3903ce9a39cab2b21189` | Owner selector contains the required All/account/org choices. |

Both crops were visually inspected before publication and contain no repository
list, authentication material, token, URL, clipboard content, or private
repository name. Raw and unselected captures remain outside product git.

## Gates and limits

- No GitHub App setting, account, repository permission, token, or device
  setting changed.
- No sign-out, uninstall, data clear, release, or merge occurred during this
  proof. The agent performed no re-authorization: the maintainer completed the
  expired device flow himself before the run, and the agent neither tapped
  through nor observed it.
- On-device actions were limited to: tap search, type, clear, open the owner
  menu, select an owner, force-stop, relaunch. `Manage GitHub access` and
  `Disconnect GitHub` were never invoked.
- The branch still requires normal CI/review and maintainer merge approval.
