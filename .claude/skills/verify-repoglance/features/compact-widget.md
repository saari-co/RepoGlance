# Compact widget truth states

The compact home-screen widget shows one repository's open issues, open PRs, and (when tall enough) PRs awaiting the user's review, with a freshness label that never lets a stale count read as current.

## Sub-features

- `compact-exact` renders live counts with the clock time they were observed (`17:00`, `Mon 19:00` from an earlier day).
- `compact-last-good` renders preserved counts with `last good <time>` in an amber family capsule on its own row under the repo name, and the counts merge onto one line (`12 issues · 4 PRs`); while the rate limit is exhausted the capsule is red and reads `rate limited · <time>` (widget-look-032, compact-crowding-034).
- `compact-no-data` renders a neutral `no data` capsule and an em dash per count (`— issues · — PRs`), never `0`.
- `compact-sizes` keeps every state legible from the 140x64dp floor (the widget's minimum width since compact-crowding-034) through 180x64 and 250x90dp with a 24-hour clock. With a 12-hour clock, `last good <day> <h:mm AM>` (data older than today) and `rate limited · <h:mm AM>` truncate at 140x64 and at the Fold's 148x89: a known, deferred gap (compact-12h-fit-035), not a pass. The `to review` count appears only when the widget is at least 84dp tall (148x89 and 250x90).

## How to get to it (user POV)

- Long-press the home screen, add the RepoGlance repo widget, choose a repository in the configuration screen.
- Developer route: open the GrillTrack widget picker, which renders the production compact composition at all three sizes for exact, last-good, no-data, and the persisted live store.
- Developer route for the current layout at the floor: `bin/verify-repoglance launch MIXED compact-picker "" "" D` renders fresh, last good, rate limited and no data at 140x64 and 148x89 (tap `repoglance:picker-canvas` for 180x64 and 250x90).

- Launcher-placed widgets: the maintainer pre-authorizes placing, resizing and state-seeding RepoGlance's own widgets on the registered test phone.
  - **Find them.** `bin/verify-repoglance widget-bounds` prints each placed RepoGlance widget as `repo|stack <bounds> <w>x<h>dp <texts>`.
  - **Resize.** `bin/verify-repoglance widget-resize repo -255` long-presses the repo widget and drags its bottom handle up 255 px (one row on the Fold).
  - **Seed a state.** `bin/verify-repoglance widget-state last_good|rate_limited|no_data` (debug build only) relabels the stored record and redraws; it stashes the real records first. Always finish with `bin/verify-repoglance widget-state restore`.
  - **Capture.** Dump the launcher, screencap, dump again; keep only the crop of the RepoGlance host view and delete the full-screen image.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- No home-screen placement is required; the picker renders through the real Glance pipeline.

- **Open the picker.** Run `bin/verify-repoglance launch MIXED picker`. The resumed activity is `co.saari.repoglance/.devpicker.WidgetVariantPickerActivity`.
- **Read the states.** Run `bin/verify-repoglance dump picker`. The text column contains `PRODUCTION — exact counts`, `PRODUCTION — last good, 3 days old`, `PRODUCTION — no observation yet`, and `PERSISTED — saari-co/RepoGlance live store, wall clock`.
- **Assert exact.** In the dump, the exact row shows `x-api`, `17:00`, `128`, `23`; the wide cell adds `to review` and `7`.
- **Assert last-good.** The last-good row shows `last good Mon 19:00` beside `x-api` with the same counts.
- **Assert no-data.** The no-observation row shows `no data` and `—` for every count; the string `0` does not appear in that row.
- **Proof.** Run `bin/verify-repoglance capture compact-states` twice and compare the SHA-256s; a differing pair means an overlay or clock tick, so capture again. Both show the four labelled rows.

## Gotchas

- The picker's clock is fixed at 2026-09-17 23:00 UTC and rendered in the phone's zone in 24-hour form; the times above are for US Eastern time and shift with the zone.

- The persisted row reflects whatever the live store holds from the last in-app visit to `saari-co/RepoGlance`; it can legitimately read `no data` on a fresh install or `last good <age>` after a failed refresh. Assert its shape, not its numbers.
- Glance widget test tags (`ledger-label`, `ledger-value`, `ledger-freshness`) are visible to the JVM Glance tests, not to `uiautomator dump`; on device assert by visible text.
- At the 120dp floor the repository name truncates so the freshness label stays whole; `Repo…` is expected, not a defect.
