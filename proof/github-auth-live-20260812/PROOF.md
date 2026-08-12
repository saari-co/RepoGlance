# GitHub App authentication and live-data proof — 2026-08-12

## Current public-device-flow repair

The current PR repair replaces the source-bound confidential web flow below
with GitHub's public device flow. The APK now carries only the public client ID,
shows the user code before opening GitHub's verification page in a Custom Tab,
polls in ViewModel scope at GitHub's required interval, honors slowdown,
expiry, and cancellation, and rotates expiring access/refresh tokens without a
confidential client credential. The authentication callback/App-Link source was
removed because device flow does not return through the app.

The repair also promotes repository-content 401 responses to local session
invalidation and **Reconnect GitHub**, updates visible age labels once per
minute only while the UI lifecycle is resumed, refreshes them immediately on
resume, and marks every persistent fixture widget surface `FIXTURE PREVIEW`.

### Current repair source and local validation

- Repair implementation commit:
  `9a159f6986647350fac2a1b9e4200459ae277782`
- Repair starting head: `fbb78927d3579294c0652f41e6f39b02825fa2a5`
- Stacked review base: `5c0a0d660b5332f40b3ccb1fbf21dd63e755f3ef`
- Secret-free clean command:
  `ANDROID_HOME=/Users/bobbybones/Library/Android/sdk ./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`
- Gradle result: `BUILD SUCCESSFUL` (54 tasks executed)
- JVM tests: 146 tests, 0 failures, 0 errors, 0 skipped
- Lint: 0 errors, 18 warnings, 3 informational findings
- Debug APK: 60,886,928 bytes
- Debug APK SHA-256:
  `fcd6c03e945ba6b42cc515cf484d98f219e8d76e21ddc724803bdcb8f1ea966e`
- `git diff --check`: clean before the implementation commit
- Proof-asset placement at the exact implementation commit: pass, with 0
  candidate media files, 0 required fixes, and 0 explicit exceptions

The unit lane includes a static guard over current source and build
configuration for obsolete confidential/callback auth paths. It also includes
deterministic device-poll timing, slowdown, expiry, cancellation, refresh,
session-invalidation, lifecycle freshness, and widget-label checks. A focused
read-only follow-up review verified that cancellation and sign-out cannot
persist a late device-flow result or allow an older flow to overwrite a newer
one.

This repair did not sign in, rotate a live token, use a device, deploy, push,
comment, or merge. Therefore the Fold evidence below remains valid only for its
exact historical source; it is not device proof for the current device-flow
source.
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
