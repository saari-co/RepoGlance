# Compact widget truth states

The compact home-screen widget shows one repository's open issues, open PRs, and (when tall enough) PRs awaiting the user's review, with a freshness label that never lets a stale count read as current.

## Sub-features

- `compact-exact` renders live counts with their bare age (`2h`).
- `compact-last-good` renders preserved counts with `last good <age>` in the error colour.
- `compact-no-data` renders `no data` and an em dash per count, never `0`.
- `compact-sizes` keeps all rows legible at 120x64, 180x64, and 250x90dp; the `to review` row appears only at the wide size.

## How to get to it (user POV)

- Long-press the home screen, add the RepoGlance repo widget, choose a repository in the configuration screen.
- Developer route: open the GrillTrack widget picker, which renders the production compact composition at all three sizes for exact, last-good, no-data, and the persisted live store.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- No home-screen placement is required; the picker renders through the real Glance pipeline.

- **Open the picker.** Run `bin/verify-repoglance launch MIXED picker`. The resumed activity is `co.saari.repoglance/.devpicker.WidgetVariantPickerActivity`.
- **Read the states.** Run `bin/verify-repoglance dump picker`. The text column contains `PRODUCTION — exact counts`, `PRODUCTION — last good, 3 days old`, `PRODUCTION — no observation yet`, and `PERSISTED — saari-co/RepoGlance live store, wall clock`.
- **Assert exact.** In the dump, the exact row shows `x-api`, `2h`, `128`, `23`; the wide cell adds `to review` and `7`.
- **Assert last-good.** The last-good row shows `last good 3d` beside `x-api` with the same counts.
- **Assert no-data.** The no-observation row shows `no data` and `—` for every count; the string `0` does not appear in that row.
- **Proof.** Run `bin/verify-repoglance capture compact-states` twice and compare the SHA-256s; a differing pair means an overlay or clock tick, so capture again. Both show the four labelled rows.

## Gotchas

- The persisted row reflects whatever the live store holds from the last in-app visit to `saari-co/RepoGlance`; it can legitimately read `no data` on a fresh install or `last good <age>` after a failed refresh. Assert its shape, not its numbers.
- Glance widget test tags (`ledger-label`, `ledger-value`, `ledger-freshness`) are visible to the JVM Glance tests, not to `uiautomator dump`; on device assert by visible text.
- At the 120dp floor the repository name truncates so the freshness label stays whole; `Repo…` is expected, not a defect.
