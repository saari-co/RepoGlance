# Native review retarget correction

Source base: `8aa2ded00fd6cedf096c9f736adc07a3ba47c3b1`.
Workflow Git blob: `b80258f0f0116a8f28f8f60a79f8c9e57f6bfa98`.

Ready PRs retargeted from another base to the default branch currently miss automatic review. Accept base edits and apply the existing live same-repository/default-branch checks; ignore body-only edits before API reads and dispatch.

The caller matches the qualified retarget template byte-for-byte. Prior live proof verified default-base retarget dispatch, nondefault-base refusal and no body-only dispatch. Current-source CI, OpenClaw P3 and native ClawSweeper review remain required before merge.

Validation: actionlint and diff hygiene passed. `actionlint` and `git diff --check` passed for the workflow-only change.

Auditable live event proof for the byte-identical workflow (Git blob `b80258f0f0116a8f28f8f60a79f8c9e57f6bfa98`):

- [Nondefault base](https://github.com/saari-co/public-oss-proof-assets/actions/runs/37636160372): request/artifact/dispatch skipped, zero artifacts.
- [Retarget to default base](https://github.com/saari-co/public-oss-proof-assets/actions/runs/37636254997): request/artifact/dispatch completed.
- [Body-only edit](https://github.com/saari-co/public-oss-proof-assets/actions/runs/37638153063): request/artifact/dispatch skipped, zero artifacts.

[Live canary PR](https://github.com/saari-co/public-oss-proof-assets/pull/4) was closed unmerged after proof. This is shared-template behavior evidence; this PR also has its own current-head local validation, CI and native/OpenClaw reviews.

The change affects repository review transport only. Product code and caller activation are unchanged.
