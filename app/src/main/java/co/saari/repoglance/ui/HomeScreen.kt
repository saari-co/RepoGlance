package co.saari.repoglance.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.CiColorRole
import co.saari.repoglance.render.CiSemanticRole
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.SnapshotStore
import co.saari.repoglance.widget.RepoWidgetReceiver
import co.saari.repoglance.widget.RepoWidgetConfigActivity
import co.saari.repoglance.widget.StackWidgetReceiver
import java.time.Instant

/**
 * Home screen (fixture mode): scenario switcher, pin-aware repo list, and
 * the widget-pinning debug/user affordance. All display strings read
 * through [SnapshotRendering] / [Ages] — nothing here re-derives a rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scenario: FixtureScenario,
    onScenarioChange: (FixtureScenario) -> Unit,
    pinnedRepos: Set<String>,
    onTogglePin: (String) -> Unit,
    onOpenNavigator: () -> Unit,
    onOpenRepo: (RepoRef) -> Unit,
) {
    val now = remember(scenario, pinnedRepos) { Instant.now() }
    val repos = remember(scenario, pinnedRepos) { SnapshotStore.repoList(scenario, pinnedRepos, now) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("RepoGlance", style = MaterialTheme.typography.headlineSmall)
            ScenarioSwitcher(scenario = scenario, onScenarioChange = onScenarioChange)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (repos.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No repositories in this fixture", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // MIXED deliberately repeats the same repo full-name across
                // several truth-rule demo entries (see Fixtures.mixedSnapshots),
                // so `repo.full` alone isn't a unique LazyColumn key within
                // that scenario — basis + rate limit disambiguate it.
                items(repos, key = { "${it.repo.full}|${it.valueBasis}|${it.rateLimit}" }) { snapshot ->
                    RepoCard(
                        snapshot = snapshot,
                        now = now,
                        isPinned = snapshot.repo.full in pinnedRepos,
                        onTogglePin = { onTogglePin(snapshot.repo.full) },
                        onOpenRepo = { onOpenRepo(snapshot.repo) },
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onOpenNavigator, modifier = Modifier.fillMaxWidth()) {
            Text("Navigator")
        }
        Spacer(modifier = Modifier.height(8.dp))
        PinWidgetsRow()
    }
}

@Composable
private fun PinWidgetsRow() {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = {
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val configureCallback = PendingIntent.getActivity(
                        context,
                        REPO_WIDGET_CONFIG_REQUEST_CODE,
                        Intent(context, RepoWidgetConfigActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                    appWidgetManager.requestPinAppWidget(
                        ComponentName(context, RepoWidgetReceiver::class.java),
                        null,
                        configureCallback,
                    )
                }
            },
        ) {
            Text("Pin repo widget")
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = {
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(
                        ComponentName(context, StackWidgetReceiver::class.java),
                        null,
                        null,
                    )
                }
            },
        ) {
            Text("Pin stack widget")
        }
    }
}

private const val REPO_WIDGET_CONFIG_REQUEST_CODE = 1101

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioSwitcher(scenario: FixtureScenario, onScenarioChange: (FixtureScenario) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = scenario.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fixture") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().width(170.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FixtureScenario.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onScenarioChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RepoCard(
    snapshot: RepoSnapshot,
    now: Instant,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onOpenRepo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onOpenRepo, modifier = modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    snapshot.repo.full,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onTogglePin) {
                    Text(
                        if (isPinned) "★" else "☆",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            SnapshotRendering.rateLimitBanner(snapshot.rateLimit)?.let { banner ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        banner,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(6.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CiDot(snapshot.defaultBranchCi)
                Spacer(modifier = Modifier.width(6.dp))
                Text(SnapshotRendering.ciLabel(snapshot.defaultBranchCi), style = MaterialTheme.typography.bodyMedium)
                SnapshotRendering.ageChip(snapshot, now)?.let { chip ->
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text("Cached · $chip") })
                }
            }
            Text(
                "PRs " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis) +
                    " · Review " + SnapshotRendering.countText(snapshot.prsAwaitingMyReview, snapshot.valueBasis) +
                    " · Issues " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(SnapshotRendering.releaseLabel(snapshot.latestRelease, now), style = MaterialTheme.typography.bodySmall)
            Text(SnapshotRendering.pushedLabel(snapshot.pushedAt, now), style = MaterialTheme.typography.bodySmall)
            Text(Ages.updatedLabel(snapshot.observedAt, now), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CiDot(ci: CiState) {
    val color = when (CiSemanticRole.of(ci)) {
        CiColorRole.POSITIVE -> MaterialTheme.colorScheme.primary
        CiColorRole.NEGATIVE -> MaterialTheme.colorScheme.error
        CiColorRole.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        CiColorRole.NEUTRAL -> MaterialTheme.colorScheme.outline
    }
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
}
