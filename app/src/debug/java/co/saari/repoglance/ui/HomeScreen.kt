package co.saari.repoglance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.CiSemanticRole
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.SnapshotStore
import co.saari.repoglance.ui.settings.WidgetKind
import co.saari.repoglance.ui.settings.WidgetPinning
import co.saari.repoglance.ui.theme.ControlCard
import co.saari.repoglance.ui.theme.ControlChip
import co.saari.repoglance.ui.theme.FamilyStatus
import co.saari.repoglance.ui.theme.LabelRole
import co.saari.repoglance.ui.theme.LabelText
import co.saari.repoglance.ui.theme.PrimaryButton
import co.saari.repoglance.ui.theme.SecondaryButton
import co.saari.repoglance.ui.theme.StatusBanner
import co.saari.repoglance.ui.theme.StatusColors
import co.saari.repoglance.ui.theme.StatusPill
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scenario: FixtureScenario,
    onScenarioChange: (FixtureScenario) -> Unit,
    pinnedRepos: Set<String>,
    onTogglePin: (String) -> Unit,
    onOpenNavigator: () -> Unit,
    onOpenRepo: (RepoRef) -> Unit,
    statusColors: StatusColors = FamilyStatus.colors(MaterialTheme.colorScheme),
) {
    val fixtureAnchor = remember(scenario) { Instant.now() }
    val now = rememberFreshnessNow()
    val repos = remember(scenario, pinnedRepos, fixtureAnchor) {
        SnapshotStore.repoList(scenario, pinnedRepos, fixtureAnchor)
    }

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
                items(repos, key = { "${it.repo.full}|${it.valueBasis}|${it.rateLimit}" }) { snapshot ->
                    RepoCard(
                        snapshot = snapshot,
                        now = now,
                        isPinned = snapshot.repo.full in pinnedRepos,
                        onTogglePin = { onTogglePin(snapshot.repo.full) },
                        onOpenRepo = { onOpenRepo(snapshot.repo) },
                        statusColors = statusColors,
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(
            onClick = onOpenNavigator,
            modifier = Modifier.fillMaxWidth().testTag("repoglance:fixture-navigator"),
        ) {
            LabelText("Navigator", LabelRole.BUTTON)
        }
        Spacer(modifier = Modifier.height(8.dp))
        PinWidgetsRow()
    }
}

@Composable
private fun PinWidgetsRow() {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            modifier = Modifier.weight(1f),
            onClick = { WidgetPinning.request(context, WidgetKind.REPOSITORY) },
        ) {
            LabelText("Pin repo widget", LabelRole.BUTTON)
        }
        SecondaryButton(
            modifier = Modifier.weight(1f),
            onClick = { WidgetPinning.request(context, WidgetKind.PINNED_REPOS) },
        ) {
            LabelText("Pin stack widget", LabelRole.BUTTON)
        }
    }
}

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
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .width(170.dp)
                .testTag("repoglance:scenario"),
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
    statusColors: StatusColors,
    modifier: Modifier = Modifier,
) {
    ControlCard(onClick = onOpenRepo, modifier = modifier.padding(vertical = 4.dp)) {
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
                val tone = statusColors.tone(SnapshotRendering.rateLimitRole(snapshot.rateLimit))
                StatusBanner(tone = tone, modifier = Modifier.fillMaxWidth()) {
                    LabelText(
                        banner,
                        LabelRole.META,
                        color = tone.onContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(6.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CiStatus(snapshot.defaultBranchCi, statusColors)
                SnapshotRendering.ageChip(snapshot, now)?.let { chip ->
                    Spacer(modifier = Modifier.width(8.dp))
                    ControlChip(label = { LabelText("Cached · $chip", LabelRole.CHIP) })
                }
            }
            Text(
                "PRs " + SnapshotRendering.countText(snapshot.openPrs, snapshot.valueBasis) +
                    " · Review " + SnapshotRendering.countText(snapshot.prsAwaitingMyReview, snapshot.valueBasis) +
                    " · Issues " + SnapshotRendering.countText(snapshot.openIssues, snapshot.valueBasis),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                SnapshotRendering.releaseLabel(snapshot.latestRelease, snapshot.valueBasis, now),
                style = MaterialTheme.typography.bodySmall,
            )
            LabelText(
                SnapshotRendering.pushedLabel(snapshot.pushedAt, now),
                LabelRole.META,
                style = MaterialTheme.typography.bodySmall,
            )
            LabelText(
                Ages.updatedLabel(snapshot.observedAt, now),
                LabelRole.META,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CiStatus(ci: CiState, statusColors: StatusColors) {
    val tone = statusColors.tone(CiSemanticRole.of(ci))
    StatusPill(tone = tone) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(tone.ink))
            Spacer(modifier = Modifier.width(6.dp))
            LabelText(
                SnapshotRendering.ciLabel(ci),
                LabelRole.CHIP,
                style = MaterialTheme.typography.labelLarge,
                color = tone.onContainer,
            )
        }
    }
}
