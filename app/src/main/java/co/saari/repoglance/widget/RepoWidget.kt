package co.saari.repoglance.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.saari.repoglance.MainActivity
import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.AppPrefs
import java.time.Instant

/**
 * Independently configured per-repository widget.
 *
 * A compact 2x1 placement is a count summary. Any placement tall enough for
 * rows becomes a single recently-updated feed whose entries are individually
 * labeled ISSUE or PR. Header/summary taps open RepoGlance at this widget's
 * repository and mode; row taps open the exact GitHub URL through Android's
 * verified-link routing.
 */
class RepoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT_SIZE, NARROW_TALL_SIZE, WIDE_TALL_SIZE),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            // Glance keeps a composition alive briefly after the first render.
            // Read storage inside the composition so an explicit update after
            // configuration/reconfiguration sees the newly persisted values.
            val config = RepoWidgetConfigStore.load(context, appWidgetId)
            val now = Instant.now()
            val scenario = AppPrefs.selectedScenario(context)
            val snapshot = config?.let { WidgetFixtureData.snapshotFor(it.repo, scenario, now) }
            val rows = config?.let { WidgetFixtureData.recentRows(it.repo, it.mode, now) }.orEmpty()
            val appIntent = config?.let { navigatorIntent(context, it) }

            GlanceTheme {
                val isTall = LocalSize.current.height >= TALL_BREAKPOINT
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background),
                ) {
                    when {
                        config == null || snapshot == null || appIntent == null -> UnconfiguredContent()
                        isTall -> TallContent(config, snapshot, rows, now, appIntent)
                        else -> CompactContent(config, snapshot, now, appIntent)
                    }
                }
            }
        }
    }

    companion object {
        private val COMPACT_SIZE = DpSize(120.dp, 64.dp)
        private val NARROW_TALL_SIZE = DpSize(120.dp, 120.dp)
        private val WIDE_TALL_SIZE = DpSize(250.dp, 140.dp)
        private val TALL_BREAKPOINT = 100.dp
    }
}

private fun navigatorIntent(context: Context, config: RepoWidgetConfig): Intent =
    Intent(context, MainActivity::class.java).apply {
        data = Uri.Builder()
            .scheme("repoglance")
            .authority("navigator")
            .appendPath(config.repo.full)
            .appendQueryParameter("mode", config.mode.name)
            .build()
        putExtra(EXTRA_REPO_FULL, config.repo.full)
        putExtra(EXTRA_NAVIGATOR_MODE, config.mode.name)
    }

private fun githubIntent(row: WidgetRow): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(row.url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

internal fun widgetCountSummary(snapshot: RepoSnapshot, mode: NavigatorMode): String = when (mode) {
    NavigatorMode.ISSUES -> "ISSUES " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis)
    NavigatorMode.PRS -> "PRS " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis)
    NavigatorMode.BOTH ->
        "ISSUES " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis) +
            " · PRS " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis)
}

@Composable
private fun UnconfiguredContent() {
    Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
        Text(
            "Choose a repository",
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
        )
        Text(
            "Long-press · Reconfigure",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun CompactContent(
    config: RepoWidgetConfig,
    snapshot: RepoSnapshot,
    now: Instant,
    appIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(appIntent))
            .padding(8.dp),
    ) {
        Text(
            config.repo.full,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
        )
        Text(
            widgetCountSummary(snapshot, config.mode),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onBackground),
        )
        Text(
            Ages.updatedLabel(snapshot.observedAt, now),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun TallContent(
    config: RepoWidgetConfig,
    snapshot: RepoSnapshot,
    rows: List<WidgetRow>,
    now: Instant,
    appIntent: Intent,
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .clickable(actionStartActivity(appIntent))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                config.repo.full,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontWeight = FontWeight.Bold),
            )
            Text(
                widgetCountSummary(snapshot, config.mode) + " · " + Ages.updatedLabel(snapshot.observedAt, now),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            if (rows.isEmpty()) {
                item {
                    Text(
                        "No recent rows",
                        modifier = GlanceModifier.padding(10.dp),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
            } else {
                items(rows.take(MAX_WIDGET_ROWS).size) { index ->
                    WidgetFeedRow(rows[index])
                }
            }
        }
    }
}

private const val MAX_WIDGET_ROWS = 10

@Composable
private fun WidgetFeedRow(row: WidgetRow) {
    val tapAction = actionStartActivity(githubIntent(row))
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(tapAction)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${row.kind.name} #${row.number}",
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            Sanitize.displayText(row.title),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onBackground),
        )
    }
}
