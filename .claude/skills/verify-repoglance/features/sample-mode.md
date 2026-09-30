# Sample mode

Someone without a GitHub account taps `Explore with sample data` on the sign-in screen. They get the real catalog, repository view and navigator, filled with made-up repositories under the maintainer's own accounts, with a `SAMPLE` marker and a `Sign in with GitHub` action on every screen. The repo and stack widgets and the Quick Settings tile work too, showing `sample` where live data shows its time. Sample mode never calls GitHub, never refreshes in the background, survives a restart, and ends when the user chooses to sign in.

## Sub-features

- `sample-entry`: an `Explore with sample data` button under `Connect GitHub` on the signed-out screen.
- `sample-catalog`: `@saariuslystoned · 7 repositories`. Owners are the maintainer's own accounts `saari-co`, `dinkuskit` and `saariuslystoned`, under repository names that do not exist on GitHub, so no real repository shows made-up numbers; there is one archived repository and one with an unknown push time. The owner filter, search, sort and pins all work.
- `sample-marker`: a `SAMPLE` chip and `Made-up repositories, not your GitHub` on the catalog and the repository view. The `LIVE` chip, `Manage GitHub access` and `Disconnect GitHub` are absent.
- `sample-pins`: pins are kept apart from the real pinned set and never refresh a widget.
- `sample-rows`: issues and PRs under each repository, all authored by `saariuslystoned`, with no third-party GitHub user named. No sample PR shows `Review requested`, because GitHub cannot request a review from a PR's own author. A tap shows `Sample item — not on GitHub` and never opens GitHub.
- `sample-persist`: the app reopens in sample mode after a force-stop until the user signs in.
- `sample-exit`: `Sign in with GitHub` returns to the Connect screen without requesting a device code.
- `sample-widget-setup`: adding a repo widget in sample mode lists the seven sample repositories, sample pins first, with the note `Sample data: saving pins this sample repository; …`. Saving pins the repository among the sample pins; removing the widget unpins it and the stack drops it at once.
- `sample-widgets`: the compact widget reads `rocket  sample  issues 5  PRs 3`, the tall size `saari-co/rocket` / `ISSUES 5 · PRS 3 · sample` over sample rows, and the stack `Pinned · N` with `sample` in each row's time slot. No clock and no rate-limit text appear on a sample widget.
- `sample-widget-taps`: a widget header, tall row or stack row opens that sample repository in the app, never GitHub, including from a stopped app.
- `sample-tile`: the tile is active and describes itself as `RepoGlance, sample data, latest push to saari-co/rocket updated 25m ago` (subtitle `Sample · saari-co/rocket · 25m`); a tap opens the sample catalog.
- `sample-widget-exit`: after `Sign in with GitHub`, repo widgets read `RepoGlance` / `Tap to choose a repository`, the stack reads `Pinned · 0` / `Pin repositories in RepoGlance`, and the tile reads `Open to connect`. Entering sample mode again starts with no pins and unconfigured widgets.

## How to get to it (user POV)

- Open RepoGlance with no GitHub session and tap `Explore with sample data`.
- After a restart, RepoGlance reopens straight into sample mode.
- Tap `Sign in with GitHub` on any sample screen to leave.
- In sample mode, add the RepoGlance repo or stack widget from the launcher (long-press the home screen, Widgets, RepoGlance), or tap an unconfigured repo widget to open its setup. Add the RepoGlance tile to Quick Settings.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- The phone has **no** GitHub session. This recipe uses only the signed-out path: an agent never signs in or out. On a phone holding the maintainer's session, the entry is unreachable; report `verified-unreachable: session present`.

- **Entry.** Run `bin/verify-repoglance launch MIXED live`, then `dump signin`. The dump contains `repoglance:connect-github` and `repoglance:explore-sample`. Run `tap repoglance:explore-sample`, then `dump sample-catalog`.
  - The dump contains `repoglance:sample-bar`, `repoglance:sample`, `repoglance:sample-sign-in`, `SAMPLE` and `@saariuslystoned · 7 repositories`.
  - It contains no `repoglance:live` and no `Account and access settings`.
  - Every repository-shaped label is one of the seven sample names: `saari-co/rocket`, `saari-co/api-server`, `saari-co/mobile-app`, `saari-co/legacy-site`, `dinkuskit/infra`, `dinkuskit/design-system` or `saariuslystoned/dotfiles`. None of them is `saari-co/RepoGlance`.
  - Run `capture sample-catalog`.
- **Owner filter and search.** `tap repoglance:owner-filter`, tap `dinkuskit`, then `dump sample-dinkuskit`. Only `dinkuskit/infra` and `dinkuskit/design-system` remain. Clear it with `tap repoglance:owner-filter` and then `tap All`. Then `tap repoglance:repo-search`, `type rocket`, and `dump sample-search`: only `saari-co/rocket` remains.
- **Pin.** `tap "Pin saari-co/rocket"`, then `dump sample-pinned`. The first repository-shaped label is `saari-co/rocket` and its control reads `Unpin saari-co/rocket`. Tap `Unpin saari-co/rocket` to restore.
- **Repository view.** Tap `saari-co/rocket`, then `dump sample-repo`. It contains `repoglance:live-home`, `repoglance:sample-bar`, `Issues`, `PRs` and `Updated just now`, with rows such as `#415`.
  - Run `capture sample-repo`.
  - Tap a row, then within a second run `adb shell dumpsys notification | grep TextToastRecord`. It shows `co.saari.repoglance` with `text=Sample item — not on GitHub`.
  - The top resumed activity is still `co.saari.repoglance/.MainActivity` (`adb shell dumpsys activity activities | grep topResumedActivity`). No `com.github.android` or `customtabs` record appears.
- **Persistence.** Run `adb shell am force-stop co.saari.repoglance`, `bin/verify-repoglance launch MIXED live`, then `dump sample-restart`. It still contains `repoglance:sample-bar` and `@saariuslystoned · 7 repositories`.
- **Exit.** `tap repoglance:sample-sign-in`, then `dump sample-exit`. It contains `repoglance:connect-github` and `repoglance:explore-sample`, and no `Enter this code on GitHub`: no device code was requested. Force-stop and relaunch. The Connect screen shows again, not the sample catalog.
- **Offline.** Run `adb shell cmd connectivity airplane-mode enable`, enter sample mode, tap `repoglance:refresh-repositories`, open `saari-co/api-server` and tap `repoglance:refresh-repository`. The catalog and rows still show, and nothing reads `could not refresh`, `Loading live GitHub data` or `No current value`. Then **Exit**, and `bin/verify-repoglance cleanup` turns airplane mode off.
- **Widgets.** Placing widgets needs the maintainer's approval for the device; it is pre-approved only on the registered test Fold. In sample mode, `launch MIXED navigator`, `tap repoglance:fixture-home`, tap `Pin stack widget`, then `Add to home screen`; tap `Pin repo widget`, `Add to home screen`, and the setup screen opens. Open `Repository`: exactly the seven sample names are listed, most recent push first. Save `saari-co/rocket`. Go home and read the launcher dump (`adb exec-out uiautomator dump /dev/tty`): the stack shows `Pinned · 1`, `saari-co/rocket`, `sample`, `issues 5 · PRs 3 · review 0`; the repo widget shows `rocket`, `sample`, `issues`, `5`, `PRs`, `3`. `launch MIXED live` shows `Unpin saari-co/rocket`.
- **Tall size and taps.** `bin/verify-repoglance widget-resize repo 300`: the widget shows `saari-co/rocket`, `ISSUES 5 · PRS 3 · sample` and rows such as `ISSUE #415 · 25m`. Tap a row: the top activity is `co.saari.repoglance/.MainActivity` on `repoglance:live-home` with `saari-co/rocket`, and no `com.github.android` or `customtabs` record exists. Then `adb shell am force-stop co.saari.repoglance`, go home, tap a stack row: the app cold-starts on that sample repository.
- **Tile.** With the maintainer's approval, `adb shell cmd statusbar add-tile co.saari.repoglance/.tile.RepoGlanceTileService`, `expand-settings`, and dump: the tile's `content-desc` is `RepoGlance, sample data, latest push to saari-co/rocket updated 25m ago`. `click-tile` opens the sample catalog (`@saariuslystoned · 7 repositories`) even when the app was left on a repository. `remove-tile` afterwards.
- **Widget exit.** Run **Exit**, go home and dump: `Pinned · 0`, `Pin repositories in RepoGlance`, `RepoGlance`, `Tap to choose a repository`; the tile reads `RepoGlance, Open to connect`. Tapping the repo widget opens `No repositories to choose from yet`.
- **Proof.** Capture `sample-catalog`, `sample-repo`, the home screen with sample widgets, and the Connect screen after exit, and record each SHA-256. Record the toast line from `dumpsys notification` as text.

## Gotchas

- Sample mode is stored on the phone. A run that stops midway leaves the next `launch MIXED live` on the sample catalog instead of the Connect screen, and `bin/verify-repoglance cleanup` does not clear it. End every run with **Exit**, or run `adb shell pm clear co.saari.repoglance` on a phone with no session to keep.
- Sample repositories sit under the maintainer's real accounts, so a sample capture shows `saari-co/…` names. Only the `SAMPLE` bar tells it apart from the live catalog. Assert `repoglance:sample-bar` before any capture, and never mix sample captures with live-screen proof.
- The `SAMPLE` marker's look is provisional (GrillTrack `sample-marker-040`). Assert its test tags and text, not its colours.
- Remove any widgets and the tile you placed when you finish: drag each widget to the launcher's `Remove` target (its position moves, so read it from a dump while dragging) and run `remove-tile`.
- The first tap on a widget right after `HOME` can be swallowed while the launcher settles. If nothing opens, dump and tap again before calling it a failure.
- A tile in a narrow Quick Settings slot shows only its icon. Prove the subtitle from the `content-desc`, not a capture.
- The toast lasts about two seconds. Neither `uiautomator dump` nor `capture` catches it: the capture's guarded dump runs first, and the toast is gone by the screenshot. Prove it from the system toast queue (`dumpsys notification`) instead.
- The approved emulator (`EMULATOR37X1X11X0`, AVD `Pixel_10_Pro_Fold`, API 36) starts with no GitHub session, so it is the easiest target for this recipe. Boot it with `-no-snapshot-save`. An older RepoGlance on it has a higher versionCode, so install over it once with `adb install -r -d` (keeps data) before `launch`.
