package co.saari.repoglance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.saari.repoglance.MainActivity
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.SnapshotStore
import java.time.Instant

/**
 * Multi-repo stack widget: a header (scenario name + oldest-observed data
 * age across the shown repos) over a [LazyColumn] of compact repo rows.
 * Content is [SnapshotStore.stackWidgetRepos] — pinned repos, or every repo
 * in the scenario when nothing is pinned yet.
 *
 * Row tap opens [MainActivity] pre-scoped to that repo (see
 * [RepoWidget]'s KDoc for why this is chosen over a raw ACTION_VIEW Intent
 * from Glance).
 */
class StackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = Instant.now()
        val scenario = AppPrefs.selectedScenario(context)
        val pins = AppPrefs.pinnedRepos(context)
        val repos = SnapshotStore.stackWidgetRepos(scenario, pins, now)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background),
                ) {
                    HeaderRow(scenario, repos, now)
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        if (repos.isEmpty()) {
                            item {
                                Text(
                                    "No repositories in this fixture",
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                                    modifier = GlanceModifier.padding(12.dp),
                                )
                            }
                        } else {
                            items(repos.size) { index -> StackRow(repos[index], now) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(scenario: FixtureScenario, repos: List<RepoSnapshot>, now: Instant) {
    val oldestObserved = repos.mapNotNull { it.observedAt }.minOrNull()
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            scenario.name,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(Ages.updatedLabel(oldestObserved, now), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
    }
}

@Composable
private fun StackRow(snapshot: RepoSnapshot, now: Instant) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionStartActivity<MainActivity>(
                    parameters = actionParametersOf(ActionKeyRepoFull to snapshot.repo.full),
                ),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            snapshot.repo.name,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            "PRs " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis) +
                " · Issues " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis) +
                " · " + SnapshotRendering.ciLabel(snapshot.defaultBranchCi),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
        SnapshotRendering.ageChip(snapshot, now)?.let { chip ->
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(chip, style = TextStyle(color = GlanceTheme.colors.tertiary, fontWeight = FontWeight.Bold))
        }
    }
}
