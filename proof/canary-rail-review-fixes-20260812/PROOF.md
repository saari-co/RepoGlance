# Canary Rail Review Repair — 2026-08-12

## Scope

This packet records the static and local validation for the PR #3 canary
workflow repair. It does **not** claim a GitHub Release, configured secret,
installed APK, cp-1 pull, phone banner, update, or live end-to-end run.

The repair addresses the independent review findings as one bounded rail:

1. A human-provisioned `canary` GitHub Actions environment supplies the
   keystore, alias, store password, and key password. Missing inputs stop the
   signing job before any release operation. The expected public signing
   certificate SHA-256 fingerprint is configured separately and must match.
2. Push is limited to `main`; manual dispatch jobs also require
   `refs/heads/main`. The publication job reads the live `main` ref before
   creating/publishing the immutable release and again immediately before
   promoting the rolling manifest. A queued obsolete run fails closed.
3. Each signed APK is named with the full source SHA and published with its
   manifest/checksums under `canary-<full-sha>`. The manifest binds source
   repository/ref/commit URL, workflow run, APK bytes, signing certificate,
   version, package, and the upstream build-artifact digest.
4. `build` has read-only contents access and checks out without persisted
   credentials. `sign` has no contents permission and alone receives signing
   inputs. `publish` has the only contents-write permission and receives no
   signing secret. All reusable Actions are pinned to full commit SHAs.
5. The rolling `canary` release never deletes or clobbers APKs. It uploads the
   uniquely named APK, per-SHA manifest, and per-SHA checksums before replacing
   the compatibility `version.json` pointer as the final mutation.

## Recoverability Boundary

GitHub Releases do not provide an atomic asset-replacement primitive. The
workflow therefore has exactly one destructive asset operation: the final
`version.json --clobber`. If that operation is interrupted, the exact APK and
manifest remain available in both the immutable per-SHA release and the
uniquely named rolling assets. A retry verifies existing bytes before it
resumes. A published immutable release is never repaired in place: missing or
mismatched bytes fail closed for human adjudication.

The stable `version.json` keys consumed by the cp-1 puller remain present:
`sha`, `apk`, `size`, `apk_sha256`, `built_at`, `package`, `version_code`, and
`version_name`. New provenance fields are additive. The rail keeps old APKs so
an old manifest always points to a complete old payload during promotion.

## Local Validation

- `python3 .github/tests/test_canary_workflow.py`
  - PASS — 5 static safety-contract tests.
- Python `yaml.BaseLoader` parse plus `bash -n` on every extracted `run` block
  - PASS — workflow parsed; all 6 shell blocks are syntactically valid.
- Ruby/Psych YAML parse
  - PASS.
- `go run github.com/rhysd/actionlint/cmd/actionlint@v1.7.7 -color=false .github/workflows/canary.yml`
  - PASS — no findings.
- `ANDROID_HOME=/Users/bobbybones/Library/Android/sdk ANDROID_SDK_ROOT=/Users/bobbybones/Library/Android/sdk ./gradlew --no-daemon clean testDebugUnitTest assembleDebug assembleRelease lintDebug`
  - PASS — 98 tasks; 110 tests across 12 suites; zero failures, errors,
    skips, or lint errors. Lint retained 13 pre-existing warnings and 2
    informational findings. The secret-free build produced the expected
    unsigned release APK (21,751,859 bytes).
- `git diff --check`
  - PASS.

The first Gradle invocation, without an explicit SDK environment, stopped
before dependency resolution because this isolated worktree intentionally has
no `local.properties`. The same command passed with the installed SDK path
provided only to the process; no local configuration file was created.

## Human Gates

- Configure and protect the `canary` GitHub Actions environment.
- Provision the four named signing secrets and the expected certificate
  fingerprint without exposing their values to a worker or log.
- Uninstall the old throwaway-debug-signed canary once before installing the
  first continuity-signed canary; subsequent updates must retain this key.
- Decide retention policy for accumulating rolling assets; safe cleanup is a
  separate, explicit, proof-backed action.
- Approve merge and allow the first main run to create releases.
- Deploy/update the cp-1 consumer and perform live release-to-phone continuity
  proof before claiming the delivery rail is operational.
