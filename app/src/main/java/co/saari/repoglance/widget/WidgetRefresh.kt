package co.saari.repoglance.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

object WidgetRefresh {
    internal val REDRAW_KEY = longPreferencesKey("redraw")

    suspend fun updateAll(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(RepoWidget::class.java).forEach { redraw(context, it) }
        StackWidget().updateAll(context)
    }

    suspend fun redraw(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { state -> state[REDRAW_KEY] = (state[REDRAW_KEY] ?: 0L) + 1L }
        RepoWidget().update(context, glanceId)
    }
}
