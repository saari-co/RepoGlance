# Widget redraw after an app update — GrillTrack `widget-update-redraw-025` (2026-09-19)

Track `gt-20260728163459-227573`, a follow-up to the live-widgets map
(`.grilltrack/maps/live-widgets.md`) from the gap recorded in
`proof/stack-widget-live-20260919/PROOF.md#gaps`. Branch
`claude/widget-update-redraw-025`, worked in the isolated worktree
`.claude/worktrees/focused-greider-004e47` on a local branch that tracks it,
on main `948d6627a3e4a59b4d7612cef537aeaeba71cc3b` (PR #23 merged).

## Decision (locked and confirmed before implementation)

Only the app's own update: a receiver declared in the manifest for
`android.intent.action.MY_PACKAGE_REPLACED` redraws both widget kinds
through `WidgetRefresh.updateAll` from saved data. It makes no network call
and uses no service or wakelock. Time zone, clock, locale and boot changes
are not handled; the next refresh (at most about 30 minutes) redraws. The
maintainer chose app update only.

Prior locks kept: read-only; truth rules (the redraw shows saved numbers
with their own saved time, so last-good still reads as last-good); auth
rules (no session or token access); CI floor.

## Implementation (`git:65dcd2196b70210c0e1133f980397213cb57d011`)

- `widget/AppUpdateReceiver.kt`: returns unless the action is
  `ACTION_MY_PACKAGE_REPLACED`; otherwise `goAsync()`, then
  `WidgetRefresh.updateAll(applicationContext)` on `Dispatchers.IO`, and
  `pending.finish()` in a `finally`.
- `AndroidManifest.xml`: `.widget.AppUpdateReceiver`, `exported="false"`,
  one intent filter with that single action. The system delivers this
  broadcast to a receiver that isn't exported (shown on device below).
- Feature map: one gotcha line each in `stack-widget.md` and
  `widget-setup.md` saying both widgets redraw from saved data after an app
  update, with no GitHub call.

## JVM test and CI floor

`AppUpdateReceiverTest` (3 tests, 0 failures):

- the manifest declares `.widget.AppUpdateReceiver`, not exported, for
  exactly `MY_PACKAGE_REPLACED`;
- across all receivers the only actions are `APPWIDGET_UPDATE` and
  `MY_PACKAGE_REPLACED`, and just one receiver has the latter, so there are
  no clock, locale or boot triggers;
- the receiver checks the action and calls `goAsync()` before
  `WidgetRefresh.updateAll(`, which comes before `pending.finish()`, and the
  finish sits in a `finally`. The source has no GitHub, Http,
  BackgroundRefresh, WakeLock, Service or WorkManager reference.

`ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon check
assembleDebug` green. The lint and detekt baselines are unchanged; the
comment ban and log ban hold.

## Device proof (Pixel 11 Pro Fold 66261FDDJ002J5, inner display 2076x2152, OPENED)

Placed widgets per `dumpsys appwidget` (launcher host 1024): stack **id 16**
and repo widget **id 18** (ISSUES, `saari-co/RepoGlance`). The handoff named
the stack as id 12. On this phone id 12 is GitHub's own widget in the
SystemUI host, so 16 is the RepoGlance stack. Unfolded, both widgets sit on
the same visible home screen.

1. `doctor` before the install failed as it should: the phone was folded,
   dreaming and locked, and the installed hash was still the PR #23 build
   `685f05ea…`. The maintainer unlocked and unfolded the phone.
2. Install 1: with the launcher resumed, logcat cleared, `adb install -r`
   of the debug APK, without `launch` and without opening the app. Pins
   unchanged. `doctor` then passed: `apk_local` = `apk_device` =
   `37e8853df67dc054c6fec40ff6b1cc41a61a986b274fa625e8e0ca44e9dacf41`,
   awake, unlocked.
   - `dumpsys activity broadcasts history`: `MY_PACKAGE_REPLACED`
     (`pkg=co.saari.repoglance`) was DELIVERED to
     `co.saari.repoglance.widget.AppUpdateReceiver` (manifest), finishing
     113 ms after dispatch.
   - Home dump `home-after-install`: stack `Pinned · 1`,
     `saari-co/RepoGlance` `9:22 AM` `issues 2 · PRs 0 · review 0`; repo
     widget `saari-co/RepoGlance` `ISSUES 2 · as of 9:22 AM` with rows
     `ISSUE #11 · 4w` and `ISSUE #7 · 5w`. No loading placeholder. Capture
     `home-after-install`
     `1b44cce23bb32119cf597cd342cca0aa7c6a4467fe791a5e721c2fc0e50c9b8e`
     (the maintainer's home screen; kept local in `runs/`, hash only),
     inspected: both widgets are drawn with content. Dump XML
     `69aab42eef80e9257c77ddca8456b562ad5aff806ca3c2f8d371651c8027b714`.
   - The foreground app was still the Pixel launcher: the app was not opened.
3. Isolating the fix. On this install the system also sent
   `APPWIDGET_UPDATE` to **both** providers (`appWidgetIds=[16]` and
   `[18]`), unlike the PR #23 install, when only 18 got one. So the placed
   picture alone doesn't show that the receiver caused the redraw. Supporting
   evidence: the Glance state redraw counter (`REDRAW_KEY` in
   `files/datastore/appWidget-<id>.preferences_pb`, read with `run-as` on the
   debug build). Only `WidgetRefresh` bumps it; the system's `onUpdate` path
   doesn't. Install 2 of the same APK, with the launcher resumed and no
   background job running: stack 16 `11 → 12`, repo 18 `28 → 29`, exactly
   one redraw each, with `MY_PACKAGE_REPLACED` delivered to
   `AppUpdateReceiver` again (terminal +44 ms). Home dump
   `home-after-install2` matches install 1 (same XML hash `69aab42e…`).
4. No `FATAL EXCEPTION` in logcat.

Run artifacts (local, ignored): `runs/verify-repoglance-runs/20260919T134018Z/`
(`broadcasts-history.txt` `4bdd39ee…`, `broadcasts-history-install2.txt`
`cd76b5e8…`, `logcat-after-install.txt` `31295c42…`,
`redraw-counters-install2.txt` `6d837b0e…`), `20260919T134103Z/`,
`20260919T134111Z/` (capture), `20260919T134208Z/`.

## Gaps and risk

- The distinguishing case, the system asking only one provider to update,
  didn't recur on these installs. The receiver's effect is shown by the
  counter increment rather than by a placeholder turning into content.
- StrictMode (debug) logs 26 `DiskReadViolation`s after the installs: both
  widgets read `AppPrefs`, `LiveSnapshotStore`, `LiveRowsStore`,
  `RateLimitStore`, `RepoWidgetConfigStore` and `CatalogNamesStore` inside
  `provideContent` on the main thread. That's existing 023/024 code which
  runs on every redraw; this slice adds one redraw per update. Not fixed
  here (not part of the lock).
- `bin/verify-repoglance doctor` prints `posture=C` on this Android build:
  its parser takes the first capital word of `Committed state: …`. The phone
  was `OPENED` (`cmd device_state state`).
- Earlier follow-ups stay open: compact 120dp last-good label fit,
  LOW/EXHAUSTED device proof, new pins read `no data` until refreshed.

## Review round 1 (source identity git:84a8665e27c2ab1f87f6a2cdbfc662d131b78970, PR #24)

- OpenClaw `req-20260919T135054Z-278029219180` (worktree materialized on
  spark-2): correct (0.98), 0 findings.
- ClawSweeper: diamond lobster 5/6 (proof diamond lobster 5/6, patch
  quality diamond lobster 5/6), no findings, no security findings, `proof:
  sufficient`, `status: ready for maintainer look` (PR #24 comment
  5742440743). Its two "before merge" merge-risk items restate the gaps
  above: the one-provider install case wasn't recreated (the evidence is
  broadcast delivery plus the redraw counters), and StrictMode logs disk
  reads from the existing redraw path. Both classified `defer`: they were
  disclosed before review, and the StrictMode work is a separate follow-up
  task. No repair round needed. Only the owner's merge authority remains.
  Recorded clean in the ledger.
