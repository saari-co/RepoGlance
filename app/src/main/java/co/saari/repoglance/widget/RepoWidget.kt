package co.saari.repoglance.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.saari.repoglance.MainActivity
import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.LiveSnapshotStore
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
            // Compact renders live counts published by the app; the tall row
            // feed is still fixture-backed and says so (widget-content-priority-008
            // is scoped to the compact slot).
            val liveSnapshot = config?.let { LiveSnapshotStore.load(context, it.repo) }
            val fixtureSnapshot = config?.let { WidgetFixtureData.snapshotFor(it.repo, scenario, now) }
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
                        config == null || appIntent == null -> UnconfiguredContent()
                        isTall && fixtureSnapshot != null ->
                            TallContent(config, fixtureSnapshot, rows, now, appIntent)
                        isTall -> UnconfiguredContent()
                        else -> CompactContent(config, liveSnapshot, appIntent)
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

/** Retained for the tall row feed, which still renders fixture rows. */
internal const val WIDGET_PREVIEW_LABEL = "FIXTURE PREVIEW"

private val LEDGER_REPO_SIZE = 10.sp
private val LEDGER_LABEL_SIZE = 8.sp
private val LEDGER_VALUE_SIZE = 11.sp

/** Below this height only the two headline rows fit. */
private val LEDGER_THIRD_ROW_BREAKPOINT = 84.dp

@Composable
private fun UnconfiguredContent() {
    Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
        Text(
            WIDGET_PREVIEW_LABEL,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
        )
        Text(
            "Choose a repository",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
internal fun CompactContent(
    config: RepoWidgetConfig,
    snapshot: RepoSnapshot?,
    appIntent: Intent,
) {
    // No stored observation yet: every count is unknown, never zero.
    val basis = snapshot?.valueBasis ?: ValueBasis.UNKNOWN
    // Right-aligned ledger (GrillTrack widget-content-priority-008): label
    // left, number hard right, so digits line up in a column. The
    // "to review" row is progressive disclosure — it renders only where
    // there is height for it.
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(appIntent))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            config.repo.name,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = LEDGER_REPO_SIZE,
            ),
        )
        LedgerRow("issues", SnapshotRendering.countText(snapshot?.openIssues, basis))
        LedgerRow("PRs", SnapshotRendering.countText(snapshot?.openPrs, basis))
        if (LocalSize.current.height >= LEDGER_THIRD_ROW_BREAKPOINT) {
            LedgerRow(
                "to review",
                SnapshotRendering.countText(snapshot?.prsAwaitingMyReview, basis),
            )
        }
    }
}

internal const val LEDGER_LABEL_TAG = "ledger-label"
internal const val LEDGER_VALUE_TAG = "ledger-value"

@Composable
internal fun LedgerRow(label: String, value: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = LEDGER_LABEL_SIZE,
            ),
            modifier = GlanceModifier
                .semantics { testTag = LEDGER_LABEL_TAG }
                .defaultWeight(),
        )
        Text(
            value,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = LEDGER_VALUE_SIZE,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.semantics { testTag = LEDGER_VALUE_TAG },
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
                "$WIDGET_PREVIEW_LABEL · " + widgetCountSummary(snapshot, config.mode) +
                    " · " + Ages.updatedLabel(snapshot.observedAt, now),
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
