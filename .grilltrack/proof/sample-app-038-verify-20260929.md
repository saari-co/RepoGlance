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

## After rebase onto #42 (targetSdk 36)

The branch was rebased onto `main` 103fc8dbc1a1da58afd8beb3a41f5e2f8fb0db78,
the #42 merge (compileSdk/targetSdk 36, AGP 8.10.1, UseKtx lint). The
rebase had no conflicts.

- **Gate:** `./gradlew assembleDebug assembleRelease check` BUILD
  SUCCESSFUL. Lint reports no new issues. There are 285 debug unit tests
  with 0 failures, and `aapt2` gives `targetSdkVersion '36'`.
- **Device:** the approved emulator (`EMULATOR37X1X11X0`, Android 16), no
  session. APK `b3b59f7a25b31cb52ea455af601312e20f2ac1924a4a4f571e25cc3fa5f26ab3`,
  `targetSdk=36`; `doctor` passed.
- **StrictMode:** counts are RepoGlance frames under
  `DiskReadViolation`/`DiskWriteViolation` after each step.

| Step | StrictMode | Observed |
| --- | --- | --- |
| Signed-out cold start | 0 | `repoglance:connect-github` 1, `repoglance:explore-sample` 1 |
| Tap Explore with sample data | 0 | `repoglance:sample-bar` 1, `@octodev · 7 repositories` 1, `repoglance:live` 0 |
| Open `acme/rocket` | — | sample bar present, 5 issue rows |
| Tap `#415` | — | toast queue `text=Sample item — not on GitHub`, top stays `co.saari.repoglance/.MainActivity` |
| BACK (predictive back at target 36) | — | returns to the sample catalog (`repoglance:refresh-repositories` present) |
| Cold start in sample | 0 | sample bar 1 |
| Sign in with GitHub (exit) | 0 | `repoglance:connect-github` 1, `Enter this code on GitHub` 0; capture `r-exit` `af8fd729b54414f43aa741ddba1c89ac1b39537de4a3e17e8081037f67a44b9d` |
| Cold start after exit | — | Connect screen, sample bar 0 |

## ClawSweeper review of 2a63d37: two P1 merge risks, both addressed

ClawSweeper reviewed head `2a63d378ff8199bbf6ccb888c40856b908fda7c2` (5/6,
no findings) and listed two maintainer-owned P1 merge risks.

### 1. Signed-in cold start not run on a device: now run

- **Device:** Pixel 10 Pro XL `63310DLCQ000RV`, Android 17. It holds the
  maintainer's GitHub session from the #42 sign-in. #43's rebased debug APK
  `b3b59f7a25b31cb52ea455af601312e20f2ac1924a4a4f571e25cc3fa5f26ab3` was
  installed over #42's build with `adb install -r`, which keeps the session;
  `doctor` passed.
- **Cold start:** force-stop, then `am start -a MAIN -c LAUNCHER`.
  - `repoglance:live` 1 and `repoglance:refresh-repositories` 1: the live
    catalog, so the real session wins.
  - `repoglance:sample-bar`, `repoglance:connect-github` and
    `repoglance:explore-sample` are all 0.
  - RepoGlance StrictMode disk frames: 0.
- **Sample prefs:** `shared_prefs/repoglance_sample.xml` exists from the
  earlier sample run and holds no keys (no stale flag).
- **Capture:** the catalog was filtered to `saari-co/RepoGlance`, the only
  repository-shaped label in the dump, then captured as
  `signedin-cold-filtered`,
  `d6df1601f2201eeeb33e2436d1886cab4390d27ae19906a2aa7583bf4bfeffea`.

### 2. Sample owner handles were real third-party accounts: owners changed

The maintainer decided on 2026-09-29 to use their own accounts
(`sample-mode-042`, which supersedes `sample-mode-037`).

- **Sample repositories:** `saari-co/rocket`, `saari-co/api-server`,
  `saari-co/mobile-app`, `saari-co/legacy-site`, `dinkuskit/infra`,
  `dinkuskit/design-system` and `saariuslystoned/dotfiles`.
- **Viewer:** `@saariuslystoned`.
- **Names checked:** `gh api repos/<name>` returns 404 for all seven
  (2026-09-29). The token belongs to the maintainer and can see their
  private repositories, so 404 means the name does not exist. No real
  repository shows made-up numbers.
- **Guard:** `SampleAccountTest` pins the exact list. A changed list fails
  until someone re-runs the 404 check. The test also checks the owner set
  and a denylist of the two public real repositories, `saari-co/RepoGlance`
  and `dinkuskit/blocks`.
- **Mutation:** renaming `saari-co/mobile-app` to `saari-co/RepoGlance`
  fails the guard. The exact-list assertion catches it before the denylist
  runs. The denylist is a written statement of intent that is redundant with
  the exact list, and it is trimmed to the two public real repositories so
  the test names no private repository.
- **Still third-party:** row authors come from the fixture cast (`octodev`,
  `mkraft`, `jrivera`, `tstone`), and some of those are real GitHub users.
  They appear only as authors of made-up rows. The maintainer decision
  covered owners only. *(Resolved by `sample-people-043` below: every
  sample author is now `saariuslystoned`.)*

Gate: `./gradlew assembleDebug check` BUILD SUCCESSFUL
(`runs/build-runs/sample-owners-check.log`); 286 debug unit tests, 0 failures
(SampleAccountTest 7/7).

Emulator run: approved emulator, Android 16, no session, APK
`2a1a957d1d67d0cefa90e941e22f9d358f4bad826463eabc8e476050445f5e9e`,
targetSdk 36, `doctor` passed.

| Step | StrictMode | Observed |
| --- | --- | --- |
| Signed-out cold start | 0 | Connect and Explore present |
| Enter sample | 0 | `@saariuslystoned · 7 repositories`, sample bar, no `repoglance:live`; labels `saari-co/rocket saari-co/api-server saari-co/mobile-app dinkuskit/infra`; no `saari-co/RepoGlance` |
| Owner filter | — | menu `All dinkuskit saari-co saariuslystoned`; `dinkuskit` leaves `dinkuskit/infra dinkuskit/design-system` |
| Open `saari-co/rocket` | — | sample bar, 5 issue rows |
| Tap a row | — | toast `Sample item — not on GitHub`, top stays `co.saari.repoglance/.MainActivity` |
| Cold start in sample | 0 | sample bar, `@saariuslystoned · 7 repositories` |
| Exit | 0 | Connect screen, no `Enter this code on GitHub` |

| Capture | SHA-256 |
| --- | --- |
| `o-sample-catalog` | `6d8363e488aa97c88536d27a81d8a0fce5fb172e00de2d03a9062d11c7c2dd6d` |
| `o-sample-repo` | `8608c5f65e7d7a912c7519dea86c964b6e729babad987554349fb3ebe84776bb` |


### Review 3 (tree a45756055555a2574c8ed0dc5361dbcf03439d99) and fixes

The independent reviewer found no P0 or P1, and the app change itself is
correct.

| # | Finding | Classification | Resolution |
| --- | --- | --- | --- |
| 1 | P2: row authors and assignees (`octodev`, `mkraft`, `jrivera`, `tstone`) are real third-party accounts | human_gate | Waiting on the maintainer |
| 2 | P2: in the ledger, `sample-app-038` still depends on the superseded `sample-mode-037`, and its choice text still says "real owners renamed" | required_fix (disclosure) | See the ledger notes below and the map |
| 3 | P3: the 037 re-lock reads as a maintainer decision | required_fix (disclosure) | See the ledger notes below |
| 4 | P3: the 404 check had no positive control, and the mutation claim credited the denylist | required_fix | Positive control recorded below; mutation wording corrected above |
| 5 | P3: plausible names under the maintainer's accounts could be created later | human_gate | Waiting on the maintainer, same question as 1 |
| 6 | P3: the denylist named six private repositories | required_fix | Trimmed to `saari-co/RepoGlance` and `dinkuskit/blocks` |
| 7 | P3: stale wording ("made-up account", the README's XL claim, the test name) | required_fix | Reworded; test renamed `sampleRepositoriesAreExactlyTheApprovedNames` |

**Positive control for the 404 check.** The token belongs to
`saariuslystoned` and lists private repositories under all three owners
(counts only): saari-co 42, dinkuskit 2, saariuslystoned 7. Across the 81
repositories those owners hold (the live catalog also shows 81), none of the
7 sample names appears. The control `saari-co/RepoGlance` appears once.

**Ledger notes.**
- **Dependency:** `sample-app-038` records `sample-mode-037` as its
  dependency. 037 is superseded by `sample-mode-042`, and the ledger tool
  cannot edit a dependency or a recorded choice. From the 042 supersession
  onward, 038 was re-implemented and re-verified under 042's owner rule
  (maintainer's own accounts, names that do not exist). Its choice text
  ("real owners renamed") describes the first implementation.
- **The 037 re-lock was mechanical.** After `reopen`, the tool refused a
  new proposal under the same ID. The `lock` that followed re-locked the old
  "fictional owners only" text, and the tool logged it as "user accepted the
  scoped decision". The maintainer never re-accepted that choice. The real
  decision is `sample-mode-042` (lock, then supersede 037), recorded next.
- **Timestamps:** the `confirm`, `implement` and `verify` events at
  22:29:23Z were recorded in one batch after the work they describe. The
  edits were 22:25–22:27Z and the emulator run 22:27–22:28Z.

### sample-people-043: authors are the maintainer's handle (maintainer "a a")

The maintainer answered review-3 findings 1 and 5:

- **(1a)** Every sample issue and PR is authored by `saariuslystoned`, with
  assignee `saariuslystoned` or none. `Fixtures.CAST` is private again and no
  longer feeds sample data. GitHub cannot request a review from a PR's own
  author, so no sample PR sets `reviewRequestedFromViewer`, and the
  `Review requested` chip no longer appears in sample mode. Drafts remain.
- **(2a)** The realistic repository names stay, and the collision risk is
  accepted. It is guarded by the pinned list plus the 404 and owner-list
  check whenever the list changes.

Guard: `SampleAccountTest.everySamplePersonIsTheMaintainersOwnHandle`.
Mutation: setting an issue author to `mkraft` fails it.

Gate: `./gradlew assembleDebug check` BUILD SUCCESSFUL
(`runs/build-runs/sample-people-check.log`); 287 debug unit tests, 0 failures
(SampleAccountTest 8/8).

Emulator run: approved emulator, Android 16, no session, APK
`ae004f5bebf89db1aa48c9513033317627a947aaa1ec13fc2741ccf938fad2bb`,
targetSdk 36, `doctor` passed.

- **Catalog:** `@saariuslystoned · 7 repositories` with the sample bar;
  RepoGlance StrictMode disk frames 0.
- **`saari-co/rocket`:** `by saariuslystoned` ×6, third-party authors 0,
  `Review requested` 0. After scrolling to the PRs: `Draft` 1,
  `Review requested` 0, third-party authors 0.
- **Exit:** the Connect screen, no code screen, StrictMode frames 0.
- **Capture:** `p-sample-repo`,
  `0a7fd48ab288416c3b213eb2e5358b05d266a3aacc44c4c16c4b32fbc72bce54`.

The `sample-app-038` slice now includes this change and was verified with it.

### Review 4 (tree dcc556bc14dbbf5bce4688a74392bdab50fb73b6): clean

The reviewer checked the delta from review 3 (the owner fixes, `sample-people-043`
and the review-3 disclosures). One P3 about proof wording was fixed. The
confirmation of tree `dcc556bc14dbbf5bce4688a74392bdab50fb73b6` is clean. Source identity: the diff
`git diff --no-color --full-index 103fc8dbc1a1da58afd8beb3a41f5e2f8fb0db78 dcc556bc14dbbf5bce4688a74392bdab50fb73b6` (20 files changed, 1866 insertions(+), 194 deletions(-))
has `sha256:f69204545c5fbf8f88733f27a8bfcf5475a7df2130f9ffa09a1c98eb08a1b10e`. It is recorded in the ledger for `sample-app-038` and
`sample-people-043`. Later edits to this file and the ledger are records only.
