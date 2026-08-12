package co.saari.repoglance.fixtures

import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.IssueRow
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorList
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorRows
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.PrRow
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.ReleaseInfo
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ReviewState
import co.saari.repoglance.model.ValueBasis
import java.time.Duration
import java.time.Instant

/**
 * Public-neutral fixture corpus: real public repos ([saari-co/RepoGlance]
 * and [dinkuskit/blocks]) plus clearly fictional ones (acme/rocket,
 * acme/api-server, octoco/infra). Nothing here claims real data about a
 * real third-party repo — the fictional repos exist purely to exercise
 * scenarios the real ones don't need to demonstrate. All ages are computed
 * relative to the `now` passed in, so fixtures stay deterministic across
 * time in golden-style tests.
 */
object Fixtures {

    private const val YOU = "octodev"
    private val CAST = listOf("octodev", "mkraft", "jrivera", "tstone")

    private val LABEL_POOL = listOf(
        "bug", "enhancement", "ci", "docs", "question", "good-first-issue", "triage",
    )

    private val TITLE_POOL = listOf(
        "Fix flaky retry in sync worker",
        "Add pagination to issue navigator",
        "Investigate CI flake on macOS runners",
        "Update docs for release process",
        "Refactor snapshot invariants for clarity",
        "Support dynamic color in widget preview",
        "Handle rate limit backoff edge case",
        "Clean up unused gradle configuration",
        "Add golden tests for age formatting",
        "Fix crash on empty navigator list",
        "Improve error message for invalid repo ref",
        "Wire up deep links for releases",
        "Reduce widget refresh battery cost",
        "Document pinning behavior for repos",
        "Split fixtures into per-scenario builders",
    )

    // ---- snapshots -----------------------------------------------------

    fun snapshots(scenario: FixtureScenario, now: Instant): List<RepoSnapshot> = when (scenario) {
        FixtureScenario.EXACT -> exactSnapshots(now)
        FixtureScenario.LAST_GOOD -> lastGoodSnapshots(now)
        FixtureScenario.UNKNOWN -> unknownSnapshots(now)
        FixtureScenario.RATE_LIMITED -> rateLimitedSnapshots(now)
        FixtureScenario.NO_CI -> noCiSnapshots(now)
        FixtureScenario.EMPTY -> emptyList()
        FixtureScenario.MIXED -> mixedSnapshots(now)
    }

    private fun exactSnapshots(now: Instant): List<RepoSnapshot> = listOf(
        RepoSnapshot(
            repo = RepoRef("saari-co", "RepoGlance"),
            openPrs = 2,
            prsAwaitingMyReview = 1,
            openIssues = 5,
            defaultBranchCi = CiState.PASSING,
            latestRelease = ReleaseInfo("v0.1.0", now.minus(Duration.ofDays(2))),
            pushedAt = now.minus(Duration.ofMinutes(4)),
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        ),
        // Legitimate zero: 0 open PRs under EXACT basis is a real, known
        // value — distinct from UNKNOWN's null/"—" rendering.
        RepoSnapshot(
            repo = RepoRef("dinkuskit", "blocks"),
            openPrs = 0,
            prsAwaitingMyReview = 0,
            openIssues = 3,
            defaultBranchCi = CiState.FAILING,
            latestRelease = ReleaseInfo("v2.3.1", now.minus(Duration.ofHours(3))),
            pushedAt = now.minus(Duration.ofMinutes(38)),
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        ),
        RepoSnapshot(
            repo = RepoRef("acme", "rocket"),
            openPrs = 4,
            prsAwaitingMyReview = 2,
            openIssues = 11,
            defaultBranchCi = CiState.RUNNING,
            latestRelease = null,
            pushedAt = now.minus(Duration.ofHours(3)),
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        ),
        RepoSnapshot(
            repo = RepoRef("acme", "api-server"),
            openPrs = 1,
            prsAwaitingMyReview = 0,
            openIssues = 7,
            defaultBranchCi = CiState.PASSING,
            latestRelease = ReleaseInfo("v1.4.0", now.minus(Duration.ofDays(10))),
            pushedAt = now.minus(Duration.ofDays(2)),
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        ),
    )

    private fun lastGoodSnapshots(now: Instant): List<RepoSnapshot> = listOf(
        RepoSnapshot(
            repo = RepoRef("saari-co", "RepoGlance"),
            openPrs = 2,
            prsAwaitingMyReview = 1,
            openIssues = 6,
            defaultBranchCi = CiState.PASSING,
            latestRelease = ReleaseInfo("v0.1.0", now.minus(Duration.ofDays(2))),
            pushedAt = now.minus(Duration.ofHours(1)),
            valueBasis = ValueBasis.LAST_GOOD,
            observedAt = now.minus(Duration.ofMinutes(45)),
            rateLimit = RateLimitBucket.OK,
        ),
        RepoSnapshot(
            repo = RepoRef("octoco", "infra"),
            openPrs = 9,
            prsAwaitingMyReview = 3,
            openIssues = 21,
            defaultBranchCi = CiState.FAILING,
            latestRelease = ReleaseInfo("v3.0.0", now.minus(Duration.ofDays(30))),
            pushedAt = now.minus(Duration.ofHours(3)),
            valueBasis = ValueBasis.LAST_GOOD,
            observedAt = now.minus(Duration.ofHours(3)),
            rateLimit = RateLimitBucket.LOW,
        ),
    )

    private fun unknownSnapshots(now: Instant): List<RepoSnapshot> = listOf(
        RepoSnapshot(
            repo = RepoRef("acme", "rocket"),
            openPrs = null,
            prsAwaitingMyReview = null,
            openIssues = null,
            defaultBranchCi = CiState.UNKNOWN,
            latestRelease = null,
            pushedAt = null,
            valueBasis = ValueBasis.UNKNOWN,
            observedAt = null,
            rateLimit = RateLimitBucket.UNKNOWN,
        ),
        RepoSnapshot(
            repo = RepoRef("octoco", "infra"),
            openPrs = null,
            prsAwaitingMyReview = null,
            openIssues = null,
            defaultBranchCi = CiState.UNKNOWN,
            latestRelease = null,
            pushedAt = null,
            valueBasis = ValueBasis.UNKNOWN,
            observedAt = null,
            rateLimit = RateLimitBucket.UNKNOWN,
        ),
    )

    private fun rateLimitedSnapshots(now: Instant): List<RepoSnapshot> = listOf(
        RepoSnapshot(
            repo = RepoRef("saari-co", "RepoGlance"),
            openPrs = 2,
            prsAwaitingMyReview = 1,
            openIssues = 5,
            defaultBranchCi = CiState.PASSING,
            latestRelease = ReleaseInfo("v0.1.0", now.minus(Duration.ofDays(2))),
            pushedAt = now.minus(Duration.ofHours(2)),
            valueBasis = ValueBasis.LAST_GOOD,
            observedAt = now.minus(Duration.ofHours(2)),
            rateLimit = RateLimitBucket.EXHAUSTED,
        ),
        RepoSnapshot(
            repo = RepoRef("acme", "api-server"),
            openPrs = 1,
            prsAwaitingMyReview = 0,
            openIssues = 7,
            defaultBranchCi = CiState.PASSING,
            latestRelease = ReleaseInfo("v1.4.0", now.minus(Duration.ofDays(10))),
            pushedAt = now.minus(Duration.ofHours(2)),
            valueBasis = ValueBasis.LAST_GOOD,
            observedAt = now.minus(Duration.ofHours(2)),
            rateLimit = RateLimitBucket.EXHAUSTED,
        ),
        RepoSnapshot(
            repo = RepoRef("octoco", "infra"),
            openPrs = 9,
            prsAwaitingMyReview = 3,
            openIssues = 21,
            defaultBranchCi = CiState.FAILING,
            latestRelease = ReleaseInfo("v3.0.0", now.minus(Duration.ofDays(30))),
            pushedAt = now.minus(Duration.ofHours(2)),
            valueBasis = ValueBasis.LAST_GOOD,
            observedAt = now.minus(Duration.ofHours(2)),
            rateLimit = RateLimitBucket.EXHAUSTED,
        ),
    )

    private fun noCiSnapshots(now: Instant): List<RepoSnapshot> = listOf(
        RepoSnapshot(
            repo = RepoRef("acme", "rocket"),
            openPrs = 0,
            prsAwaitingMyReview = 0,
            openIssues = 2,
            defaultBranchCi = CiState.NO_CI,
            latestRelease = null,
            pushedAt = now.minus(Duration.ofDays(5)),
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        ),
        RepoSnapshot(
            repo = RepoRef("octoco", "infra"),
            openPrs = 3,
            prsAwaitingMyReview = 1,
            openIssues = 4,
            defaultBranchCi = CiState.NO_CI,
            latestRelease = ReleaseInfo("v3.0.0", now.minus(Duration.ofDays(30))),
            pushedAt = now.minus(Duration.ofDays(1)),
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        ),
    )

    // Dogfood default: one repo from each other scenario, so a single
    // MIXED corpus demonstrates every truth-rule state side by side.
    private fun mixedSnapshots(now: Instant): List<RepoSnapshot> = listOf(
        exactSnapshots(now)[1],
        lastGoodSnapshots(now)[0],
        unknownSnapshots(now)[0],
        rateLimitedSnapshots(now)[1],
        noCiSnapshots(now)[1],
    )

    // ---- navigator lists ------------------------------------------------

    /**
     * Builds one [NavigatorList] for the given [scope]/[mode]/[filter]
     * combination. [mode] must be [NavigatorMode.ISSUES] or
     * [NavigatorMode.PRS] — a single [NavigatorList] cannot hold both row
     * types (see [NavigatorRows]); a caller wanting [NavigatorMode.BOTH]
     * calls this twice and holds the two lists side by side.
     */
    fun navigatorList(
        scope: NavigatorScope,
        mode: NavigatorMode,
        filter: NavigatorFilter,
        state: ListState,
        now: Instant,
    ): NavigatorList {
        require(mode != NavigatorMode.BOTH) {
            "navigatorList builds one typed list per call; BOTH composes an ISSUES list and a PRS list at the call site"
        }
        // scope only affects which repo/org the rows are notionally drawn
        // from in a wired-up app; the fixture corpus's row content does not
        // vary by scope in Slice 1.
        return when (state) {
            ListState.EMPTY -> NavigatorList(
                filter = filter,
                rows = emptyRows(mode),
                valueBasis = ValueBasis.EXACT,
                observedAt = now,
                hasMorePages = false,
            )
            ListState.LOADED -> NavigatorList(
                filter = filter,
                rows = buildRows(mode, filter, count = 8, now = now),
                valueBasis = ValueBasis.EXACT,
                observedAt = now,
                hasMorePages = false,
            )
            ListState.PAGED -> NavigatorList(
                filter = filter,
                rows = buildRows(mode, filter, count = NavigatorList.PAGE_SIZE, now = now),
                valueBasis = ValueBasis.EXACT,
                observedAt = now,
                hasMorePages = true,
            )
            ListState.LAST_GOOD -> {
                val observedAt = now.minus(Duration.ofHours(1))
                NavigatorList(
                    filter = filter,
                    rows = buildRows(mode, filter, count = 8, now = observedAt),
                    valueBasis = ValueBasis.LAST_GOOD,
                    observedAt = observedAt,
                    hasMorePages = false,
                )
            }
        }
    }

    private fun emptyRows(mode: NavigatorMode): NavigatorRows = when (mode) {
        NavigatorMode.ISSUES -> NavigatorRows.Issues(emptyList())
        NavigatorMode.PRS -> NavigatorRows.Prs(emptyList())
        NavigatorMode.BOTH -> error("unreachable: BOTH is rejected before this point")
    }

    private fun buildRows(mode: NavigatorMode, filter: NavigatorFilter, count: Int, now: Instant): NavigatorRows =
        when (mode) {
            NavigatorMode.ISSUES -> NavigatorRows.Issues(buildIssueRows(filter, count, now))
            NavigatorMode.PRS -> NavigatorRows.Prs(buildPrRows(filter, count, now))
            NavigatorMode.BOTH -> error("unreachable: BOTH is rejected before this point")
        }

    private fun rowTitle(filter: NavigatorFilter, index: Int): String {
        val base = TITLE_POOL[index % TITLE_POOL.size]
        return if (filter == NavigatorFilter.MENTIONS) "$base (cc @$YOU)" else base
    }

    private fun buildIssueRows(filter: NavigatorFilter, count: Int, now: Instant): List<IssueRow> =
        (0 until count).map { i ->
            IssueRow(
                number = 100 + i,
                title = rowTitle(filter, i),
                state = if (filter == NavigatorFilter.OPEN) "open"
                else if (i % 5 == 0) "closed" else "open",
                labels = listOf(LABEL_POOL[i % LABEL_POOL.size]),
                author = CAST[i % CAST.size],
                assignee = if (filter == NavigatorFilter.MINE) YOU else CAST[(i + 1) % CAST.size],
                commentCount = i % 6,
                updatedAt = now.minus(Duration.ofMinutes((i + 1) * 7L)),
            )
        }

    private fun buildPrRows(filter: NavigatorFilter, count: Int, now: Instant): List<PrRow> =
        (0 until count).map { i ->
            PrRow(
                number = 200 + i,
                title = rowTitle(filter, i),
                state = if (
                    filter == NavigatorFilter.OPEN ||
                    filter == NavigatorFilter.AWAITING_MY_REVIEW
                ) "open" else if (i % 7 == 0) "closed" else "open",
                labels = listOf(LABEL_POOL[i % LABEL_POOL.size]),
                author = CAST[i % CAST.size],
                assignee = if (filter == NavigatorFilter.MINE) YOU else CAST[(i + 1) % CAST.size],
                commentCount = i % 6,
                updatedAt = now.minus(Duration.ofMinutes((i + 1) * 7L)),
                isDraft = filter != NavigatorFilter.AWAITING_MY_REVIEW && i % 4 == 0,
                reviewState = if (filter == NavigatorFilter.AWAITING_MY_REVIEW) {
                    ReviewState.REVIEW_REQUIRED
                } else {
                    ReviewState.entries[i % ReviewState.entries.size]
                },
                ciRollup = CiState.entries[i % CiState.entries.size],
            )
        }
}
