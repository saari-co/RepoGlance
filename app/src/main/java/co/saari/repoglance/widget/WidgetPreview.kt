package co.saari.repoglance.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.collection.intSetOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.GlanceStateDefinition
import co.saari.repoglance.BuildConfig
import co.saari.repoglance.MainActivity
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.ClockLabel
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

enum class WidgetPreviewKind { REPOSITORY, PINNED_REPOS }

internal data class WidgetPreviewData(
    val config: RepoWidgetConfig,
    val snapshot: RepoSnapshot?,
    val entries: List<StackEntry>,
    val freshness: WidgetFreshness,
)

object WidgetPreviews {
    val REPOSITORY: RepoRef = RepoRef("saari-co", "rocket")
    val PINS: List<RepoRef> = listOf(REPOSITORY, RepoRef("saari-co", "api-server"), RepoRef("dinkuskit", "infra"))
    const val LOOK_VERSION = 1

    internal fun data(context: Context, now: Instant = Instant.now()): WidgetPreviewData =
        data(now, widgetClock(context))

    internal fun data(now: Instant, clock: ClockLabel): WidgetPreviewData =
        WidgetPreviewData(
            config = RepoWidgetConfig(REPOSITORY, NavigatorMode.BOTH),
            snapshot = SampleWidgetData.snapshot(REPOSITORY, now),
            entries = StackRows.order(
                pins = PINS.map { it.full },
                catalogPushedAt = SampleWidgetData.pushedAt(now),
            ) { SampleWidgetData.snapshot(it, now) },
            freshness = WidgetFreshness(now = now, clock = clock, rateLimitedUntil = null, sample = true),
        )

    fun stamp(): String = "${BuildConfig.VERSION_CODE}.$LOOK_VERSION"

    internal fun isDue(published: Boolean, stored: String?, stamp: String): Boolean = !published || stored != stamp

    suspend fun publishIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val glance = GlanceAppWidgetManager(context)
        val due = RECEIVERS.filter { receiver ->
            val published = attempt { publishedState(context, receiver) }
            published != null && isDue(published, prefs.getString(receiver.java.name, null), stamp())
        }
        due.forEach { receiver ->
            val result = attempt {
                glance.setWidgetPreviews(receiver, intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN))
            }
            if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                prefs.edit { putString(receiver.java.name, stamp()) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun publishedState(context: Context, receiver: KClass<out GlanceAppWidgetReceiver>): Boolean? {
        val info = AppWidgetManager.getInstance(context)
            .getInstalledProvidersForPackage(context.packageName, null)
            .firstOrNull { it.provider == ComponentName(context, receiver.java) }
            ?: return null
        return info.generatedPreviewCategories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    private const val PREFS_NAME = "widget_previews"
    private val RECEIVERS: List<KClass<out GlanceAppWidgetReceiver>> =
        listOf(RepoWidgetReceiver::class, StackWidgetReceiver::class)
}

internal suspend fun GlanceAppWidget.provideWidgetPreview(context: Context, kind: WidgetPreviewKind) {
    val tones = WidgetTones.of(context)
    val data = WidgetPreviews.data(context)
    val intent = Intent(context, MainActivity::class.java)
    provideContent { WidgetPreviewContent(kind, data, tones, intent) }
}

@Composable
internal fun WidgetPreviewContent(
    kind: WidgetPreviewKind,
    data: WidgetPreviewData,
    tones: WidgetTones,
    intent: Intent,
) {
    CompositionLocalProvider(LocalWidgetTones provides tones) {
        when (kind) {
            WidgetPreviewKind.REPOSITORY -> GlanceTheme {
                Box(modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background)) {
                    CompactContent(data.config, data.snapshot, intent, data.freshness)
                }
            }
            WidgetPreviewKind.PINNED_REPOS -> TallLook {
                GlanceTheme {
                    Column(modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background)) {
                        StackHeader(
                            stackHeaderLabel(data.entries.size, data.freshness),
                            intent,
                            null,
                            sampleMarker(data.freshness),
                        )
                        data.entries.forEach { StackRow(intent, it, data.freshness) }
                    }
                }
            }
        }
    }
}

class WidgetSheetPreview(private val kind: WidgetPreviewKind) : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*>? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideWidgetPreview(context, kind)
}
