# Settings and Widgets

The three-dot menu at the top right holds `Widgets` and `Settings`. `Widgets` opens a screen that adds the Repository widget or the Pinned repos widget to the home screen through the launcher's own `Add to home screen` sheet. It also lists every RepoGlance widget already placed; tapping a repository widget opens its setup to change the repository or feed. `Settings` gathers a Widgets row, Appearance (the theme), GitHub access and About. Nothing here writes to GitHub.

## Sub-features

- `menu`: the three-dot menu (`More options`) lists `Widgets` and `Settings` on the live catalog and the sample catalog, and only `Settings` on the signed-out Connect screen. The repository view has no menu.
- `settings-live`: with a GitHub session, Settings shows `Widgets`, `Appearance` (see [Theme choice](./theme.md)), `GitHub` with `Manage GitHub access` and `Disconnect GitHub`, and `About` with `Version`, `Privacy policy` and `Source code`. `Disconnect GitHub` asks first, then returns to the Connect screen.
- `settings-sample`: in sample mode Settings shows `Widgets`, `Appearance` and `About`, with no GitHub section.
- `settings-signed-out`: signed out, Settings shows `Appearance` and `About` only, with no Widgets row.
- `widgets-add`: two tiles, `Repository widget` and `Pinned repos widget`, each with a sketch of the widget, a sentence and `Add`. `Add` opens the launcher's `Add to home screen` sheet, which uses the names `Repository` and `Pinned repos`. After `Add to home screen`, a repository widget opens its setup screen, and the Pinned repos widget lands without one.
- `widgets-placed`: `On your home screen` lists the placed widgets after every return to the screen. A set-up widget shows its repository and `Repository widget · <feed>`, an unset one shows `Not set up · tap to choose a repository`, and the Pinned repos widget shows its pin count (`No pins yet · pin repositories in the list` at zero). A repository row opens that widget's setup. With nothing placed the screen reads `No RepoGlance widgets on your home screen yet.`
- `widgets-remove-hint`: removing stays on the launcher; the screen says `To remove a widget, long-press it on your home screen and drag it to Remove.`
- `widgets-how-to`: on a launcher that cannot pin, the `Add` buttons are replaced by a short how-to for the launcher's widget picker. This was not seen on a device: Pixel Launcher always pins.
- `widgets-sample`: in sample mode the screen notes `Sample mode: widgets you add show the sample repositories.`, the setup lists the sample repositories, and the rows show the sample setups and sample pins.
- `settings-state`: Settings and Widgets stay open through a screen recreation (a dark-mode switch or a theme change), and back returns the way it came (Widgets → Settings → catalog). A widget or tile tap closes them and shows the repository or catalog.

## How to get to it (user POV)

- Tap the three-dot menu at the top right of the catalog, then `Widgets` or `Settings`.
- On the Connect screen, tap the three-dot menu, then `Settings`.
- From Settings, tap `Widgets`.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes. Signed-out and sample steps need a phone with no GitHub session, such as the approved emulator; the live steps need the maintainer's session and never tap `Manage GitHub access` or `Disconnect GitHub`.
- **Placement.** Adding widgets places them on the home screen. It is pre-approved only on the registered test Fold; on any other device it needs the maintainer's approval. Remove what you add, and leave widgets that were already there.

- **Menu, signed out.** `bin/verify-repoglance launch MIXED live`, `tap repoglance:menu`, `dump menu`: only `repoglance:menu-settings` (`Settings`). `tap repoglance:menu-settings`, `dump settings-signed-out`: `repoglance:settings`, `About`, `repoglance:settings-version`, `repoglance:settings-privacy`, `repoglance:settings-source`; no `repoglance:settings-widgets` and no `repoglance:settings-manage-access`. `capture settings-signed-out`.
- **Menu, sample.** `tap repoglance:settings-back`, `tap repoglance:explore-sample`, `tap repoglance:menu`, `dump menu-sample`: `repoglance:menu-widgets` and `repoglance:menu-settings`. In Settings, `dump settings-sample` contains `repoglance:settings-widgets` and no `GitHub` section.
- **Menu, live.** On the maintainer's session, `launch MIXED live`, filter to `saari-co/RepoGlance`, `tap repoglance:menu`, `tap repoglance:menu-settings`, `dump settings-live`: `GitHub`, `repoglance:settings-manage-access`, `repoglance:settings-disconnect`. Do not tap either.
- **Widgets screen.** `tap repoglance:settings-widgets` (or `repoglance:menu-widgets`), `dump widgets`: `repoglance:widgets`, `Repository widget`, `Pinned repos widget`, `repoglance:widgets-add-repository`, `repoglance:widgets-add-pinned`, `On your home screen`, and one `repoglance:widgets-placed-<id>` per placed widget (ids match `adb shell dumpsys appwidget`). `capture widgets`.
- **Add a repository widget.** `tap repoglance:widgets-add-repository`: the top activity is the launcher's `AddItemActivity` and the dump shows `Repository` and `Add to home screen`. `tap "Add to home screen"`: `RepoWidgetConfigActivity` opens. Choose a repository, `tap "Save widget"`: the app is back on Widgets, and a new `repoglance:widgets-placed-<id>` row reads the repository and `Repository widget · Issues and PRs`.
- **Add the Pinned repos widget.** `tap repoglance:widgets-add-pinned`, `tap "Add to home screen"`: no setup opens, and a new `Pinned repos widget` row shows the pin count.
- **Change a widget.** Tap the new repository row: its setup opens on the saved repository. Pick another repository and `PRS`, `Save widget`: the row reads the new repository and `Repository widget · PRs`, and the pin moves with it.
- **Home screen.** `bin/verify-repoglance widget-bounds` lists the new widgets with their content (`sample` in sample mode, a clock time on live data).
- **Remove.** Remove each widget you added on the launcher (see Gotchas), reopen Widgets and dump: those rows are gone, and the pin a removed repository widget held is released (`Pinned · 0` on the stack in sample mode; the live catalog row reads `Pin <repo>` again).
- **Recreation and back.** With Settings open, `adb shell cmd uimode night yes`, wait, dump: still `repoglance:settings`; `adb shell cmd uimode night no` to restore. From Widgets opened via Settings, `BACK` returns to Settings, then to the catalog.
- **Widget tap closes Settings.** With Settings open, go `HOME` and tap a RepoGlance widget that opens the app: the dump shows the catalog or repository, not `repoglance:settings`.
- **Proof.** Capture Settings in each state, Widgets before and after adding, and the launcher's `Add to home screen` sheet; record each SHA-256. Live captures show no repository but `saari-co/RepoGlance`.

## Gotchas

- The menu, the `Disconnect RepoGlance?` dialog and the launcher's sheet are separate windows. The menu and dialog publish their `repoglance:` ids; if one is missing from a dump, report a defect rather than tapping by coordinates.
- On the Pixel 10 Pro XL, long-pressing a widget opens a small menu with `Remove`. That is the reliable way to remove one. On the Fold emulator, drag it with `input motionevent` (down, hold 1.5 s, small moves, up) onto the `Remove` target. Read the target's bounds from a dump taken mid-drag, because they move. `input draganddrop` only long-presses there. Check `dumpsys appwidget` a few seconds after the drop, because the removal is not instant.
- Saving a live repository widget pins that repository, and removing the widget unpins it if no other widget uses it. On the maintainer's phone, choose a repository that is not pinned yet, so the pins end as they started.
- Saving a live widget runs one background refresh at once, so its numbers and clock time appear without opening the app.
- The emulator already holds RepoGlance widgets from earlier runs (`dumpsys appwidget`); leave them. Which home-screen widget has which id is not shown in a dump; tell yours apart by position and size, and confirm by the id that disappears.
- A cold start right after `adb install` on the emulator can raise `RepoGlance isn't responding` for WorkManager's `onStartJob` while ART verifies the new APK; tap `Wait`. It did not recur on later starts.
- An unexpected back gesture can close the setup screen mid-run on a shared phone (seen once on the XL, 2026-09-30; not RepoGlance). If the setup closes without `Save widget`, the row reads `Not set up`; tap it to finish.
