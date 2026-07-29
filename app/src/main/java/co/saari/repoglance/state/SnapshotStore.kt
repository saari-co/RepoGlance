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

/**
 * Pure function layer over [Fixtures]: pin-aware sorting, widget-content
 * selection, cached-row search, and [NavigatorMode.BOTH] composition. No
 * state lives here — callers own scenario/pin state (see [AppPrefs]) and
 * pass it in explicitly.
 */
object SnapshotStore {

    /** Pinned repos first, fixture order otherwise preserved on both sides
     *  of the split (stable — toggling a pin only ever moves that one repo
     *  across the pinned/unpinned boundary, nothing else reorders). */
    fun repoList(scenario: FixtureScenario, pins: Set<String>, now: Instant): List<RepoSnapshot> {
        val all = Fixtures.snapshots(scenario, now)
        val (pinned, rest) = all.partition { it.repo.full in pins }
        return pinned + rest
    }

    /** Stack-widget content: pinned snapshots when any exist, otherwise
     *  every snapshot in the scenario — so a fresh install with no pins yet
     *  still shows widget content instead of an empty stack. */
    fun stackWidgetRepos(scenario: FixtureScenario, pins: Set<String>, now: Instant): List<RepoSnapshot> {
        val all = Fixtures.snapshots(scenario, now)
        val pinned = all.filter { it.repo.full in pins }
        return pinned.ifEmpty { all }
    }

    /** Case-insensitive substring search over title + number + labels +
     *  author. A blank [query] returns [rows] unchanged (empty query = all). */
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

    /**
     * Navigator rows for [mode] at [scope]/[filter]/[state], composing two
     * [Fixtures.navigatorList] calls for [NavigatorMode.BOTH] (an ISSUES
     * list and a PRS list, sectioned issues-then-prs) since a single
     * [NavigatorList] cannot hold both row types (see [NavigatorRows]).
     *
     * [NavigatorFilter.AWAITING_MY_REVIEW] is PR-only — [Fixtures.navigatorList]
     * throws if asked for it under [NavigatorMode.ISSUES]. In [NavigatorMode.BOTH]
     * with that filter selected, the issues section is therefore returned
     * empty (never populated by silently substituting a different filter's
     * rows); its `valueBasis`/`observedAt` still come from a real ISSUES/OPEN
     * call so the section's data-age label matches the PRS section for the
     * same [state].
     */
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
                // NavigatorList's own init invariant forbids AWAITING_MY_REVIEW
                // paired with Issues rows, so the emptied list keeps its
                // basis source's OPEN filter rather than reporting
                // AWAITING_MY_REVIEW on an issues list — nothing downstream
                // reads NavigatorList.filter, only rows/valueBasis/observedAt.
                val basisSource = Fixtures.navigatorList(scope, NavigatorMode.ISSUES, NavigatorFilter.OPEN, state, now)
                basisSource.copy(rows = NavigatorRows.Issues(emptyList()))
            } else {
                Fixtures.navigatorList(scope, NavigatorMode.ISSUES, filter, state, now)
            }
            NavigatorSection(issues = issues, prs = prs)
        }
    }
}

/** One or both sections of a [NavigatorMode.BOTH] result; exactly one of
 *  [issues]/[prs] is non-null for [NavigatorMode.ISSUES]/[NavigatorMode.PRS],
 *  both are non-null for [NavigatorMode.BOTH]. */
data class NavigatorSection(
    val issues: NavigatorList?,
    val prs: NavigatorList?,
)
