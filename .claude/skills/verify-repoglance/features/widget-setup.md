# Widget setup

Adding the compact/tall RepoGlance widget opens a setup screen listing the repositories from the last catalog load, pinned ones first; saving pins the chosen repository, and removing the widget unpins it. The widget shows the counts and rows saved the last time that repository was opened in the app, each with its age.

## Sub-features

- `widget-setup-list` lists catalog repositories with pinned ones first; with nothing loaded yet it shows `No repositories to choose from yet` and `Open RepoGlance`.
- `widget-setup-pin` saving pins the repository in the live catalog (thumbtack filled).
- `widget-setup-unpin` deleting the widget, or pointing it at another repository, unpins the old one when no other widget uses it.
- `widget-tall-rows` the tall size shows saved issue/PR rows with ages and an `as of` age in the header, or `no data · open RepoGlance to load` and `No saved rows · open RepoGlance to load`.

## How to get to it (user POV)

- Long-press the home screen, Widgets, RepoGlance, drag the compact widget out; the setup screen opens.
- Tap a placed widget's header to open that repository's live view in the app (issues and PRs with the `LIVE` chip); tap a row to open it on GitHub.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes and the maintainer's live session exists (the list comes from the last catalog load).
- **Human-gated placement.** Placing a widget changes the home screen; the maintainer either drags it or approves an adb placement through the debug widget host used by the picker. Remove it afterwards unless asked to keep it.

- **Setup list.** Open the setup screen for a new widget, then `dump widget-setup`. The `Repository` field shows the first pinned repository, and the note reads `Saving pins this repository in RepoGlance`.
- **Pin on save.** Save the widget for `saari-co/RepoGlance`, then `bin/verify-repoglance launch MIXED live`, filter to `saari-co/RepoGlance`, `dump after-widget`: the row control reads `Unpin saari-co/RepoGlance`.
- **Rows.** Open the repository in the app once, return to the home screen, `dump widget-tall`: the widget header contains `as of` and at least one `ISSUE #` or `PR #` row with an age; capture only with private rows out of frame or redacted.
- **Header tap.** With the app closed, tap the widget header text `saari-co/RepoGlance` on the home screen, wait, then `dump after-header-tap`: the top activity is `co.saari.repoglance/.MainActivity`, the dump contains `repoglance:live-home` (the live repository view's back control) and `saari-co/RepoGlance`, and no `repoglance:fixture-home`.
- **Unpin on delete.** Delete the widget, launch the live catalog again, filter, `dump after-delete`: the control reads `Pin saari-co/RepoGlance`.

## Gotchas

- The list is empty until the app has loaded a catalog at least once. Disconnect GitHub clears it together with pins, saved counts, rows and every widget's configuration, and placed widgets re-render as unconfigured.
- Rows and counts are only as fresh as the last in-app open; the ages say so and a stale header reads `last good`.
- Fixture scenarios do not feed live widgets; the GrillTrack widget picker still renders the compact widget from fixtures for proof.
