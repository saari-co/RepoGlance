# Agent Contract

RepoGlance is a read-only, widget-first GitHub glance and issue navigator
for Google Pixels. This file is the repo-wide operating contract; keep it
durable and contract-only.

## Source Priority

1. This `AGENTS.md`
2. Current explicit human (Bobby) instruction
3. The x-api product plan `plans/repobar-pixel-companion-plan-20260728.md`
   (saari-co/x-api#522) at its pinned/merged commit
4. `.grilltrack/ledger.json` and `docs/PRODUCT_DECISIONS.md`
5. Current source, tests, fixtures, and committed proof

## Implementation Hold

No Android project, source, CI, icon, or dependency work exists or may begin
until Bobby explicitly routes the Slice 1–2 build lane. Until then this
repository holds contracts, decisions, and proof only.

## Truth Rules

1. Read-only: no GitHub mutation of any kind in v0.x — no comment, close,
   label, assign, merge, re-run, subscribe.
2. Unknown never renders as zero; failed fetches show last-good with age or
   Unknown.
3. Stale is labelled: every surface renders its data age; last-good never
   claims to be current.
4. Rate limit is first-class state; back off visibly, never silently serve
   old numbers as fresh.
5. Auth is a GitHub App user token via Chrome Custom Tab sign-in. Never read
   or reuse browser cookies. Never handle, print, store, or commit token
   values; sign-in is Bobby-present, on-device, Keystore-backed.
6. Pinned repos are the only background-refresh set; discovery lists fetch
   on demand. No wakelocks, no foreground polling service.

## Boundaries

- No CP-1, broker, WireGuard, x-api, or SwarmDash runtime dependency.
- Not a session roster (SwarmBar's job) and not a Moshi replacement.
- Never a full GitHub client: full threads and all actions deep-link to the
  installed GitHub app.
- Registered swarm test devices only (Fold first) until Bobby gates other
  installs; store distribution is a separate human gate.

## Hygiene And Proof

- Follow `REPO_HYGIENE.md`. Root markdown stays at the front-door set.
- Committed proof is text/machine-readable under `proof/<dated-packet>/`;
  screenshots/recordings go to `saari-co/swarm-pr-assets` release assets and
  are linked from `PROOF.md`.
- Generated/run output lives in ignored `runs/*-runs/`.
- `.grilltrack/` is the durable decision ledger: ledger and curated proof
  are tracked; `work/` is ignored and never auto-deleted. GrillTrack
  activates only on Bobby's explicit request.

## Hard Gates

Human approval is required for: merges; GitHub App registration or
installation; any sign-in or token rotation; installs on non-test devices;
store distribution; any write scope; license/visibility changes; deletes of
proof or decisions. Never inspect or commit secrets, tokens, keystores,
`local.properties`, or auth logs.
