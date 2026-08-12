package co.saari.repoglance.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Called after a scenario or pin change so home-screen widgets track the
 * in-app fixture state immediately — fixtures don't otherwise refresh
 * (`updatePeriodMillis` is 0 in both widget-info XMLs).
 */
object WidgetRefresh {
    suspend fun updateAll(context: Context) {
        RepoWidget().updateAll(context)
        StackWidget().updateAll(context)
    }
}
