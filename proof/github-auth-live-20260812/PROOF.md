# GitHub App authentication and live-data proof — 2026-08-12

## Source identity

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

## Build and automated verification

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

## Callback and Android App Link

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

## GitHub App configuration and secret custody

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

## Fold live-data proof

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

## Publication loop

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

## Deliberate limits and next gates

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
