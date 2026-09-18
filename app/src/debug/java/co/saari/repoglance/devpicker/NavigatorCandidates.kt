@file:OptIn(ExperimentalMaterial3Api::class)

package co.saari.repoglance.devpicker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.ListState
import co.saari.repoglance.link.GitHubAppLauncher
import co.saari.repoglance.link.GitHubLinks
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.state.SnapshotStore
import co.saari.repoglance.ui.DetailPane
import co.saari.repoglance.ui.ListPane
import co.saari.repoglance.ui.NavigatorControls
import co.saari.repoglance.ui.RowItem
import co.saari.repoglance.ui.findItem
import co.saari.repoglance.ui.rememberFreshnessNow
import kotlinx.coroutines.launch
import java.time.Instant

/*
 * Every candidate is a delta on the production canvas. The chrome
 * (NavigatorControls), the list (ListPane) and the detail (DetailPane) are the
 * real production composables; only the wide-layout branch differs per
 * WideSelectionCandidate. The narrow branch (which the window falls back to
 * when the GitHub app takes half the screen) is the production behaviour
 * verbatim, so earlier locks stay applied.
 */

private const val WIDE_BREAKPOINT_DP = 600

private fun narrowTapEffect(sheet: Boolean): String =
    if (sheet) "raises a detail sheet over the list (hybrid)" else "replaces the list with the detail"

private typealias ListSlot = @Composable (
    openGitHubOnSelect: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) -> Unit

@Composable
fun NavigatorCandidate(
    candidate: WideSelectionCandidate,
    scenario: FixtureScenario,
    initialScope: NavigatorScope,
    initialMode: NavigatorMode,
    onBackToHome: () -> Unit,
    onTrace: (String) -> Unit,
    modifier: Modifier = Modifier,
    narrowSheet: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    var scopeKind by rememberSaveable { mutableStateOf(NavigatorScopeCodec.kindOf(initialScope)) }
    var scopeValue by rememberSaveable { mutableStateOf(NavigatorScopeCodec.valueOf(initialScope)) }
    val scope = remember(scopeKind, scopeValue) { NavigatorScopeCodec.decode(scopeKind, scopeValue) }
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var filter by rememberSaveable { mutableStateOf(NavigatorFilter.OPEN) }
    var fixtureListState by rememberSaveable { mutableStateOf(ListState.LOADED) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var externalGitHubMode by rememberSaveable { mutableStateOf(false) }
    var issuesExtraPages by rememberSaveable(scope, mode, filter, fixtureListState) { mutableIntStateOf(0) }
    var prsExtraPages by rememberSaveable(scope, mode, filter, fixtureListState) { mutableIntStateOf(0) }

    val fixtureAnchor = remember(scenario) { Instant.now() }
    val now = rememberFreshnessNow()
    val section = remember(scope, mode, filter, fixtureListState, fixtureAnchor) {
        SnapshotStore.navigatorRows(scope, mode, filter, fixtureListState, fixtureAnchor)
    }
    val selectedItem = findItem(section, issuesExtraPages, prsExtraPages, selectedNumber)

    fun changeMode(newMode: NavigatorMode) {
        if (newMode == mode) return
        mode = newMode
        if (newMode == NavigatorMode.ISSUES && filter == NavigatorFilter.AWAITING_MY_REVIEW) {
            filter = NavigatorFilter.OPEN
        }
        selectedNumber = null
        coroutineScope.launch { scrollState.scrollToItem(0) }
    }

    fun openOnGitHub(item: RowItem) {
        val url = if (item.pr != null) {
            GitHubLinks.pull(item.repo, item.number)
        } else {
            GitHubLinks.issue(item.repo, item.number)
        }
        if (GitHubAppLauncher.open(context, url, adjacent = true)) {
            externalGitHubMode = true
            onTrace("#${item.number} \u2192 GitHub app opened beside the list")
        } else {
            Toast.makeText(context, "GitHub app is not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun select(number: Int) {
        selectedNumber = number
        onTrace("tap on #$number \u2192 ${candidate.tapEffect}")
    }

    fun selectNarrow(number: Int) {
        selectedNumber = number
        onTrace("tap on #$number \u2192 ${narrowTapEffect(narrowSheet)}")
    }

    fun clearSelection() {
        selectedNumber = null
        onTrace("Back \u2192 selection cleared, list restored")
    }

    BackHandler {
        if (selectedNumber != null) clearSelection() else onBackToHome()
    }

    val controls: @Composable () -> Unit = {
        NavigatorControls(
            scenario = scenario,
            scope = scope,
            mode = mode,
            filter = filter,
            fixtureListState = fixtureListState,
            query = query,
            searchExpanded = searchExpanded,
            onBackToHome = onBackToHome,
            onScopeChange = { newScope ->
                scopeKind = NavigatorScopeCodec.kindOf(newScope)
                scopeValue = NavigatorScopeCodec.valueOf(newScope)
                selectedNumber = null
            },
            onFilterChange = {
                filter = it
                selectedNumber = null
            },
            onListStateChange = {
                fixtureListState = it
                selectedNumber = null
            },
            onQueryChange = { query = it },
            onSearchExpandedChange = { expanded ->
                searchExpanded = expanded
                if (!expanded) query = ""
            },
        )
    }

    val list: ListSlot = { openGitHubOnSelect, onSelect, listModifier ->
        ListPane(
            section = section,
            filter = filter,
            fixtureListState = fixtureListState,
            issuesExtraPages = issuesExtraPages,
            prsExtraPages = prsExtraPages,
            onLoadMoreIssues = { issuesExtraPages += 1 },
            onLoadMorePrs = { prsExtraPages += 1 },
            query = query,
            selectedNumber = selectedNumber,
            onSelect = onSelect,
            onOpenGitHub = ::openOnGitHub,
            openGitHubOnSelect = openGitHubOnSelect,
            now = now,
            scrollState = scrollState,
            mode = mode,
            onModeChange = ::changeMode,
            controls = controls,
            modifier = listModifier,
        )
    }
    val detail: @Composable () -> Unit = {
        DetailPane(item = selectedItem, now = now, onOpenGitHub = ::openOnGitHub)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (maxWidth < WIDE_BREAKPOINT_DP.dp) {
            NarrowLayout(
                selected = selectedNumber != null,
                externalGitHubMode = externalGitHubMode,
                sheet = narrowSheet,
                list = list,
                detail = detail,
                onSelect = ::selectNarrow,
                onBack = ::clearSelection,
            )
        } else {
            WideLayout(
                candidate = candidate,
                selectedItem = selectedItem,
                list = list,
                detail = detail,
                onSelect = ::select,
                onOpenRow = ::openOnGitHub,
                onBack = ::clearSelection,
            )
        }
    }
}

/**
 * Narrow (cover display) behaviour. [sheet] = false is the pre-017 production
 * behaviour ("detail replaces the list") that round 1 ran on; [sheet] = true
 * is the maintainer-requested hybrid (D's sheet on the folded phone) that
 * replaced the five candidates and is now production.
 */
@Composable
private fun NarrowLayout(
    selected: Boolean,
    externalGitHubMode: Boolean,
    sheet: Boolean,
    list: ListSlot,
    detail: @Composable () -> Unit,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    when {
        sheet -> ListWithSheet(
            selected = selected && !externalGitHubMode,
            list = list,
            detail = detail,
            onSelect = onSelect,
            onBack = onBack,
        )
        !selected || externalGitHubMode -> list(externalGitHubMode, onSelect, Modifier.fillMaxSize())
        else -> DetailWithBack(detail = detail, onBack = onBack)
    }
}

@Composable
private fun WideLayout(
    candidate: WideSelectionCandidate,
    selectedItem: RowItem?,
    list: ListSlot,
    detail: @Composable () -> Unit,
    onSelect: (Int) -> Unit,
    onOpenRow: (RowItem) -> Unit,
    onBack: () -> Unit,
) {
    when (candidate) {
        WideSelectionCandidate.PANE_FILL -> ListBesideDetail(list = list, detail = detail, onSelect = onSelect)
        WideSelectionCandidate.SECOND_TAP -> ListBesideDetail(
            list = list,
            detail = detail,
            onSelect = { number ->
                if (selectedItem?.number == number) onOpenRow(selectedItem) else onSelect(number)
            },
        )
        WideSelectionCandidate.LIST_ONLY -> ListOnly(list = list, onSelect = onSelect)
        WideSelectionCandidate.SHEET -> ListWithSheet(
            selected = selectedItem != null,
            list = list,
            detail = detail,
            onSelect = onSelect,
            onBack = onBack,
        )
        WideSelectionCandidate.SINGLE_PANE -> SinglePane(
            selected = selectedItem != null,
            list = list,
            detail = detail,
            onSelect = onSelect,
            onBack = onBack,
        )
    }
}

@Composable
private fun ListOnly(list: ListSlot, onSelect: (Int) -> Unit) {
    list(true, onSelect, Modifier.fillMaxSize())
}

@Composable
private fun ListWithSheet(
    selected: Boolean,
    list: ListSlot,
    detail: @Composable () -> Unit,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    list(false, onSelect, Modifier.fillMaxSize())
    if (selected) {
        ModalBottomSheet(
            onDismissRequest = onBack,
            modifier = Modifier.testTag("repoglance:navigator-detail"),
        ) { detail() }
    }
}

@Composable
private fun SinglePane(
    selected: Boolean,
    list: ListSlot,
    detail: @Composable () -> Unit,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    if (selected) {
        DetailWithBack(detail = detail, onBack = onBack)
    } else {
        list(false, onSelect, Modifier.fillMaxSize())
    }
}

@Composable
private fun ListBesideDetail(list: ListSlot, detail: @Composable () -> Unit, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        list(false, onSelect, Modifier.weight(1f).fillMaxHeight())
        Box(modifier = Modifier.weight(1f).fillMaxHeight().testTag("repoglance:navigator-detail")) { detail() }
    }
}

@Composable
private fun DetailWithBack(detail: @Composable () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Back")
        }
        detail()
    }
}
