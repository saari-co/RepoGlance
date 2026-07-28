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
| `github-app-topology-006` | RepoGlance GitHub App owned by saari-co; installations on all three owners (`saariuslystoned`, `saari-co`, `dinkuskit`); sign-in identity `saariuslystoned`; registration/installation is Bobby-manual before Slice 3. | locked |
| `license-posture-007` | Private repository, no license until a distribution decision. | verified |
| `widget-content-priority-008` | Which two-three numbers earn the small-widget slot. | deferred — next grill, five-variant picker on real Slice 2 fixtures |

Supersession note: `watch-roster-003` was locked in the first frontier batch
and superseded minutes later at the shared-understanding gate when Bobby
corrected the model to RepoBar-faithful account-first discovery; history is
preserved in the ledger.
