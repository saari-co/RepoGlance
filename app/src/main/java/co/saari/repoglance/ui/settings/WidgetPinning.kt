package co.saari.repoglance.ui.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import co.saari.repoglance.widget.RepoWidgetConfigActivity
import co.saari.repoglance.widget.RepoWidgetReceiver
import co.saari.repoglance.widget.StackWidgetReceiver
import co.saari.repoglance.widget.WidgetPreviewKind
import co.saari.repoglance.widget.WidgetSheetPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            false
        }
    }

    private suspend fun pin(context: Context, glance: GlanceAppWidgetManager, kind: WidgetKind): Boolean =
        when (kind) {
            WidgetKind.REPOSITORY -> glance.requestPinGlanceAppWidget(
                receiver = RepoWidgetReceiver::class.java,
                preview = WidgetSheetPreview(WidgetPreviewKind.REPOSITORY),
                previewState = null,
                successCallback = PendingIntent.getActivity(
                    context,
                    REPO_WIDGET_CONFIG_REQUEST_CODE,
                    Intent(context, RepoWidgetConfigActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                ),
            )
            WidgetKind.PINNED_REPOS -> glance.requestPinGlanceAppWidget(
                receiver = StackWidgetReceiver::class.java,
                preview = WidgetSheetPreview(WidgetPreviewKind.PINNED_REPOS),
                previewState = null,
                successCallback = null,
            )
        }

    fun setupIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, RepoWidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
