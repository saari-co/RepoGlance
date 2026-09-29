# Compact widget truth states

The compact home-screen widget shows one repository's open issues, open PRs, and (when tall enough) PRs awaiting the user's review, with a freshness label that never lets a stale count read as current.

## Sub-features

- `compact-exact` renders live counts with the clock time they were observed (`17:00`, `Mon 19:00` from an earlier day).
- `compact-last-good` renders preserved counts with `last good <time>` in an amber family capsule on its own row under the repo name, and the counts merge onto one line (`12 issues · 4 PRs`); while the rate limit is exhausted the capsule is red and reads `rate limited · <time>` (widget-look-032, compact-crowding-034).
- `compact-no-data` renders a neutral `no data` capsule and an em dash per count (`— issues · — PRs`), never `0`.
- `compact-sizes` keeps every state legible from the 140x64dp floor (the widget's minimum width since compact-crowding-034) through 180x64 and 250x90dp with a 24-hour clock. With a 12-hour clock, `last good <day> <h:mm AM>` (data older than today) and `rate limited · <h:mm AM>` truncate at 140x64 and at the Fold's 148x89: a known, deferred gap (compact-12h-fit-035), not a pass. The `to review` count appears only when the widget is at least 84dp tall (148x89 and 250x90).

## How to get to it (user POV)

- Long-press the home screen, add the RepoGlance repo widget, choose a repository in the configuration screen.
- Developer route for the current layout at the floor: `bin/verify-repoglance launch MIXED compact-picker "" "" D` renders fresh, last good, rate limited and no data at 140x64 and 148x89 (tap `repoglance:picker-canvas` for 180x64 and 250x90).

- Launcher-placed widgets: the maintainer pre-authorizes placing, resizing and state-seeding RepoGlance's own widgets on the registered test phone.
  - **Find them.** `bin/verify-repoglance widget-bounds` goes to the home screen and prints each placed RepoGlance widget as `repo|stack <bounds> <w>x<h>dp <texts>`.
  - **Resize.** `bin/verify-repoglance widget-resize repo -255` long-presses the repo widget and drags its bottom handle up 255 px (one row on the Fold).
  - **Seed a state.** `bin/verify-repoglance widget-state last_good|rate_limited|no_data` (debug build only) relabels the stored record and redraws; it stashes the real records first. Always finish with `bin/verify-repoglance widget-state restore`.
  - **Capture.** Dump the launcher, screencap, dump again; keep only the crop of the RepoGlance host view and delete the full-screen image.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes and `adb shell dumpsys trust` shows `deviceLocked=0`.
- The debug APK is current (`./gradlew assembleDebug`); `launch` installs it when the hash differs.

- **Open the picker.** Run `bin/verify-repoglance launch MIXED compact-picker "" "" D`. The resumed activity is `co.saari.repoglance/.devpicker.CompactVariantPickerActivity`; D is the production layout (`WidgetLook.Family`).
- **Read the states.** Run `bin/verify-repoglance dump compact`. The text column contains the trace `On canvas: compact D merged counts · floor + placed · light` and the captions `fresh · 140x64`, `last good · 3d · 140x64`, `rate limited · 140x64`, `no data · 140x64`, plus the same four at `148x89`.
- **Assert fresh.** Each fresh cell shows `RepoGlance`, a clock, `issues` `12` and `PRs` `4` on separate rows; the 148x89 cell adds `to review` `1`.
- **Assert last-good.** The last-good cells show `last good <day> <time>` and the merged line `12 issues · 4 PRs`, with no separate `issues` / `PRs` rows.
- **Assert rate-limited.** The rate-limited cells show `rate limited · <time>` and `12 issues · 4 PRs`.
- **Assert no-data.** The no-data cells show `no data` and `— issues · — PRs`; the string `0` does not appear in those cells.
- **Other sizes.** Run `bin/verify-repoglance tap repoglance:picker-canvas` and dump again for the `180x64` and `250x90` captions.
- **Launcher-placed.** Run `bin/verify-repoglance widget-bounds` (the repo widget should read about `148x89dp` on the Fold). Then run `bin/verify-repoglance widget-state last_good`, `rate_limited` or `no_data`, dump and capture the launcher, and finish with `bin/verify-repoglance widget-state restore`.
- **Proof.** Run `bin/verify-repoglance capture compact-d` twice and compare the SHA-256s; a differing pair means an overlay or clock tick, so capture again. For launcher captures, keep only the crop of the RepoGlance widget and delete the full-screen image.

## Gotchas

- The compact picker renders with the wall clock and the phone's own 12/24-hour setting (`widgetClock`), so assert shapes such as `last good <day> <time>`, not exact times.
- `uiautomator dump` carries the full text even when a capsule is visibly cut; judge truncation on the capture, not the dump. With a 12-hour clock, `last good <day> <h:mm AM>` (data older than today) and `rate limited · <h:mm AM>` truncate at 140x64 and 148x89. That is the known deferred gap compact-12h-fit-035, not a pass.
- Glance widget test tags (`ledger-label`, `ledger-value`, `ledger-freshness`, `ledger-merged`) are visible to the JVM Glance tests, not to `uiautomator dump`; on device assert by visible text.
- `launch MIXED picker` (`WidgetVariantPickerActivity`) is round-1 tooling with a fixed 24-hour clock and the old 120dp floor; do not use it for compact proof.
- Launcher-placed stale states come from the debug-only seeder, which keeps the real counts and times and stashes the real records; always finish with `widget-state restore`.
