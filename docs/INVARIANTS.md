# Invariants

RepoGlance enforces its rules in CI, not in prose. This file records each
invariant, the guard that enforces it, and the enforcement layer, following
the ordering the maintainers adopted from Lauren Tan (@poteto): architecture
first, then a compiler or CI check, and a written rule only where no harder
boundary fits. Every time a reviewer wants to say "don't do X" on a pull
request, X belongs in this table with a guard, or with an honest `unguarded`.

Enforcement layers:

| Layer | Meaning |
| --- | --- |
| 1 | Compiler diagnostics and type system (`allWarningsAsErrors`, StrictMode at runtime) |
| 2 | Static analysis in CI (Android Lint, detekt, tests that assert structure) |
| 3 | Written rule in `AGENTS.md` or this file, which an agent can forget |
| 4 | A human review comment, which is the anti-pattern this file exists to retire |

## The floor (GrillTrack `ci-floor-014`, 2026-09-18)

`./gradlew check` is the merge floor and CI runs it on every pull request:

- **Android Lint** runs with `abortOnError` and `warningsAsErrors`. The
  warnings that existed when the gate landed are frozen in
  `app/lint-baseline.xml`; that file only ever shrinks. `ContentDescription`
  is promoted to error outside the baseline. Honest scope: that lint check
  covers View and XML surfaces only. A Compose `contentDescription = null`
  is invisible to it, which the deliberate-violation probe confirmed. The
  Compose guard is the Accessibility Test Framework check in the emulator
  verifier tier (Grill C), not lint.
- **detekt** (`detektDebug`, with type resolution) runs the default rule set,
  the Compose rule set, and the ktlint formatting wrapper against the debug
  variant's sources (`app/src/main` and `app/src/debug`). `maxIssues` is
  zero. The complexity and size findings that existed when the gate landed
  are frozen in `app/detekt-baseline-debug.xml`, which only ever shrinks, exactly
  like the lint baseline. The plain `detekt` task is disabled so there is one
  detekt gate, not two with different rule coverage.
- **detekt configuration decisions** (`config/detekt/detekt.yml`):
  `FunctionNaming`, `LongMethod`, `LongParameterList`, and `TooManyFunctions`
  ignore `@Composable` functions, which are PascalCase and parameter-heavy by
  Compose convention. `ReturnCount` allows three returns and excludes guard
  clauses. `InjectDispatcher` is off: RepoGlance has no dependency-injection
  framework and uses `Dispatchers.IO` directly by design; if one is adopted,
  re-enable it. `MagicNumber` is off.
- **Kotlin** compiles with `allWarningsAsErrors`.
- **Comments are banned in `app/src/main`** (`ForbiddenComment`, any
  comment). Rationale lives here, in tests, or in commit and PR history. The
  build script keeps its comments: it is outside the ban and its version
  derivation notes are load-bearing.
- **`android.util.Log` and `println` are banned** outside the single
  permitted file `app/src/main/java/co/saari/repoglance/log/RedactingLog.kt`,
  which does not exist yet: RepoGlance currently logs nothing, and the first
  logger must be built there so header redaction has one home.
- **`java.util.ArrayList` / `java.util.HashMap` imports are banned** in
  `app/src/main`; model rows expose immutable views only.
- **StrictMode** is installed in debug builds by `DebugHooks`: network on the
  main thread kills the process, disk on the main thread is logged, and the
  release flavour of `DebugHooks` is a no-op.

## Guard table

Seeded from the accepted review findings on RepoGlance pull requests #2, #4,
#5, and #12. `guard` names the thing that fails when the invariant is broken.

| Invariant | Origin | Guard | Layer |
| --- | --- | --- | --- |
| Unknown never renders as zero; a failed or truncated fetch ages the previous value as `LAST_GOOD` or renders `—` | `AGENTS.md` truth rules 2-3; PR #12 | `RepoSnapshotInvariantsTest`, `LiveSnapshotFactoryTest`, `CompactFreshnessUnitTest`, `LedgerRowUnitTest` | 2 |
| Every surface shows its data age; last-good never reads as current | truth rule 3; ClawSweeper P1 on PR #12 | `CompactFreshnessUnitTest`, `AgesTest`, `FreshnessClockTest`, `ValueBasisRenderingTest` | 2 |
| Model rows and snapshot maps expose no mutable aliases | PR #2 review, three repair cycles | `RowImmutabilityTest`, `NavigatorListInvariantsTest`, `ForbiddenImport` on `java.util.ArrayList`/`HashMap` | 2 |
| `UNKNOWN` snapshots carry no known metadata | PR #2 review | `RepoSnapshot` constructor `require` checks + `RepoSnapshotInvariantsTest` | 1 (runtime) + 2 |
| Display strings are sanitised for Unicode control and format characters | PR #2 review | `SanitizeTest`; every widget/navigator text passes through `Sanitize.displayText` | 2, call-site discipline unguarded |
| Authorization headers never reach a log path | PR #2 review | `ForbiddenImport` on `android.util.Log`, `ForbiddenMethodCall` on `println`; `GitHubApiClientTest` redaction cases | 2 |
| No disk or network I/O on the main thread | PR #12 review (`LiveSnapshotStore.load`) | StrictMode in debug builds; verifier tiers fail on `StrictMode` log lines (Grill C) | 1 (runtime) |
| Network requests wait for session and request-generation checks | PR #12 review | `RepoGlanceViewModel` ordering; unguarded by test, tracked | 3 |
| Repository metadata is requested only when it can complete an exact count | ClawSweeper P2 on PR #12 | `LiveSnapshotFactoryTest` (`needsRepositoryMetadata` cases) | 2 |
| Interactive elements carry a content description | Play tracker #7 | Android Lint `ContentDescription` at error for View/XML; **unguarded for Compose** until the Grill C emulator tier runs `enableAccessibilityChecks()` | 2 (partial) |
| Release builds contain no debug tooling | `AGENTS.md`; PR #12 picker | debug source set only (`app/src/debug`); no lint guard yet | 3 |
| Screen-level composables expose a `modifier` parameter | Compose rules `ModifierMissing` | six existing screens are frozen in `app/detekt-baseline-debug.xml`; any new emitting composable without a modifier fails detekt | 2 (ratchet) |
| Cover-display app shell must not overlap the status bar | Fold proof run 2026-08-11 | unguarded until the device verifier tier (Grill C) captures the `CLOSED` posture | 4 |

## Migrated rationale

The comments below were removed from `app/src/main` when the comment ban
landed. They are kept verbatim by file so no reasoning was lost; anything
still load-bearing should be promoted into the guard table or a test.


### `co/saari/repoglance/MainActivity.kt`

- Publishes Compose testTag values as accessibility resource ids so

- source-blind on-device validators can address controls unambiguously.


### `co/saari/repoglance/RepoGlanceViewModel.kt`

- Publishes the compact widget's counts. Runs only after a successful
  content load, so a failed refresh leaves the previous stored snapshot
  alone to age honestly rather than overwriting it with nothing.

- Only worth a request when it can produce an exact count: the

- issues page is truncated AND the PR page is whole. Otherwise the

- factory must fall back regardless, and the call is wasted quota.

- When the data was observed, not when it was written to disk.


### `co/saari/repoglance/auth/GitHubDeviceFlowClient.kt`

- A bounded transport failure remains pending. The next request

- still waits for GitHub's current interval and local expiry.

- Preserve the fixed persistence failure below; never reflect

- storage exception details into the UI or auth state.


### `co/saari/repoglance/auth/SecureTokenStore.kt`

- v2 deliberately invalidates v1 web-flow sessions: only tokens minted

- by device flow are eligible for secret-free refresh.


### `co/saari/repoglance/data/GitHubApiClient.kt`

- Repository-level counters. One core-quota call; the response's
  `open_issues_count` includes pull requests, which
  [LiveSnapshotFactory] corrects for.


### `co/saari/repoglance/data/LiveGitHubModels.kt`

- Repository-level counters from `GET /repos/{owner}/{name}`.
  
  [openIssuesAndPullRequests] mirrors GitHub's `open_issues_count`, which
  counts pull requests as issues. Subtract the exact open-PR count to recover
  true open issues; see [co.saari.repoglance.data.LiveSnapshotFactory].


### `co/saari/repoglance/data/LiveSnapshotFactory.kt`

- Builds a [RepoSnapshot] from live GitHub responses.
  
  Truth rules this enforces, in order:
  
  1. An open-PR count is exact only when the open-PR page was not truncated.
     A truncated page is a lower bound, never a count.
  2. `GET /repos/{owner}/{name}` reports `open_issues_count` as issues **plus**
     pull requests — GitHub's long-standing quirk — so true open issues are
     `open_issues_count - openPrs`. That subtraction is only sound when
     `openPrs` is itself exact, which is why rule 1 comes first.
  3. When the current fetch cannot produce exact counts, fall back to the
     previous snapshot as [ValueBasis.LAST_GOOD] so the widget ages an honest
     number instead of inventing one.
  4. With no exact fetch and no previous value, the basis is
     [ValueBasis.UNKNOWN] and every count renders as "—", never as zero.
  
  Default-branch CI and latest release are not fetched by [GitHubApiClient];
  they stay absent rather than being guessed.

- Whether a repository-metadata request can change the outcome. Every
  exact count derives from a whole open-PR page, so when that page is
  missing or truncated the metadata counter would be fetched and then
  discarded, spending core quota for a result that must fall back to
  LAST_GOOD or UNKNOWN anyway. An untruncated issues page is already
  exact on its own and needs no counter either.

- Age the previous observation rather than publish a bound as a count.

- Counts that survive every truth rule, or null when any of them fails.

- Every count is derived from the open-PR page, so it must be whole.

- Prefers the page already in hand: an untruncated issues page is
  exact on its own, because [GitHubApiClient] strips pull requests out
  of `/issues` before it ever gets here. Only when that page was
  truncated does the repository counter earn its keep, and then it has
  to have pull requests subtracted back out.


### `co/saari/repoglance/fixtures/Fixtures.kt`

- Public-neutral fixture corpus: real public repos ([saari-co/RepoGlance]
  and [dinkuskit/blocks]) plus clearly fictional ones (acme/rocket,
  acme/api-server, octoco/infra). Nothing here claims real data about a
  real third-party repo — the fictional repos exist purely to exercise
  scenarios the real ones don't need to demonstrate. All ages are computed
  relative to the `now` passed in, so fixtures stay deterministic across
  time in golden-style tests.

- ---- snapshots -----------------------------------------------------

- Legitimate zero: 0 open PRs under EXACT basis is a real, known

- value — distinct from UNKNOWN's null/"—" rendering.

- Dogfood default: one repo from each other scenario, so a single

- MIXED corpus demonstrates every truth-rule state side by side.

- ---- navigator lists ------------------------------------------------

- Builds one [NavigatorList] for the given [scope]/[mode]/[filter]
  combination. [mode] must be [NavigatorMode.ISSUES] or
  [NavigatorMode.PRS] — a single [NavigatorList] cannot hold both row
  types (see [NavigatorRows]); a caller wanting [NavigatorMode.BOTH]
  calls this twice and holds the two lists side by side.


### `co/saari/repoglance/link/GitHubAppLauncher.kt`

- Explicit GitHub-app routing for navigator detail. Adjacent launch is a
  best-effort Android request, not a guarantee: the system decides whether
  the current posture and task state can form a split. Browser fallback is
  intentionally not chosen in this slice.


### `co/saari/repoglance/link/GitHubLinks.kt`

- The ONLY place app deep links are built. Every function builds strictly
  from typed parts ([RepoRef], validated numbers, tags) and every output
  starts with "https://github.com/" — no function here accepts a raw URL
  string, so there is no way to produce a non-github.com URL from this
  object.


### `co/saari/repoglance/link/Sanitize.kt`

- Text-safety layer for anything sourced from GitHub (titles, labels, repo
  names, tags, ...) before it is ever shown on screen.
  
  Pipeline: replace Unicode control characters with spaces, strip Unicode
  format controls, collapse ASCII and Unicode separator whitespace runs to a
  single space (and trim), then redact any token-shaped or bearer-shaped
  substring with "•••". The redaction patterns below are regex *shapes*, not
  secret literals — no token-shaped string literal appears anywhere in this
  file or its tests; hostile inputs are built at test time by runtime
  concatenation only.

- Classic GitHub PAT prefixes: ghp_/gho_/ghu_/ghs_/ghr_ + 20+ alnum.

- Fine-grained PAT.

- Scheme + credential authorization headers (Bearer, Basic, Token, ...).

- "Authorization: <anything non-whitespace>", case-insensitive.

- "Bearer <token>", case-insensitive.


### `co/saari/repoglance/model/CiState.kt`

- Default-branch CI state for a repo. [NO_CI] is distinct from [PASSING]:
  a repo with no workflows at all is never rendered as "passing" — that
  would be unknown-as-zero in disguise. [UNKNOWN] is for "we could not
  determine CI state", distinct from both.


### `co/saari/repoglance/model/IssueRow.kt`

- "open" | "closed"


### `co/saari/repoglance/model/NavigatorFilter.kt`

- [AWAITING_MY_REVIEW] is valid only for PR lists — see [NavigatorList]'s
  init invariant, which enforces this at construction time.


### `co/saari/repoglance/model/NavigatorList.kt`

- Row payload for a [NavigatorList].
  
  Shape decision (Slice 1): a navigator list holds EITHER issue rows OR PR
  rows, never a mix — every call site is already scoped to one leaf
  [NavigatorMode] (ISSUES or PRS). [NavigatorMode.BOTH] is expressed by the
  caller holding two separate [NavigatorList] instances side by side (one
  `Issues`, one `Prs`), not by a single list carrying both row types. This
  keeps row rendering monomorphic and makes the "AWAITING_MY_REVIEW is
  PR-only" rule a type-level invariant enforced in [NavigatorList]'s init,
  rather than a runtime field check that could silently pass an empty list.

- One page (or accumulated pages) of navigator rows for a single
  [NavigatorFilter]. Rate-limit state lives here because account and org
  navigator lists do not necessarily have an enclosing [RepoSnapshot].
  `pageSize` is fixed at [PAGE_SIZE]; pagination beyond that is expressed by
  `hasMorePages`, not by a variable page size.


### `co/saari/repoglance/model/PrRow.kt`

- "open" | "closed"


### `co/saari/repoglance/model/RateLimitBucket.kt`

- GitHub API rate-limit headroom, as a first-class state rather than a
  silent failure mode. [EXHAUSTED] must always surface visibly; back off,
  never silently keep serving old numbers as if they were fresh.


### `co/saari/repoglance/model/RepoRef.kt`

- A typed, validated `owner/name` GitHub repo reference. Construction
  throws [IllegalArgumentException] for anything outside GitHub's charset —
  this is the one place that charset is enforced, so every downstream
  consumer (deep links included) can trust `owner` and `name` are safe to
  splice into a URL path segment.

- Simplified from GitHub's real owner rule (no leading/trailing/double

- hyphen enforcement) per the product spec: charset + length, plus an

- explicit no-leading-hyphen check.


### `co/saari/repoglance/model/RepoSnapshot.kt`

- A single repo's at-a-glance state.
  
  Truth-rule invariants (enforced here, not left to the UI layer):
  - `valueBasis == UNKNOWN` forces counts and repo observation metadata to
    unknown values — we know nothing, so nothing renders as current or as
    zero. Rate-limit state remains independent and may still be known.
  - `valueBasis` in {EXACT, LAST_GOOD} requires all three counts non-null,
    non-negative, AND `observedAt` non-null — a value with a basis always
    carries the moment it was observed. Awaiting-review PRs cannot exceed
    the open-PR total.
  - Navigator maps and row collections are defensively snapshotted before
    validation, so caller-owned mutable aliases cannot invalidate truth.


### `co/saari/repoglance/model/ValueBasis.kt`

- How current a numeric value is. [EXACT] is a fresh fetch. [LAST_GOOD] is
  an honestly aged value from a previous successful fetch (paired with a
  non-null `observedAt`). [UNKNOWN] means we have no value at all — never
  rendered as zero.


### `co/saari/repoglance/render/Ages.kt`

- Single source of truth for rendering an age (an [Instant] relative to
  "now") as a short human label. Never renders a negative age — a
  then-instant in the future clamps to "just now" rather than showing
  something nonsensical like "-3m".

- "just now" (<60s), "Nm" (<60m), "Nh" (<24h), "Nd" (<14d), otherwise "Nw".

- "Updated: unknown" when [observedAt] is null; otherwise "Updated <age> ago",
   special-cased to "Updated just now" (no trailing "ago") for the freshest bucket.


### `co/saari/repoglance/render/CiSemanticRole.kt`

- Semantic color role for a [CiState], decoupled from any concrete Color —
  the Compose and Glance layers each map a role to their own theme color
  (`MaterialTheme.colorScheme.*` / `GlanceTheme.colors.*`); no hardcoded hex
  lives in either. [CiState.NO_CI] and [CiState.UNKNOWN] intentionally
  share [CiColorRole.NEUTRAL] — neither is "passing" nor "failing", so
  neither earns its own accent color.


### `co/saari/repoglance/render/SnapshotRendering.kt`

- Display-rule single source of truth for [RepoSnapshot] fields. Slice 2's
  UI reads only through these functions — no widget or screen re-derives
  these rules independently.

- "—" (em dash) for UNKNOWN basis or a null count — never "0" for unknown.

- Non-null exactly when the snapshot's basis is LAST_GOOD.


### `co/saari/repoglance/state/AppPrefs.kt`

- Thin SharedPreferences wrapper for Slice 2's on-device state: the
  fixture-scenario switcher and the pinned-repo set (repos stored as
  [co.saari.repoglance.model.RepoRef.full] strings). Two keys don't need
  DataStore.

- Toggles [repoFull] in the pinned set and persists the result.

- Recomposes on [KEY_SCENARIO] changes, including ones made outside
   the calling composable (e.g. another screen's setter).

- Recomposes on [KEY_PINNED] changes.


### `co/saari/repoglance/state/LiveSnapshotStore.kt`

- Durable per-repository live counts, so a widget can render without a
  network call. Only the fields the compact widget needs are persisted;
  navigator rows stay out of storage.
  
  A record that cannot be read back exactly as it was written is discarded
  rather than partially reconstructed — a half-restored snapshot would be a
  count of unknown basis, which the truth rules forbid.

- Includes RepoSnapshot's own truth-rule IllegalArgumentException: a

- record that no longer satisfies the invariants is not a snapshot.


### `co/saari/repoglance/state/NavigatorScopeCodec.kt`

- Encodes/decodes [NavigatorScope] to a (kind, value) pair of primitive
  `String`s. A sealed interface holding a [RepoRef] is not directly
  Bundle-saveable (it implements neither `Parcelable` nor `Serializable`),
  so screens that need [NavigatorScope] to survive `rememberSaveable`
  (config changes / process death) store these two strings instead and
  decode back through this object.

- Falls back to [NavigatorScope.Account] for an unrecognized kind or an
   invalid/unparseable repo [value] — never throws.


### `co/saari/repoglance/state/SnapshotStore.kt`

- Pure function layer over [Fixtures]: pin-aware sorting, widget-content
  selection, cached-row search, and [NavigatorMode.BOTH] composition. No
  state lives here — callers own scenario/pin state (see [AppPrefs]) and
  pass it in explicitly.

- Pinned repos first, fixture order otherwise preserved on both sides
   of the split (stable — toggling a pin only ever moves that one repo
   across the pinned/unpinned boundary, nothing else reorders).

- Stack-widget content: pinned snapshots when any exist, otherwise
   every snapshot in the scenario — so a fresh install with no pins yet
   still shows widget content instead of an empty stack.

- Case-insensitive substring search over title + number + labels +
   author. A blank [query] returns [rows] unchanged (empty query = all).

- Navigator rows for [mode] at [scope]/[filter]/[state], composing two
  [Fixtures.navigatorList] calls for [NavigatorMode.BOTH] (an ISSUES
  list and a PRS list, sectioned issues-then-prs) since a single
  [NavigatorList] cannot hold both row types (see [NavigatorRows]).
  
  [NavigatorFilter.AWAITING_MY_REVIEW] is PR-only — [Fixtures.navigatorList]
  throws if asked for it under [NavigatorMode.ISSUES]. In [NavigatorMode.BOTH]
  with that filter selected, the issues section is therefore returned
  empty (never populated by silently substituting a different filter's
  rows); its `valueBasis`/`observedAt` still come from a real ISSUES/OPEN
  call so the section's data-age label matches the PRS section for the
  same [state].

- NavigatorList's own init invariant forbids AWAITING_MY_REVIEW

- paired with Issues rows, so the emptied list keeps its

- basis source's OPEN filter rather than reporting

- AWAITING_MY_REVIEW on an issues list — nothing downstream

- reads NavigatorList.filter, only rows/valueBasis/observedAt.

- One or both sections of a [NavigatorMode.BOTH] result; exactly one of
   [issues]/[prs] is non-null for [NavigatorMode.ISSUES]/[NavigatorMode.PRS],
   both are non-null for [NavigatorMode.BOTH].


### `co/saari/repoglance/ui/HomeScreen.kt`

- Home screen (fixture mode): scenario switcher, pin-aware repo list, and
  the widget-pinning debug/user affordance. All display strings read
  through [SnapshotRendering] / [Ages] — nothing here re-derives a rule.

- MIXED deliberately repeats the same repo full-name across

- several truth-rule demo entries (see Fixtures.mixedSnapshots),

- so `repo.full` alone isn't a unique LazyColumn key within

- that scenario — basis + rate limit disambiguate it.


### `co/saari/repoglance/ui/LiveRepoGlanceScreen.kt`

- Accessibility resource id for the repository search field on the loaded home.

- Accessibility resource id for the owner (account/organization) filter on the loaded home.


### `co/saari/repoglance/ui/NavigatorScreen.kt`

- Navigator screen with a compact app-owned control block that scrolls away
  as part of the real result list. The mode selector is the only sticky app
  chrome. It settles below the system status bar because the opaque screen
  surface paints behind system bars while all interactive content respects
  safe insets.

- Row payload unifying [IssueRow]/[PrRow] for shared list/detail rendering;
   [pr] is non-null exactly when the row came from a PR list, carrying the
   draft/review/CI-rollup fields issues don't have.

- Fixture-mode "Load more" paging: appends another copy of the same fixture
   rows with numbers offset so they never collide with an earlier page or
   with the other section's number range (issues start at 100+, PRs at
   200+) — documented in the PR spec as an acceptable fixture stand-in for
   a real next-page fetch.

- Compose's experimental sticky header can be dropped after a long

- cover-display scroll on the API 36 Fold emulator. Keep an animated,

- opaque copy over the list once the controls item has left the viewport

- so the sole persistent app control remains available in every posture.


### `co/saari/repoglance/ui/theme/Theme.kt`

- RepoGlance theme: Material You dynamic color on API 31+ (minSdk is already
  31, so this is always available on-device), falling back to static
  light/dark color schemes otherwise so the theme still compiles and renders
  correctly on any future lower minSdk.


### `co/saari/repoglance/widget/RepoWidget.kt`

- Independently configured per-repository widget.
  
  A compact 2x1 placement is a count summary. Any placement tall enough for
  rows becomes a single recently-updated feed whose entries are individually
  labeled ISSUE or PR. Header/summary taps open RepoGlance at this widget's
  repository and mode; row taps open the exact GitHub URL through Android's
  verified-link routing.

- Glance keeps a composition alive briefly after the first render.

- Read storage inside the composition so an explicit update after

- configuration/reconfiguration sees the newly persisted values.

- Compact renders live counts published by the app; the tall row

- feed is still fixture-backed and says so (widget-content-priority-008

- is scoped to the compact slot).

- Retained for the tall row feed, which still renders fixture rows.

- Below this height only the two headline rows fit.

- Freshness for the compact slot, in as few characters as the 120dp floor
  allows. Truth rules 2-3: every surface shows its data age, and last-good
  never reads as current. EXACT renders the bare age ("12m"), LAST_GOOD is
  prefixed so it cannot be mistaken for a fresh value, and no observation at
  all says so rather than showing nothing.

- No stored observation yet: every count is unknown, never zero.

- Right-aligned ledger (GrillTrack widget-content-priority-008): label

- left, number hard right, so digits line up in a column. The

- "to review" row is progressive disclosure — it renders only where

- there is height for it.


### `co/saari/repoglance/widget/RepoWidgetConfigActivity.kt`

- System-launched configuration and reconfiguration surface for one repo
  widget instance. A canceled activity leaves the widget unconfigured; Save
  commits only this launcher's app-widget ID and then renders that ID.


### `co/saari/repoglance/widget/RepoWidgetConfigStore.kt`

- Private, per-app-widget configuration. App-widget IDs are assigned by the
  launcher, so every placed widget gets independent repository and mode keys.


### `co/saari/repoglance/widget/StackWidget.kt`

- Multi-repo stack widget: a header (scenario name + oldest-observed data
  age across the shown repos) over a [LazyColumn] of compact repo rows.
  Content is [SnapshotStore.stackWidgetRepos] — pinned repos, or every repo
  in the scenario when nothing is pinned yet.
  
  Row tap opens [MainActivity] pre-scoped to that repo (see
  [RepoWidget]'s KDoc for why this is chosen over a raw ACTION_VIEW Intent
  from Glance).


### `co/saari/repoglance/widget/WidgetActions.kt`

- Shared repo Intent-extra contract between the widgets' tap actions and
  [co.saari.repoglance.MainActivity], which reads this extra to pre-scope
  its navigator to the tapped repo. Each widget builds its own explicit
  `Intent(context, MainActivity::class.java).putExtra(EXTRA_REPO_FULL, ...)`
  and passes it to `actionStartActivity(intent)` — chosen over a raw
  ACTION_VIEW Intent from Glance because opening the app pre-scoped is the
  documented Slice 2 choice (see widget/RepoWidget.kt and
  widget/StackWidget.kt KDoc).

- Optional companion to [EXTRA_REPO_FULL] for opening the navigator in the
  mode selected for one widget instance. Missing or malformed values stay
  safe and predictable by falling back to [NavigatorMode.BOTH].


### `co/saari/repoglance/widget/WidgetFixtureData.kt`

- Fixture-only projection used by widget configuration and rendering. The
  catalog is stable across fixture scenario switches; current-scenario data
  wins when available, with a deterministic catalog fallback otherwise.

- A single recently-updated feed. BOTH intentionally interleaves issue
  and PR rows by update time instead of presenting two sections.


### `co/saari/repoglance/widget/WidgetRefresh.kt`

- Called after a scenario or pin change so home-screen widgets track the
  in-app fixture state immediately — fixtures don't otherwise refresh
  (`updatePeriodMillis` is 0 in both widget-info XMLs).
