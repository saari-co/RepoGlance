# Navigator

The navigator lists open issues and pull requests for an account, an organization, or one repository, with mode (issues, PRs, both), filters, cached-row search, and a row detail that deep-links to the GitHub app.

## Sub-features

- `nav-open-repo` opens the navigator scoped to one fixture repository.
- `nav-mode` switches between `ISSUES`, `PRS`, and `BOTH`.
- `nav-filter` applies `Open`, `Mine`, `Mentions`, `Recently updated`, or `Awaiting my review`.
- `nav-row-wide` (inner display) opens a tapped row in the GitHub app beside the list.
- `nav-row-narrow` (cover display) selects a tapped row and shows the in-app detail with `Open on GitHub`.
- `nav-home` returns to the fixture home, which carries the scenario switcher and the pin toggles.
- `nav-truth` shows `Loaded`, `Empty`, `Last-good` (rendered as a `Cached · <age>` chip), or `Data unavailable` list states chosen under `Filters → Fixture state`.

## How to get to it (user POV)

- Tap a repo widget's header or a row: opens the navigator for that repository and mode.
- From the fixture home screen, tap `Navigator` (account scope) or a repository card (repo scope).

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- Scenario `MIXED` unless the recipe names another; the navigator's own list state is chosen in `Filters`, not by the launcher.

- **Open for a repository.** Run `bin/verify-repoglance launch MIXED navigator acme/rocket BOTH`. The helper waits until `co.saari.repoglance/.MainActivity` is resumed and the dump contains `repoglance:fixture-home`.
- **Read the chrome.** Run `bin/verify-repoglance dump navigator`. The dump contains resource ids `repoglance:fixture-home`, `repoglance:fixture-mode-ISSUES`, `repoglance:fixture-mode-PRS`, `repoglance:fixture-mode-BOTH`, the scope text `acme/rocket`, and the `Issues` section header (`PRs` sits below the fold in `BOTH` mode; assert it in `PRS` mode).
- **Switch mode.** Run `bin/verify-repoglance tap repoglance:fixture-mode-PRS` then `dump prs-only`: the `PRs` header is present and `Issues` is absent. Then `tap repoglance:fixture-mode-ISSUES` and `dump issues-only`: `Issues` is present, `PRs` absent, with rows shaped `#100` / `Fix flaky retry in sync worker` / `open` / `bug` / `by octodev · 0 comments · Updated 7m ago`.
- **Truth state.** Run `bin/verify-repoglance tap Filters`, then `dump filters`. The sheet lists `Fixture state` with `Loaded`, `Empty`, `Last-good`, and (below the fold; scroll the sheet to reach it) `Data unavailable`. Tap `Last-good`; the sheet closes on selection, so do not press `BACK`. Run `dump last-good`: the list header carries a `Cached · <age>` chip, the visible form of last-good data, instead of rows presented as fresh. The filter label now reads `Filters · 1`; tap it, tap `Loaded`, and the chip disappears.
- **Select a row on the inner display.** Run `bin/verify-repoglance tap "Fix flaky retry in sync worker"`, wait 2 seconds, `dump row-wide`. The GitHub app opens in the adjacent pane and the navigator stays visible on the left; the dump reads the GitHub window (`com.github.android` ids) and, for a fixture repository, `Could not resolve to a Repository with the name 'acme/rocket'`. Capture, then run `adb shell am force-stop com.github.android` to close the pane.
- **Select a row on the cover display (maintainer-gated).** Emulating `CLOSED` with `adb shell cmd device_state state 0` locks the phone and lights the cover display's always-on screen, so an agent cannot reach this step alone. The maintainer folds the phone and unlocks it on the cover display; then run `bin/verify-repoglance launch MIXED navigator acme/rocket ISSUES`, `dump cover-list`, `tap "Fix flaky retry in sync worker"`, wait 2 seconds, `dump cover-detail`. The detail replaces the list and shows `Author:`, `Labels:`, `Comments:`, and an `Open on GitHub` control; capture follows the active panel. Without the maintainer, report `verified-unreachable: cover display needs an unlocked folded phone`.
- **Return home.** Run `bin/verify-repoglance tap repoglance:fixture-home` then `dump home`. The dump contains `repoglance:scenario` and `repoglance:fixture-navigator`.
- **Proof.** `capture navigator-issues` after the mode switch, `capture navigator-last-good` with the `Cached · <age>` chip showing, `capture navigator-row-wide` with the GitHub pane open, `capture navigator-cover-detail` on the cover display when the maintainer has folded and unlocked it, and `capture navigator-home` after returning.

## Gotchas

- `launch … navigator` always opens the fixture navigator, even when the phone holds a live session; the live repository view is a different surface (see Find a repository).
- The launcher's scenario sets the fixture home and widget snapshots; the navigator list state is a separate `Filters → Fixture state` choice and defaults to `Loaded` regardless of scenario.
- Layout follows width, not posture: 600dp and wider (inner display) is the two-pane form where taps go to GitHub; narrower (cover display) is single-pane where a tap selects and the detail replaces the list. `adb shell cmd device_state state` reports posture; emulating `CLOSED` locks the phone, so the cover layout is reached by physically folding and unlocking, not by the command.
- `BACK` with a row selected clears the selection first; a second `BACK` goes home. Use the `repoglance:fixture-home` control for a deterministic return.
- On the inner display a tap opens the row in the GitHub app beside the navigator (the in-app detail pane there reads `Select a row to see details` and is never filled, which is a product observation, not a recipe error). Android remembers that split pair: later launches can come up with the GitHub app on top and every dump then reads its screen (`Could not resolve to a Repository with the name 'acme/rocket'`). `launch` and `cleanup` stop the GitHub app to break the pair; if a dump shows `com.github.android` ids, relaunch rather than tapping through.
- `Load more (fixture)` appends synthetic pages; it is a fixture control, not a product feature to verify.
- The Filters sheet closes as soon as a chip is tapped. A `BACK` after that goes to the navigator's own back handler (clear selection, then home), and a second one leaves the app.
- With any non-default filter active the label reads `Filters · N`; `tap Filters` matches it by prefix.
