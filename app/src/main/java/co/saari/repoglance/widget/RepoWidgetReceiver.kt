package co.saari.repoglance.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import co.saari.repoglance.state.AppPrefs

class RepoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RepoWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val removed = appWidgetIds.toList().mapNotNull { RepoWidgetConfigStore.load(context, it)?.repo?.full }
        appWidgetIds.forEach { RepoWidgetConfigStore.remove(context, it) }
        AppPrefs.removeLivePins(
            context,
            WidgetPins.releasedRepositories(removed, RepoWidgetConfigStore.configuredRepos(context)),
        )
        super.onDeleted(context, appWidgetIds)
    }
}
