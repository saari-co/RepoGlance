package co.saari.repoglance.widget

import co.saari.repoglance.data.CatalogSort
import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.data.findRepositoryByName
import co.saari.repoglance.data.orderRepositories
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.sample.SampleAccount
import java.time.Instant

internal const val SAMPLE_TIME_LABEL = "sample"

object SampleWidgetData {

    fun names(now: Instant): List<String> =
        orderRepositories(repositories(now), CatalogSort.RECENT).map { it.ref.full }

    fun configurationList(pins: Set<String>, now: Instant): List<RepoRef> {
        val names = names(now)
        return WidgetPins.configurationList(names, pins.filter { it in names }.toSet())
    }

    fun pushedAt(now: Instant): Map<String, Instant> =
        repositories(now).mapNotNull { repo -> repo.pushedAt?.let { repo.ref.full to it } }.toMap()

    fun snapshot(ref: RepoRef, now: Instant): RepoSnapshot? {
        val repository = findRepositoryByName(repositories(now), ref.full) ?: return null
        val content = SampleAccount.content(repository, now)
        val issues = (content.issues as? GitHubApiResult.Success)?.value?.rows ?: return null
        val prs = (content.pullRequests as? GitHubApiResult.Success)?.value?.rows ?: return null
        return RepoSnapshot(
            repo = repository.ref,
            openPrs = prs.size,
            prsAwaitingMyReview = prs.count { it.reviewRequestedFromViewer },
            openIssues = issues.size,
            defaultBranchCi = CiState.UNKNOWN,
            latestRelease = null,
            pushedAt = repository.pushedAt,
            valueBasis = ValueBasis.EXACT,
            observedAt = now,
            rateLimit = RateLimitBucket.UNKNOWN,
        )
    }

    fun rows(ref: RepoRef, now: Instant): List<WidgetRow> {
        val repository = findRepositoryByName(repositories(now), ref.full) ?: return emptyList()
        val content = SampleAccount.content(repository, now)
        val issues = (content.issues as? GitHubApiResult.Success)?.value?.rows.orEmpty()
            .map { WidgetRow(WidgetRowKind.ISSUE, it.number, it.title, it.updatedAt, it.htmlUrl) }
        val prs = (content.pullRequests as? GitHubApiResult.Success)?.value?.rows.orEmpty()
            .map { WidgetRow(WidgetRowKind.PR, it.number, it.title, it.updatedAt, it.htmlUrl) }
        return (issues + prs).sortedByDescending { it.updatedAt }
    }

    private fun repositories(now: Instant): List<LiveRepository> = SampleAccount.catalog(now).repositories
}
