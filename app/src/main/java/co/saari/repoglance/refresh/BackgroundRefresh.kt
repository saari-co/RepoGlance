package co.saari.repoglance.refresh

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

object BackgroundRefresh {
    const val PERIODIC_WORK = "pinned-refresh-periodic"
    const val ONE_TIME_WORK = "pinned-refresh-now"
    val INTERVAL: Duration = Duration.ofMinutes(30)

    private val constraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PinnedRefreshWorker>(INTERVAL)
                .setConstraints(constraints)
                .build(),
        )
    }

    fun refreshNow(context: Context) {
        schedule(context)
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PinnedRefreshWorker>()
                .setConstraints(constraints)
                .build(),
        )
    }

    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONE_TIME_WORK)
    }
}
