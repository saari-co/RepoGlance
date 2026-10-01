# Exact-source review: settings-044, widgets-entry-045, widgets-look-047

PR saari-co/RepoGlance#53.

## Round 1, `b63fd386867acf288aecafa4011a54a0a4e22c71`

**Reviewers.** A self-review by the implementing session, on both axes
below, and ClawSweeper, whose verdict comment on #53 reads "Ready for
maintainer review", finds nothing, and rates proof sufficient and readiness
5/6.

**Standards (AGENTS.md, INVARIANTS.md, repo style).**
- **Read-only.** Settings and Widgets make no GitHub call and no store
  write. Disconnect stays the activity's one session action, behind its
  confirm dialog.
- **Main thread.** Store reads run on `Dispatchers.IO`. The emulator run
  logged no StrictMode lines.
- **Style.** There are no comments in `app/src/main`, and lint and detekt
  pass with unchanged baselines. The one new CompositionLocal was
  allowlisted, then removed with the seam.
- **Tooling.** The picker lived in the debug source set and has been
  deleted.
- **Proof.** It is text only, with screenshot hashes; the images stay in
  ignored `runs/`.

**Source intent (the confirmed summary).**
- **Menu.** It holds Widgets and Settings on the live and sample catalogs,
  and Settings only on the Connect screen.
- **Settings.** Sections are Widgets, Appearance, GitHub (live only) and
  About; Manage access and Disconnect moved out of the menu.
- **Widgets screen.** The look is C. `Add` goes through
  `requestPinAppWidget`, with a how-to when pinning is unavailable. The
  placed list opens setups, and the removal hint is shown.
- **Sample mode and names.** Sample mode behaves alike. The launcher labels
  use the new names, and the store copy follows them.

Every item was found in source and proven on a device, except the how-to,
which is guarded by source only.

**Findings, adjudicated:**

| finding | class | outcome |
| --- | --- | --- |
| Both tiles' buttons read only "Add" to TalkBack, so a screen-reader user can't tell the widgets apart. | `required_fix` | Fixed in `ce81aca`: `contentDescription = "Add <widget>"`. Seen in the emulator dump and guarded by `SettingsGuardTest`. |
| The screen assumed pin support until its first load, so on a launcher that can't pin, `Add` would flash before the how-to. The sample note had the same first-frame gap. | `required_fix` | Fixed in `ce81aca`: pin support (a binder call, no disk) and the in-memory sample flag are read up front. Guarded. |
| On the Fold's inner display, Settings and Widgets stretch to full width. | `defer` | Recorded as unresolved in `docs/design.md`; it needs a large-screen grill. |
| The can't-pin how-to was never seen on a device, because Pixel Launcher always pins. | `defer` | Source guard only; disclosed in the PR and the proof. |
| `UntaggedSocketViolation` StrictMode lines on the XL. | `reject_false_positive` | Pre-existing in the HTTP client (catalog load, refresh worker); not this change. |
| An ANR (`onStartJob`) on the first emulator cold start after install. | `defer` | Seen once, on an unoptimized APK on a loaded emulator, and not again. Recorded in the proof. |

## Round 2, `ce81aca66da729044777074d022e59d979fefb73`

- **Diff from round 1:**
  - Two production files changed: `WidgetsScreen.kt` (the button
    descriptions and the initial state) and `MainActivity.kt` (passes
    `sampleMode` to the Widgets screen).
  - `SettingsGuardTest` gains two assertions.
  - One proof line was added.
- **Checks:** `./gradlew assembleDebug check` is green, and the emulator
  dump confirmed the descriptions.
- **ClawSweeper re-review on this head:** the comment on #53 (Revision 2,
  2026-10-01 01:35 UTC) carries the marker `clawsweeper-verdict:needs-human
  item=53 sha=ce81aca66da729044777074d022e59d979fefb73`.
  - It reads "Ready for maintainer review" and lists nothing for "Before
    merge". It has no findings and no security items.
  - Proof is sufficient and readiness is 5/6. `needs-human` is the owner
    merge gate.

Result: clean on both axes. Merging still needs the maintainer's OK, and a
fresh ClawSweeper run on any later head.
