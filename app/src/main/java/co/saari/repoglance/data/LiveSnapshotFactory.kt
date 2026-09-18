package co.saari.repoglance.data

import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import java.time.Instant

object LiveSnapshotFactory {

    fun needsRepositoryMetadata(
        issues: LivePage<LiveIssue>?,
        pullRequests: LivePage<LivePullRequest>?,
    ): Boolean {
        if (pullRequests == null || pullRequests.hasMorePages) return false
        return issues == null || issues.hasMorePages
    }

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
            return previous.copy(
                valueBasis = ValueBasis.LAST_GOOD,
                rateLimit = rateLimit,
            )
        }
        return unknown(repository, rateLimit)
    }

    private fun exactCounts(
        metadata: LiveRepositoryMetadata?,
        issues: LivePage<LiveIssue>?,
        pullRequests: LivePage<LivePullRequest>?,
    ): ExactCounts? {
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
