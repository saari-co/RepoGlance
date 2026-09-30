package co.saari.repoglance.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.SampleModeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RepoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RepoWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val removed = appWidgetIds.toList().mapNotNull { RepoWidgetConfigStore.load(context, it)?.repo?.full }
        appWidgetIds.forEach { RepoWidgetConfigStore.remove(context, it) }
        AppPrefs.removeLivePins(
            context,
            WidgetPins.releasedRepositories(removed, RepoWidgetConfigStore.configuredRepos(context)),
        )
        val removedSample = appWidgetIds.toList().mapNotNull { SampleModeStore.widgetConfig(context, it)?.repo?.full }
        SampleModeStore.removeWidgetConfigs(context, appWidgetIds.toList())
        SampleModeStore.removePins(
            context,
            WidgetPins.releasedRepositories(removedSample, SampleModeStore.widgetRepos(context)),
        )
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { WidgetRefresh.updateStacks(app) }
    }
}
