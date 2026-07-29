@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package co.saari.repoglance.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.fixtures.ListState
import co.saari.repoglance.link.GitHubLinks
import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.IssueRow
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorList
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorRows
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.PrRow
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.Ages
import co.saari.repoglance.render.SnapshotRendering
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.state.NavigatorSection
import co.saari.repoglance.state.SnapshotStore
import java.time.Instant

/**
 * Navigator screen: scope/mode/filter/list-state pickers over
 * [SnapshotStore.navigatorRows], instant local search, and a two-pane
 * (>= 600dp width) or single-pane-with-overlay-detail layout. Row selection
 * is kept in [rememberSaveable] by row number so it survives a width/posture
 * change; [NavigatorScope] itself isn't directly saveable (it holds a
 * [RepoRef]), so it's round-tripped through [NavigatorScopeCodec].
 */
@Composable
fun NavigatorScreen(
    scenario: FixtureScenario,
    initialScope: NavigatorScope,
    onBackToHome: () -> Unit,
) {
    val context = LocalContext.current

    var scopeKind by rememberSaveable { mutableStateOf(NavigatorScopeCodec.kindOf(initialScope)) }
    var scopeValue by rememberSaveable { mutableStateOf(NavigatorScopeCodec.valueOf(initialScope)) }
    val scope = remember(scopeKind, scopeValue) { NavigatorScopeCodec.decode(scopeKind, scopeValue) }

    var mode by rememberSaveable { mutableStateOf(NavigatorMode.ISSUES) }
    var filter by rememberSaveable { mutableStateOf(NavigatorFilter.OPEN) }
    var listState by rememberSaveable { mutableStateOf(ListState.LOADED) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedNumber by rememberSaveable { mutableStateOf<Int?>(null) }

    var issuesExtraPages by rememberSaveable(scope, mode, filter, listState) { mutableStateOf(0) }
    var prsExtraPages by rememberSaveable(scope, mode, filter, listState) { mutableStateOf(0) }

    val now = remember(scope, mode, filter, listState) { Instant.now() }
    val section = remember(scope, mode, filter, listState, now) {
        SnapshotStore.navigatorRows(scope, mode, filter, listState, now)
    }
    val repoRef = remember(scenario, scope, now) { representativeRepoRef(scenario, scope, now) }

    fun changeMode(newMode: NavigatorMode) {
        mode = newMode
        if (newMode == NavigatorMode.ISSUES && filter == NavigatorFilter.AWAITING_MY_REVIEW) {
            filter = NavigatorFilter.OPEN
        }
        selectedNumber = null
    }

    fun openOnGitHub(item: RowItem) {
        val ref = repoRef ?: return
        val url = if (item.pr != null) GitHubLinks.pull(ref, item.number) else GitHubLinks.issue(ref, item.number)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBackToHome) { Text("< Home") }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                ScopeSwitcher(
                    scenario = scenario,
                    scope = scope,
                    onScopeChange = { newScope ->
                        scopeKind = NavigatorScopeCodec.kindOf(newScope)
                        scopeValue = NavigatorScopeCodec.valueOf(newScope)
                        selectedNumber = null
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        ModeSwitcher(mode = mode, onModeChange = ::changeMode)

        Spacer(modifier = Modifier.height(8.dp))
        FilterChips(mode = mode, filter = filter, onFilterChange = { filter = it; selectedNumber = null })

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ListStateSwitcher(listState = listState, onChange = { listState = it; selectedNumber = null })
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                SearchField(query = query, onQueryChange = { query = it })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    ListPane(
                        section = section,
                        filter = filter,
                        listState = listState,
                        issuesExtraPages = issuesExtraPages,
                        prsExtraPages = prsExtraPages,
                        onLoadMoreIssues = { issuesExtraPages += 1 },
                        onLoadMorePrs = { prsExtraPages += 1 },
                        query = query,
                        selectedNumber = selectedNumber,
                        onSelect = { selectedNumber = it },
                        onOpenGitHub = ::openOnGitHub,
                        now = now,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    val selectedItem = findItem(section, issuesExtraPages, prsExtraPages, selectedNumber)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DetailPane(item = selectedItem, repoRef = repoRef, now = now, onOpenGitHub = ::openOnGitHub)
                    }
                }
            } else if (selectedNumber == null) {
                ListPane(
                    section = section,
                    filter = filter,
                    listState = listState,
                    issuesExtraPages = issuesExtraPages,
                    prsExtraPages = prsExtraPages,
                    onLoadMoreIssues = { issuesExtraPages += 1 },
                    onLoadMorePrs = { prsExtraPages += 1 },
                    query = query,
                    selectedNumber = selectedNumber,
                    onSelect = { selectedNumber = it },
                    onOpenGitHub = ::openOnGitHub,
                    now = now,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val selectedItem = findItem(section, issuesExtraPages, prsExtraPages, selectedNumber)
                Column(modifier = Modifier.fillMaxSize()) {
                    TextButton(onClick = { selectedNumber = null }) { Text("< Back") }
                    DetailPane(item = selectedItem, repoRef = repoRef, now = now, onOpenGitHub = ::openOnGitHub)
                }
            }
        }
    }
}

/** Row payload unifying [IssueRow]/[PrRow] for shared list/detail rendering;
 *  [pr] is non-null exactly when the row came from a PR list, carrying the
 *  draft/review/CI-rollup fields issues don't have. */
private data class RowItem(
    val number: Int,
    val title: String,
    val state: String,
    val labels: List<String>,
    val author: String,
    val assignee: String?,
    val commentCount: Int,
    val updatedAt: Instant,
    val pr: PrRow?,
)

private fun IssueRow.toItem() = RowItem(number, title, state, labels, author, assignee, commentCount, updatedAt, null)
private fun PrRow.toItem() = RowItem(number, title, state, labels, author, assignee, commentCount, updatedAt, this)

/** Fixture-mode "Load more" paging: appends another copy of the same fixture
 *  rows with numbers offset so they never collide with an earlier page or
 *  with the other section's number range (issues start at 100+, PRs at
 *  200+) — documented in the PR spec as an acceptable fixture stand-in for
 *  a real next-page fetch. */
private fun pagedIssueRows(list: NavigatorList, extraPages: Int): List<IssueRow> {
    val base = (list.rows as NavigatorRows.Issues).rows
    return (0..extraPages).flatMap { pageIndex ->
        base.map { it.copy(number = it.number + pageIndex * PAGE_OFFSET) }
    }
}

private fun pagedPrRows(list: NavigatorList, extraPages: Int): List<PrRow> {
    val base = (list.rows as NavigatorRows.Prs).rows
    return (0..extraPages).flatMap { pageIndex ->
        base.map { it.copy(number = it.number + pageIndex * PAGE_OFFSET) }
    }
}

private const val PAGE_OFFSET = 10_000

private fun findItem(section: NavigatorSection, issuesExtraPages: Int, prsExtraPages: Int, number: Int?): RowItem? {
    if (number == null) return null
    section.issues?.let { list ->
        pagedIssueRows(list, issuesExtraPages).firstOrNull { it.number == number }?.let { return it.toItem() }
    }
    section.prs?.let { list ->
        pagedPrRows(list, prsExtraPages).firstOrNull { it.number == number }?.let { return it.toItem() }
    }
    return null
}

/**
 * Slice 1's [IssueRow]/[PrRow] carry no repo field, so Account/Org scope has
 * no single backing repo for "Open on GitHub". This picks a representative
 * one (the scope's own repo for [NavigatorScope.Repo]; the first fixture
 * repo owned by the org for [NavigatorScope.Org]; the first fixture repo
 * overall for [NavigatorScope.Account]) so the button has somewhere honest
 * to point — a later slice that tags rows with their originating repo
 * removes the need for this.
 */
private fun representativeRepoRef(scenario: FixtureScenario, scope: NavigatorScope, now: Instant): RepoRef? =
    when (scope) {
        is NavigatorScope.Repo -> scope.ref
        is NavigatorScope.Org -> Fixtures.snapshots(scenario, now).firstOrNull { it.repo.owner == scope.login }?.repo
        is NavigatorScope.Account -> Fixtures.snapshots(scenario, now).firstOrNull()?.repo
    }

@Composable
private fun ScopeSwitcher(scenario: FixtureScenario, scope: NavigatorScope, onScopeChange: (NavigatorScope) -> Unit) {
    val now = remember(scenario) { Instant.now() }
    val repos = remember(scenario, now) { Fixtures.snapshots(scenario, now) }
    val owners = remember(repos) { repos.map { it.repo.owner }.distinct() }
    var expanded by remember { mutableStateOf(false) }

    val label = when (scope) {
        is NavigatorScope.Account -> "Account"
        is NavigatorScope.Org -> "Org: ${scope.login}"
        is NavigatorScope.Repo -> scope.ref.full
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Scope") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Account") },
                onClick = { onScopeChange(NavigatorScope.Account); expanded = false },
            )
            owners.forEach { owner ->
                DropdownMenuItem(
                    text = { Text("Org: $owner") },
                    onClick = { onScopeChange(NavigatorScope.Org(owner)); expanded = false },
                )
            }
            repos.forEach { snapshot ->
                DropdownMenuItem(
                    text = { Text(snapshot.repo.full) },
                    onClick = { onScopeChange(NavigatorScope.Repo(snapshot.repo)); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ModeSwitcher(mode: NavigatorMode, onModeChange: (NavigatorMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        NavigatorMode.entries.forEachIndexed { index, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onModeChange(m) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = NavigatorMode.entries.size),
                label = { Text(m.name) },
            )
        }
    }
}

@Composable
private fun FilterChips(mode: NavigatorMode, filter: NavigatorFilter, onFilterChange: (NavigatorFilter) -> Unit) {
    val available = if (mode == NavigatorMode.ISSUES) {
        NavigatorFilter.entries.filter { it != NavigatorFilter.AWAITING_MY_REVIEW }
    } else {
        NavigatorFilter.entries.toList()
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        available.forEach { f ->
            FilterChip(selected = filter == f, onClick = { onFilterChange(f) }, label = { Text(f.name) })
        }
    }
}

@Composable
private fun ListStateSwitcher(listState: ListState, onChange: (ListState) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (listState) {
        ListState.EMPTY -> "Empty"
        ListState.LOADED -> "Loaded"
        ListState.PAGED -> "Paged"
        ListState.LAST_GOOD -> "Last-good"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("List state") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().width(160.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(ListState.LOADED, ListState.EMPTY, ListState.PAGED, ListState.LAST_GOOD).forEach { s ->
                DropdownMenuItem(text = { Text(s.name) }, onClick = { onChange(s); expanded = false })
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search cached rows") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ListPane(
    section: NavigatorSection,
    filter: NavigatorFilter,
    listState: ListState,
    issuesExtraPages: Int,
    prsExtraPages: Int,
    onLoadMoreIssues: () -> Unit,
    onLoadMorePrs: () -> Unit,
    query: String,
    selectedNumber: Int?,
    onSelect: (Int) -> Unit,
    onOpenGitHub: (RowItem) -> Unit,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        section.issues?.let { list ->
            item { SectionHeader("Issues", list, now) }
            val rows = pagedIssueRows(list, issuesExtraPages)
            val filtered = (SnapshotStore.search(NavigatorRows.Issues(rows), query) as NavigatorRows.Issues).rows
            if (filtered.isEmpty()) {
                item { EmptyRowsText() }
            } else {
                items(filtered, key = { "issue-${it.number}" }) { row ->
                    NavigatorRowView(
                        item = row.toItem(),
                        now = now,
                        selected = selectedNumber == row.number,
                        onSelect = { onSelect(row.number) },
                        onOpenGitHub = onOpenGitHub,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (listState == ListState.PAGED && filter != NavigatorFilter.AWAITING_MY_REVIEW) {
                item { LoadMoreButton(onClick = onLoadMoreIssues) }
            }
        }
        section.prs?.let { list ->
            item { SectionHeader("PRs", list, now) }
            val rows = pagedPrRows(list, prsExtraPages)
            val filtered = (SnapshotStore.search(NavigatorRows.Prs(rows), query) as NavigatorRows.Prs).rows
            if (filtered.isEmpty()) {
                item { EmptyRowsText() }
            } else {
                items(filtered, key = { "pr-${it.number}" }) { row ->
                    NavigatorRowView(
                        item = row.toItem(),
                        now = now,
                        selected = selectedNumber == row.number,
                        onSelect = { onSelect(row.number) },
                        onOpenGitHub = onOpenGitHub,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (listState == ListState.PAGED) {
                item { LoadMoreButton(onClick = onLoadMorePrs) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, list: NavigatorList, now: Instant) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (list.valueBasis == ValueBasis.LAST_GOOD) {
            val chipAge = list.observedAt?.let { Ages.format(it, now) } ?: "unknown"
            AssistChip(onClick = {}, label = { Text("Cached · $chipAge") })
        }
    }
}

@Composable
private fun EmptyRowsText() {
    Text(
        "No rows match",
        modifier = Modifier.padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun LoadMoreButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Load more (fixture)")
    }
}

@Composable
private fun NavigatorRowView(
    item: RowItem,
    now: Instant,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenGitHub: (RowItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onSelect, onLongClick = { onOpenGitHub(item) }),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${item.number}", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    Sanitize.displayText(item.title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                if (item.pr?.isDraft == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AssistChip(onClick = {}, label = { Text("Draft") })
                }
            }
            Text(item.state, style = MaterialTheme.typography.labelSmall)
            if (item.labels.isNotEmpty()) {
                Text(
                    item.labels.joinToString(", ") { Sanitize.displayText(it) },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                "by ${Sanitize.displayText(item.author)} · ${item.commentCount} comments · " +
                    Ages.updatedLabel(item.updatedAt, now),
                style = MaterialTheme.typography.bodySmall,
            )
            item.pr?.let { pr ->
                val ciLabel = SnapshotRendering.ciLabel(pr.ciRollup)
                Text(
                    "Review: ${pr.reviewState.name} · CI ${ciLabel.first()} ($ciLabel)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DetailPane(item: RowItem?, repoRef: RepoRef?, now: Instant, onOpenGitHub: (RowItem) -> Unit) {
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a row to see details", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(Sanitize.displayText(item.title), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text("#${item.number} · ${item.state}", style = MaterialTheme.typography.bodyMedium)
        if (item.labels.isNotEmpty()) {
            Text("Labels: " + item.labels.joinToString(", ") { Sanitize.displayText(it) })
        }
        Text("Author: ${Sanitize.displayText(item.author)}")
        Text("Assignee: " + (item.assignee?.let { Sanitize.displayText(it) } ?: "unassigned"))
        Text("Comments: ${item.commentCount}")
        Text(Ages.updatedLabel(item.updatedAt, now))
        item.pr?.let { pr ->
            Text("Draft: " + if (pr.isDraft) "yes" else "no")
            Text("Review: ${pr.reviewState.name}")
            Text("CI: " + SnapshotRendering.ciLabel(pr.ciRollup))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { onOpenGitHub(item) }, enabled = repoRef != null) {
            Text("Open on GitHub")
        }
    }
}
