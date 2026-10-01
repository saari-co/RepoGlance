package co.saari.repoglance.ui.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import co.saari.repoglance.widget.RepoWidgetConfigActivity
import co.saari.repoglance.widget.RepoWidgetReceiver
import co.saari.repoglance.widget.StackWidgetReceiver

object WidgetPinning {
    private const val REPO_WIDGET_CONFIG_REQUEST_CODE = 1101

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun request(context: Context, kind: WidgetKind): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return when (kind) {
            WidgetKind.REPOSITORY -> manager.requestPinAppWidget(
                ComponentName(context, RepoWidgetReceiver::class.java),
                null,
                PendingIntent.getActivity(
                    context,
                    REPO_WIDGET_CONFIG_REQUEST_CODE,
                    Intent(context, RepoWidgetConfigActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                ),
            )
            WidgetKind.PINNED_REPOS -> manager.requestPinAppWidget(
                ComponentName(context, StackWidgetReceiver::class.java),
                null,
                null,
            )
        }
    }

    fun setupIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, RepoWidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
