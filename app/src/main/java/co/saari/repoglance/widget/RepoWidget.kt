package co.saari.repoglance.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
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
import co.saari.repoglance.link.GitHubAppLauncher
import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.ClockLabel
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.LiveRowsStore
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.state.RateLimitStore
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class RepoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT_SIZE, NARROW_TALL_SIZE, WIDE_TALL_SIZE),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        val read = { readRepoWidgetData(context, appWidgetId) }
        val initial = readWidgetStores(read)

        provideContent {
            val data = redrawnWidgetData(initial, read)
            val config = data.config
            val appIntent = config?.let { liveRepositoryIntent(context, it.repo) }

            GlanceTheme {
                val isTall = LocalSize.current.height >= TALL_BREAKPOINT
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background),
                ) {
                    when {
                        config == null || appIntent == null -> UnconfiguredContent()
                        isTall -> TallContent(config, data.snapshot, data.rows, data.freshness, appIntent)
                        else -> CompactContent(config, data.snapshot, appIntent, data.freshness)
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

internal data class RepoWidgetData(
    val config: RepoWidgetConfig?,
    val snapshot: RepoSnapshot?,
    val rows: List<WidgetRow>,
    val freshness: WidgetFreshness,
)

internal fun readRepoWidgetData(context: Context, appWidgetId: Int): RepoWidgetData {
    val config = RepoWidgetConfigStore.load(context, appWidgetId)
    val now = Instant.now()
    return RepoWidgetData(
        config = config,
        snapshot = config?.let { LiveSnapshotStore.load(context, it.repo) },
        rows = config?.let { rowsForMode(LiveRowsStore.load(context, it.repo), it.mode) }.orEmpty(),
        freshness = WidgetFreshness(
            now = now,
            clock = widgetClock(context),
            rateLimitedUntil = RateLimitStore.exhaustedUntil(RateLimitStore.load(context), now),
        ),
    )
}

internal fun liveRepositoryIntent(context: Context, repo: RepoRef): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        data = Uri.Builder()
            .scheme("repoglance")
            .authority("live")
            .appendPath(repo.full)
            .build()
        putExtra(EXTRA_LIVE_REPO_FULL, repo.full)
    }

internal data class WidgetFreshness(
    val now: Instant,
    val clock: ClockLabel,
    val rateLimitedUntil: Instant?,
)

internal fun widgetClock(context: Context): ClockLabel = ClockLabel(
    zone = ZoneId.systemDefault(),
    is24Hour = DateFormat.is24HourFormat(context),
    locale = Locale.getDefault(),
)

private fun githubIntent(row: WidgetRow): Intent = GitHubAppLauncher.intent(row.url, adjacent = false)

internal fun widgetCountSummary(snapshot: RepoSnapshot, mode: NavigatorMode): String = when (mode) {
    NavigatorMode.ISSUES -> "ISSUES " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis)
    NavigatorMode.PRS -> "PRS " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis)
    NavigatorMode.BOTH ->
        "ISSUES " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis) +
            " · PRS " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis)
}

internal const val WIDGET_PREVIEW_LABEL = "FIXTURE PREVIEW"

private val LEDGER_REPO_SIZE = 10.sp
private val LEDGER_LABEL_SIZE = 8.sp
private val LEDGER_VALUE_SIZE = 11.sp

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

internal fun compactFreshnessLabel(snapshot: RepoSnapshot?, freshness: WidgetFreshness): String {
    val observedAt = snapshot?.observedAt
    if (snapshot == null || observedAt == null || snapshot.valueBasis == ValueBasis.UNKNOWN) return "no data"
    val clock = freshness.clock.format(observedAt, freshness.now)
    return when {
        freshness.rateLimitedUntil != null -> "rate limited · $clock"
        snapshot.valueBasis == ValueBasis.LAST_GOOD -> "last good $clock"
        else -> clock
    }
}

internal const val LEDGER_FRESHNESS_TAG = "ledger-freshness"

@Composable
internal fun CompactFreshness(snapshot: RepoSnapshot?, freshness: WidgetFreshness) {
    val stale = snapshot?.valueBasis != ValueBasis.EXACT || freshness.rateLimitedUntil != null
    Text(
        compactFreshnessLabel(snapshot, freshness),
        maxLines = 1,
        style = TextStyle(
            color = if (stale) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
            fontSize = LEDGER_LABEL_SIZE,
            fontWeight = if (stale) FontWeight.Bold else FontWeight.Normal,
        ),
        modifier = GlanceModifier.semantics { testTag = LEDGER_FRESHNESS_TAG },
    )
}

@Composable
internal fun CompactContent(
    config: RepoWidgetConfig,
    snapshot: RepoSnapshot?,
    appIntent: Intent,
    freshness: WidgetFreshness,
) {
    val basis = snapshot?.valueBasis ?: ValueBasis.UNKNOWN

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(appIntent))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                config.repo.name,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = LEDGER_REPO_SIZE,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            CompactFreshness(snapshot, freshness)
        }
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
    snapshot: RepoSnapshot?,
    rows: List<WidgetRow>,
    freshness: WidgetFreshness,
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
                tallHeaderLabel(snapshot, config.mode, freshness),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.semantics { testTag = TALL_HEADER_TAG },
            )
        }
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            if (rows.isEmpty()) {
                item {
                    Text(
                        NO_ROWS_LABEL,
                        modifier = GlanceModifier.padding(10.dp),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
            } else {
                items(rows.take(MAX_WIDGET_ROWS).size) { index ->
                    WidgetFeedRow(rows[index], freshness.now)
                }
            }
        }
    }
}

private const val MAX_WIDGET_ROWS = 10
internal const val TALL_HEADER_TAG = "tall-header"
internal const val NO_ROWS_LABEL = "No saved rows · open RepoGlance to load"

internal fun tallHeaderLabel(snapshot: RepoSnapshot?, mode: NavigatorMode, freshness: WidgetFreshness): String {
    val observedAt = snapshot?.observedAt
    if (snapshot == null || observedAt == null || snapshot.valueBasis == ValueBasis.UNKNOWN) {
        return "no data · open RepoGlance to load"
    }
    val limited = freshness.rateLimitedUntil
        ?.let { "rate limited · resets " + freshness.clock.time(it) + " · " }
        .orEmpty()
    val basis = if (snapshot.valueBasis == ValueBasis.LAST_GOOD) "last good · " else ""
    val age = " · as of " + freshness.clock.format(observedAt, freshness.now)
    return limited + basis + widgetCountSummary(snapshot, mode) + age
}

@Composable
private fun WidgetFeedRow(row: WidgetRow, now: Instant) {
    val tapAction = actionStartActivity(githubIntent(row))
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(tapAction)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            "${row.kind.name} #${row.number} \u00b7 ${Ages.format(row.updatedAt, now)}",
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
        )
        Text(
            Sanitize.displayText(row.title),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onBackground),
        )
    }
}
