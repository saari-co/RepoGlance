# Sign in with GitHub

A user connects RepoGlance to GitHub through GitHub's device flow: the app shows a short code, the user copies it and opens GitHub's verification page in a Custom Tab, and on return the app finishes signing in and keeps the encrypted session across restarts.

## Sub-features

- `signin-start` shows `Connect GitHub` when there is no session.
- `signin-code` shows the user code, `Copy code & open GitHub`, and `Cancel sign-in`, with an expiry countdown (observed by the maintainer only; never persisted by the harness).
- `signin-return` completes on return from the Custom Tab and lands on the live catalog with the `LIVE` chip.
- `signin-persist` survives a force-stop and cold start without reconnecting.
- `signin-disconnect` (`Disconnect GitHub` in the account menu) clears the session.

## How to get to it (user POV)

- Open RepoGlance with no session: the sign-in screen is the first screen.
- From the live catalog, the account menu (`Account and access settings`) offers `Manage GitHub access` and `Disconnect GitHub`.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- **Human-gated.** Starting, completing, or cancelling a sign-in, and disconnecting, act on the maintainer's GitHub account. An agent may only observe; the maintainer performs the gated taps on the phone.

- **Observe the entry state.** Run `bin/verify-repoglance launch MIXED live` then `dump signin`. Either the dump contains `repoglance:connect-github` (no session) or it contains `repoglance:live` (session present). Record which; do not tap `repoglance:connect-github`.
- **Maintainer step (gated).** The maintainer taps `Connect GitHub`, then `Copy code & open GitHub`, enters the code on GitHub, and returns. While the code screen is visible the agent runs **no** `dump`, `tap`, or `capture`: the helper refuses both (`redact-guard: GitHub device-code screen detected`) because a dump persists every visible string, and a capture would show the code. The agent waits for the maintainer to say the sign-in finished.
- **Assert completion.** After the maintainer returns, run `dump after-signin`. The dump contains `repoglance:live` and no `Connect GitHub`.
- **Persistence.** Run `adb shell am force-stop co.saari.repoglance`, then `bin/verify-repoglance launch MIXED live`, then `dump after-restart`. The dump still contains `repoglance:live`.
- **Proof.** Capture only screens that contain no code value and no unfiltered catalog: the `Connect GitHub` screen and the guarded filtered catalog from Find a repository. `bin/verify-repoglance dump` and `capture` refuse to write anything while the device-code screen is showing.

## Gotchas

- GitHub's segmented code fields on Android do not reliably accept a whole-code paste; manual entry is a documented provider limitation, not an app defect.
- Returning from the Custom Tab wakes a paused poll; if the screen still shows the code after return, wait for GitHub's minimum interval before judging.
- A `401` on repository content invalidates the session and shows `Reconnect GitHub`; that is designed behaviour.
- The code screen is never dumped, captured, printed, or quoted; `scripts/verify_redact_guard.py` enforces that in the helper, and the recipe never asks for it.
