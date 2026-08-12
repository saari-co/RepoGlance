# Product Decisions

Canonical ledger: `.grilltrack/ledger.json` (track
`gt-20260728163459-227573`). This file is the readable index; the ledger
wins on conflict.

## GrillTrack cycle 1 — product identity and bootstrap contract (2026-07-28)

| id | decision | status |
| --- | --- | --- |
| `product-name-001` | Name is **RepoGlance**; repo `saari-co/RepoGlance`, checkout `~/Developer/side-quests/RepoGlance`, GitHub App named RepoGlance. | verified |
| `repo-bootstrap-002` | Private saari-co repository created now with a contract-only seed (no Android project until the build lane is routed). | verified |
| `watch-roster-003` | Curated watch-list as the access boundary (Swarm core + website family). | superseded by `access-model-004` |
| `access-model-004` | **Account-first discovery**: sign in as `saariuslystoned`; owner switcher `saariuslystoned` · `saari-co` · `dinkuskit`; all repos + PR/issue pressure per owner; pin/hide curation. Default pins: x-api, swarmpocket, SwarmBar, SwarmDash, smokyworker_setup, pixel-fold, spark-dgx, smokyworks, smokyproductco, smokyclub, aicommerce, dinkuskit/blocks. Pinned repos are the widget + background-refresh set; discovery fetches on demand. | verified |
| `navigator-scope-005` | Issue navigator scope model: account-wide \| org-wide \| single-repo, each × issues-only \| PRs-only \| both, composing with the Open/Mine/Mentions/Recently-updated/Awaiting-my-review filters. | verified |
| `github-app-topology-006` | RepoGlance GitHub App owned by saari-co; installations on all three owners; sign-in identity `saariuslystoned`. | superseded by `app-public-011` |
| `license-posture-007` | Private repository, no license until a distribution decision. | superseded by `public-mit-009` |
| `widget-content-priority-008` | Which two-three numbers earn the small-widget slot. | deferred — next grill, five-variant picker on real Slice 2 fixtures |

Supersession note: `watch-roster-003` was locked in the first frontier batch
and superseded minutes later at the shared-understanding gate when Bobby
corrected the model to RepoBar-faithful account-first discovery; history is
preserved in the ledger.

## GrillTrack cycle 2 — public release posture (2026-07-29)

Reopened by the maintainer: quick build expected, others will want to use
it. `access-model-004` was re-verified under the public model with its
default-pin aspect refined by `pin-model-010`.

| id | decision | status |
| --- | --- | --- |
| `public-mit-009` | Public repository under the MIT license (copyright saari-co), published as-is with full history; honest-main discipline — README distinguishes planned, available, and proven. Supersedes `license-posture-007`. | implemented (verified at PR #1 merge) |
| `pin-model-010` | No preset pins. Any repo visible in the connected account/orgs is pinnable via an in-place row toggle (Claude Code session-pinning interaction); pinned repos sort first and form the widget and background-refresh set. The maintainer's twelve pins are personal config, not product defaults. | implemented (in code at PR #4: in-place toggle, pins-first sort, pinned set drives the stack widget; device-proven on the XL) |
| `app-public-011` | The RepoGlance GitHub App is public and installable by any user on their own accounts/orgs; the maintainer's `saariuslystoned`/`saari-co`/`dinkuskit` installations are personal configuration. Registration remains maintainer-manual before Slice 3. Supersedes `github-app-topology-006`. | locked |
| `delivery-rail-012` | Merge → CI builds the APK → rolling GitHub release → cp-1 pulls within 30 minutes → update banner on the maintainer's phone over WireGuard → one-tap install (the SwarmPocket rail, generalized). The public rolling release doubles as the public sideload channel; puller/banner config stays swarm-side. First install is one manual sideload; signing keystore and CI secrets are maintainer-gated. | locked |
