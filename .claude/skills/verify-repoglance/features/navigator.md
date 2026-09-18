# Navigator

The navigator lists open issues and pull requests for an account, an organization, or one repository, with mode (issues, PRs, both), filters, cached-row search, and a row detail that deep-links to the GitHub app.

## Sub-features

- `nav-open-repo` opens the navigator scoped to one fixture repository.
- `nav-mode` switches between `ISSUES`, `PRS`, and `BOTH`.
- `nav-filter` applies `Open`, `Mine`, `Mentions`, `Recently updated`, or `Awaiting my review`.
- `nav-row-wide` (inner display) fills the in-app detail pane beside the list on a tap; a second tap on the highlighted row, or the pane's `Open on GitHub`, opens the item in the GitHub app beside the list.
- `nav-row-narrow` (cover display) raises a detail sheet over the list on a tap, with `Open on GitHub`; the list stays underneath and is back when the sheet is dismissed.
- `nav-row-long-press` opens a tapped-and-held row in the GitHub app on both displays.
- `nav-home` returns to the fixture home, which carries the scenario switcher and the pin toggles.
- `nav-truth` shows the list state chosen under `Filters → Fixture state`: `Loaded`; `Empty` (`No rows match`); `Last-good` and `Rate-limited` (`Cached · <age>` chip); `Unknown` (`Data unavailable`).

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
- **Truth state.** Run `bin/verify-repoglance tap Filters`, then `dump filters`. The sheet lists `Fixture state` with `Loaded`, `Empty`, `Last-good`, `Unknown`, and `Rate-limited` (`Paged` appears where a scope pages). Tap `Unknown`; the sheet closes on selection, so do not press `BACK`. Run `dump unknown`: the list shows `Data unavailable` with an `Unknown` chip instead of rows presented as fresh. Then `tap "Filters ·"` (the label now reads `Filters · 1`), tap `Last-good`, `dump last-good`: rows return under a `Cached · <age>` chip, which is also how `Rate-limited` renders. `Empty` renders `No rows match`. Finish with `tap "Filters ·"` → `Loaded`.
- **Select a row on the inner display.** Run `bin/verify-repoglance tap "Fix flaky retry in sync worker"`, wait 2 seconds, `dump row-wide`. The row is highlighted and the pane `repoglance:navigator-detail` on the right fills with the title, `#100 · open`, `Labels:`, `Author:`, `Assignee:`, `Comments:`, and the `repoglance:navigator-open-github` button; the placeholder `Select a row to see details` is gone and the list is still present. Capture `navigator-row-wide`.
- **Hand off to GitHub on the inner display.** From the selected state, `tap "Fix flaky retry in sync worker"` again (or `tap repoglance:navigator-open-github`), wait 3 seconds, `dump row-github`. The GitHub app opens in the adjacent pane and the navigator stays visible on the left; the dump reads the GitHub window (`com.github.android` ids) and, for a fixture repository, `Could not resolve to a Repository with the name 'acme/rocket'`. Capture `navigator-row-github`, then run `adb shell am force-stop com.github.android` to close the pane.
- **Select a row on the cover display (maintainer-gated).** Emulating `CLOSED` with `adb shell cmd device_state state 0` locks the phone and lights the cover display's always-on screen, so an agent cannot reach this step alone. The maintainer folds the phone and unlocks it on the cover display; then run `bin/verify-repoglance launch MIXED navigator acme/rocket ISSUES`, `dump cover-list`, `tap "Fix flaky retry in sync worker"`, wait 2 seconds, `dump cover-sheet`. A sheet `repoglance:navigator-detail-sheet` rises over the list showing the title, `Author:`, `Labels:`, `Comments:`, and `repoglance:navigator-open-github`; capture `navigator-cover-sheet` (capture follows the active panel). One `BACK` dismisses the sheet and `dump cover-list-again` shows the rows with no sheet. Without the maintainer, report `verified-unreachable: cover display needs an unlocked folded phone`.
- **Return home.** Run `bin/verify-repoglance tap repoglance:fixture-home` then `dump home`. The dump contains `repoglance:scenario` and `repoglance:fixture-navigator`.
- **Proof.** `capture navigator-issues` after the mode switch, `capture navigator-unknown` with `Data unavailable` showing and `capture navigator-last-good` with the `Cached · <age>` chip showing, `capture navigator-row-wide` with the detail pane filled, `capture navigator-row-github` with the GitHub pane open, `capture navigator-cover-sheet` on the cover display when the maintainer has folded and unlocked it, and `capture navigator-home` after returning.

## Gotchas

- `launch … navigator` always opens the fixture navigator, even when the phone holds a live session; the live repository view is a different surface (see Find a repository).
- The launcher's scenario sets the fixture home and widget snapshots; the navigator list state is a separate `Filters → Fixture state` choice and defaults to `Loaded` regardless of scenario.
- Layout follows width, not posture: 600dp and wider (inner display) is the two-pane form where a tap fills the pane and a second tap on the same row hands off to GitHub; narrower (cover display) is single-pane where a tap raises the detail sheet. `adb shell cmd device_state state` reports posture; emulating `CLOSED` locks the phone, so the cover layout is reached by physically folding and unlocking, not by the command.
- `BACK` with a row selected clears the selection (and dismisses the sheet on the cover display) first; a second `BACK` goes home. Use the `repoglance:fixture-home` control for a deterministic return.
- On the inner display the first tap only fills the pane; GitHub opens on the second tap of the same row or from `Open on GitHub`. Tapping a different row moves the selection without leaving the app. Android remembers the RepoGlance + GitHub split pair once it has formed: later launches can come up with the GitHub app on top and every dump then reads its screen (`Could not resolve to a Repository with the name 'acme/rocket'`). `launch` and `cleanup` stop the GitHub app to break the pair; if a dump shows `com.github.android` ids, relaunch rather than tapping through.
- Once the GitHub pane has opened beside the app in a session, the app window is narrower than 600dp and a tap on a row goes straight to GitHub in that pane (no sheet); force-stop the GitHub app and relaunch to get the two-pane form back.
- `Load more (fixture)` appends synthetic pages; it is a fixture control, not a product feature to verify.
- The Filters sheet closes as soon as a chip is tapped. A `BACK` after that goes to the navigator's own back handler (clear selection, then home), and a second one leaves the app.
- With any non-default filter active the label reads `Filters · N`; tap it as `Filters ·` (the helper matches text by prefix).
