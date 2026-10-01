# Theme choice

Someone opens Settings in RepoGlance and picks how the app looks: `Light`, `Dark` or `System default`. The choice changes only RepoGlance, including its start window and status-bar icons, and stays after a restart. `System default`, the default, follows the phone's own Dark theme. Home-screen widgets and the Quick Settings tile keep following the phone.

## Sub-features

- `theme-row`: Settings → Appearance shows `Theme` with the current choice under it; a fresh install reads `System default`.
- `theme-dialog`: a tap opens `Choose theme` with radio buttons `Light`, `Dark` and `System default` (the current one selected), and `Cancel` / `OK`. A radio tap only marks a choice; `OK` applies it; `Cancel`, back or a tap outside leaves the theme unchanged.
- `theme-apply`: `OK` redraws RepoGlance in the chosen theme and stays on Settings, with the row showing the new choice.
- `theme-persist`: the choice survives a force-stop, and the next cold start opens in it, start window included.
- `theme-system`: `System default` hands the look back to the phone.
- `theme-scope`: home-screen widgets and the Quick Settings tile stay on the phone's theme; signing out and leaving sample mode keep the choice.

## How to get to it (user POV)

- Open RepoGlance, tap the three-dot menu at the top right, tap `Settings`, then `Theme`. The menu is on the Connect screen, the sample catalog and the live catalog.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- Read the phone's own theme with `adb shell cmd uimode night` and leave it as it is: this recipe never changes the phone's Dark theme. Pick the choice that differs from the phone (`Light` on a dark phone, `Dark` on a light one) as the override under test.
- The signed-out path needs no session; on a phone holding the maintainer's session, the same steps work from the live catalog, and no catalog capture is taken unless it is filtered to `saari-co/RepoGlance`.

- **Open.** Run `bin/verify-repoglance launch MIXED live`, `tap repoglance:menu`, `tap repoglance:menu-settings`, then `dump settings`. The dump contains `repoglance:settings`, `Appearance`, `repoglance:settings-theme`, `Theme` and the current choice.
- **Dialog.** `tap repoglance:settings-theme`, then `dump theme-dialog`. It contains `repoglance:theme-dialog`, `Choose theme`, `repoglance:theme-light`, `repoglance:theme-dark`, `repoglance:theme-system`, `Cancel` and `OK`; the current choice's radio node is `checked="true"`. Run `capture theme-dialog`.
- **Cancel.** Tap the override, then `tap repoglance:theme-cancel`, and `dump theme-cancel`: the row still shows the old choice, and `adb shell dumpsys activity activities` shows the same night state for `co.saari.repoglance` as before.
- **Apply.** Open the dialog again, tap the override, `tap repoglance:theme-ok`, wait two seconds, then `dump theme-applied`. It still contains `repoglance:settings` (the screen came back on Settings) and the row reads the override. In `adb shell dumpsys activity activities`, the `co.saari.repoglance/.MainActivity` record's configuration carries ` night` for `Dark` and no ` night` for `Light`, while `adb shell cmd uimode night` still prints the phone's own mode. Run `capture theme-applied`.
- **Persist and start window.** Run `adb shell am force-stop co.saari.repoglance`, start `adb shell screenrecord --time-limit 4 /sdcard/theme-start.mp4` in the background, then `adb shell am start -n co.saari.repoglance/.MainActivity`. Pull the recording into the run directory and check its first frames: the start window is already in the override's background. `dump theme-restart` shows the override's look on the first screen.
- **System default.** Open the dialog, tap `repoglance:theme-system`, `tap repoglance:theme-ok`, and `dump theme-system`: the row reads `System default` and the activity's night state matches `adb shell cmd uimode night` again.
- **Widgets and tile.** With the override applied and the maintainer's approval to place widgets on the device, go home and capture the RepoGlance widgets: they keep the phone's theme, not the override. The tile is drawn by the system and always follows the phone.
- **Proof.** Capture `theme-dialog`, `theme-applied`, the restart, and the home screen with widgets; record each SHA-256 and the `dumpsys` night lines as text. End on `System default`.

## Gotchas

- Applying a choice recreates RepoGlance's screen, so a `dump` taken right after `OK` can catch the old frame. Wait about two seconds and dump twice.
- When the choice matches what the phone already shows (for example `Dark` on a dark phone), nothing visibly changes and the screen is not recreated. Prove the override with the choice that differs from the phone.
- The choice outlives `bin/verify-repoglance cleanup` and `force-stop`; only clearing the app's data resets it. End every run on `System default` so later recipes see the phone's theme.
- Never change the phone's Dark theme to prove `System default`: the phone is shared, and the recipe proves it by matching the activity's night state to `cmd uimode night` instead.
