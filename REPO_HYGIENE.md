# Repo Hygiene

The root is a lobby, not a warehouse.

## Front door

`README.md`, `AGENTS.md`, `VISION.md`, `REPO_HYGIENE.md` — plus standard
build manifests once the Android lane is authorized. No other root markdown.

## Where things go

```text
docs/       durable depth (PRODUCT_DECISIONS.md, architecture notes)
plans/      dated plans and migration notes (product plan stays in x-api)
proof/      committed text/machine-readable proof packets by date
runs/       ignored local run output as runs/<lane>-runs/<run-id>/
worktrees/  ignored local checkout forest
.grilltrack/  GrillTrack ledger (tracked) + work/ (ignored)
```

## Rules

- Screenshots/recordings are `saari-co/swarm-pr-assets` release assets
  linked from `PROOF.md`, not Git blobs here.
- Never commit tokens, keystores, `local.properties`, or auth material.
- Evidence is never deleted; cleanup is inventory-first and human-gated.
