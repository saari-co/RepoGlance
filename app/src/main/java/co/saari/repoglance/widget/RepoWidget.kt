package co.saari.repoglance.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import co.saari.repoglance.state.SampleModeStore
import co.saari.repoglance.ui.theme.SampleMarker
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

        val tones = WidgetTones.of(context)
        provideContent {
            val data = redrawnWidgetData(initial, read)
            val config = data.config
            val appIntent = config?.let { liveRepositoryIntent(context, it.repo) }

            CompositionLocalProvider(LocalWidgetTones provides tones) {
                GlanceTheme {
                    val isTall = LocalSize.current.height >= TALL_BREAKPOINT
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.background),
                    ) {
                        when {
                            config == null || appIntent == null ->
                                UnconfiguredContent(widgetSetupIntent(context, appWidgetId))
                            isTall -> TallContent(config, data.snapshot, data.rows, data.freshness, appIntent)
                            else -> CompactContent(config, data.snapshot, appIntent, data.freshness)
                        }
                    }
                }
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) =
        provideWidgetPreview(context, WidgetPreviewKind.REPOSITORY)

    companion object {
        private val COMPACT_SIZE = DpSize(140.dp, 64.dp)
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

internal fun readRepoWidgetData(context: Context, appWidgetId: Int): RepoWidgetData =
    if (SampleModeStore.isActive(context)) {
        readSampleRepoWidgetData(context, appWidgetId)
    } else {
        readLiveRepoWidgetData(context, appWidgetId)
    }

internal fun readSampleRepoWidgetData(context: Context, appWidgetId: Int): RepoWidgetData {
    val config = SampleModeStore.widgetConfig(context, appWidgetId)
    val now = Instant.now()
    val persona = SampleModeStore.persona(context)
    return RepoWidgetData(
        config = config,
        snapshot = config?.let { SampleWidgetData.snapshot(it.repo, now, persona) },
        rows = config?.let { rowsForMode(SampleWidgetData.rows(it.repo, now, persona), it.mode) }.orEmpty(),
        freshness = WidgetFreshness(
            now = now,
            clock = widgetClock(context),
            rateLimitedUntil = null,
            sample = true,
            sampleMarker = SampleModeStore.marker(context),
        ),
    )
}

internal fun readLiveRepoWidgetData(context: Context, appWidgetId: Int): RepoWidgetData {
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

internal fun widgetSetupIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, RepoWidgetConfigActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.Builder()
            .scheme("repoglance")
            .authority("widget-setup")
            .appendPath(appWidgetId.toString())
            .build()
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

internal data class WidgetFreshness(
    val now: Instant,
    val clock: ClockLabel,
    val rateLimitedUntil: Instant?,
    val sample: Boolean = false,
    val sampleMarker: SampleMarker? = null,
) {
    val showsSampleLabel: Boolean get() = sample && sampleMarker != SampleMarker.NONE
}

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

internal const val UNCONFIGURED_TITLE = "RepoGlance"
internal const val UNCONFIGURED_PROMPT = "Tap to choose a repository"

private val LEDGER_REPO_SIZE = 10.sp
private val LEDGER_LABEL_SIZE = 8.sp
private val LEDGER_VALUE_SIZE = 11.sp
private val LEDGER_MERGED_SIZE = 9.sp

private val LEDGER_THIRD_ROW_BREAKPOINT = 84.dp

@Composable
private fun UnconfiguredContent(setupIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(setupIntent))
            .padding(10.dp),
    ) {
        Text(
            UNCONFIGURED_TITLE,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
        )
        Text(
            UNCONFIGURED_PROMPT,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

internal fun compactFreshnessLabel(
    snapshot: RepoSnapshot?,
    freshness: WidgetFreshness,
    short: Boolean = false,
): String {
    val observedAt = snapshot?.observedAt
    if (snapshot == null || observedAt == null || snapshot.valueBasis == ValueBasis.UNKNOWN) return "no data"
    if (freshness.showsSampleLabel) return SAMPLE_TIME_LABEL
    val clock = freshness.clock.format(observedAt, freshness.now)
    return when {
        freshness.rateLimitedUntil != null -> (if (short) "limited · " else "rate limited · ") + clock
        snapshot.valueBasis == ValueBasis.LAST_GOOD -> (if (short) "cached " else "last good ") + clock
        else -> clock
    }
}

internal const val LEDGER_FRESHNESS_TAG = "ledger-freshness"

@Composable
internal fun CompactFreshness(
    snapshot: RepoSnapshot?,
    freshness: WidgetFreshness,
    modifier: GlanceModifier = GlanceModifier,
) {
    FreshnessText(
        compactFreshnessLabel(
            snapshot,
            freshness,
            short = LocalWidgetLook.current.compact == CompactLayout.SHORT_NAME_FIRST,
        ),
        freshnessRole(snapshot, freshness),
        LEDGER_FRESHNESS_TAG,
        modifier = modifier,
        fontSize = LEDGER_LABEL_SIZE,
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
    val layout = LocalWidgetLook.current.compact
    val place = capsulePlace(
        layout,
        stale = freshnessRole(snapshot, freshness) != null,
        narrow = LocalSize.current.width < COMPACT_RESPONSIVE_BREAKPOINT,
    )
    val ownRow = place == CapsulePlace.OWN_ROW
    val bottom = place == CapsulePlace.BOTTOM
    val nameFirst = layout == CompactLayout.SHORT_NAME_FIRST

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(appIntent))
            .padding(horizontal = 8.dp, vertical = if (ownRow || bottom) 3.dp else 6.dp),
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
                    fontFamily = labelFamily(),
                ),
                modifier = if (nameFirst) GlanceModifier else GlanceModifier.defaultWeight(),
            )
            if (!ownRow && !bottom) {
                Spacer(modifier = GlanceModifier.width(4.dp))
                CompactSlot(snapshot, freshness, if (nameFirst) GlanceModifier.defaultWeight() else GlanceModifier)
            }
        }
        if (ownRow) CompactFreshness(snapshot, freshness)
        CompactCounts(snapshot, basis, merged = ownRow && layout == CompactLayout.MERGED_COUNTS)
        if (bottom) CompactFreshness(snapshot, freshness)
    }
}

@Composable
private fun CompactSlot(
    snapshot: RepoSnapshot?,
    freshness: WidgetFreshness,
    modifier: GlanceModifier = GlanceModifier,
) {
    if (sampleMarker(freshness) == SampleMarker.BANNER) {
        SampleCapsule(SAMPLE_TIME_LABEL, modifier)
    } else {
        CompactFreshness(snapshot, freshness, modifier = modifier)
    }
}

internal enum class CapsulePlace { INLINE, OWN_ROW, BOTTOM }

internal fun capsulePlace(layout: CompactLayout, stale: Boolean, narrow: Boolean): CapsulePlace = when {
    !stale -> CapsulePlace.INLINE
    layout == CompactLayout.OWN_ROW || layout == CompactLayout.MERGED_COUNTS -> CapsulePlace.OWN_ROW
    layout == CompactLayout.RESPONSIVE && narrow -> CapsulePlace.BOTTOM
    else -> CapsulePlace.INLINE
}

@Composable
private fun CompactCounts(snapshot: RepoSnapshot?, basis: ValueBasis, merged: Boolean) {
    val tallEnough = LocalSize.current.height >= LEDGER_THIRD_ROW_BREAKPOINT
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        if (merged) {
            MergedCounts(snapshot, basis)
        } else {
            LedgerRow("issues", SnapshotRendering.countText(snapshot?.openIssues, basis))
            LedgerRow("PRs", SnapshotRendering.countText(snapshot?.openPrs, basis))
        }
        if (tallEnough) {
            LedgerRow(
                "to review",
                SnapshotRendering.countText(snapshot?.prsAwaitingMyReview, basis),
            )
        }
    }
}

internal const val LEDGER_MERGED_TAG = "ledger-merged"
private val COMPACT_RESPONSIVE_BREAKPOINT = 180.dp

@Composable
private fun MergedCounts(snapshot: RepoSnapshot?, basis: ValueBasis) {
    Text(
        SnapshotRendering.countText(snapshot?.openIssues, basis) + " issues · " +
            SnapshotRendering.countText(snapshot?.openPrs, basis) + " PRs",
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = LEDGER_MERGED_SIZE,
            fontWeight = FontWeight.Bold,
            fontFamily = labelFamily(),
        ),
        modifier = GlanceModifier.semantics { testTag = LEDGER_MERGED_TAG },
    )
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
                fontFamily = labelFamily(),
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
internal fun TallContent(
    config: RepoWidgetConfig,
    snapshot: RepoSnapshot?,
    rows: List<WidgetRow>,
    freshness: WidgetFreshness,
    appIntent: Intent,
) {
    TallLook { TallBody(config, snapshot, rows, freshness, appIntent) }
}

@Composable
private fun TallBody(
    config: RepoWidgetConfig,
    snapshot: RepoSnapshot?,
    rows: List<WidgetRow>,
    freshness: WidgetFreshness,
    appIntent: Intent,
) {
    val role = freshnessRole(snapshot, freshness)
        .takeIf { LocalWidgetLook.current.freshness != FreshnessStyle.MATERIAL_ERROR }
    val band = sampleMarker(freshness) == SampleMarker.BANNER
    val ink = if (band) GlanceTheme.colors.onTertiaryContainer else headerInk(role)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(if (band) GlanceTheme.colors.tertiaryContainer else headerBackground(role))
                .clickable(actionStartActivity(appIntent))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                config.repo.full,
                maxLines = 1,
                style = TextStyle(color = ink, fontWeight = FontWeight.Bold),
            )
            if (band || LocalWidgetLook.current.freshness == FreshnessStyle.TONE_HEADER) {
                Text(
                    tallHeaderLabel(snapshot, config.mode, freshness),
                    maxLines = 1,
                    style = TextStyle(color = ink, fontFamily = labelFamily()),
                    modifier = GlanceModifier.semantics { testTag = TALL_HEADER_TAG },
                )
            } else {
                FreshnessText(tallHeaderLabel(snapshot, config.mode, freshness), role, TALL_HEADER_TAG)
            }
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
                    val row = rows[index]
                    WidgetFeedRow(row, freshness.now, rowIntent(row, freshness, appIntent))
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
    if (freshness.showsSampleLabel) return widgetCountSummary(snapshot, mode) + " · " + SAMPLE_TIME_LABEL
    val limited = freshness.rateLimitedUntil
        ?.let { "rate limited · resets " + freshness.clock.time(it) + " · " }
        .orEmpty()
    val basis = if (snapshot.valueBasis == ValueBasis.LAST_GOOD) "last good · " else ""
    val age = " · as of " + freshness.clock.format(observedAt, freshness.now)
    return limited + basis + widgetCountSummary(snapshot, mode) + age
}

internal fun rowIntent(row: WidgetRow, freshness: WidgetFreshness, appIntent: Intent): Intent =
    if (freshness.sample) appIntent else githubIntent(row)

@Composable
private fun WidgetFeedRow(row: WidgetRow, now: Instant, intent: Intent) {
    val tapAction = actionStartActivity(intent)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(tapAction)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            "${row.kind.name} #${row.number} \u00b7 ${Ages.format(row.updatedAt, now)}",
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontFamily = labelFamily(),
            ),
        )
        Text(
            Sanitize.displayText(row.title),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onBackground),
        )
    }
}
