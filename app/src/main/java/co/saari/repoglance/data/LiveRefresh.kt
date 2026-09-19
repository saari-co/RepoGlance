package co.saari.repoglance.data

import android.content.Context
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.state.LiveRowsStore
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.state.RateLimitStore
import java.time.Instant

object LiveRefresh {

    data class Persisted(val rateLimit: RateLimitSnapshot?)

    fun persist(
        context: Context,
        services: LiveGitHub.Services,
        sessionGeneration: Long,
        repository: LiveRepository,
        content: LiveRepositoryContent,
    ): Persisted? {
        val issues = (content.issues as? GitHubApiResult.Success)?.value
        val prSuccess = content.pullRequests as? GitHubApiResult.Success
        val metadataResult = if (LiveSnapshotFactory.needsRepositoryMetadata(issues, prSuccess?.value)) {
            services.apiClient.loadRepositoryMetadata(repository)
        } else {
            null
        }
        val metadata = metadataResult as? GitHubApiResult.Success
        val snapshot = LiveSnapshotFactory.build(
            repository = repository,
            metadata = metadata?.value,
            issues = issues,
            pullRequests = prSuccess?.value,
            previous = LiveSnapshotStore.load(context, repository.ref),
            observedAt = metadata?.observedAt ?: prSuccess?.observedAt ?: Instant.now(),
            rateLimit = metadata?.rateLimit?.bucket
                ?: prSuccess?.rateLimit?.bucket
                ?: RateLimitBucket.UNKNOWN,
        )
        val rows = LiveRowsStore.replacementRows(issues?.rows, prSuccess?.value?.rows)
        val rateLimit = latestRateLimit(listOfNotNull(content.issues, content.pullRequests, metadataResult))
        return services.session.commitIfCurrent(sessionGeneration) {
            LiveSnapshotStore.save(context, snapshot)
            rows?.let { LiveRowsStore.save(context, repository.ref, it) }
            rateLimit?.let { RateLimitStore.record(context, it, Instant.now()) }
            Persisted(rateLimit)
        }
    }

    internal fun latestRateLimit(results: List<GitHubApiResult<*>>): RateLimitSnapshot? = results
        .map { it.rateLimit() }
        .lastOrNull { it.bucket != RateLimitBucket.UNKNOWN }

    private fun GitHubApiResult<*>.rateLimit(): RateLimitSnapshot = when (this) {
        is GitHubApiResult.Success -> rateLimit
        is GitHubApiResult.Failure -> rateLimit
    }
}
