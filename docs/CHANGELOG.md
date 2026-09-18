# Changelog

All notable changes to RepoGlance. Each `## X.Y.Z` section here is the exact
release-notes body used for that tag's GitHub Release — see
[RELEASING.md](RELEASING.md).

## 0.3.0

- Switch to semver tag-driven GitHub Releases (`vX.Y.Z`, `-beta.N`), retiring
  the rolling `canary` prerelease model. `versionName`/`versionCode` are now
  derived from the pushed git tag at build time instead of being
  hand-committed.
- Add `.github/workflows/release.yml`: builds a release APK and AAB on
  `v*` tag push, signs conditionally when `ANDROID_KEYSTORE_B64` is
  provisioned (falls back to an unsigned build otherwise), and publishes a
  GitHub Release with the APK, the AAB, and a `SHA256SUMS` checksum file.
  Play Store upload is stubbed (`play-internal-upload`, disabled) pending
  Play Console provisioning.
- Add `docs/RELEASING.md` documenting the tag convention, the versionCode
  formula, the cut procedure, and the signing model.
