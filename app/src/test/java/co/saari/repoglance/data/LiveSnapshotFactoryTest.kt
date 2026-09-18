package co.saari.repoglance.data

import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveSnapshotFactoryTest {

    private val now = Instant.parse("2026-09-17T23:00:00Z")
    private val earlier = Instant.parse("2026-09-17T21:00:00Z")
    private val repo = LiveRepository(
        id = 1L,
        ref = RepoRef("saari-co", "RepoGlance"),
        isPrivate = false,
        isArchived = false,
        pushedAt = earlier,
    )

    @Test
    fun subtractsPullRequestsFromGitHubsOpenIssuesCount() {
        val snapshot = build(
            openIssuesAndPrs = 12,
            prs = page(listOf(pr(1, false), pr(2, true), pr(3, false))),
        )

        assertEquals(ValueBasis.EXACT, snapshot.valueBasis)
        assertEquals(3, snapshot.openPrs)
        // GitHub counts PRs as issues: 12 total - 3 PRs = 9 true issues.
        assertEquals(9, snapshot.openIssues)
        assertEquals(1, snapshot.prsAwaitingMyReview)
    }

    @Test
    fun truncatedPullRequestPageIsNeverPublishedAsAnExactCount() {
        val snapshot = build(
            openIssuesAndPrs = 400,
            prs = page(listOf(pr(1, true)), hasMorePages = true),
        )

        assertEquals(ValueBasis.UNKNOWN, snapshot.valueBasis)
        assertNull(snapshot.openPrs)
        assertNull(snapshot.openIssues)
        assertNull(snapshot.prsAwaitingMyReview)
    }

    @Test
    fun truncatedPageAgesThePreviousObservationInsteadOfInventingOne() {
        val previous = build(
            openIssuesAndPrs = 12,
            prs = page(listOf(pr(1, true), pr(2, false))),
        )

        val aged = build(
            openIssuesAndPrs = 400,
            prs = page(listOf(pr(1, true)), hasMorePages = true),
            previous = previous,
        )

        assertEquals(ValueBasis.LAST_GOOD, aged.valueBasis)
        assertEquals(previous.openPrs, aged.openPrs)
        assertEquals(previous.openIssues, aged.openIssues)
        // The aged value keeps the moment it was actually observed.
        assertEquals(previous.observedAt, aged.observedAt)
    }

    @Test
    fun missingRepositoryMetadataYieldsUnknownRatherThanZero() {
        val snapshot = build(openIssuesAndPrs = null, prs = page(listOf(pr(1, false))))

        assertEquals(ValueBasis.UNKNOWN, snapshot.valueBasis)
        assertNull(snapshot.openIssues)
    }

    @Test
    fun anUnknownPreviousSnapshotIsNotAgedIntoLastGood() {
        val unknownPrevious = build(openIssuesAndPrs = null, prs = null)
        assertEquals(ValueBasis.UNKNOWN, unknownPrevious.valueBasis)

        val snapshot = build(
            openIssuesAndPrs = null,
            prs = null,
            previous = unknownPrevious,
        )

        assertEquals(ValueBasis.UNKNOWN, snapshot.valueBasis)
    }

    @Test
    fun incoherentCountsDoNotProduceNegativeIssues() {
        // More PRs than GitHub's combined counter: the pair cannot be trusted.
        val snapshot = build(
            openIssuesAndPrs = 1,
            prs = page(listOf(pr(1, false), pr(2, false), pr(3, false))),
        )

        assertEquals(ValueBasis.UNKNOWN, snapshot.valueBasis)
        assertNull(snapshot.openIssues)
    }

    @Test
    fun ciAndReleaseStayAbsentBecauseTheLiveClientNeverFetchesThem() {
        val snapshot = build(openIssuesAndPrs = 5, prs = page(listOf(pr(1, false))))

        assertNull(snapshot.latestRelease)
        assertEquals(co.saari.repoglance.model.CiState.UNKNOWN, snapshot.defaultBranchCi)
    }

    @Test
    fun anUntruncatedIssuesPageIsExactWithoutTheRepositoryCounter() {
        // No metadata at all: the issues page already excludes pull requests,
        // so an untruncated page is an exact count on its own.
        val snapshot = LiveSnapshotFactory.build(
            repository = repo,
            metadata = null,
            issues = LivePage(rows = listOf(issue(1), issue(2), issue(3)), hasMorePages = false),
            pullRequests = page(listOf(pr(1, true))),
            previous = null,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        )

        assertEquals(ValueBasis.EXACT, snapshot.valueBasis)
        assertEquals(3, snapshot.openIssues)
        assertEquals(1, snapshot.openPrs)
    }

    @Test
    fun aTruncatedIssuesPageFallsBackToTheRepositoryCounter() {
        val snapshot = LiveSnapshotFactory.build(
            repository = repo,
            metadata = LiveRepositoryMetadata(openIssuesAndPullRequests = 12, pushedAt = earlier),
            issues = LivePage(rows = listOf(issue(1)), hasMorePages = true),
            pullRequests = page(listOf(pr(1, false), pr(2, false), pr(3, false))),
            previous = null,
            observedAt = now,
            rateLimit = RateLimitBucket.OK,
        )

        assertEquals(ValueBasis.EXACT, snapshot.valueBasis)
        // Falls back to 12 - 3 = 9 rather than trusting the truncated page.
        assertEquals(9, snapshot.openIssues)
    }

    private fun issue(number: Int): LiveIssue = LiveIssue(
        number = number,
        title = "Issue $number",
        author = "someone",
        assignee = null,
        labels = emptyList(),
        commentCount = null,
        updatedAt = earlier,
        htmlUrl = "https://github.com/saari-co/RepoGlance/issues/$number",
    )

    private fun build(
        openIssuesAndPrs: Int?,
        prs: LivePage<LivePullRequest>?,
        previous: RepoSnapshot? = null,
    ): RepoSnapshot = LiveSnapshotFactory.build(
        repository = repo,
        metadata = LiveRepositoryMetadata(openIssuesAndPrs, pushedAt = earlier),
        issues = null,
        pullRequests = prs,
        previous = previous,
        observedAt = now,
        rateLimit = RateLimitBucket.OK,
    )

    private fun page(
        rows: List<LivePullRequest>,
        hasMorePages: Boolean = false,
    ): LivePage<LivePullRequest> = LivePage(rows = rows, hasMorePages = hasMorePages)

    private fun pr(number: Int, awaitingViewerReview: Boolean): LivePullRequest = LivePullRequest(
        number = number,
        title = "PR $number",
        author = "someone",
        assignee = null,
        labels = emptyList(),
        isDraft = false,
        reviewRequestedFromViewer = awaitingViewerReview,
        updatedAt = earlier,
        htmlUrl = "https://github.com/saari-co/RepoGlance/pull/$number",
    )
}
