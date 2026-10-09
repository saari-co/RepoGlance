# Native comment transport migration

Replaces the legacy comment producer with the template reviewed and merged in saari-co/spark-dgx#209 (`4a66d1e439703d441df5ab3ebf8107aa21f5985f`).

Parent template SHA256: `f7321a628d3b0a9dbbd9ebdd07eee2fca136606e424dbf56e91ae481db8766b1`.

The new producer runs only when `CLAWSWEEPER_NATIVE_INTAKE_ENABLED=true`. Existing scoped dispatch credentials are consumed unchanged. The receiver validates live repository, source run, artifact, actor, PR and base/head identities before accepting work. No duplicate comment producers remain. Automatic PR transport stays as enrolled.

Publication and live review happen only after maintainer-authorized merge and flag cutover.

Validation: `actionlint .github/workflows/clawsweeper-comment-trigger.yml` and `git diff --cached --check` passed. The shared template is adapted to preserve the public-repository OWNER/MEMBER-only publication boundary; no app/UI behavior changes.

Accepted native P1 finding: the parent template includes COLLABORATOR publication authority. This deployment removes COLLABORATOR from the trusted source set to preserve the previous public-repository policy. PR-author reruns remain read-only (`publish=false`).

After-fix execution proof: 30 controlled executions of the actual extracted workflow program per repository passed (60 total). OWNER/MEMBER publish; non-author COLLABORATOR never dispatches; non-maintainer PR-author re-review/re-run dispatches publish=false; first review, multiple commands, inline prose, draft/closed/wrong-base requests skip. Repository ID/head/comment identity vary per case and emitted payloads follow them. `behavior-report.json` and `source-identity.json` bind evidence to workflow SHA256 `ddc7f178617304c61d0ccd4d6813de108f85b884fbbe52edf7c9f1caf91a3bd0`. These are controlled event/API executions, not live GitHub Actions cutover proof.

The feature-flagged comment path cannot execute from an unmerged feature branch: issue_comment always runs the default-branch workflow. Live cutover proof is a post-merge deployment gate; the new source flag stays off until receiver readiness. Existing automatic native source and admission/dispatch artifact contract have live production receipts already. No template credential scopes are enlarged.
