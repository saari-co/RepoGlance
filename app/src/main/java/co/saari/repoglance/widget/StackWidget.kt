package co.saari.repoglance.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.CatalogNamesStore
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.state.RateLimitStore
import java.time.Instant

class StackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            key(currentState(WidgetRefresh.REDRAW_KEY)) {
                val now = Instant.now()
                val freshness = WidgetFreshness(
                    now = now,
                    clock = widgetClock(context),
                    rateLimitedUntil = RateLimitStore.exhaustedUntil(RateLimitStore.load(context), now),
                )
                val catalogPushedAt = CatalogNamesStore.pushedAt(context)
                val entries = StackRows.order(
                    pins = AppPrefs.livePins(context),
                    catalogPushedAt = catalogPushedAt,
                ) { LiveSnapshotStore.load(context, it) }
                val catalogIntent = liveCatalogIntent(context)

                GlanceTheme {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.background),
                    ) {
                        StackHeader(stackHeaderLabel(entries.size, freshness), catalogIntent)
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            if (entries.isEmpty()) {
                                item { StackEmpty(catalogIntent) }
                            } else {
                                items(entries.size) { index ->
                                    val entry = entries[index]
                                    StackRow(liveRepositoryIntent(context, entry.repo), entry, freshness)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class StackEntry(
    val repo: RepoRef,
    val snapshot: RepoSnapshot?,
)

object StackRows {
    fun order(
        pins: Collection<String>,
        catalogPushedAt: Map<String, Instant>,
        snapshotFor: (RepoRef) -> RepoSnapshot?,
    ): List<StackEntry> {
        val entries = pins.mapNotNull(::parseRef).distinct().map { StackEntry(it, snapshotFor(it)) }
        val latestPush = entries.associateWith { entry ->
            listOfNotNull(entry.snapshot?.pushedAt, catalogPushedAt[entry.repo.full]).maxOrNull()
        }
        return entries.sortedWith(
            compareBy<StackEntry> { latestPush[it] == null }
                .thenByDescending { latestPush[it] ?: Instant.EPOCH }
                .thenBy { it.repo.full.lowercase() },
        )
    }

    private fun parseRef(full: String): RepoRef? {
        val parts = full.split('/', limit = 2)
        if (parts.size != 2) return null
        return runCatching { RepoRef(parts[0], parts[1]) }.getOrNull()
    }
}

internal const val STACK_EMPTY_LABEL = "Pin repositories in RepoGlance"
internal const val STACK_HEADER_TAG = "stack-header"
internal const val STACK_AGE_TAG = "stack-age"
internal const val STACK_COUNTS_TAG = "stack-counts"

internal fun stackHeaderLabel(pinCount: Int, freshness: WidgetFreshness): String {
    val limited = freshness.rateLimitedUntil
        ?.let { " · rate limited · resets " + freshness.clock.time(it) }
        .orEmpty()
    return "Pinned · $pinCount$limited"
}

internal fun stackRowCounts(snapshot: RepoSnapshot?): String? {
    if (snapshot == null || snapshot.valueBasis == ValueBasis.UNKNOWN || snapshot.observedAt == null) return null
    val basis = snapshot.valueBasis
    return "issues " + SnapshotRendering.countText(snapshot.openIssues, basis) +
        " · PRs " + SnapshotRendering.countText(snapshot.openPrs, basis) +
        " · review " + SnapshotRendering.countText(snapshot.prsAwaitingMyReview, basis)
}

internal fun stackRowAge(snapshot: RepoSnapshot?, freshness: WidgetFreshness): String {
    val observedAt = snapshot?.observedAt
    if (snapshot == null || observedAt == null || snapshot.valueBasis == ValueBasis.UNKNOWN) return "no data"
    val clock = freshness.clock.format(observedAt, freshness.now)
    return if (snapshot.valueBasis == ValueBasis.LAST_GOOD) "last good $clock" else clock
}

private fun liveCatalogIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    action = Intent.ACTION_MAIN
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    putExtra(EXTRA_LIVE_CATALOG, true)
}

@Composable
private fun StackHeader(label: String, catalogIntent: Intent) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(catalogIntent))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.semantics { testTag = STACK_HEADER_TAG },
        )
    }
}

@Composable
private fun StackEmpty(catalogIntent: Intent) {
    Text(
        STACK_EMPTY_LABEL,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(catalogIntent))
            .padding(12.dp),
    )
}

@Composable
private fun StackRow(rowIntent: Intent, entry: StackEntry, freshness: WidgetFreshness) {
    val stale = entry.snapshot?.valueBasis != ValueBasis.EXACT
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(rowIntent))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                Sanitize.displayText(entry.repo.full),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                stackRowAge(entry.snapshot, freshness),
                maxLines = 1,
                style = TextStyle(
                    color = if (stale) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                    fontWeight = if (stale) FontWeight.Bold else FontWeight.Normal,
                ),
                modifier = GlanceModifier.semantics { testTag = STACK_AGE_TAG },
            )
        }
        stackRowCounts(entry.snapshot)?.let { counts ->
            Text(
                counts,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.semantics { testTag = STACK_COUNTS_TAG },
            )
        }
    }
}
