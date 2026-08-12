package co.saari.repoglance.widget

import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.fixtures.ListState
import co.saari.repoglance.link.GitHubLinks
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorRows
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.state.SnapshotStore
import java.time.Instant

enum class WidgetRowKind { ISSUE, PR }

data class WidgetRow(
    val kind: WidgetRowKind,
    val number: Int,
    val title: String,
    val updatedAt: Instant,
    val url: String,
)

/** Fixture-only projection used by widget configuration and rendering. The
 * catalog is stable across fixture scenario switches; current-scenario data
 * wins when available, with a deterministic catalog fallback otherwise. */
object WidgetFixtureData {
    private val catalogScenarioOrder = listOf(
        FixtureScenario.EXACT,
        FixtureScenario.LAST_GOOD,
        FixtureScenario.UNKNOWN,
        FixtureScenario.RATE_LIMITED,
        FixtureScenario.NO_CI,
    )

    fun availableSnapshots(now: Instant): List<RepoSnapshot> =
        catalogScenarioOrder
            .flatMap { Fixtures.snapshots(it, now) }
            .distinctBy { it.repo.full }

    fun snapshotFor(
        repo: RepoRef,
        scenario: FixtureScenario,
        now: Instant,
    ): RepoSnapshot? = Fixtures.snapshots(scenario, now)
        .firstOrNull { it.repo == repo }
        ?: availableSnapshots(now).firstOrNull { it.repo == repo }

    /** A single recently-updated feed. BOTH intentionally interleaves issue
     * and PR rows by update time instead of presenting two sections. */
    fun recentRows(repo: RepoRef, mode: NavigatorMode, now: Instant): List<WidgetRow> {
        val section = SnapshotStore.navigatorRows(
            scope = NavigatorScope.Repo(repo),
            mode = mode,
            filter = NavigatorFilter.OPEN,
            state = ListState.LOADED,
            now = now,
        )
        val issues = section.issues
            ?.rows
            ?.let { it as NavigatorRows.Issues }
            ?.rows
            .orEmpty()
            .map { row ->
                WidgetRow(
                    kind = WidgetRowKind.ISSUE,
                    number = row.number,
                    title = row.title,
                    updatedAt = row.updatedAt,
                    url = GitHubLinks.issue(repo, row.number),
                )
            }
        val prs = section.prs
            ?.rows
            ?.let { it as NavigatorRows.Prs }
            ?.rows
            .orEmpty()
            .map { row ->
                WidgetRow(
                    kind = WidgetRowKind.PR,
                    number = row.number,
                    title = row.title,
                    updatedAt = row.updatedAt,
                    url = GitHubLinks.pull(repo, row.number),
                )
            }
        return (issues + prs).sortedByDescending { it.updatedAt }
    }
}
