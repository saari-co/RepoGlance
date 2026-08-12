# Authentication architecture

RepoGlance is a public Android client of a GitHub App. It uses GitHub's OAuth
device authorization flow, which needs only the app's public client ID:

1. RepoGlance posts the client ID to GitHub's device-code endpoint.
2. The app keeps the returned user code visible and does not open a browser
   automatically. An explicit **Copy code & open GitHub** tap copies the code
   for pasting, marks it sensitive on supported Android versions, and opens the
   exact GitHub verification page in an Android Custom Tab. Returning from the
   tab leaves the code visible in RepoGlance.
3. A ViewModel-scoped coroutine waits for GitHub's minimum interval before
   each token request, adds GitHub's slowdown interval, and stops on success,
   expiry, local cancellation, or a terminal GitHub response.
4. The resulting GitHub App user token is encrypted with a non-exportable
   Android Keystore key and stored in the app's no-backup directory.

GitHub issues expiring user tokens by default. A device-flow token can be
refreshed using the public client ID and refresh token without a confidential
client credential. RepoGlance rotates both encrypted values before access-token
expiry. A missing, expired, revoked, or rejected session is cleared locally and
the UI returns to an explicit **Reconnect GitHub** state. A GitHub API 401 on
either repository-content section is promoted to that same session-level
invalidation instead of being left as a partial row error.

The implementation follows GitHub's current primary documentation for
[GitHub App user tokens](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-user-access-token-for-a-github-app)
and
[refresh token rotation](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/refreshing-user-access-tokens).

## Network and credential boundary

Authentication and data reads go directly from the APK to fixed HTTPS hosts at
`github.com` and `api.github.com`. Current source has no authentication
callback, Android App Link, static callback-site source, relay, token broker,
storage service, or analytics dependency. The previously deployed historical
callback may still exist externally, but the current app neither references nor
requires it. The repository accepts no secret-bearing build environment input
and generates no confidential auth field in `BuildConfig`.

The encrypted token payload format is bumped for this transition. An installed
prototype clears any legacy web-flow session locally and asks the user to
connect through device flow, ensuring later refreshes never depend on the
removed confidential flow.

The live catalog/navigator needs only Metadata, Issues, and Pull requests at
read access. Checks, Commit statuses, and Contents remain future read-only
inputs for widget pressure fields. The GitHub App installation controls which
accounts and repositories are visible. RepoGlance does not request or
implement GitHub writes.

## Human gate and proof status

An operator-approved Codex browser action changed only **Device Flow** in the
RepoGlance GitHub App settings on 2026-08-12 and verified GitHub's
successful-update confirmation and checked state. It did not inspect a
credential or auth log, sign in, or use a device. The source repair did not
rotate a live token, deploy, or release.
No release-signing App Link fingerprint is needed because device flow has no
Android App Link callback.

The earlier web-flow checkpoint has source-bound Fold proof. That proof does
not transfer to the current device-flow source. A physical attempt on the
pre-UX-repair device-flow head was stopped and rejected before authorization:
the verification page opened automatically after the code appeared briefly,
and the code had no copy action. That attempt is defect evidence, not auth or
live-data proof. The current repair is supported by local unit/build/lint and
static public-client/explicit-action checks only until an authorized exact-head
Fold install/sign-in proof lane reruns.
