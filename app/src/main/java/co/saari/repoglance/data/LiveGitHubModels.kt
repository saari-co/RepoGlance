package co.saari.repoglance.data

import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.time.Instant

data class GitHubViewer(
    val login: String,
    val avatarUrl: String?,
)

data class GitHubInstallation(
    val id: Long,
    val ownerLogin: String?,
)

data class LiveRepository(
    val id: Long,
    val ref: RepoRef,
    val isPrivate: Boolean,
    val isArchived: Boolean,
    val pushedAt: Instant?,
)

data class LiveIssue(
    val number: Int,
    val title: String,
    val author: String,
    val assignee: String?,
    val labels: List<String>,
    val commentCount: Int?,
    val updatedAt: Instant,
    val htmlUrl: String,
)

data class LivePullRequest(
    val number: Int,
    val title: String,
    val author: String,
    val assignee: String?,
    val labels: List<String>,
    val isDraft: Boolean,
    val reviewRequestedFromViewer: Boolean,
    val updatedAt: Instant,
    val htmlUrl: String,
)

data class RateLimitSnapshot(
    val bucket: RateLimitBucket,
    val remaining: Int?,
    val limit: Int?,
    val resetsAt: Instant?,
)

sealed class GitHubApiResult<out T> {
    data class Success<T>(
        val value: T,
        val observedAt: Instant,
        val rateLimit: RateLimitSnapshot,
    ) : GitHubApiResult<T>()

    data class Failure(
        val message: String,
        val statusCode: Int?,
        val rateLimit: RateLimitSnapshot,
        val needsNewSignIn: Boolean = false,
    ) : GitHubApiResult<Nothing>()
}

data class LiveRepositoryCatalog(
    val viewer: GitHubViewer,
    val installations: List<GitHubInstallation>,
    val repositories: List<LiveRepository>,
)

data class LiveRepositoryContent(
    val repository: LiveRepository,
    val issues: GitHubApiResult<LivePage<LiveIssue>>,
    val pullRequests: GitHubApiResult<LivePage<LivePullRequest>>,
)

data class LivePage<T>(
    val rows: List<T>,
    val hasMorePages: Boolean,
)
