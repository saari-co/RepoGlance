package co.saari.repoglance.state

import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.fixtures.ListState
import co.saari.repoglance.model.IssueRow
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorList
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorRows
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.PrRow
import co.saari.repoglance.model.RepoSnapshot
import java.time.Instant

object SnapshotStore {

    fun repoList(scenario: FixtureScenario, pins: Set<String>, now: Instant): List<RepoSnapshot> {
        val all = Fixtures.snapshots(scenario, now)
        val (pinned, rest) = all.partition { it.repo.full in pins }
        return pinned + rest
    }

    fun stackWidgetRepos(scenario: FixtureScenario, pins: Set<String>, now: Instant): List<RepoSnapshot> {
        val all = Fixtures.snapshots(scenario, now)
        val pinned = all.filter { it.repo.full in pins }
        return pinned.ifEmpty { all }
    }

    fun search(rows: NavigatorRows, query: String): NavigatorRows {
        if (query.isBlank()) return rows
        val needle = query.trim().lowercase()
        return when (rows) {
            is NavigatorRows.Issues -> NavigatorRows.Issues(rows.rows.filter { it.matches(needle) })
            is NavigatorRows.Prs -> NavigatorRows.Prs(rows.rows.filter { it.matches(needle) })
        }
    }

    private fun IssueRow.matches(needle: String): Boolean =
        title.lowercase().contains(needle) ||
            number.toString().contains(needle) ||
            author.lowercase().contains(needle) ||
            labels.any { it.lowercase().contains(needle) }

    private fun PrRow.matches(needle: String): Boolean =
        title.lowercase().contains(needle) ||
            number.toString().contains(needle) ||
            author.lowercase().contains(needle) ||
            labels.any { it.lowercase().contains(needle) }

    fun navigatorRows(
        scope: NavigatorScope,
        mode: NavigatorMode,
        filter: NavigatorFilter,
        state: ListState,
        now: Instant,
    ): NavigatorSection = when (mode) {
        NavigatorMode.ISSUES -> NavigatorSection(
            issues = Fixtures.navigatorList(scope, NavigatorMode.ISSUES, filter, state, now),
            prs = null,
        )
        NavigatorMode.PRS -> NavigatorSection(
            issues = null,
            prs = Fixtures.navigatorList(scope, NavigatorMode.PRS, filter, state, now),
        )
        NavigatorMode.BOTH -> {
            val prs = Fixtures.navigatorList(scope, NavigatorMode.PRS, filter, state, now)
            val issues = if (filter == NavigatorFilter.AWAITING_MY_REVIEW) {
                val basisSource = Fixtures.navigatorList(scope, NavigatorMode.ISSUES, NavigatorFilter.OPEN, state, now)
                basisSource.copy(rows = NavigatorRows.Issues(emptyList()))
            } else {
                Fixtures.navigatorList(scope, NavigatorMode.ISSUES, filter, state, now)
            }
            NavigatorSection(issues = issues, prs = prs)
        }
    }
}

data class NavigatorSection(
    val issues: NavigatorList?,
    val prs: NavigatorList?,
)
