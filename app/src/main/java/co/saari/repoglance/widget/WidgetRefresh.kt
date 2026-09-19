package co.saari.repoglance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

internal suspend fun <T> readWidgetStores(read: () -> T): T = withContext(Dispatchers.IO) { read() }

@Composable
internal fun <T> redrawnWidgetData(initial: T, read: () -> T): T {
    val currentRead by rememberUpdatedState(read)
    var data by remember { mutableStateOf(initial) }
    LaunchedEffect(currentState(WidgetRefresh.REDRAW_KEY)) { data = readWidgetStores(currentRead) }
    return data
}
