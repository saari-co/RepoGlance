package co.saari.repoglance.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetRefresh {
    suspend fun updateAll(context: Context) {
        RepoWidget().updateAll(context)
        StackWidget().updateAll(context)
    }
}
