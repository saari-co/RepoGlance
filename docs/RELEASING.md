# Releasing RepoGlance

RepoGlance ships from annotated `v*` git tags, not from every push to `main`.
Pushing a tag triggers `.github/workflows/release.yml`, which builds a
release APK and AAB, cuts a GitHub Release, and attaches artifacts and
checksums. This replaces the earlier rolling `canary` model (a per-push
prerelease off `main`).

## Tag convention

- Stable: `vMAJOR.MINOR.PATCH`, e.g. `v0.3.0`, `v1.2.4`.
- Beta: `vMAJOR.MINOR.PATCH-beta.N`, e.g. `v0.3.0-beta.1`.
- No other tag shapes are supported. `app/build.gradle.kts` fails the build
  with a clear message if a `v*` tag doesn't match one of these two forms.

## versionCode formula

`app/build.gradle.kts` derives `versionName` and `versionCode` from the tag
at configure time — the tag is the single source of truth, no version number
is hand-committed:

```
versionName = tag with the leading "v" stripped
versionCode = MAJOR*1_000_000 + MINOR*10_000 + PATCH*100 + preCode
  preCode = 99 for a stable release
  preCode = N  for a "-beta.N" prerelease
```

Caps (the build fails with a clear message if violated):

- `MINOR < 100`
- `PATCH < 100`
- beta `N` in `1..98`

This keeps a beta's versionCode sorted below its matching stable release,
and versionCode always climbing release to release. Worked examples:

| Tag              | versionName    | versionCode | Arithmetic                              |
| ---------------- | -------------- | ----------- | ---------------------------------------- |
| `v0.3.0-beta.1`  | `0.3.0-beta.1` | `30001`     | `0*1_000_000 + 3*10_000 + 0*100 + 1`     |
| `v0.3.0`         | `0.3.0`        | `30099`     | `0*1_000_000 + 3*10_000 + 0*100 + 99`    |
| `v0.3.1`         | `0.3.1`        | `30199`     | `0*1_000_000 + 3*10_000 + 1*100 + 99`    |
| `v0.4.0`         | `0.4.0`        | `40099`     | `0*1_000_000 + 4*10_000 + 0*100 + 99`    |

A local build off a non-tag commit (no `GITHUB_REF_NAME` starting with `v`,
no exact-match `git describe --tags`) never fails: it falls back to
`versionName = "0.0.0-dev"`, `versionCode = 1`.

Verify the computed version at any time with:

```
./gradlew -q printVersion
```

## Cut procedure

1. Land the changes on `main`, including a `## X.Y.Z` section at the top of
   `docs/CHANGELOG.md` describing what's in the release.
2. From an up-to-date `main`, tag and push:

   ```
   git tag -a v0.3.0 -m "RepoGlance 0.3.0"
   git push origin v0.3.0
   ```

   Use `-beta.N` for a prerelease tag, e.g. `git tag -a v0.3.0-beta.1 -m "RepoGlance 0.3.0-beta.1"`.
3. `release.yml` runs on the tag push: it builds the release APK and AAB,
   creates the GitHub Release titled `RepoGlance <versionName>` (prerelease
   iff the tag contains `-beta`), and attaches:
   - `RepoGlance-<versionName>.apk`
   - `RepoGlance-<versionName>.aab`
   - `SHA256SUMS` (checksums for both of the above)
4. Release body: the `## <versionName>` section of `docs/CHANGELOG.md`
   verbatim if that heading exists, otherwise `gh release create
   --generate-notes`.
5. Before announcing a release, verify:
   - the tag exists on `main`'s history (`git log --oneline main | grep <sha>`,
     or check the release's target commit on GitHub),
   - the APK, the AAB, and `SHA256SUMS` are all attached,
   - the release notes match the intended `docs/CHANGELOG.md` section (or
     the generated notes, if there was no matching section),
   - the release body correctly says **unsigned** if it does — see below.

## Signing model

Releases are signed with a dedicated upload keystore, **not** a debug
keystore:

- The keystore file lives in 1Password. CI never sees the file directly —
  it's stored **base64-encoded** as the repo secret `ANDROID_KEYSTORE_B64`,
  alongside `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and
  `ANDROID_KEY_PASSWORD`.
- `release.yml` decodes `ANDROID_KEYSTORE_B64` to a temp file
  (`$RUNNER_TEMP/repoglance-release.jks`) only for the duration of the build,
  then deletes it in an `if: always()` step.
- `app/build.gradle.kts` wires a `release` `signingConfig` only when that
  temp file exists (`ANDROID_KEYSTORE_PATH` env var points at a real file).
  If the keystore secret isn't provisioned yet, the build falls back to
  Gradle's default **unsigned** release output — CI does not hard-fail, and
  the GitHub Release body is marked unsigned.
- Signing secrets are never printed, logged, or committed. Provisioning them
  is a Bobby-gated step (see Follow-ups below); this doc does not contain
  key material.

## Play Store stage (TODO, not wired)

`release.yml` has a `play-internal-upload` job that is intentionally
disabled (`if: false`) — it's a stub, not a live upload path. To enable it:

1. Provision secret `PLAY_SERVICE_ACCOUNT_JSON` — a Play Console
   service-account JSON key scoped to release management for
   `co.saari.repoglance`.
2. Create the Play Console app entry for `co.saari.repoglance` and complete
   the mandatory first manual upload/store listing (the Play Developer API
   refuses uploads to an app with zero prior releases).
3. Configure an `internal` testing track on that app.
4. Wire an upload step (e.g. `r0adkll/upload-google-play`) that takes the
   AAB built by `build-and-release` and uploads it to `track: internal`
   using `PLAY_SERVICE_ACCOUNT_JSON`.
5. Flip the job's `if: false` guard once 1-3 are done and reviewed.

## Retired: rolling canary

The previous delivery model (`codex/canary-rail-20260729`, RepoGlance PR #3)
built and signed a prerelease APK on every push to `main`, keyed off
`github.run_number`. That branch never merged to `main`; `canary.yml` was
never present there. This tag-driven model replaces that design outright —
see the PR that introduced this document for the full rationale.
