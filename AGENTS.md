# Agent Contract

RepoGlance is a read-only, widget-first GitHub glance and issue navigator
for Google Pixels. This file is the repo-wide operating contract for humans
and agents; keep it durable and contract-only.

## Source Priority

1. This `AGENTS.md`
2. Current explicit maintainer instruction
3. `docs/PRODUCT_DECISIONS.md` and `.grilltrack/ledger.json` (canonical
   decision ledger; the maintainer's private planning repo holds the
   originating product plan)
4. Current source, tests, fixtures, and committed proof

## Implementation State

The Android project, fixture-first widgets/navigator, and prototype live GitHub
read path exist on stacked development branches. Source-changing work requires
an explicitly routed, isolated worktree and proof trail. `main` remains honest
about what has actually merged and been proven.

## Truth Rules

1. Read-only: no GitHub mutation of any kind in v0.x — no comment, close,
   label, assign, merge, re-run, subscribe.
2. Unknown never renders as zero; failed fetches show last-good with age
   or Unknown.
3. Stale is labelled: every surface renders its data age; last-good never
   claims to be current.
4. Rate limit is first-class state; back off visibly, never silently serve
   old numbers as fresh.
5. Auth is a GitHub App user token obtained through GitHub's device flow: the
   user code stays visible in RepoGlance and verification opens in a Custom
   Tab. Never read or reuse browser cookies. Never expose, print, log, or
   commit token values; sign-in is user-present, tokens are handled only
   on-device, and persistent custody is Keystore-backed.
6. Pinned repos are the only background-refresh set; discovery lists fetch
   on demand. No wakelocks, no foreground polling service. Pinning is an
   in-place row toggle with no preset pins.

## Boundaries

- No server-side component and no authentication callback. Authentication and
  live reads go directly from the Android app to fixed GitHub HTTPS endpoints.
- Not a session dashboard, not a chat client, and never a full GitHub
  client: full threads and all actions deep-link to the installed GitHub
  app.
- Honest `main`: README and docs distinguish planned, available, and
  proven behavior; unproven behavior is never claimed.

## Hygiene And Proof

- Root markdown stays at the front-door set (`README.md`, `AGENTS.md`,
  `VISION.md`, `REPO_HYGIENE.md`, `LICENSE`).
- Committed proof is text/machine-readable under `proof/<dated-packet>/`;
  screenshots/recordings live in external release-asset storage and are
  linked from proof, not committed as Git blobs.
- Generated/run output lives in ignored `runs/*-runs/`.
- `.grilltrack/` is the durable decision ledger: ledger and curated proof
  are tracked; `work/` is ignored and never auto-deleted. GrillTrack
  activates only on the maintainer's explicit request; history is
  non-destructive — decisions are superseded, never erased.

## Hard Gates

Maintainer approval is required for: merges; GitHub App registration,
installation, or configuration; any sign-in or token rotation; releases,
signing, and store distribution; any write scope; license or visibility
changes; deletes of proof or decisions. Never inspect or commit secrets,
tokens, keystores, `local.properties`, or auth logs.
