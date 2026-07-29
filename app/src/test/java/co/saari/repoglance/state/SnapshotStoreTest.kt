package co.saari.repoglance.state

import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.fixtures.ListState
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorRows
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SnapshotStoreTest {

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    // ---- repoList: pin sorting -----------------------------------------

    @Test
    fun repoListWithNoPinsPreservesFixtureOrder() {
        val expected = Fixtures.snapshots(FixtureScenario.EXACT, now).map { it.repo.full }
        val actual = SnapshotStore.repoList(FixtureScenario.EXACT, emptySet(), now).map { it.repo.full }
        assertEquals(expected, actual)
    }

    @Test
    fun repoListPutsPinnedReposFirstStableOtherwise() {
        val all = Fixtures.snapshots(FixtureScenario.EXACT, now)
        // Pin the last repo in fixture order; everything else must keep its
        // relative order after it.
        val pinnedFull = all.last().repo.full
        val result = SnapshotStore.repoList(FixtureScenario.EXACT, setOf(pinnedFull), now)

        assertEquals(pinnedFull, result.first().repo.full)
        val rest = result.drop(1).map { it.repo.full }
        val expectedRest = all.dropLast(1).map { it.repo.full }
        assertEquals(expectedRest, rest)
    }

    @Test
    fun togglingPinRecomputesOrder() {
        val all = Fixtures.snapshots(FixtureScenario.EXACT, now)
        val target = all.last().repo.full

        val beforePin = SnapshotStore.repoList(FixtureScenario.EXACT, emptySet(), now)
        assertEquals(all.map { it.repo.full }, beforePin.map { it.repo.full })

        val afterPin = SnapshotStore.repoList(FixtureScenario.EXACT, setOf(target), now)
        assertEquals(target, afterPin.first().repo.full)

        val afterUnpin = SnapshotStore.repoList(FixtureScenario.EXACT, emptySet(), now)
        assertEquals(all.map { it.repo.full }, afterUnpin.map { it.repo.full })
    }

    // ---- stackWidgetRepos: fallback --------------------------------------

    @Test
    fun stackWidgetReposReturnsAllWhenNoPins() {
        val all = Fixtures.snapshots(FixtureScenario.MIXED, now)
        val result = SnapshotStore.stackWidgetRepos(FixtureScenario.MIXED, emptySet(), now)
        assertEquals(all.map { it.repo.full }, result.map { it.repo.full })
    }

    @Test
    fun stackWidgetReposReturnsOnlyPinnedWhenPresent() {
        val all = Fixtures.snapshots(FixtureScenario.EXACT, now)
        val pinnedFull = all.first().repo.full
        val result = SnapshotStore.stackWidgetRepos(FixtureScenario.EXACT, setOf(pinnedFull), now)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.repo.full == pinnedFull })
    }

    // ---- search -----------------------------------------------------------

    @Test
    fun searchWithBlankQueryReturnsAllRows() {
        val list = Fixtures.navigatorList(
            NavigatorScope.Account,
            NavigatorMode.ISSUES,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        val result = SnapshotStore.search(list.rows, "")
        assertEquals(list.rows.size, result.size)
    }

    @Test
    fun searchMatchesTitleCaseInsensitive() {
        val list = Fixtures.navigatorList(
            NavigatorScope.Account,
            NavigatorMode.ISSUES,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        val rows = (list.rows as NavigatorRows.Issues).rows
        val needle = rows.first().title.substring(0, 5).uppercase()
        val result = SnapshotStore.search(list.rows, needle) as NavigatorRows.Issues
        assertTrue(result.rows.isNotEmpty())
        assertTrue(result.rows.all { it.title.lowercase().contains(needle.lowercase()) })
    }

    @Test
    fun searchMatchesIssueNumber() {
        val list = Fixtures.navigatorList(
            NavigatorScope.Account,
            NavigatorMode.ISSUES,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        val rows = (list.rows as NavigatorRows.Issues).rows
        val target = rows.first().number
        val result = SnapshotStore.search(list.rows, target.toString()) as NavigatorRows.Issues
        assertTrue(result.rows.any { it.number == target })
    }

    @Test
    fun searchMatchesLabelCaseInsensitive() {
        val list = Fixtures.navigatorList(
            NavigatorScope.Account,
            NavigatorMode.ISSUES,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        val rows = (list.rows as NavigatorRows.Issues).rows
        val label = rows.first().labels.first()
        val result = SnapshotStore.search(list.rows, label.uppercase()) as NavigatorRows.Issues
        assertTrue(result.rows.isNotEmpty())
        assertTrue(result.rows.all { row -> row.labels.any { it.equals(label, ignoreCase = true) } })
    }

    @Test
    fun searchMatchesAuthorCaseInsensitive() {
        val list = Fixtures.navigatorList(
            NavigatorScope.Account,
            NavigatorMode.PRS,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        val rows = (list.rows as NavigatorRows.Prs).rows
        val author = rows.first().author
        val result = SnapshotStore.search(list.rows, author.uppercase()) as NavigatorRows.Prs
        assertTrue(result.rows.isNotEmpty())
        assertTrue(result.rows.all { it.author.equals(author, ignoreCase = true) })
    }

    @Test
    fun searchWithNoMatchesReturnsEmpty() {
        val list = Fixtures.navigatorList(
            NavigatorScope.Account,
            NavigatorMode.ISSUES,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        val result = SnapshotStore.search(list.rows, "zzz-no-such-token-zzz")
        assertEquals(0, result.size)
    }

    // ---- navigatorRows: BOTH-mode composition ------------------------------

    @Test
    fun issuesModeYieldsOnlyIssuesSection() {
        val section = SnapshotStore.navigatorRows(
            NavigatorScope.Account,
            NavigatorMode.ISSUES,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        assertTrue(section.issues != null)
        assertNull(section.prs)
    }

    @Test
    fun prsModeYieldsOnlyPrsSection() {
        val section = SnapshotStore.navigatorRows(
            NavigatorScope.Account,
            NavigatorMode.PRS,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        assertNull(section.issues)
        assertTrue(section.prs != null)
    }

    @Test
    fun bothModeYieldsBothSectionsPopulatedForOpenFilter() {
        val section = SnapshotStore.navigatorRows(
            NavigatorScope.Account,
            NavigatorMode.BOTH,
            NavigatorFilter.OPEN,
            ListState.LOADED,
            now,
        )
        assertTrue(section.issues!!.rows.size > 0)
        assertTrue(section.prs!!.rows.size > 0)
    }

    @Test
    fun bothModeAwaitingMyReviewYieldsEmptyIssuesAndPopulatedPrs() {
        val section = SnapshotStore.navigatorRows(
            NavigatorScope.Account,
            NavigatorMode.BOTH,
            NavigatorFilter.AWAITING_MY_REVIEW,
            ListState.LOADED,
            now,
        )
        assertEquals(0, section.issues!!.rows.size)
        assertTrue(section.prs!!.rows.size > 0)
        assertTrue((section.prs!!.rows as NavigatorRows.Prs).rows.all { it.reviewState == ReviewState.REVIEW_REQUIRED })
    }

    @Test
    fun scenarioPassesThroughToRepoList() {
        for (scenario in FixtureScenario.entries) {
            val expected = Fixtures.snapshots(scenario, now).map { it.repo.full }.toSet()
            val actual = SnapshotStore.repoList(scenario, emptySet(), now).map { it.repo.full }.toSet()
            assertEquals(expected, actual)
        }
    }
}
