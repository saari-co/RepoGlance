# RepoGlance callback site

This directory is the static, secret-free return surface for RepoGlance's
GitHub authorization flow. It does not exchange codes, store tokens, relay API
traffic, run analytics, or load third-party assets.

The callback first relies on the verified HTTPS App Link. The current
`assetlinks.json` fingerprint is public certificate metadata read from the
prototype debug APK (not from a keystore). Add the release/Play signing
certificate before distributing a differently signed build. The local fallback
returns the same allowlisted OAuth parameters through the app's PKCE-protected
custom URI if Android has not verified the HTTPS association yet.

This source is deployed by direct upload to the Cloudflare Pages project
`repoglance-callback` at `https://repoglance-callback.pages.dev` and is served
at `https://repoglance.ztoned.com`. On 2026-08-12 the custom domain was active
with SSL, its callback and asset-link routes returned direct 200 responses,
unknown routes returned 404, and Android reported the domain verified for the
prototype debug build on the maintainer's Pixel 10 Pro Fold.

Deployments and signing-certificate changes are maintainer-gated. Never put a
GitHub client secret, token, keystore, or auth log in this directory.
