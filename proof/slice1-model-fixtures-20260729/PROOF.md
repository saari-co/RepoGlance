# Slice 1 Proof — Snapshot Model, Fixture Corpus, Truth-Rule Tests

Date: 2026-07-29

Repo: `saari-co/RepoGlance` (public), branch `codex/slice1-model-fixtures-20260729`
Base: `origin/main` @ `7c9789e8f4996f40912dc94fc516baf92ab41306` (PR #1 merge)
Worktree: `~/Developer/side-quests/RepoGlance/worktrees/slice1-model-fixtures`
Implementation head this proof binds to: `6a3ed6028f7f450fcec4cb44cba8f6abc20eb0e5`

## Task

Slice 1 of the RepoBar Pixel companion plan (x-api#522), routed by the
maintainer's build-lane goal (implementation hold lifted for Slices 1–2 +
preview rail): Kotlin Android skeleton, the per-repo snapshot client model,
a seven-scenario fixture corpus with navigator lists, and unit tests that
lock the truth rules. No network stack.

## What Exists At The Bound Head

- Root-level Android project: AGP 8.7.3, Kotlin 2.0.21, Gradle 8.14.4
  wrapper, compileSdk/targetSdk 35, minSdk 31, JDK 17, Compose BOM
  2024.09.03 + Material 3 + Glance 1.1.1 declared. `applicationId
  co.saari.repoglance`.
- `model/`: `RepoSnapshot` (counts, `default_branch_ci`
  passing|failing|running|no_ci|unknown, `latest_release`, pushed age,
  `value_basis` exact|last_good|unknown, `observed_at`, rate-limit bucket),
  `RepoRef` (charset-validated), issue/PR rows, navigator scope
  (account|org|repo), mode (issues|prs|both), filters (Open / Mine /
  Mentions / Recently updated / Awaiting my review), `NavigatorList`
  (PAGE_SIZE 30, per-list value basis + observed_at). Truth invariants are
  enforced in model constructors: UNKNOWN basis forces null counts (unknown
  can never become zero); EXACT/LAST_GOOD require counts + observedAt.
  Sealed `NavigatorRows` makes "Awaiting my review is PR-only" a type-level
  rule.
- `render/`: `Ages` + `SnapshotRendering` — the display-rule single source
  of truth: `countText` renders "—" (never "0") without a basis; age chip
  non-null exactly for LAST_GOOD; rate-limit banner first-class for
  EXHAUSTED/LOW; `No CI` distinct from `Passing`; data-age label always
  derivable.
- `link/`: `Sanitize` (control-char strip + token/header-shape redaction)
  and `GitHubLinks` (the only deep-link builder; typed parts only, every
  output starts `https://github.com/`, no raw-URL input path exists).
- `fixtures/`: scenarios exact, last_good, unknown, rate_limited, no_ci,
  empty, mixed + navigator list states empty|loaded|paged|last_good per
  filter. Fixture text is clean prose; hostile test inputs are built by
  runtime concatenation only (no token-shaped literals anywhere in the
  repo — scanner-safe by construction).
- `.github/workflows/ci.yml`: `testDebugUnitTest assembleDebug` on PRs and
  on push to `main`/`codex/**`.
- No network dependency of any kind; the manifest declares zero
  permissions (no INTERNET) — the APK cannot perform network I/O.

## Verification

- CI run [30452997188](https://github.com/saari-co/RepoGlance/actions/runs/30452997188)
  on exactly `6a3ed6028f7f450fcec4cb44cba8f6abc20eb0e5`:
  `status completed / conclusion success`, `BUILD SUCCESSFUL` for
  `testDebugUnitTest` + `assembleDebug` (re-verified independently via
  `gh run view --json headSha,conclusion`).
- 62 unit tests across 8 test classes: model invariants, value-basis
  rendering rules, age formatting, CI-state labels, rate-limit banner,
  sanitization (runtime-built hostile shapes redacted; clean prose passes),
  deep-link safety (hostile owner/name/number constructions throw; all
  outputs origin-pinned), fixture integrity (scenario semantics + PAGE_SIZE
  + PR-only filter enforcement).
- `git diff --check origin/main..6a3ed60` clean except stock CRLF in the
  standard `gradlew.bat` (unmodified Gradle 8.14.4 wrapper file).
- Grep proof: no `okhttp|retrofit|ktor|INTERNET` outside the manifest
  comment that documents the permission's absence; no token-shaped or
  bearer-shaped literals.

## Gates Honored

- Build lane routed by the maintainer's explicit goal; scope held to
  Slice 1 (no widgets, no network, no auth, no GitHub App, no CI release
  lane, no device work).
- No secrets, keystores, `local.properties`, or APKs committed
  (`.gitignore` unweakened).
- Merge remains the maintainer's gate; no self-merge.
- `.grilltrack/` untouched (no decision in this slice implements
  `pin-model-010` or `delivery-rail-012`; `widget-content-priority-008`
  stays deferred for the five-variant grill).

## Next Route

Slice 2 (PR B): fixture-first Glance widgets + in-app fixture mode
(pin toggle per `pin-model-010`, navigator screens, filters, instant
cached search, github.com deep-link stubs), Fold cover/inner sizes, device
screenshot proof per house media policy.
