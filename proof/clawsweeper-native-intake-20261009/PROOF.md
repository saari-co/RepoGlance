# Native comment transport migration

Replaces the legacy comment producer with the exact template reviewed and merged in saari-co/spark-dgx#209 (`4a66d1e439703d441df5ab3ebf8107aa21f5985f`).

Template SHA256: `f7321a628d3b0a9dbbd9ebdd07eee2fca136606e424dbf56e91ae481db8766b1`.

The new producer runs only when `CLAWSWEEPER_NATIVE_INTAKE_ENABLED=true`. Existing scoped dispatch credentials are consumed unchanged. The receiver validates live repository, source run, artifact, actor, PR and base/head identities before accepting work. No duplicate comment producers remain. Automatic PR transport stays as enrolled.

Publication and live review happen only after maintainer-authorized merge and flag cutover.

Validation: `actionlint .github/workflows/clawsweeper-comment-trigger.yml` and `git diff --cached --check` passed. The transport is byte-identical to the template already reviewed in spark-dgx#209; no app/UI behavior changes.
