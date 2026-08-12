# Authentication architecture

RepoGlance's personal prototype uses a GitHub App user access token. The app
starts a user-present authorization in an Android Custom Tab, binds the return
with a random state and PKCE S256 challenge, accepts the verified
`https://repoglance.ztoned.com/oauth/callback` App Link, and stores the
resulting rotating user and refresh tokens with an
Android Keystore-backed key. RepoGlance then reads GitHub directly. CP-1 and
SwarmPocket are not in the authentication or data path.

The live catalog/navigator slice needs only Metadata, Issues, and Pull requests
at read access. The registered prototype also preauthorizes read-only Checks,
Commit statuses, and Contents for the existing widget pressure fields, which
remain fixture-backed in this checkpoint. Its installation controls which
accounts and repositories are visible. RepoGlance does not request or
implement GitHub writes.

## Prototype client credential boundary

GitHub currently requires a GitHub App client secret for web-flow code
exchange and refresh, even when PKCE is used. An Android APK cannot keep an
embedded client credential confidential. For this prototype, the credential
is therefore treated as public, replaceable configuration and never as proof
that a request came from an authentic RepoGlance binary.

The credential is injected only through the transient
`REPOGLANCE_GITHUB_CLIENT_SECRET` process environment of an explicitly
approved local prototype build. It is not accepted from a Gradle property,
committed, logged, stored in a file, included in CI, or published as a release
artifact. Builds without it remain safely signed out. A credential-bearing APK
must not be described or distributed as having a confidential client secret.

Before general distribution, choose one of these explicit follow-ups:

1. Enable and implement GitHub's device flow so the public client does not
   carry a client secret, accepting the less seamless sign-in UX.
2. Revise the no-server boundary and put code exchange and refresh behind a
   narrowly scoped confidential authentication broker.

If a credential-bearing APK has ever been distributed outside the approved
prototype devices, rotate that client secret before further use.

## Static callback

`callback-site/` is a secret-free static return surface. It performs no token
exchange and stores no data. Android App Links should deliver the HTTPS return
directly to RepoGlance; the static page provides a narrow custom-URI fallback
for devices where association is not yet verified. The page forwards exactly
one state and exactly one code-or-error outcome and loads no third-party code.

The source is deployed by direct upload to the Cloudflare Pages project
`repoglance-callback` (`https://repoglance-callback.pages.dev`) and served at
`https://repoglance.ztoned.com`. The custom domain is active with SSL, and
Android reported the domain as verified for the prototype debug signing
certificate on the maintainer's Pixel 10 Pro Fold on 2026-08-12. Release/Play
signing requires adding its public certificate fingerprint before distribution.

Future production deploys, DNS changes, signing-fingerprint changes, GitHub App
configuration, sign-in, token rotation, and distribution remain
maintainer-gated.
