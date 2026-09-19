package co.saari.repoglance.refresh

import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.time.Instant

data class RefreshStep(
    val bucket: RateLimitBucket,
    val stop: Boolean,
)

object RefreshPlan {
    const val MAX_PER_RUN = 20

    fun order(pins: Collection<String>, observedAt: (RepoRef) -> Instant?): List<RepoRef> = pins
        .mapNotNull(::parseRef)
        .distinct()
        .sortedWith(compareBy<RepoRef, Instant?>(nullsFirst()) { observedAt(it) }.thenBy { it.full })

    fun execute(
        ordered: List<RepoRef>,
        widgetRepos: Set<String>,
        initialBucket: RateLimitBucket,
        refreshOne: (RepoRef) -> RefreshStep,
    ): List<RepoRef> {
        val refreshed = mutableListOf<RepoRef>()
        var bucket = initialBucket
        var stopped = false
        val queue = ordered.iterator()
        fun budgetLeft() = refreshed.size < MAX_PER_RUN && bucket != RateLimitBucket.EXHAUSTED
        while (!stopped && budgetLeft() && queue.hasNext()) {
            val repo = queue.next()
            if (bucket != RateLimitBucket.LOW || repo.full in widgetRepos) {
                val step = refreshOne(repo)
                refreshed += repo
                stopped = step.stop
                if (step.bucket != RateLimitBucket.UNKNOWN) bucket = step.bucket
            }
        }
        return refreshed.toList()
    }

    private fun parseRef(full: String): RepoRef? {
        val parts = full.split('/', limit = 2)
        if (parts.size != 2) return null
        return runCatching { RepoRef(parts[0], parts[1]) }.getOrNull()
    }
}
