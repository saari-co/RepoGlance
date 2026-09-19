# Widget setup

Adding the compact/tall RepoGlance widget opens a setup screen listing the repositories from the last catalog load, pinned ones first; saving pins the chosen repository, and removing the widget unpins it. The widget shows the saved counts and rows with the clock time they were observed (`as of 1:31 AM`); pinned repositories refresh in the background about every 30 minutes, right after a widget is saved, and whenever the repository is opened in the app.

## Sub-features

- `widget-setup-list` lists catalog repositories with pinned ones first; with nothing loaded yet it shows `No repositories to choose from yet` and `Open RepoGlance`.
- `widget-setup-pin` saving pins the repository in the live catalog (thumbtack filled).
- `widget-setup-unpin` deleting the widget, or pointing it at another repository, unpins the old one when no other widget uses it.
- `widget-tall-rows` the tall size shows saved issue/PR rows with ages and an `as of <clock time>` in the header (`as of Tue 14:05` from an earlier day, `as of 12 Sep` from a week or more ago), `last good ·` after a failed fetch, `rate limited · resets <time> ·` while backing off, or `no data · open RepoGlance to load` and `No saved rows · open RepoGlance to load`.
- `widget-background-refresh` a WorkManager job refreshes the pinned set every 30 minutes on a network (up to 20 per run, least recently refreshed first; widget repositories only when the rate limit is low; nothing while it is exhausted), and saving a widget runs it once immediately.

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
- **Background refresh.** With a placed widget, note its header `as of` time, open its setup screen (`adb shell am start -a android.appwidget.action.APPWIDGET_CONFIGURE --ei appWidgetId <id> -n co.saari.repoglance/.widget.RepoWidgetConfigActivity`; find the id with `adb shell dumpsys appwidget`), keep the repository and feed, `tap "Save widget"`, go home, wait 20 seconds, `dump widget-after-save`: the header reads `as of` the current minute without the repository having been opened in the app, and `adb logcat -d | grep PinnedRefreshWorker` shows `Worker result SUCCESS`. `adb shell dumpsys jobscheduler co.saari.repoglance` lists the periodic job with a `Minimum latency` near 30 minutes and an `INTERNET` network request.
- **Unpin on delete.** Delete the widget, launch the live catalog again, filter, `dump after-delete`: the control reads `Pin saari-co/RepoGlance`.

## Gotchas

- The list is empty until the app has loaded a catalog at least once. Disconnect GitHub clears it together with pins, saved counts, rows and every widget's configuration, and placed widgets re-render as unconfigured.
- After an app update both widgets (repo and stack) redraw from saved data without the app being opened; no GitHub call is made, so the numbers and their time are whatever was last saved.
- The `as of` time is a clock time on purpose: the widget picture is frozen between redraws, so a relative age would understate it. A stale header reads `last good`.
- `adb shell cmd jobscheduler run -f` on the periodic job does not refresh anything: WorkManager reschedules a periodic worker that fires before its period is due. Use the widget save (one-time run) or wait for the period.
- Do not `launch` or `cleanup` while waiting for a periodic run: both force-stop the app, which cancels its scheduled jobs until the app starts again.
- Fixture scenarios do not feed live widgets; the GrillTrack widget picker still renders the compact widget from fixtures for proof.
