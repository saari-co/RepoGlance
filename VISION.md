# Vision

Answer "what is the current pressure and CI truth of the repositories I care
about" from a Pixel home screen, honestly, in two seconds.

## Scope

- Material You Glance widgets: per-repo and stack, always showing data age.
- Owner switcher across the connected account's orgs; all repos visible;
  pin/hide curation where pins earn widgets and background refresh.
- Read-only issue navigator: account/org/repo scopes × issues/PRs/both,
  with instant search over cached rows and deep links to the GitHub app.
- Pixel-specific fit: Fold cover/inner sizes, Quick Settings attention
  tile, one watched-run live notification, battery-honest refresh.
- Installable by collaborators with nothing but their own GitHub sign-in.

## Non-Scope

- GitHub writes (v0.x), full GitHub-client ambitions, live search APIs.
- Swarm session truth (SwarmBar), chat/artifacts (SwarmPocket), CSv0.1.
- Store distribution, analytics, telemetry, notification firehoses.

## Guardrails

Truthful rendering beats freshness theater: unknown is never zero, stale is
never current, rate-limited is a visible state. If a surface cannot tell the
truth cheaply, it does not ship.
