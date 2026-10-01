package co.saari.repoglance.ui.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import co.saari.repoglance.widget.RepoWidgetConfigActivity
import co.saari.repoglance.widget.RepoWidgetReceiver
import co.saari.repoglance.widget.StackWidgetReceiver
import co.saari.repoglance.widget.WidgetPreviewKind
import co.saari.repoglance.widget.WidgetSheetPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

object WidgetPinning {
    private const val REPO_WIDGET_CONFIG_REQUEST_CODE = 1101

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    suspend fun request(context: Context, kind: WidgetKind): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val glance = GlanceAppWidgetManager(context)
        return try {
            withContext(Dispatchers.Default) { pin(context, glance, kind) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            withoutPreview(manager, context, kind)
        }
    }

    private fun withoutPreview(manager: AppWidgetManager, context: Context, kind: WidgetKind): Boolean =
        try {
            manager.requestPinAppWidget(provider(context, kind), null, callback(context, kind))
        } catch (_: Exception) {
            false
        }

    private suspend fun pin(context: Context, glance: GlanceAppWidgetManager, kind: WidgetKind): Boolean =
        when (kind) {
            WidgetKind.REPOSITORY -> glance.requestPinGlanceAppWidget(
                receiver = RepoWidgetReceiver::class.java,
                preview = WidgetSheetPreview(WidgetPreviewKind.REPOSITORY),
                previewState = null,
                successCallback = callback(context, kind),
            )
            WidgetKind.PINNED_REPOS -> glance.requestPinGlanceAppWidget(
                receiver = StackWidgetReceiver::class.java,
                preview = WidgetSheetPreview(WidgetPreviewKind.PINNED_REPOS),
                previewState = null,
                successCallback = callback(context, kind),
            )
        }

    private fun provider(context: Context, kind: WidgetKind): ComponentName = when (kind) {
        WidgetKind.REPOSITORY -> ComponentName(context, RepoWidgetReceiver::class.java)
        WidgetKind.PINNED_REPOS -> ComponentName(context, StackWidgetReceiver::class.java)
    }

    private fun callback(context: Context, kind: WidgetKind): PendingIntent? = when (kind) {
        WidgetKind.REPOSITORY -> PendingIntent.getActivity(
            context,
            REPO_WIDGET_CONFIG_REQUEST_CODE,
            Intent(context, RepoWidgetConfigActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        WidgetKind.PINNED_REPOS -> null
    }

    fun setupIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, RepoWidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
