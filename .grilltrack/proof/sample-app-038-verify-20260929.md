# sample-app-038: verification (2026-09-29)

GrillTrack track `gt-20260728163459-227573`, decision `sample-app-038`. It
depends on `sample-mode-037` and `reviewer-access-036`. Map:
`.grilltrack/maps/sample-mode.md`. Branch `claude/grilltrack-036-sample-data`,
worktree `.claude/worktrees/sample-data-036`, based on `origin/main` 432be33.

## Confirmed scope (maintainer, 2026-09-29)

This cycle builds the in-app sample mode:

- an `Explore with sample data` entry;
- a fictional healthy account behind the real catalog, repository view and
  navigator, with no network or token-store access;
- separate sample pins and a provisional `SAMPLE` marker;
- a `Sign in with GitHub` exit that requests no code, and a sample-item note;
- persistence until sign-in;
- a verify-repoglance feature file.

Widgets, the tile, the marker's final look, the `FIXTURE PREVIEW` label and
the Play Console text are later nodes.

## One disclosed difference from the summary

The summary said a sample issue's detail view would replace `Open on GitHub`
with a note. The signed-in navigator has no detail view: a row tap goes
straight to GitHub (`LiveRepoGlanceScreen.kt` `LiveNavigator.open`). So in
sample mode, that tap shows the toast `Sample item — not on GitHub` instead
of opening GitHub. The intent is the same: sample rows never open GitHub.

## Build gate

```
./gradlew assembleDebug check          BUILD SUCCESSFUL (runs/build-runs/sample-check-3.log)
testDebugUnitTest                      284 tests, 0 failures
  SampleAccountTest                    5/5
  SampleModeGuardTest                  6/6
checkFeatureMap                        ok (9 features)
```

Detekt forced two structural changes; neither loosened the gate:

- **Complexity:** `LiveRepositoryHome` was split into `CatalogTitleRow`,
  `CatalogRateLimitLine`, `OwnerFilter`, `RepositoryCard` and
  `DisconnectDialog`. It no longer trips `CyclomaticComplexMethod`, so its
  baseline entry was deleted (`app/detekt-baseline-debug.xml`, 20 → 19 IDs).
- **Function count:** `RepoGlanceViewModel` holds `TooManyFunctions` at 19
  (the rule fires at 20). The sample API is a single `setSampleMode(active)`,
  and the single-use `persistLiveSnapshot` helper was inlined into its only
  caller.

Mutation checks. Each change was made, the guard ran, and the file was
restored:

| Mutation | Guard | Result |
| --- | --- | --- |
| Sample row tap no longer shows the note (falls through to `GitHubAppLauncher.open`) | `SampleModeGuardTest` | failed as expected |
| `octoco/blocks` renamed to the real `dinkuskit/blocks` | `SampleAccountTest` | failed as expected |
| Sample branch in `refreshSelectedRepository` disabled (content goes to the network) | `SampleModeGuardTest` | failed as expected |

## Device run

The Pixel 10 Pro XL was disconnected. The Pixel 10 Pro Fold was locked
(maintainer away) and had been in use by another agent. The maintainer asked
for an emulator: AVD `Pixel_10_Pro_Fold`, serial `EMULATOR37X1X11X0`, Android 16
(SDK 36), inner display, booted with `-no-snapshot-save`. It was added to
`devices.tsv` as maintainer-approved.

- The emulator had RepoGlance 0.2.0 (versionCode 2) from 2026-08-12. The
  debug build was installed over it with `adb install -r -d`, which keeps
  its data.
- `bin/verify-repoglance doctor` passed: the device APK
  `1b390bced19e96037a3efdaa40915c2bce2f7d3ba889f3cc6ea65b7b30bc2a76` equals
  the local build.
- The phone had no GitHub session.

| Step | Observed |
| --- | --- |
| Entry | `launch MIXED live`: `repoglance:connect-github` 1, `repoglance:explore-sample` 1, `repoglance:live` 0 |
| Enter sample | `repoglance:sample-bar`, `repoglance:sample`, `repoglance:sample-sign-in`, `SAMPLE`, `@octodev · 7 repositories`, `Made-up repositories, not your GitHub` all present; `repoglance:live`, `Account and access settings`, `Manage GitHub access` all absent; visible labels `acme/rocket`, `acme/api-server`, `acme/mobile-app`, `octoco/infra` |
| Owner filter | menu `All acme octoco octodev`; `octoco` leaves `octoco/infra`, `octoco/blocks` |
| Search `legacy` | `acme/legacy-site` · `Public · Archived` · `Push time unknown` |
| Pin | `Pin acme/mobile-app` moves it first, and the control reads `Unpin acme/mobile-app`. `shared_prefs/repoglance_sample.xml` holds `active=true` and `pins=acme/mobile-app`. `repoglance.xml` has no `live_pinned_repos` key. Unpin restores recent order. |
| Repository view | `acme/rocket`: `repoglance:live-home`, `repoglance:sample-bar`, `Issues`, `PRs`, `Updated just now`, rows `#415`–`#419`, PR `#412` with `Review requested` |
| Row tap | `dumpsys notification` Toast Queue: `TextToastRecord{… co.saari.repoglance … text=Sample item — not on GitHub …}`. Top resumed stays `co.saari.repoglance/.MainActivity`; 0 `com.github.android`/`customtabs` records. |
| Restart | force-stop + `launch MIXED live`: `repoglance:sample-bar` 1, `@octodev · 7 repositories` 1, `repoglance:connect-github` 0 |
| Exit | `tap repoglance:sample-sign-in`: `repoglance:connect-github` 1, `repoglance:explore-sample` 1, `Enter this code on GitHub` 0; `repoglance_sample.xml` emptied; force-stop + relaunch shows Connect again; logcat device-code lines 0 |
| Offline | airplane mode on: sample catalog, catalog refresh, `acme/api-server` and repository refresh all render (5 rows) with no `could not refresh`, `Loading live GitHub data` or `No current value`; exit to Connect; `cleanup` turned airplane mode off |

Captures (images stay in ignored `runs/verify-repoglance-runs/`):

| Capture | SHA-256 |
| --- | --- |
| `signin-entry` | `18711800389e9b320dced7be7a4c8db191a65d56f0ee3094a38947cbc0d9d2e8` |
| `sample-catalog` | `89734b6aca92d47735f30ed644eb379392d029eeff6ef81b50b10dd09b594677` |
| `sample-archived-unknown` | `c04f99958eeef79af431d93794a394c03964d8295ad4fbaf33bf4e979968f613` |
| `sample-pinned` | `3e378466ff38cb8b23511a24916714f07cbc224da77c2010e6921e479b753324` |
| `sample-repo` | `65d1da0cce622e5c624d3083ae24753dcf3cd6db30f136f0c96044f738b9e093` |
| `sample-exit` | `e741621d0730bd16cf9f624aad810dac152e4126e28299f25d1894d5e9ff39b9` |
| `sample-offline-repo` | `e970b5ae9c4c465b8d3abd90ef31f8024fb9a89c0d8673851147a60798ad90a7` |

## Fictional names

The GitHub API, queried 2026-09-29, returns 404 for all seven sample repositories:
`acme/rocket`, `acme/api-server`, `acme/mobile-app`, `acme/legacy-site`,
`octoco/infra`, `octoco/blocks` and `octodev/dotfiles`. The owner handles
`acme`, `octoco` and `octodev`, and the fixture authors, are existing GitHub
accounts: every plausible short name is taken. Sample rows carry no URL, and
a tap never opens GitHub.

## Not proven here

- **Physical phone:** covered by the Pixel 10 Pro XL run at the end of this
  file (Android 17, reviewed build). A signed-in cold start was not run on any
  device.
- **Fold two-pane and cover display:** the emulator ran on the inner display
  only.
- **Widgets and tile:** the widgets and the Quick Settings tile show no sample
  data, which is `sample-widgets-039`.
- **Marker look:** provisional (`sample-marker-040`).
- **`FIXTURE PREVIEW` label:** still on an unconfigured repo widget
  (`sample-widgets-039`).

## Review 1 (tree 04ecc1ebd79d298a2efcbcb46498a84ee03bffd4) and repair

Source identity: the diff `git diff --no-color --full-index
432be33df55d14c5f0d58f10c5e3fa35fc94d118 04ecc1ebd79d298a2efcbcb46498a84ee03bffd4`
(19 files, +1140 −192) has
`sha256:4ffd1bf51d91f1860a2d1b95949811ead907f10d4c6c4c9924ba0487f7649e59`.

**Ledger correction.** The ledger's review-1 event records
`sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
That is the SHA-256 of empty input and is not a valid identity: it was
computed against a mistyped base commit (`432be337f2ae…`, which does not
exist), so the diff was empty. The ledger is append-only, so that event
stays. The identity above is the correct one for review 1. The same mistyped
base SHA was in `.grilltrack/maps/sample-mode.md` and has been corrected
there.

An independent reviewer (fresh-context subagent, read-only) checked the staged
diff for standards and source intent. It found no P0 or P1.

| # | Finding | Classification | Repair |
| --- | --- | --- | --- |
| 1 | P2: sample prefs, and for a signed-out user `AppPrefs`, loaded on the main thread in composition | required_fix | Bootstrap now runs `AppPrefs.preload` and reads the sample flag on `sessionDispatcher` on every branch. Measured on the emulator before and after, below. |
| 2 | P3: a stale sample flag survives when a real session wins | required_fix (one line) | When bootstrap finds a session, a stored flag is cleared with `SampleModeStore.leave` |
| 3 | P3: entering sample keeps an open repository, and a repository outside the sample set rendered as `Success` with empty lists (unknown shown as zero) | required_fix | `setSampleMode` drops the open repository and any pending widget repository on each transition. `SampleAccount.content` returns `Failure("Not part of the sample account")` with an `UNKNOWN` rate limit for such a repository. |
| 4 | P3: the docs said "no real owner", but the owner handles are real GitHub accounts | required_fix (wording) | `docs/INVARIANTS.md` and `sample-mode.md` now say: none of the maintainer's accounts, and the repositories don't exist on GitHub. Whether `@octodev` is acceptable in Play screenshots is a human_gate for `play-app-access-041`. |
| 5 | P3: the emulator serial identifies the emulator build, not the AVD | defer (structure); wording fixed | The `devices.tsv` note says so and asks for one emulator at a time |
| 6 | P3: guard substrings could pass vacuously | required_fix | Every anchor goes through `section()`, which fails on a missing anchor; tag indices are asserted present; the forbidden list adds `signOut(`, `clearSavedSession` and `BackgroundRefresh`; new guards cover the preload, the stale-flag clear and the transition reset |
| 7 | P3: the widget gotcha claimed unobserved behaviour | required_fix (wording) | The recipe now says a widget placed in sample mode is not covered |

StrictMode `DiskReadViolation`/`DiskWriteViolation` lines, emulator, launcher
cold start (`am start -a MAIN -c LAUNCHER`):

| Path | Before (apk 1b390bce…) | After (apk 1dadf2a7…) |
| --- | --- | --- |
| Signed-out cold start | 0 | 0 |
| Tap Explore with sample data | 2 (`AppPrefs.catalogSort` via `LiveRepositoryHome`) | 0 |
| Cold start in sample mode | 2 (same stack) | 0 |
| Exit to Connect | — | 0 |
| Cold start after exit | — | 0 |

The signed-in cold start could not run: the emulator has no session, and
sign-in is the maintainer's. The preload runs before the session branch, so
that path now loads both prefs files off the main thread too. It is guarded
by `SampleModeGuardTest`, not by a device run.

Re-verification on the fixed build (apk
`1dadf2a772eaff984a5f3711cc893193fe22fc6b225a24aa1be7148563c2e7c7`; `doctor`
passed):

- **Catalog:** `repoglance:sample-bar` 1, `@octodev · 7 repositories` 1,
  `repoglance:live` 0.
- **`acme/rocket`:** 6 visible rows.
- **Row tap:** the toast queue shows `text=Sample item — not on GitHub`, and
  the top activity stays `co.saari.repoglance/.MainActivity`.
- **Restart in sample:** the catalog, not a repository (`repoglance:live-home`
  0).
- **Exit:** `repoglance:connect-github` 1 and `Enter this code on GitHub` 0.
  A restart after exit shows Connect.

The offline run above used the first build. The repair changed
`setSampleMode`: it resets navigation on a mode change, and a refresh inside
sample mode is not a change. The repair also added the failure branch for
repositories outside the sample set. Neither changes what the offline run
exercised: entry, catalog refresh, and opening and refreshing a sample
repository.

| Capture (fixed build) | SHA-256 |
| --- | --- |
| `fix-sample-catalog` | `a8d15163ed3455107f060015eba54d5912a5c0b5cc42ecc85cf2a210f1734fd4` |
| `fix-exit` | `2d4d0fc565f9e549734f18886dedd82da2fa6bdaad7387bba33da563dc22336e` |

Build gate after the repair: `./gradlew assembleDebug check` BUILD SUCCESSFUL
(`runs/build-runs/sample-check-5.log`). There are 285 debug unit tests with 0
failures, including SampleAccountTest 6/6 and SampleModeGuardTest 6/6.

## Review 2 (tree 26396c273ab752558c1c73ec1ca8492a998955f4): clean

Source identity: the diff `git diff --no-color --full-index 432be33df55d14c5f0d58f10c5e3fa35fc94d118 26396c273ab752558c1c73ec1ca8492a998955f4`
(20 files changed, 1351 insertions(+), 192 deletions(-)) has `sha256:68c1ca58c0ec5f056be479fd999233019ba462043f8ac643e54aeb3375dc62e7`. This is the identity
recorded in the ledger for review 2. It supersedes the invalid review-1 event
hash noted above.

- **First re-review** (tree f2db456f): the repairs are correct and there are
  no regressions. Five P3s remained.
- **P3s 1, 2 and 5 fixed** (tests and docs only):
  - the transition guard now also asserts the check comes before the new
    mode is stored;
  - the pin guard isolates the sample branch of `onTogglePin` and forbids
    `WidgetRefresh` and `AppPrefs.`;
  - the INVARIANTS row now records the 404 check as a dated observation, not
    a guard;
  - the proof sentence about the offline run is reworded.
- **Mutation checks for the new guards:**

  | Mutation | Result |
  | --- | --- |
  | Move `sampleMode.value = active` above the transition check | fails the guard |
  | Add a `WidgetRefresh` launch before `togglePin` in the sample branch | fails the guard |

- **Deferred P3s:**
  - "Made-up account" wording while `@octodev` is a real handle. This goes
    to `play-app-access-041`, together with the human gate on Play
    screenshots.
  - The invalid review-1 hash stays in the append-only ledger. This section
    and the review-2 identity supersede it.
- **Final confirmation** (tree 26396c27): **clean.** Nothing under
  `app/src/main` changed since f2db456f, and the debug APK is still
  `1dadf2a772eaff984a5f3711cc893193fe22fc6b225a24aa1be7148563c2e7c7`, the
  build verified on the emulator.
- **Gate:** `./gradlew assembleDebug check` BUILD SUCCESSFUL
  (`runs/build-runs/sample-check-6.log`); 285 debug unit tests, 0 failures
  (from the test-result XML).

Later edits to this proof file and to `.grilltrack/ledger.json` and
`events.jsonl` are records only. They do not change the reviewed source.

## Physical phone run (after review 2)

Device: Pixel 10 Pro XL `63310DLCQ000RV`, Android 17, dark theme. The phone
had no GitHub session: RepoGlance was first installed on it on 2026-09-29 and
never signed in.

- **Build:** the reviewed debug APK
  `1dadf2a772eaff984a5f3711cc893193fe22fc6b225a24aa1be7148563c2e7c7`
  (source tree 26396c27). `doctor` passed, and the device APK equals the local
  build.
- **Cold starts:** each one used `am start -a MAIN -c LAUNCHER`.
- **StrictMode:** counts are `DiskReadViolation`/`DiskWriteViolation` lines
  in logcat after each step.

| Step | StrictMode | Observed |
| --- | --- | --- |
| Signed-out cold start | 0 | `repoglance:connect-github` 1, `repoglance:explore-sample` 1, `repoglance:live` 0 |
| Tap Explore with sample data | 0 | `repoglance:sample-bar` 1, `@octodev · 7 repositories` 1, `repoglance:live` 0, `Account and access settings` 0; labels `acme/rocket acme/api-server acme/mobile-app octoco/infra octoco/blocks` |
| Open `acme/rocket` | — | sample bar present, issues `#415`–`#419`, `Updated just now`, PR `#412` `Review requested`, `#413` `Draft` |
| Tap `#415` | — | toast queue `text=Sample item — not on GitHub`; top `co.saari.repoglance/.MainActivity`; 0 `com.github.android`/`customtabs` records |
| Cold start in sample | 0 | sample bar 1, header 1, `repoglance:connect-github` 0 |
| Sign in with GitHub (exit) | 0 | `repoglance:connect-github` 1, `repoglance:explore-sample` 1, `Enter this code on GitHub` 0, sample bar 0 |
| Cold start after exit | 0 | Connect screen; logcat device-code lines 0 |

`bin/verify-repoglance cleanup` ran afterwards.

| Capture (XL) | SHA-256 |
| --- | --- |
| `xl-signin` | `55710ff9136f98cea26f4f049f8448c68f8467ac1323d192261e41def8419886` |
| `xl-sample-catalog` | `f8f6492aef10c357b3dcbf26563c62c8863624f27c16f0e2492ed9d4bf50a378` |
| `xl-sample-repo` | `4c8e5f912ef0a5095f5ad8748c6cf2735fac0c6ffe9931abb4016b63715c4918` |
| `xl-sample-exit` | `8fce558621f0f7fc7f17f8138848ec74756771c716faa2eedb807e67cc4cafa3` |
