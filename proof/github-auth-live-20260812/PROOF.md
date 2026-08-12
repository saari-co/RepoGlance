# GitHub App authentication and live-data proof — 2026-08-12

## Current public-device-flow repair

The current PR repair replaces the source-bound confidential web flow below
with GitHub's public device flow. The APK now carries only the public client ID,
keeps the user code visible without opening a browser automatically, and waits
for an explicit **Copy code & open GitHub** tap. That action copies the code,
marks the clipboard content sensitive on Android 13 and later, and opens the
exact returned GitHub verification URI in a Custom Tab. Returning from the tab
leaves the code visible and **Cancel sign-in** remains available. Activity
resume now wakes the pending poll after the Custom Tab may have paused or frozen
background execution. Every wake rechecks the absolute next-request deadline,
so it cannot bypass GitHub's required interval or widened slowdown interval.
The ViewModel honors expiry and cancellation and rotates expiring
access/refresh tokens without a confidential client credential. The
authentication callback/App-Link source was removed because device flow does
not return through the app.

Transient transport I/O while checking authorization now remains pending only
until local code expiry and always waits the current GitHub interval before
retrying. Session persistence relies on `AtomicFile.finishWrite()` for its
documented sync/close/commit instead of performing a redundant raw descriptor
sync first. A persistence exception clears partial local state and becomes a
fixed user-facing message that authorization succeeded but the device could not
save the session. No token, response body, exception message, exception class,
or cause is logged or reflected.

The repair also promotes repository-content 401 responses to local session
invalidation and **Reconnect GitHub**, updates visible age labels once per
minute only while the UI lifecycle is resumed, refreshes them immediately on
resume, and marks every persistent fixture widget surface `FIXTURE PREVIEW`.

### Current repair source and local validation

- Repair implementation commit:
  `9a159f6986647350fac2a1b9e4200459ae277782`
- Device-authorization UX repair starting head:
  `9da86ce0a970624af05feeab0f0cc1efe53a1cbe`
- Browser-resume polling repair starting head:
  `2a385e9cfc783c8d3215ad29c66a3aa04e6325df`
- Poll-I/O/session-persistence repair starting head:
  `b3d8b2f8aa903c1f0574ffb206f0f397b2a72502`
- Repair starting head: `fbb78927d3579294c0652f41e6f39b02825fa2a5`
- Stacked review base: `5c0a0d660b5332f40b3ccb1fbf21dd63e755f3ef`
- Secret-free clean command:
  `ANDROID_HOME=/Users/bobbybones/Library/Android/sdk ./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`
- Gradle result: `BUILD SUCCESSFUL` (54 tasks executed)
- JVM tests: 158 tests, 0 failures, 0 errors, 0 skipped
- Lint: 0 errors, 18 warnings, 3 informational findings
- Debug APK: 60,886,928 bytes
- Debug APK SHA-256:
  `8e20f59718d2a45aaa13aafe58f5bea624b84f50b7bbe382580605acd46e9473`
- `git diff --check`: clean for the complete poll/persistence repair tree
  before commit
- Post-commit proof-asset placement: pass, with 0 candidate media files, 0
  required fixes, and 0 explicit exceptions

The unit lane includes a static guard over current source and build
configuration for obsolete confidential/callback auth paths. A dedicated UI
guard rejects an automatic authorization-screen launch and requires one
explicit copy-and-open callback, a visible code and paste instruction, the
Android 13+ sensitive clipboard marker, copy-before-open ordering, and the
unchanged verification URI. Lifecycle guards require the Activity resume hook
to wake only a current active authorization. Deterministic poller tests prove
an early resume cannot poll before GitHub's minimum interval, cannot bypass a
slowdown deadline, and reaches an authorized result promptly when resumed
after the deadline. The lane also includes device-poll timing, expiry,
cancellation, refresh, session-invalidation, lifecycle freshness, and
widget-label checks. A focused read-only follow-up review verified that
cancellation and sign-out cannot persist a late device-flow result or allow an
older flow to overwrite a newer one.

The poll/persistence regression lane fault-injects a transport `IOException`
and proves the app discards its message/cause, waits the required interval, can
later accept an authorized result, and stops retrying at local expiry. A failing
token store proves partial state is cleared and the fixed persistence message
contains neither injected storage detail nor token text. The static store guard
requires atomic finish/fail handling and rejects a redundant descriptor sync.

A maintainer first attempted the pre-UX-repair build at
`9da86ce0a970624af05feeab0f0cc1efe53a1cbe` on the registered Pixel 10 Pro
Fold. The code appeared only briefly before the screen automatically opened
GitHub; Android Back recovered the code, but RepoGlance offered no copy action.
The maintainer rejected and stopped the flow before entering or authorizing the
code, so no sign-in, session, or live-data proof was completed. That attempt is
defect evidence only.

The maintainer then exercised the corrected UX build at
`2a385e9cfc783c8d3215ad29c66a3aa04e6325df` on the Fold. RepoGlance kept the
code visible and provided the explicit copy/open action. GitHub accepted the
code and displayed its connected confirmation. After the maintainer returned,
RepoGlance remained on the active **waiting safely** device-code screen rather
than visibly completing the session. This proves the corrected handoff and
GitHub-side code acceptance, but it does not prove token receipt, encrypted
session persistence, catalog loading, or live data at that head. Source review
found no lifecycle cancellation; the resilience gap was the absence of an
Activity-resume wake-up for the ViewModel poll wait after background execution.
The current source repair adds that wake-up without creating another poll loop
or relaxing GitHub's interval. This source lane did not perform a device action,
sign in, rotate a token, deploy, push, comment, or merge. An authorized
exact-head Fold install/sign-in proof run must be performed again.

The maintainer next exercised the resume-wake build at
`b3d8b2f8aa903c1f0574ffb206f0f397b2a72502` on the Fold. In a source-blind
attempt, GitHub again accepted the device code for **RepoGlance by Saari** and
reached the exact `/login/device/success` confirmation. Returning to
`MainActivity` woke the poll and RepoGlance left the awaiting screen, but then
rendered **RepoGlance could not refresh**, **Could not start GitHub sign-in
right now**, and **Reconnect GitHub**. There was no saved session. This proves
the resume signal reached the authorization job and localizes the failure to a
poll/parse/persist step before catalog loading, but it does not reveal the exact
exception and no auth logs or credentials were inspected.

Source inspection found no unsupported Fold/API call: the device is above the
app's API 31 minimum, lint is clean of API errors, and Android Keystore
AES-256-GCM is supported. Two safe failure classes remain consistent with the
evidence: transient URL-connection I/O while polling, or an exception while
persisting the authorized token. The store contained a redundant raw
`FileDescriptor.sync()` immediately before Android's `AtomicFile.finishWrite()`,
which already performs sync/close/commit. The current repair removes that
extra failure point, retries only poll `IOException` at the existing bounded
schedule, and converts any token-store exception into a fixed sanitized
persistence failure after clearing partial state. It does not claim which
unobserved exception occurred on the Fold. An authorized exact-head rerun must
still prove token receipt, encrypted persistence, and live catalog loading.
After the source repair began, an operator-approved Codex browser action
changed only **Device Flow** for the RepoGlance GitHub App and verified GitHub's
successful-update confirmation and checked state. There was no credential/auth
log inspection and no sign-in/device action. A
release-signing App Link fingerprint is no longer required for authentication
because the current source has no App Link callback. The remaining proof gate
is an authorized live-device install/sign-in run bound to the repaired commit.

Before any public APK distribution, the maintainer should revoke or rotate the
GitHub App client secret that was injected into the earlier prototype APK. The
repaired source neither references nor accepts that credential, but removing
the source path does not revoke an already issued account credential. This is a
separate human/account gate; this repair did not inspect, rotate, or revoke it.
The historical callback origin and custom callback URL may also remain deployed
or configured externally. Device flow no longer uses either one, and this
repair makes no external-teardown claim.

## Historical predecessor proof (confidential web flow)

### Source identity

- Implementation commit: `9ee4e28ed301f624934df77bf36aa5307e29ce76`
- Pre-auth Fold/widget checkpoint: `b023d01304438449d82f3035fba270af299581ad`
- Stacked review base: `83228f9cb83814cfc4d0f22115a06e4594a01ef5`
  (`codex/slice2-glance-fixture-ui-20260729`, PR #4)
- Application: `co.saari.repoglance`, version code `3`, version name
  `0.3.0-auth-live`

The implementation adds user-present GitHub App authorization, rotating
Keystore-backed session custody, direct allowlisted GitHub reads, live
repository discovery, and repo-scoped open issue/PR lists. Widgets remain
fixture-backed in this checkpoint.

### Build and automated verification

Executed from the isolated owner worktree with the client-secret environment
explicitly absent:

```text
env -u REPOGLANCE_GITHUB_CLIENT_SECRET ./gradlew --no-daemon \
  :app:clean testDebugUnitTest assembleDebug lintDebug
```

Result:

- Gradle: `BUILD SUCCESSFUL`
- JVM tests: 107 tests, 0 failures/errors
- Lint: 0 errors, 15 warnings, 3 informational findings
- Secret-free debug APK: 60,854,460 bytes
- Secret-free debug APK SHA-256:
  `11769d83b5196cc9945fda1cff54d5f90a73a682fe05bfae40de5a84d9685751`
- Generated default `BuildConfig.GITHUB_APP_CLIENT_SECRET`: empty string
- `git diff --check`: clean before the implementation commit

The credential-bearing personal prototype APK was installed only on the
registered Pixel 10 Pro Fold. It was not hashed for publication, copied into
proof, staged, pushed, or released. The local build output was then cleaned and
replaced by the secret-free APK described above.

### Callback and Android App Link

- Secret-free static source: `callback-site/`
- Cloudflare Pages project: `repoglance-callback`
- Pages origin: `https://repoglance-callback.pages.dev`
- Custom host: `https://repoglance.ztoned.com`
- OAuth return: `https://repoglance.ztoned.com/oauth/callback`
- GitHub App: `https://github.com/apps/repoglance-by-saari`

The Pages project was created as a direct-upload project and deployed without
Functions, storage, analytics, or secrets. `ztoned.com` is Cloudflare-DNS
authoritative. Cloudflare created the `repoglance` record, activated the custom
domain, and reported SSL enabled. The mistakenly started, DNS-inactive
`repoglance.saari.co` Pages association was removed; no `saari.co` DNS record
was created or changed.

Acceptance checks:

- Authoritative and public recursive DNS returned Cloudflare addresses for
  `repoglance.ztoned.com`.
- `/.well-known/assetlinks.json`: direct HTTPS 200,
  `application/json`, no redirect.
- `/oauth/callback?code=sample&state=sample`: direct HTTPS 200,
  `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, restrictive CSP,
  no redirect.
- Unknown route: HTTPS 404.
- Android `pm get-app-links co.saari.repoglance` on the Fold reported
  `repoglance.ztoned.com: verified` for the prototype debug certificate.

The public prototype debug certificate fingerprint is tracked in
`callback-site/.well-known/assetlinks.json`. A differently signed release must
add its public certificate fingerprint before distribution.

### GitHub App configuration and secret custody

The maintainer approved and completed the public GitHub App registration and
configuration. The app is installed for all repositories on
`saariuslystoned`, `saari-co`, and `dinkuskit`. The callback is the verified
`ztoned.com` URL above. The configured read permissions are Metadata, Issues,
Pull requests, Checks, Commit statuses, and Contents. No write permission is
requested or implemented.

The GitHub App web flow requires a client secret even with PKCE. The Android
prototype therefore treats that value as public, revocable build configuration,
not as proof of binary identity. The final value was entered through a native
hidden macOS dialog, injected only into the one local Gradle process, and never
shown to the agent or recorded in source/proof. Two earlier unused values from
failed terminal handoffs were revoked. GitHub showed exactly one client secret
after cleanup.

### Fold live-data proof

On the registered Pixel 10 Pro Fold:

1. Installed the approved credential-bearing `0.3.0-auth-live` debug build.
2. Android verified `repoglance.ztoned.com` for the installed package.
3. Launched RepoGlance and confirmed an enabled **Connect GitHub** action.
4. The maintainer completed the GitHub authorization in the Custom Tab.
5. RepoGlance returned through the verified App Link and rendered `LIVE` for
   `@saariuslystoned`, with 63 accessible repositories and fresh rate-limit
   state.
6. Filtered to the public `saari-co/RepoGlance` repository and opened it in
   BOTH mode.
7. The app showed no matching open issues and live open PRs #2 and #4 plus
   draft PR #3, matching GitHub at the time of capture.

### Publication loop

- Pushed branch: `codex/github-app-auth-live-20260812`
- Draft PR: https://github.com/saari-co/RepoGlance/pull/5
- Review base: `codex/slice2-glance-fixture-ui-20260729` (PR #4)

After PR #5 was created, RepoGlance refreshed the live `saari-co/RepoGlance`
repository on the Fold. The app rendered `#5 Add configurable widgets, Fold
shell, and live GitHub data` with its Draft badge and fresh GitHub rate-limit
state. This proves the published PR round trip through RepoGlance's own live
read path.

No browser callback URL, authorization code, token, private repository
inventory, auth log, screenshot, or UI XML is committed in this packet.

### Deliberate limits and next gates at the historical source

The bullets in this subsection describe the predecessor commit only; the
current device-flow repair supersedes its client-credential and App-Link gates.

- Live data is currently app-only; widget content and background refresh remain
  fixture-backed/deferred.
- Issue and PR sections explicitly model a first-page limit and display when
  more rows are available.
- The APK-held client credential is extractable. Do not publish a
  credential-bearing build. General distribution requires GitHub device flow
  or a confidential exchange/refresh broker.
- Release/Play signing needs its own App Link fingerprint and signed-device
  proof.
- Merge, release, signing, distribution, future DNS/deploy changes, GitHub App
  configuration changes, sign-in, and token rotation remain maintainer-gated.
