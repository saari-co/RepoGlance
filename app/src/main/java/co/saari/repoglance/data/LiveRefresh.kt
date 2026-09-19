package co.saari.repoglance.data

import android.content.Context
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.state.LiveRowsStore
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.state.RateLimitStore
import java.time.Instant

object LiveRefresh {

    fun persist(
        context: Context,
        apiClient: GitHubApiClient,
        repository: LiveRepository,
        content: LiveRepositoryContent,
    ): RateLimitSnapshot? {
        val issues = (content.issues as? GitHubApiResult.Success)?.value
        val prSuccess = content.pullRequests as? GitHubApiResult.Success
        val metadataResult = if (LiveSnapshotFactory.needsRepositoryMetadata(issues, prSuccess?.value)) {
            apiClient.loadRepositoryMetadata(repository)
        } else {
            null
        }
        val metadata = metadataResult as? GitHubApiResult.Success
        LiveSnapshotFactory.build(
            repository = repository,
            metadata = metadata?.value,
            issues = issues,
            pullRequests = prSuccess?.value,
            previous = LiveSnapshotStore.load(context, repository.ref),
            observedAt = metadata?.observedAt ?: prSuccess?.observedAt ?: Instant.now(),
            rateLimit = metadata?.rateLimit?.bucket
                ?: prSuccess?.rateLimit?.bucket
                ?: RateLimitBucket.UNKNOWN,
        ).also { LiveSnapshotStore.save(context, it) }
        LiveRowsStore.replacementRows(issues?.rows, prSuccess?.value?.rows)?.let { fresh ->
            LiveRowsStore.save(context, repository.ref, fresh)
        }
        return latestRateLimit(listOfNotNull(content.issues, content.pullRequests, metadataResult))
            ?.also { RateLimitStore.record(context, it, Instant.now()) }
    }

    internal fun latestRateLimit(results: List<GitHubApiResult<*>>): RateLimitSnapshot? = results
        .map { it.rateLimit() }
        .lastOrNull { it.bucket != RateLimitBucket.UNKNOWN }

    private fun GitHubApiResult<*>.rateLimit(): RateLimitSnapshot = when (this) {
        is GitHubApiResult.Success -> rateLimit
        is GitHubApiResult.Failure -> rateLimit
    }
}
