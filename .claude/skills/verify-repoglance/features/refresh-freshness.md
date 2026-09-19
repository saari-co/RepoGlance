# Refresh and freshness

Every RepoGlance surface says how old its data is and what the GitHub rate limit looks like, refresh is explicit, and a refresh that fails leaves the last good data visible with its age instead of pretending it is current.

## Sub-features

- `fresh-catalog-age` shows `Updated <age>` and `GitHub rate limit: N remaining` under the catalog title.
- `fresh-refresh-catalog` re-fetches the catalog from the refresh control.
- `fresh-refresh-repo` re-fetches one repository's issues and PRs.
- `fresh-failed` shows `Could not refresh GitHub right now` and `No current value` after a failed refresh, and marks the compact widget's persisted counts `last good <time>`.
- `fresh-rate-limit` shows `GitHub rate limit is low`, `is exhausted`, or `is unknown` states with a visible wait when exhausted.
- `fresh-rate-limit-widgets` while exhausted, the placed widgets read `rate limited · <time>` (compact), `rate limited · resets <time> · …` (tall) and `Pinned · N · rate limited · resets <time>` (stack) with their last saved counts; at low, no widget shows rate-limit text and only repositories with their own repo widget refresh.

## How to get to it (user POV)

- Live catalog: age and rate-limit lines under the title; the refresh icon beside `LIVE`.
- Repository view: the refresh icon beside the repository name; section headers carry `Updated <age> · Rate limit N remaining`.
- Compact widget: the freshness label beside the repository name, a clock time rather than an age because the widget picture is frozen between redraws.
- Tall repo widget and stack widget: the header line carries `rate limited · resets <time>` while the background refresh backs off.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- Live session present (human-gated; otherwise the fixture-only parts of this recipe still apply through the picker and navigator).
- Airplane mode off at the start.

- **Read catalog freshness.** Run `bin/verify-repoglance launch MIXED live` then `dump fresh-catalog`. The dump contains `repoglance:rate-limit` with text starting `GitHub rate limit` and a title line containing `Updated`.
- **Refresh the catalog.** Run `bin/verify-repoglance tap repoglance:refresh-repositories`, wait 3 seconds, `dump fresh-refreshed`. The title line reads `Updated just now`.
- **Open the public repo.** Follow Find a repository through the guard, open `saari-co/RepoGlance`, then `dump repo-fresh`. Section headers contain `Updated just now · Rate limit`.
- **Force a failed refresh.** Run `adb shell cmd connectivity airplane-mode enable`, wait 60 seconds, `bin/verify-repoglance tap repoglance:refresh-repository`, wait 5 seconds, `dump repo-offline`. The dump contains `Could not refresh GitHub right now` and `No current value`.
- **Widget consequence.** Run `bin/verify-repoglance launch MIXED picker` then `dump picker-offline`. The `PERSISTED` row reads `last good <time>` with counts preserved.
- **Restore.** Run `adb shell cmd connectivity airplane-mode disable` (cleanup also does this) and confirm `adb shell settings get global airplane_mode_on` prints `0`.
- **Proof.** `capture fresh-offline` on the repository view and `capture picker-offline` on the picker; both SHA-256s go in the packet.
- **Force a rate limit (debug fault).** The debug launcher arms a rate-limit fault on the app's GitHub reads until a reset time, and can start the production background refresh. `LOW` makes the real call and reports 250 of 5000 remaining; `EXHAUSTED` answers 403 with 0 remaining and never calls GitHub. Arm and run: `adb shell am start -n co.saari.repoglance/.devlaunch.ScenarioLaunchActivity --es screen none --es rateFault EXHAUSTED --el rateFaultResetSeconds 180 --ez refreshNow true`, wait 15 seconds, go to the home page holding the widgets, `dump widgets-exhausted`. The dump shows the `rate limited` strings above with unchanged counts, never `0`. `adb shell run-as co.saari.repoglance cat shared_prefs/repoglance_debug_fault.xml` counts the faulted responses (`served.EXHAUSTED`, last status `403`).
- **Low.** Arm `--es rateFault LOW` the same way and run twice (the second run starts from the saved low state), then `dump widgets-low`: the repo widget's time advanced and no widget shows `rate limited`. `launch MIXED live` then `dump catalog-low`: `GitHub rate limit is low: 250 remaining`.
- **Recovery.** After the reset time, `dump widgets-after-reset` still shows `rate limited · resets <past time>` (widgets redraw only when a run finishes); `--es screen none --ez refreshNow true`, wait 15 seconds, `dump widgets-recovered`: no `rate limited` text, times advanced, real counts.
- **Disarm.** `--es screen none --es rateFault OFF`; the fault also expires on its own at its reset time.

## Gotchas

- `Updated <age>` ticks once a minute only while the screen is resumed; wait a full minute after airplane mode before expecting `1m`.
- The catalog refresh control lives beside `LIVE`; the repository refresh control is a different control with its own id. Tapping the wrong one refreshes the wrong thing and still succeeds.
- A failed refresh keeps the previous rows on screen by design; the failure text sits in the section header, not in a dialog.
- Force `LOW`/`EXHAUSTED` with the debug fault above, never by burning real quota. It is debug-only; a release build ignores it. The fixture navigator's `RATE_LIMITED` scenario still covers the fixture screens.
- After an `EXHAUSTED` fault the app also holds GitHub reads in memory until the reset time; a refresh before then stays rate limited even once the fault is disarmed.
- The low-limit skip of pinned repositories without their own repo widget is only visible with at least two pins; with one pin, only the widget repo's refresh shows.
