package co.saari.repoglance.data

import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import java.time.Instant

/**
 * Builds a [RepoSnapshot] from live GitHub responses.
 *
 * Truth rules this enforces, in order:
 *
 * 1. An open-PR count is exact only when the open-PR page was not truncated.
 *    A truncated page is a lower bound, never a count.
 * 2. `GET /repos/{owner}/{name}` reports `open_issues_count` as issues **plus**
 *    pull requests — GitHub's long-standing quirk — so true open issues are
 *    `open_issues_count - openPrs`. That subtraction is only sound when
 *    `openPrs` is itself exact, which is why rule 1 comes first.
 * 3. When the current fetch cannot produce exact counts, fall back to the
 *    previous snapshot as [ValueBasis.LAST_GOOD] so the widget ages an honest
 *    number instead of inventing one.
 * 4. With no exact fetch and no previous value, the basis is
 *    [ValueBasis.UNKNOWN] and every count renders as "—", never as zero.
 *
 * Default-branch CI and latest release are not fetched by [GitHubApiClient];
 * they stay absent rather than being guessed.
 */
object LiveSnapshotFactory {

    fun build(
        repository: LiveRepository,
        metadata: LiveRepositoryMetadata?,
        issues: LivePage<LiveIssue>?,
        pullRequests: LivePage<LivePullRequest>?,
        previous: RepoSnapshot?,
        observedAt: Instant,
        rateLimit: RateLimitBucket,
    ): RepoSnapshot {
        val exact = exactCounts(metadata, issues, pullRequests)
        if (exact != null) {
            return RepoSnapshot(
                repo = repository.ref,
                openPrs = exact.openPrs,
                prsAwaitingMyReview = exact.awaitingReview,
                openIssues = exact.openIssues,
                defaultBranchCi = CiState.UNKNOWN,
                latestRelease = null,
                pushedAt = metadata?.pushedAt ?: repository.pushedAt,
                valueBasis = ValueBasis.EXACT,
                observedAt = observedAt,
                rateLimit = rateLimit,
            )
        }
        if (previous != null && previous.valueBasis != ValueBasis.UNKNOWN) {
            // Age the previous observation rather than publish a bound as a count.
            return previous.copy(
                valueBasis = ValueBasis.LAST_GOOD,
                rateLimit = rateLimit,
            )
        }
        return unknown(repository, rateLimit)
    }

    /** Counts that survive every truth rule, or null when any of them fails. */
    private fun exactCounts(
        metadata: LiveRepositoryMetadata?,
        issues: LivePage<LiveIssue>?,
        pullRequests: LivePage<LivePullRequest>?,
    ): ExactCounts? {
        // Every count is derived from the open-PR page, so it must be whole.
        if (pullRequests == null || pullRequests.hasMorePages) return null
        val openPrs = pullRequests.rows.size

        val openIssues = openIssueCount(metadata, issues, openPrs) ?: return null

        val awaitingReview = pullRequests.rows.count { it.reviewRequestedFromViewer }
        return ExactCounts(
            openPrs = openPrs,
            openIssues = openIssues,
            awaitingReview = awaitingReview,
        )
    }

    /**
     * Prefers the page already in hand: an untruncated issues page is
     * exact on its own, because [GitHubApiClient] strips pull requests out
     * of `/issues` before it ever gets here. Only when that page was
     * truncated does the repository counter earn its keep, and then it has
     * to have pull requests subtracted back out.
     */
    private fun openIssueCount(
        metadata: LiveRepositoryMetadata?,
        issues: LivePage<LiveIssue>?,
        openPrs: Int,
    ): Int? {
        if (issues != null && !issues.hasMorePages) return issues.rows.size

        val combined = metadata?.openIssuesAndPullRequests ?: return null
        val derived = combined - openPrs
        return derived.takeIf { it >= 0 }
    }

    private fun unknown(repository: LiveRepository, rateLimit: RateLimitBucket): RepoSnapshot =
        RepoSnapshot(
            repo = repository.ref,
            openPrs = null,
            prsAwaitingMyReview = null,
            openIssues = null,
            defaultBranchCi = CiState.UNKNOWN,
            latestRelease = null,
            pushedAt = null,
            valueBasis = ValueBasis.UNKNOWN,
            observedAt = null,
            rateLimit = rateLimit,
        )

    private data class ExactCounts(
        val openPrs: Int,
        val openIssues: Int,
        val awaitingReview: Int,
    )
}
