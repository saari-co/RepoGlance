# GitHub Access Menu And Owner Filter Proof — 2026-08-13

## Scope

This packet binds the repository-home access-control cleanup and dynamic owner
filter to exact source, automated checks, and privacy-safe Pixel 10 Pro Fold
evidence. It does not claim a GitHub App configuration change, sign-out,
re-authorization, release, or public distribution.

## Exact identity

- Proven source: `6a45a50eb964a36c4031a60528d6dbeca0ed8bff`
- Base: RepoGlance `main` at
  `08d8545f0e94878fe3031a1f5812bd8c1ac0960d`
- APK SHA-256:
  `329dda255896f7b107b276c03196f8e5297859969cfd74def7a43a41e955e423`
- APK size: 60,919,696 bytes
- Installed package: `co.saari.repoglance`, `0.3.0-auth-live` / version code 3
- Device: registered Pixel 10 Pro Fold, Android 16 / API 36, 2076×2152 inner
  display

The fresh local APK and the device's installed `base.apk` had the same SHA-256.
Installation used replacement mode only; app data and the existing authorized
session were preserved.

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

- Focused `GitHubAccessUiSourceWiringTest`: 8 tests, 0 failures/errors/skips.
- Full `clean testDebugUnitTest assembleDebug lintDebug`: PASS, 54 tasks.
- Android lint: 0 errors; 18 warnings retained.
- `git diff --check`: PASS.
- Independent read-only review: CLEAN; no P1/P2/P3 finding.

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

The validator stopped before typing into search because Android accessibility
exposed two editable controls without a unique allowlisted search label. Search
composition remains covered by deterministic JVM tests; the packet does not
claim source-blind Fold proof for search typing, the `dinkuskit` result list, or
filter persistence across a cold launch.

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
- No sign-out, uninstall, data clear, re-authorization, release, or merge
  occurred during this proof.
- The branch still requires normal CI/review and maintainer merge approval.
