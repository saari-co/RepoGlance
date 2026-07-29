package co.saari.repoglance.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import co.saari.repoglance.MainActivity
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.CiColorRole
import co.saari.repoglance.render.CiSemanticRole
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.AppPrefs
import java.time.Instant

/**
 * Per-repo Glance widget.
 *
 * Fixture-mode placeholder behavior (v0.1-honest simplification — a real
 * per-widget repo picker is future work): renders the first pinned repo,
 * matched against the currently selected fixture scenario so the widget
 * follows the in-app scenario switcher when pins exist there; when there
 * are no pins at all (or none match the selected scenario, e.g. EMPTY is
 * selected), it falls back to the first repo of the MIXED scenario
 * specifically — not the selected scenario — so a fresh install's widget
 * always shows something.
 *
 * [SizeMode.Responsive] with two breakpoints: SMALL (width < 180dp, fits
 * the Fold cover display) shows name/CI/PRs/data-age only; WIDE (>= 180dp)
 * adds the full count trio, release, pushed age, stale chip, and rate-limit
 * banner. Tapping opens [MainActivity] pre-scoped to the shown repo.
 */
class RepoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = Instant.now()
        val scenario = AppPrefs.selectedScenario(context)
        val pins = AppPrefs.pinnedRepos(context)
        val snapshot = placeholderRepo(scenario, pins, now)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            snapshot?.let { putExtra(EXTRA_REPO_FULL, it.repo.full) }
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val tapAction = actionStartActivity(tapIntent)
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .clickable(tapAction),
                ) {
                    if (size.width < WIDE_BREAKPOINT) {
                        SmallContent(snapshot, now)
                    } else {
                        WideContent(snapshot, now)
                    }
                }
            }
        }
    }

    private fun placeholderRepo(scenario: FixtureScenario, pins: Set<String>, now: Instant): RepoSnapshot? {
        if (pins.isNotEmpty()) {
            Fixtures.snapshots(scenario, now).firstOrNull { it.repo.full in pins }?.let { return it }
        }
        return Fixtures.snapshots(FixtureScenario.MIXED, now).firstOrNull()
    }

    companion object {
        private val SMALL_SIZE = DpSize(120.dp, 64.dp)
        private val WIDE_SIZE = DpSize(250.dp, 140.dp)
        private val WIDE_BREAKPOINT = 180.dp
    }
}

@Composable
private fun ciColorProvider(ci: CiState): ColorProvider = when (CiSemanticRole.of(ci)) {
    CiColorRole.POSITIVE -> GlanceTheme.colors.primary
    CiColorRole.NEGATIVE -> GlanceTheme.colors.error
    CiColorRole.IN_PROGRESS -> GlanceTheme.colors.tertiary
    CiColorRole.NEUTRAL -> GlanceTheme.colors.outline
}

@Composable
private fun CiDot(ci: CiState) {
    Box(modifier = GlanceModifier.size(8.dp).background(ciColorProvider(ci))) {}
}

@Composable
private fun SmallContent(snapshot: RepoSnapshot?, now: Instant) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        if (snapshot == null) {
            Text("No repo pinned", style = TextStyle(color = GlanceTheme.colors.onBackground))
            Text(Ages.updatedLabel(null, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CiDot(snapshot.defaultBranchCi)
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                snapshot.repo.name,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
            )
        }
        Text(SnapshotRendering.ciLabel(snapshot.defaultBranchCi), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        Text(
            "PRs " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis),
            style = TextStyle(color = GlanceTheme.colors.onBackground),
        )
        Text(Ages.updatedLabel(snapshot.observedAt, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
    }
}

@Composable
private fun WideContent(snapshot: RepoSnapshot?, now: Instant) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        if (snapshot == null) {
            Text(
                "No repo pinned",
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
            )
            Text(Ages.updatedLabel(null, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            return@Column
        }
        SnapshotRendering.rateLimitBanner(snapshot.rateLimit)?.let { banner ->
            Text(
                banner,
                style = TextStyle(color = GlanceTheme.colors.onErrorContainer, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.errorContainer)
                    .padding(4.dp),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        Text(
            snapshot.repo.full,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CiDot(snapshot.defaultBranchCi)
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(SnapshotRendering.ciLabel(snapshot.defaultBranchCi), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            SnapshotRendering.ageChip(snapshot, now)?.let { chip ->
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    "Cached · $chip",
                    style = TextStyle(color = GlanceTheme.colors.tertiary, fontWeight = FontWeight.Bold),
                )
            }
        }
        Text(
            "PRs " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis) +
                "  Review " + SnapshotRendering.countText(snapshot.prsAwaitingMyReview, snapshot.valueBasis) +
                "  Issues " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis),
            style = TextStyle(color = GlanceTheme.colors.onBackground),
        )
        Text(SnapshotRendering.releaseLabel(snapshot.latestRelease, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        Text(SnapshotRendering.pushedLabel(snapshot.pushedAt, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        Text(Ages.updatedLabel(snapshot.observedAt, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
    }
}
