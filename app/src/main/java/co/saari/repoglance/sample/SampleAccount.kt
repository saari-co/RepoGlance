package co.saari.repoglance.sample

import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.GitHubViewer
import co.saari.repoglance.data.LiveIssue
import co.saari.repoglance.data.LivePage
import co.saari.repoglance.data.LivePullRequest
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.data.LiveRepositoryCatalog
import co.saari.repoglance.data.LiveRepositoryContent
import co.saari.repoglance.data.RateLimitSnapshot
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.time.Duration
import java.time.Instant

object SampleAccount {
    const val VIEWER_LOGIN = "saariuslystoned"
    const val ITEM_NOTE = "Sample item — not on GitHub"
    const val OUTSIDE_SAMPLE = "Not part of the sample account"

    val RATE_LIMIT = RateLimitSnapshot(RateLimitBucket.OK, remaining = null, limit = null, resetsAt = null)
    private val UNKNOWN_RATE =
        RateLimitSnapshot(RateLimitBucket.UNKNOWN, remaining = null, limit = null, resetsAt = null)

    private data class SampleRepo(
        val id: Long,
        val ref: RepoRef,
        val isPrivate: Boolean,
        val isArchived: Boolean,
        val pushedAgo: Duration?,
        val issueCount: Int,
        val pullRequestCount: Int,
        val firstNumber: Int,
    )

    private val REPOS = listOf(
        SampleRepo(1L, RepoRef("saari-co", "rocket"), false, false, Duration.ofMinutes(25), 5, 3, 412),
        SampleRepo(2L, RepoRef("saari-co", "api-server"), true, false, Duration.ofHours(2), 3, 2, 218),
        SampleRepo(3L, RepoRef("saari-co", "mobile-app"), false, false, Duration.ofHours(6), 4, 2, 87),
        SampleRepo(4L, RepoRef("dinkuskit", "infra"), true, false, Duration.ofDays(1), 2, 1, 1_203),
        SampleRepo(5L, RepoRef("dinkuskit", "design-system"), false, false, Duration.ofDays(3), 1, 0, 36),
        SampleRepo(6L, RepoRef(VIEWER_LOGIN, "dotfiles"), false, false, Duration.ofDays(9), 0, 1, 14),
        SampleRepo(7L, RepoRef("saari-co", "legacy-site"), false, true, null, 0, 0, 1),
    )

    fun catalog(now: Instant): LiveRepositoryCatalog = LiveRepositoryCatalog(
        viewer = GitHubViewer(login = VIEWER_LOGIN, avatarUrl = null),
        installations = emptyList(),
        repositories = REPOS.map { it.toLive(now) },
    )

    fun content(repository: LiveRepository, now: Instant): LiveRepositoryContent {
        val index = REPOS.indexOfFirst { it.ref.full.equals(repository.ref.full, ignoreCase = true) }
        val sample = REPOS.getOrNull(index) ?: return LiveRepositoryContent(
            repository = repository,
            issues = GitHubApiResult.Failure(OUTSIDE_SAMPLE, statusCode = null, rateLimit = UNKNOWN_RATE),
            pullRequests = GitHubApiResult.Failure(OUTSIDE_SAMPLE, statusCode = null, rateLimit = UNKNOWN_RATE),
        )
        return LiveRepositoryContent(
            repository = repository,
            issues = GitHubApiResult.Success(
                LivePage(issueRows(sample, index, now), hasMorePages = false),
                now,
                RATE_LIMIT,
            ),
            pullRequests = GitHubApiResult.Success(
                LivePage(pullRequestRows(sample, index, now), hasMorePages = false),
                now,
                RATE_LIMIT,
            ),
        )
    }

    private fun SampleRepo.toLive(now: Instant) = LiveRepository(
        id = id,
        ref = ref,
        isPrivate = isPrivate,
        isArchived = isArchived,
        pushedAt = pushedAgo?.let { now.minus(it) },
    )

    private fun issueRows(repo: SampleRepo, repoIndex: Int, now: Instant): List<LiveIssue> =
        (0 until repo.issueCount).map { i ->
            val seed = repoIndex * 3 + i
            LiveIssue(
                number = repo.firstNumber + repo.pullRequestCount + i,
                title = pick(Fixtures.TITLE_POOL, seed),
                author = VIEWER_LOGIN,
                assignee = if (i % 2 == 0) VIEWER_LOGIN else null,
                labels = labels(seed, count = 1 + i % 2),
                commentCount = (seed * 3) % 7,
                updatedAt = rowUpdatedAt(repo, i, now),
                htmlUrl = "",
            )
        }

    private fun pullRequestRows(repo: SampleRepo, repoIndex: Int, now: Instant): List<LivePullRequest> =
        (0 until repo.pullRequestCount).map { j ->
            val seed = repoIndex * 5 + j + Fixtures.TITLE_POOL.size / 2
            LivePullRequest(
                number = repo.firstNumber + j,
                title = pick(Fixtures.TITLE_POOL, seed),
                author = VIEWER_LOGIN,
                assignee = null,
                labels = labels(seed, count = j % 2),
                isDraft = j == 1,
                reviewRequestedFromViewer = false,
                updatedAt = rowUpdatedAt(repo, j, now),
                htmlUrl = "",
            )
        }

    private fun labels(seed: Int, count: Int): List<String> =
        (0 until count).map { pick(Fixtures.LABEL_POOL, seed + it * 3) }.distinct()

    private fun rowUpdatedAt(repo: SampleRepo, index: Int, now: Instant): Instant {
        val latest = repo.pushedAgo ?: Duration.ofDays(30)
        return now.minus(latest).minus(Duration.ofMinutes(37L * index))
    }

    private fun <T> pick(pool: List<T>, seed: Int): T = pool[Math.floorMod(seed, pool.size)]
}
