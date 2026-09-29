# Sample mode

Someone without a GitHub account taps `Explore with sample data` on the sign-in screen. They get the real catalog, repository view and navigator, filled with a made-up account, with a `SAMPLE` marker and a `Sign in with GitHub` action on every screen. Sample mode never calls GitHub, survives a restart, and ends when the user chooses to sign in.

## Sub-features

- `sample-entry`: an `Explore with sample data` button under `Connect GitHub` on the signed-out screen.
- `sample-catalog`: `@octodev · 7 repositories`. Owners are `acme`, `octoco` and `octodev`. None of them is the maintainer's, and none of the seven repositories exists on GitHub, although the owner handles are real GitHub accounts; there is one archived repository and one with an unknown push time. The owner filter, search, sort and pins all work.
- `sample-marker`: a `SAMPLE` chip and `Made-up repositories, not your GitHub` on the catalog and the repository view. The `LIVE` chip, `Manage GitHub access` and `Disconnect GitHub` are absent.
- `sample-pins`: pins are kept apart from the real pinned set and never refresh a widget.
- `sample-rows`: issues and PRs under each repository. A tap shows `Sample item — not on GitHub` and never opens GitHub.
- `sample-persist`: the app reopens in sample mode after a force-stop until the user signs in.
- `sample-exit`: `Sign in with GitHub` returns to the Connect screen without requesting a device code.

## How to get to it (user POV)

- Open RepoGlance with no GitHub session and tap `Explore with sample data`.
- After a restart, RepoGlance reopens straight into sample mode.
- Tap `Sign in with GitHub` on any sample screen to leave.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- The phone has **no** GitHub session. This recipe uses only the signed-out path: an agent never signs in or out. On a phone holding the maintainer's session, the entry is unreachable; report `verified-unreachable: session present`.

- **Entry.** Run `bin/verify-repoglance launch MIXED live`, then `dump signin`. The dump contains `repoglance:connect-github` and `repoglance:explore-sample`. Run `tap repoglance:explore-sample`, then `dump sample-catalog`.
  - The dump contains `repoglance:sample-bar`, `repoglance:sample`, `repoglance:sample-sign-in`, `SAMPLE` and `@octodev · 7 repositories`.
  - It contains no `repoglance:live` and no `Account and access settings`.
  - Every repository-shaped label starts with `acme/`, `octoco/` or `octodev/`.
  - Run `capture sample-catalog`.
- **Owner filter and search.** `tap repoglance:owner-filter`, tap `octoco`, then `dump sample-octoco`. Only `octoco/infra` and `octoco/blocks` remain. Clear it with `tap repoglance:owner-filter` and then `tap All`. Then `tap repoglance:repo-search`, `type rocket`, and `dump sample-search`: only `acme/rocket` remains.
- **Pin.** `tap "Pin acme/rocket"`, then `dump sample-pinned`. The first repository-shaped label is `acme/rocket` and its control reads `Unpin acme/rocket`. Tap `Unpin acme/rocket` to restore.
- **Repository view.** Tap `acme/rocket`, then `dump sample-repo`. It contains `repoglance:live-home`, `repoglance:sample-bar`, `Issues`, `PRs` and `Updated just now`, with rows such as `#415`.
  - Run `capture sample-repo`.
  - Tap a row, then within a second run `adb shell dumpsys notification | grep TextToastRecord`. It shows `co.saari.repoglance` with `text=Sample item — not on GitHub`.
  - The top resumed activity is still `co.saari.repoglance/.MainActivity` (`adb shell dumpsys activity activities | grep topResumedActivity`). No `com.github.android` or `customtabs` record appears.
- **Persistence.** Run `adb shell am force-stop co.saari.repoglance`, `bin/verify-repoglance launch MIXED live`, then `dump sample-restart`. It still contains `repoglance:sample-bar` and `@octodev · 7 repositories`.
- **Exit.** `tap repoglance:sample-sign-in`, then `dump sample-exit`. It contains `repoglance:connect-github` and `repoglance:explore-sample`, and no `Enter this code on GitHub`: no device code was requested. Force-stop and relaunch. The Connect screen shows again, not the sample catalog.
- **Offline.** Run `adb shell cmd connectivity airplane-mode enable`, enter sample mode, tap `repoglance:refresh-repositories`, open `acme/api-server` and tap `repoglance:refresh-repository`. The catalog and rows still show, and nothing reads `could not refresh`, `Loading live GitHub data` or `No current value`. Then **Exit**, and `bin/verify-repoglance cleanup` turns airplane mode off.
- **Proof.** Capture `sample-catalog`, `sample-repo` and the Connect screen after exit, and record each SHA-256. Record the toast line from `dumpsys notification` as text.

## Gotchas

- Sample mode is stored on the phone. A run that stops midway leaves the next `launch MIXED live` on the sample catalog instead of the Connect screen, and `bin/verify-repoglance cleanup` does not clear it. End every run with **Exit**, or run `adb shell pm clear co.saari.repoglance` on a phone with no session to keep.
- The `SAMPLE` marker's look is provisional (GrillTrack `sample-marker-040`). Assert its test tags and text, not its colours.
- Widgets and the Quick Settings tile do not show sample data yet (`sample-widgets-039`). This recipe does not cover a widget placed while in sample mode.
- The toast lasts about two seconds. Neither `uiautomator dump` nor `capture` catches it: the capture's guarded dump runs first, and the toast is gone by the screenshot. Prove it from the system toast queue (`dumpsys notification`) instead.
- The approved emulator (`EMULATOR37X1X11X0`, AVD `Pixel_10_Pro_Fold`, API 36) starts with no GitHub session, so it is the easiest target for this recipe. Boot it with `-no-snapshot-save`. An older RepoGlance on it has a higher versionCode, so install over it once with `adb install -r -d` (keeps data) before `launch`.
