# sample-widgets-039: verification and review (2026-09-30)

Track `gt-20260728163459-227573`, decision `sample-widgets-039`, map
`.grilltrack/maps/sample-mode.md` node 2. Base
`a0625da73c02e299b7a122b6ab78b22c19a169b3` (includes #42 targetSdk 36 and
#43 sample mode). Branch `claude/grilltrack-039-sample-widgets` in worktree
`.claude/worktrees/dazzling-jemison-935f18`, uncommitted.

## Decision

Frontier batch of six, maintainer answer `1A 2A 3A 4A 5A 6A` after the
behaviour prototype (question, per-candidate tap result, live trace, Reset).
Confirmation: "confirmed, use the emulator, add-tile ok".

- 1A: `sample` wherever a live widget shows its observed clock time.
- 2A: setup lists the seven sample repositories, sample pins first; save pins,
  remove or repoint unpins; no background refresh.
- 3A: sample pins start empty.
- 4A: leaving sample mode forgets sample widget setups; repo widgets fall to
  unconfigured, the stack follows live pins; nothing auto-assigned.
- 5A: unconfigured repo widget `RepoGlance` / `Tap to choose a repository`,
  tap opens its setup; `FIXTURE PREVIEW` retired for everyone.
- 6A: sample tile active, `Sample · <repo> · <age>`, tap opens the sample
  catalog; locked shade unchanged.

## Implementation

- `widget/SampleWidgetData.kt` (new): sample counts, rows and push times from
  `SampleAccount`, no store.
- `state/SampleModeStore.kt`: per-widget sample setups and pin add/remove in
  the existing `repoglance_sample` file, so `leave()` clears them.
- `widget/RepoWidget.kt`, `widget/StackWidget.kt`: `read*WidgetData` branches
  on `SampleModeStore.isActive` into a sample reader and a live reader;
  `WidgetFreshness.sample` puts `sample` in the time slot; sample rows open the
  app (`rowIntent`); unconfigured content taps into `widgetSetupIntent`.
- `widget/RepoWidgetConfigActivity.kt`: setup lists load on `Dispatchers.IO`;
  sample branch saves to the sample store, pins, releases a repointed pin and
  redraws; no `BackgroundRefresh`.
- `widget/RepoWidgetReceiver.kt`: removal releases sample pins and redraws the
  stack widgets.
- `tile/`: `TileTexts.sample`; the service reads the sample flag off the main
  thread; the tap intent carries `EXTRA_LIVE_CATALOG`.
- `RepoGlanceViewModel.kt`: redraw after `SampleModeStore.enter/leave`
  (including a stale flag cleared by a real session); bootstrap restore keeps
  a pending widget repository and opens it.
- `ui/LiveRepoGlanceScreen.kt`: any pin toggle redraws the widgets.
- `MainActivity.kt` (review repair): a recreated activity no longer replays a
  widget or tile intent (`if (savedInstanceState == null) handleLiveIntent`).
- `RepoGlanceViewModel.kt` (review repair): sample enter/leave and the
  bootstrap stale-flag leave run on `sessionDispatcher + NonCancellable`, with
  the redraw in `withContext(Dispatchers.IO)`, so neither `onCleared` nor a
  cancelled bootstrap loses it.
- Guards: `SampleWidgetsGuardTest` (8), `SampleWidgetsTest` (9),
  `SampleModeGuardTest` (relaxed to redraw-only, cold-start restore guarded),
  `TileTextTest` (+3), `WidgetStoreReadsTest` (sample stores out of
  composition), `RepoWidgetConfigurationTest`.
- Docs: `docs/INVARIANTS.md` sample row, `docs/design.md` v6, verify-skill
  `sample-mode`, `widget-setup`, `stack-widget`, `quick-settings-tile`,
  `README`; `bin/verify-repoglance widget-bounds` finds repo widgets by package.

## Gate

- `./gradlew assembleDebug check` on the final tree: BUILD SUCCESSFUL
  (`runs/build-runs/sample-widgets-check-6.log`); 308 debug unit tests,
  0 failures, 0 errors (test-result XML); detekt, lint, feature map and helper
  checks green. Final debug APK
  `5c08d1ea3469621814a169eeb195153528cb4c71ffd6b4a11993009002608f3f`.
- Mutation checks (`runs/build-runs/sample-widgets-mutations.log`), each
  applied alone and reverted:

  | Mutation | Guard | Result |
  | --- | --- | --- |
  | sample repo widget reads `LiveSnapshotStore` | SampleWidgetsGuardTest | caught |
  | live stack also reads sample pins | SampleWidgetsGuardTest | caught |
  | sample row opens GitHub | SampleWidgetsTest | caught (assertion) |
  | leaving sample does not redraw | SampleModeGuardTest | caught |
  | cold start drops the pending repository | SampleModeGuardTest | caught (assertion, after tightening the guard to the exact statement) |
  | sample setup schedules `BackgroundRefresh` | SampleWidgetsGuardTest | caught |
  | tile tap without `EXTRA_LIVE_CATALOG` | TileTextTest | caught |
  | sample stack row shows a clock | SampleWidgetsTest | caught |
  | activity recreation replays the tile intent | SampleModeGuardTest | caught (assertion) |
  | leave redraw cancellable with the scope | SampleModeGuardTest | caught (assertion) |
  | leave redraw back on the closable `sessionDispatcher` | SampleModeGuardTest | caught (assertion) |
  | bootstrap stale-flag leave cancellable | SampleModeGuardTest | caught (assertion) |

## Emulator run (approved `EMULATOR37X1X11X0`, AVD Pixel_10_Pro_Fold, API 36)

Signed out, no GitHub session on the device. Widget placement and
`add-tile`/`remove-tile` approved by the maintainer for this run. Run dir
`runs/verify-repoglance-runs/sample-widgets-039-emu/` (ignored). Launcher text
read from `uiautomator` dumps.

Builds: steps 1-6 ran on APK `a3999f97…`; the tile-catalog fix and steps 7-11
on `7b17c37f…`; the stack-redraw-on-remove fix and steps 12-13 on `f7485f6c…`.

1. **Entry.** `Explore with sample data`: `repoglance:sample-bar`,
   `@saariuslystoned · 7 repositories`.
2. **Setup (2A).** Stack placed via the debug fixture home `Pin stack widget`
   → `Add to home screen`; repo widget via `Pin repo widget` → setup
   `RepoWidgetConfigActivity`. The note reads `Sample data: saving pins this
   sample repository; …`. The `Repository` list is exactly `saari-co/rocket,
   saari-co/api-server, saari-co/mobile-app, dinkuskit/infra,
   dinkuskit/design-system, saariuslystoned/dotfiles, saari-co/legacy-site`.
3. **Widgets (1A).** After saving `saari-co/rocket`: stack `Pinned · 1`,
   `saari-co/rocket`, `sample`, `issues 5 · PRs 3 · review 0`; compact
   `rocket`, `sample`, `issues 5`, `PRs 3`. Catalog shows
   `Unpin saari-co/rocket`. Capture `sample-widgets-home.png`
   `601d530288334066a96337b6ba0ac7ab1ef3c12018905f4e764243963c0ebf31`.
4. **In-app pin redraw.** Pin `saari-co/api-server`: stack `Pinned · 2`,
   rocket then api-server, both `sample`.
5. **Tall size.** `widget-resize repo 300` (190x197 dp): `saari-co/rocket`,
   `ISSUES 5 · PRS 3 · sample`, `ISSUE #415 · 25m`, `PR #412 · 25m`, …
   Capture `sample-widgets-tall.png`
   `0de5c02b1b5351ba19a53c4900a0d409bf2a25146ba2d1a2f398531370039ed2`.
6. **Taps.** Row tap: top activity `co.saari.repoglance/.MainActivity`,
   `repoglance:live-home` + `saari-co/rocket` + sample bar; zero
   `com.github.android`/`customtabs` records. Cold start: `am force-stop`,
   `pidof` empty, stack row tap → logcat `START … dat=repoglance://live/…`,
   screen `saari-co/api-server` repo view with sample bar. (The first tap right
   after `HOME` was swallowed by the launcher; the second opened the app.)
   Tile: `add-tile`, shade `content-desc` = `RepoGlance, sample data, latest
   push to saari-co/rocket updated 25m ago`, tile drawn active (capture
   `shade-sample.png`
   `16bc755aaa9e2269bf19984225cfc66a6d778feb7cdffdf7917fcc6dc8871726`; narrow
   slot, icon only). **Finding:** `click-tile` resumed the repository view the
   app was left on, not the catalog (existing tile behaviour, live and sample).
   Fixed by adding `EXTRA_LIVE_CATALOG` to the tile intent (matches 019's
   "opens the app on the live catalog" and 6A).
7. **Tile after fix.** Repo widget header tap → `saari-co/rocket` view; home;
   `click-tile` → `@saariuslystoned · 7 repositories` + sample bar.
8. **Exit (4A).** `Sign in with GitHub` → `repoglance:connect-github`,
   `repoglance:explore-sample`, no device code. Home: `Pinned · 0`,
   `Pin repositories in RepoGlance`, `RepoGlance`, `Tap to choose a
   repository`; tile `RepoGlance, Open to connect`. Capture
   `home-after-exit.png`
   `d340520182645b7af7691dd2bb59e38ec87b33ef37356da35e46077f87e5f5ba`.
9. **Unconfigured tap, signed out (5A).** Opens `RepoWidgetConfigActivity`:
   `No repositories to choose from yet`.
10. **Re-entry (3A).** Explore again: zero `Unpin` controls; widgets stay
    unconfigured / `Pinned · 0`.
11. **Unconfigured tap in sample (5A + 2A).** Opens sample setup; save
    `dinkuskit/infra` with feed `PRS`: tall header `PRS 1 · sample`, row
    `PR #1203 · 1d`; stack `Pinned · 1`, `dinkuskit/infra`. Removing the
    widget released the pin (catalog: zero `Unpin`), **but the stack still
    listed `dinkuskit/infra`**: removal never redrew the stack (live has the
    same gap, hidden by the 30-minute refresh; sample never refreshes). Fixed:
    `onDeleted` redraws the stack widgets.
12. **Remove after fix.** New widget for `saari-co/rocket` → stack
    `Pinned · 1`; drag to `Remove` → stack `Pinned · 0`,
    `Pin repositories in RepoGlance` with no other redraw.
13. **Cleanup.** Exit to the Connect screen, `remove-tile`, both widgets
    removed (`widget-bounds` empty).

StrictMode (logcat 17:05 onward, covering steps 6-13): no violation from any
sample path. Two `DiskReadViolation`s from the existing live line
`RepoWidgetReceiver.onDeleted` → `RepoWidgetConfigStore.load` on the main
thread (predates this change; recorded as debt, not fixed here).

## Final smoke on the reviewed tree (APK `654e28f3…` then `5c08d1ea…`)

Emulator rebooted (`-no-snapshot-save`) after another session had shut it
down. Run dir `runs/verify-repoglance-runs/sample-widgets-039-final/`.

- On `654e28f3…` (tree `da68a22d`): entry; both widgets placed; setup list
  exactly the seven sample names; after saving `saari-co/rocket` the stack and
  compact widget read as in step 3; tile `content-desc` as in step 6;
  `click-tile` → sample catalog. **Recreation (review finding 1):** tile →
  catalog → open `saari-co/api-server` → `cmd uimode night yes` (activity
  relaunched) → still `repoglance:live-home` + `saari-co/api-server`.
- On `5c08d1ea…` (final tree `c8b449ba`), doctor `apk_local == apk_device`:
  the app-update redraw shows `Pinned · 2`, `saari-co/rocket`, `sample`,
  `saari-co/api-server`, `sample`, compact `rocket` `sample` `5` `3` (capture
  `final-sample-home.png`
  `35931f479690b63da683b3a4a78c68b208fe3716bb3647ef5e618b7256f2be2c`).
  **Leave then Back at once (review finding 5):** `input tap` on
  `repoglance:sample-sign-in` and `KEYCODE_BACK` in one shell call; the
  launcher is on top, and home reads `Pinned · 0`,
  `Pin repositories in RepoGlance`, `RepoGlance`, `Tap to choose a
  repository`; tile `RepoGlance, Open to connect`; unconfigured tap →
  `No repositories to choose from yet`.
- Logcat for the whole final-APK run: no StrictMode violation, no
  `FATAL EXCEPTION`.
- Cleanup: tile removed, both widgets removed (`widget-bounds` empty),
  signed out and not in sample mode, emulator shut down.

## Signed-in physical Fold (ClawSweeper P1 follow-up, maintainer option 2)

ClawSweeper (`9bd85f6a`, 5/6, proof sufficient, no findings) held one merge
risk: no physical phone and no signed-in device. The maintainer chose to run
the signed-in side on the Fold (`59151FDCG000JA`, inner display, posture
`OPENED`). Before driving, the Fold sat idle on the launcher, with no other
adb client on it. The PR's APK
`5c08d1ea3469621814a169eeb195153528cb4c71ffd6b4a11993009002608f3f` was
installed over the Fold's earlier `6a110834…` debug build with
`install -r -d`, so data and session were kept. Doctor reported
`apk_local == apk_device`. There was no sign-in, sign-out or capture.
Launcher text was read with repository names masked; only the public
`saari-co/RepoGlance` is named here.

- **Baseline, then after install:** a repo widget (148x89 dp) reading
  `RepoGlance`, `Tue 8:36 AM`, `issues` `3`, `PRs` `2`, and the stack reading
  `Pinned · 0`, `Pin repositories in RepoGlance`. Both before and after the
  app-update redraw, the widgets show no `sample` and no `FIXTURE PREVIEW`.
- **Session:** the app shows `@saariuslystoned · 82 repositories` with
  `repoglance:live`, and no `repoglance:sample-bar`, `connect-github` or
  `explore-sample`.
- **Live setup:** `APPWIDGET_CONFIGURE` for widget 16 shows the live note
  `Saving pins this repository in RepoGlance; …` with `saari-co/RepoGlance`
  selected. The open list has 11 visible repository entries and none of the
  seven sample names. Cancelled, so the widget was unchanged.
- **Live pin redraw:** pinning `saari-co/RepoGlance` in the live catalog
  redraws the stack at once to `Pinned · 1`, `saari-co/RepoGlance`,
  `Tue 8:36 AM`, `issues 3 · PRs 2 · review 0` (a live clock, no `sample`).
  It was unpinned afterwards, and the stack is back to `Pinned · 0`.
- **Tile:** already in the shade, so no `add-tile`. It reads
  `RepoGlance, latest push to <repo> updated 4m ago` (live, no sample).
  After a widget header tap opened the live `saari-co/RepoGlance` view
  (`repoglance:live-home`), `click-tile` landed on the live catalog:
  `repoglance:live` and `Find a repository`, with no `live-home` and no
  sample bar.
- **Logcat for the run:** no RepoGlance crash, and no main-thread disk
  violation. One `UntaggedSocketViolation` from the live network transport
  (`HttpTransport.kt:50`), which predates this change and which this PR does
  not touch.
- **End state:** the launcher is showing, pins are as found, and the Fold now
  runs the PR build.

## Not proven here

- **Sample mode on a physical phone and the Fold's cover display:** the
  sample-mode run used the emulator's inner display. The physical Fold ran
  only the signed-in side, because entering sample mode needs a signed-out
  device and agents never sign out.
- **Signed-in isolation:** proven on the signed-in Fold for placed widgets,
  setup, pin redraw and the tile. The cross-mode case (sample mode followed
  by a real sign-in) rests on the leave redraw, proven on the emulator, and
  the guards.
- **Real launcher drag from the widget picker:** placement used the app's
  `requestPinAppWidget` route; the setup launch is the same `APPWIDGET_CONFIGURE`.
- **Tile subtitle pixels:** proven from `content-desc`; the narrow slot shows
  the icon only.
- **Marker look:** provisional (`sample-marker-040`).

## Review (independent agent, exact source)

Scope: `git diff --no-color --full-index <base> <tree> -- . ':!.grilltrack'`.

| Round | Tree | Diff sha256 | Result |
| --- | --- | --- | --- |
| 1 | `34bf98f9748cf8eb5e7bb3bac0c97338a2c3ac64` | `563935966f62240e969df69c339a3d982b74ca93706e9ed7b75acd44218a4643` (25 files, +713 −45) | 1 P2, 7 P3 |
| 2 | `da68a22dee28b2cbcfb44d65e7ab2e274e8014ff` | `e9701f6cc0908f1bb7bd92acee3ea0513f7cd2ac49aa37fb85bc7286152d2517` (delta `53d890e4…`) | 3 P3 |
| 3 | `c8b449ba19aa93eb1392961845dd5ba9b55febbb` | `a29eea4b930c167ce676661916fe7446fad305f36f56f82e156dc312790b731f` (26 files, +733 −48; delta `cb456287…`) | **clean** |

Each round checked the identity hash first.

Round 1 adjudication:

| # | Finding | Class | Action |
| --- | --- | --- | --- |
| 1 | P2: the tile's catalog extra is replayed on activity recreation (unfold, rotate, dark mode) | required_fix | `onCreate` handles live intents only when `savedInstanceState == null`; guarded; device-proven |
| 2 | P3: `onDeleted` reads sample prefs on the main thread | defer | Recorded as debt in the INVARIANTS sample row (the live reads there predate this change) |
| 3 | P3: the stack redraw after removal is not tied to the broadcast lifetime | defer | `goAsync` is taken by Glance; risk is small |
| 4 | P3: sample setup save races `leave()` (split screen); the live save path never re-checks mode | defer | Needs two activities racing; no GitHub reach |
| 5 | P3: the redraw after leaving sample can be cancelled with the ViewModel | required_fix | `NonCancellable` (round 2), then the redraw on `Dispatchers.IO` (round 3); device-proven |
| 6 | P3: widget-tap setup opens in the app's task, so Save lands in the app | defer | UX, not grilled |
| 7 | P3: locked sample tile always active | reject_false_positive | The live locked tile is active when a record exists; a sample record always exists |
| 8 | P3: `widget-bounds` labels a loading stack as `repo` | required_fix | Package match requires text |

Round 2 adjudication: R1 (the redraw is still lost when `onCleared` closes
`sessionDispatcher`) required_fix; R2 (the bootstrap repair is unguarded)
required_fix; R3 (the INVARIANTS debt note is too narrow) required_fix. The
existing `clearSavedSession` redraw has the same `onCleared` flaw; it predates
this change and is left out of scope.

Round 3: clean. The write is kept, the IO redraw finishes, and the rejected
hand-back after `withContext` is swallowed without a crash.
