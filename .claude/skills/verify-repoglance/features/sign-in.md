# Sign in with GitHub

A user connects RepoGlance to GitHub through GitHub's device flow: the app shows a short code, the user copies it and opens GitHub's verification page in a Custom Tab, and once GitHub authorizes it the app brings itself back over the tab, shows the RepoGlance mark while it finishes signing in and loads repositories, and keeps the encrypted session across restarts.

## Sub-features

- `signin-checking` shows the RepoGlance mark (magnifier with the commit eye) with two pulsing rings and `Checking your GitHub session…` while the saved session is read; no counts, no `Connect GitHub`. The rings hold still when the system animator scale is off.
- `signin-start` shows `Connect GitHub` when there is no session.
- `signin-code` shows the user code, `Copy code & open GitHub`, `Cancel sign-in`, an expiry countdown, and a line (`repoglance:signin-return-instruction`) saying RepoGlance comes back on its own and to close the GitHub tab if it doesn't (observed by the maintainer only; never persisted by the harness).
- `signin-return` brings RepoGlance back over the Custom Tab by itself once the authorization is saved, with no tap on the tab, then lands on the live catalog with the `LIVE` chip.
- `signin-finishing` shows the RepoGlance mark with its pulsing rings and `Finishing sign-in…`, then `Loading your repositories…`, with no spinner, between authorization and the catalog. The rings keep running across the message change. Refreshing the catalog later still shows the plain spinner.
- `signin-persist` survives a force-stop and cold start without reconnecting.
- `signin-disconnect` (`Disconnect GitHub` in the account menu) clears the session.

## How to get to it (user POV)

- Open RepoGlance with no session: the sign-in screen is the first screen.
- From the live catalog, the account menu (`Account and access settings`) offers `Manage GitHub access` and `Disconnect GitHub`.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- **Human-gated.** Starting, completing, or cancelling a sign-in, and disconnecting, act on the maintainer's GitHub account. An agent may only observe; the maintainer performs the gated taps on the phone.

- **Checking state.** It lasts tens of milliseconds in the real app, so prove it through the debug holder: `bin/verify-repoglance launch MIXED checking`, then `dump checking`. The dump contains `repoglance:checking-mark` and `repoglance:checking-message` and no `repoglance:connect-github`; then `capture checking`.
- **Observe the entry state.** Run `bin/verify-repoglance launch MIXED live` then `dump signin`. Either the dump contains `repoglance:connect-github` (no session) or it contains `repoglance:live` (session present). Record which; do not tap `repoglance:connect-github`.
- **Post-token screens.** They last milliseconds in the real app, so prove them through the debug holder: `bin/verify-repoglance launch MIXED signin-finishing`, then `dump signin-finishing`. The dump contains `repoglance:signin-mark` and `repoglance:signin-message` with text `Finishing sign-in…`, and no progress indicator; `capture signin-finishing`. Then `tap repoglance:signin-message`, `dump signin-loading`: same mark, message `Loading your repositories…`; `capture signin-loading`.
- **Maintainer step (gated).** The maintainer taps `Connect GitHub`, then `Copy code & open GitHub`, enters the code on GitHub, and authorizes, then leaves the GitHub tab alone. RepoGlance comes back over it on its own, within GitHub's poll interval (about 5 seconds). While the code screen is visible the agent runs **no** `dump`, `tap`, or `capture`: the helper refuses both (`redact-guard: GitHub device-code screen detected`) because a dump persists every visible string, and a capture would show the code. The agent waits for the maintainer to say the sign-in finished.
- **Assert completion.** After the maintainer returns, run `dump after-signin`. The dump contains `repoglance:live` and no `Connect GitHub`.
- **Persistence.** Run `adb shell am force-stop co.saari.repoglance`, then `bin/verify-repoglance launch MIXED live`, then `dump after-restart`. The dump still contains `repoglance:live`.
- **Proof.** Capture only screens that contain no code value and no unfiltered catalog: the `Connect GitHub` screen and the guarded filtered catalog from Find a repository. `bin/verify-repoglance dump` and `capture` refuse to write anything while the device-code screen is showing.

## Gotchas

- GitHub's segmented code fields on Android do not reliably accept a whole-code paste; manual entry is a documented provider limitation, not an app defect.
- Returning from the Custom Tab wakes a paused poll; if the screen still shows the code after return, wait for GitHub's minimum interval before judging.
- Closing the GitHub tab by hand before RepoGlance returns lands on the code screen until the next poll succeeds (up to GitHub's interval, typically 5 seconds). That is designed: the code stays until the app knows the authorization went through.
- The automatic return happens only for a sign-in whose GitHub tab RepoGlance opened, and never when RepoGlance is already in front. Entering the code on another device finishes the sign-in in place with no relaunch.
- `bin/verify-repoglance launch` force-stops the app and kills a sign-in in progress. Never run it while the maintainer is signing in; to apply extras without disturbing the foreground use `adb shell am start --activity-clear-task -n co.saari.repoglance/.devlaunch.ScenarioLaunchActivity --es screen none`.
- A `401` on repository content invalidates the session and shows `Reconnect GitHub`; that is designed behaviour.
- The code screen is never dumped, captured, printed, or quoted; `scripts/verify_redact_guard.py` enforces that in the helper, and the recipe never asks for it.
