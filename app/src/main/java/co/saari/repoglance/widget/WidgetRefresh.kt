package co.saari.repoglance.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

object WidgetRefresh {
    internal val REDRAW_KEY = longPreferencesKey("redraw")

    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(RepoWidget::class.java).forEach { redraw(context, RepoWidget(), it) }
        manager.getGlanceIds(StackWidget::class.java).forEach { redraw(context, StackWidget(), it) }
    }

    suspend fun redraw(context: Context, glanceId: GlanceId) = redraw(context, RepoWidget(), glanceId)

    private suspend fun redraw(context: Context, widget: GlanceAppWidget, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { state -> state[REDRAW_KEY] = (state[REDRAW_KEY] ?: 0L) + 1L }
        widget.update(context, glanceId)
    }
}
