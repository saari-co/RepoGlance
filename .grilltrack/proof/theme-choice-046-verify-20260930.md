# theme-choice-046: decision, verification and review (2026-09-30)

Track `gt-20260728163459-227573`, decision `theme-choice-046`. Base
`3db76ca` (`claude/settings-widgets-entry`: settings-044 and
widgets-entry-045 on top of main `d7421d3`). Branch `claude/theme-choice-046`
in worktree `.claude/worktrees/vibrant-newton-e86c15`. Stacked on the
Settings branch at the maintainer's request ("confirmed, stack on the
settings branch").

## Decision (frontier batch, maintainer answers 2026-09-30)

The maintainer first noticed that the app already followed the phone's Dark
theme, and asked whether Settings should only link to the system setting.
Facts checked: Android's dark theme guide recommends in-app `Light`, `Dark`,
`System default` with `System default` as the default; the public SDK has
no intent for the system Dark theme page (`ACTION_DISPLAY_SETTINGS` only;
`android.jar` API 36 checked). Options put to him: in-app choice, a link to
Display settings, or nothing. He chose the in-app choice, then took the
recommended option in each of four questions:

| question | choice | rejected |
| --- | --- | --- |
| where | Appearance section on the shared Settings screen (settings-044) | header menu item |
| mechanism | per-app night mode, `UiModeManager.setApplicationNightMode` | Compose-only override |
| widgets | stay on the phone's theme | follow the app |
| control | `Theme` row → `Choose theme` radio dialog | inline segmented buttons |

Wording reference: Google Calculator on the Pixel 10 Pro XL
(`63310DLCQ000RV`), overflow → `Choose theme`: radio rows `Light`, `Dark`,
`System default` and buttons `Cancel` / `OK`, with no personal data on
screen. RepoGlance matches it, including `OK` to apply. This differs from
the preview the maintainer picked (radios only, `Cancel`); the change is
disclosed in the PR. Capture `d08a22e8e718c47d`.

## Implementation

- `state/ThemePrefs.kt`: `ThemeChoice` (`LIGHT` → `MODE_NIGHT_NO`, `DARK` →
  `MODE_NIGHT_YES`, `SYSTEM` → `MODE_NIGHT_AUTO`) and `ThemePrefs`
  (`repoglance_theme` prefs; `choose` stores, then applies).
- `ui/settings/ThemeSetting.kt`: `ThemeSettingItem` (shared `SettingsRow`,
  reads the choice on `Dispatchers.IO`) and the dialog (pending selection,
  `OK` applies; the dialog publishes its own test tags because it is its own
  window).
- `MainActivity`: `SettingsScreen(..., appearance = { ThemeSettingItem() })`.
- Tests: `ThemeChoiceTest` (5), `ThemeChoiceGuardTest` (8).
- Docs: `docs/design.md` version 8, `docs/INVARIANTS.md` row, verify-skill
  `features/theme.md`.

## Gate

`./gradlew check assembleDebug` on the stacked head: `BUILD SUCCESSFUL`
(`runs/check-runs/theme-check-2.log`).

Deliberate-violation probe (reverted, tree clean afterwards): adding
`android:configChanges="uiMode"` to `MainActivity` and importing
`ThemePrefs` into `widget/WidgetLook.kt` failed
`activitiesRecreateOnANightModeChangeSoTheSystemBarsFollow`
(".MainActivity must not handle uiMode itself") and
`widgetsAndTheTileStayOnTheSystemTheme`; the other five passed.

## Device verification (approved emulator)

Device `EMULATOR37X1X11X0` (AVD `Pixel_10_Pro_Fold`, API 36, booted with
`-no-snapshot-save`, `OPENED`), signed out. `bin/verify-repoglance doctor`
passed: device APK SHA-256 = local debug build
`9bf2648c0453340f67b1365b401cf0512ad65d7e709046422397268f6bcfb4ad`. The
phone's own mode stayed `Night mode: no` throughout; this run never changed
it. Night state is read from `cmd uimode night` (phone) and the
`co.saari.repoglance` activity's `CurrentConfiguration` in
`dumpsys activity activities` (` night` present or not).

| step | observed | evidence (SHA-256, 16) |
| --- | --- | --- |
| Open from the Connect screen's menu | `repoglance:settings`, `Appearance`, `repoglance:settings-theme`, `Theme`, `System default`, then `About`; phone no, app notnight | `744b6d207874f5dd` |
| Dialog | `repoglance:theme-dialog`, `Choose theme`, `repoglance:theme-light/-dark/-system` checkable, `theme-system` `checked=true`, `Cancel`, `OK` | `b24999af4180c727` |
| Mark Dark | only `theme-dark` `checked=true`; app still notnight | dump `theme-marked` |
| Cancel | row still `System default`; app notnight | dump `theme-cancel` |
| Dark + OK | screen came back on Settings, row `Dark`; phone no, app night; status-bar and gesture-handle icons turn light | `561aa20f20f87b4a` |
| Force-stop, cold start | frames: home (luma 128) → start window with the mark on near-black (19 → 0) → dark Connect screen; no light frame; app night after restart | video `51c7085f4d62c406`, sheet `e0bad0b369eb36dc` |
| Sample mode while Dark | `repoglance:sample-bar`, `SAMPLE`, `@saariuslystoned · 7 repositories`; app night; sample Settings lists Widgets, Appearance (`Dark`), About | `c896b6986a67499e` |
| Widgets after a redraw while Dark | placed stack widget redrew with the sample header band (`Pinned · 0`) and the repo widget (`RepoGlance` / `Tap to choose a repository`); both light, like the phone | `175030af2214d598` |
| Leave sample | `Sign in with GitHub` → `repoglance:connect-github`, `repoglance:explore-sample`, no `Enter this code`; Settings still `Dark` | dumps `theme-sample-exit`, `theme-after-exit` |
| Widget setup while Dark | tapping the placed repo widget opened `.widget.RepoWidgetConfigActivity` with ` night`; `No repositories to choose from yet`; `BACK` left the widget unconfigured | `12ec6da21006f27a` |
| System default + OK | row `System default`; app notnight = phone | `37ad3caf40d87084` |

End state: `System default`, sample mode off, both placed widgets
unconfigured as found (`Pinned · 0`, `Tap to choose a repository`),
`bin/verify-repoglance cleanup` run. Evidence in
`runs/verify-repoglance-runs/20261001T0037*`–`20261001T0044*` and
`runs/verify-repoglance-runs/theme-choice-046-refs/` (ignored by git).

Earlier de-risk spike (uncommitted call site on the Connect screen, removed
before the stacked build): same results for Dark, System default and the
dark cold start (`63366fc8349ec276`). It also found that the dialog's tags
were missing from dumps, fixed in `6335006`.

## Not proven here

- `System default` following a **dark** phone: on the light emulator it
  gives light, which a forced-light bug would also give. The phone's Dark
  theme is never changed by an agent; see the XL row below once run.
- `Light` overriding a dark phone (the mirror of the Dark proof).
- API 31–33 devices.
