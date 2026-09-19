package co.saari.repoglance.refresh

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.saari.repoglance.data.LiveGitHub
import co.saari.repoglance.data.LiveRefresh
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.data.sessionInvalidationFailure
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.CatalogNamesStore
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.state.RateLimitStore
import co.saari.repoglance.widget.RepoWidgetConfigStore
import co.saari.repoglance.widget.WidgetRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class PinnedRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val ran = withContext(Dispatchers.IO) { refreshPinnedSet(context) }
        if (ran) WidgetRefresh.updateAll(context)
        return Result.success()
    }

    private fun refreshPinnedSet(context: Context): Boolean {
        val services = LiveGitHub.services(context)
        val viewerLogin = CatalogNamesStore.viewerLogin(context)
        if (viewerLogin == null || !services.session.hasSavedSession()) return false
        val ordered = RefreshPlan.order(AppPrefs.livePins(context)) { LiveSnapshotStore.load(context, it)?.observedAt }
        RefreshPlan.execute(
            ordered = ordered,
            widgetRepos = RepoWidgetConfigStore.configuredRepos(context).toSet(),
            initialBucket = RateLimitStore.effectiveBucket(RateLimitStore.load(context), Instant.now()),
        ) { ref -> refreshOne(context, ref, viewerLogin) }
        return true
    }

    private fun refreshOne(context: Context, ref: RepoRef, viewerLogin: String): RefreshStep {
        val services = LiveGitHub.services(context)
        val repository = LiveRepository(
            id = 0L,
            ref = ref,
            isPrivate = false,
            isArchived = false,
            pushedAt = LiveSnapshotStore.load(context, ref)?.pushedAt,
        )
        val content = services.apiClient.loadRepositoryContent(repository, viewerLogin)
        if (isStopped || content.sessionInvalidationFailure() != null || !services.session.hasSavedSession()) {
            return RefreshStep(RateLimitBucket.UNKNOWN, stop = true)
        }
        val bucket = LiveRefresh.persist(context, services.apiClient, repository, content)?.bucket
            ?: RateLimitBucket.UNKNOWN
        return RefreshStep(bucket, stop = bucket == RateLimitBucket.EXHAUSTED)
    }
}
