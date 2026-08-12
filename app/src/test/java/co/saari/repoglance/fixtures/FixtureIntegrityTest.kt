package co.saari.repoglance.fixtures

import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorList
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorRows
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.ReviewState
import co.saari.repoglance.model.ValueBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FixtureIntegrityTest {

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun everyScenarioSnapshotHasCleanRepoAndReleaseText() {
        for (scenario in FixtureScenario.entries) {
            for (snapshot in Fixtures.snapshots(scenario, now)) {
                assertTrue(Sanitize.isClean(snapshot.repo.owner))
                assertTrue(Sanitize.isClean(snapshot.repo.name))
                snapshot.latestRelease?.let { assertTrue(Sanitize.isClean(it.tag)) }
            }
        }
    }

    @Test
    fun unknownScenarioIsAllUnknownBasis() {
        val snapshots = Fixtures.snapshots(FixtureScenario.UNKNOWN, now)
        assertTrue(snapshots.isNotEmpty())
        for (snapshot in snapshots) {
            assertEquals(ValueBasis.UNKNOWN, snapshot.valueBasis)
        }
    }

    @Test
    fun rateLimitedScenarioIsAllExhausted() {
        val snapshots = Fixtures.snapshots(FixtureScenario.RATE_LIMITED, now)
        assertTrue(snapshots.isNotEmpty())
        for (snapshot in snapshots) {
            assertEquals(RateLimitBucket.EXHAUSTED, snapshot.rateLimit)
        }
    }

    @Test
    fun noCiScenarioIsAllNoCi() {
        val snapshots = Fixtures.snapshots(FixtureScenario.NO_CI, now)
        assertTrue(snapshots.isNotEmpty())
        for (snapshot in snapshots) {
            assertEquals(CiState.NO_CI, snapshot.defaultBranchCi)
        }
    }

    @Test
    fun emptyScenarioIsEmpty() {
        assertTrue(Fixtures.snapshots(FixtureScenario.EMPTY, now).isEmpty())
    }

    @Test
    fun mixedScenarioCoversAtLeastFourDistinctCombosIncludingLastGoodUnknownExhausted() {
        val snapshots = Fixtures.snapshots(FixtureScenario.MIXED, now)
        val combos = snapshots.map { Triple(it.valueBasis, it.defaultBranchCi, it.rateLimit) }.toSet()
        assertTrue("expected >= 4 distinct combos, got ${combos.size}", combos.size >= 4)
        assertEquals(snapshots.size, snapshots.map { it.repo }.toSet().size)
        assertTrue(snapshots.any { it.valueBasis == ValueBasis.LAST_GOOD })
        assertTrue(snapshots.any { it.valueBasis == ValueBasis.UNKNOWN })
        assertTrue(snapshots.any { it.rateLimit == RateLimitBucket.EXHAUSTED })
    }

    @Test
    fun pagedNavigatorListHasExactlyPageSizeRowsAndMoreFlag() {
        val list = Fixtures.navigatorList(
            scope = NavigatorScope.Account,
            mode = NavigatorMode.ISSUES,
            filter = NavigatorFilter.OPEN,
            state = ListState.PAGED,
            now = now,
        )
        assertEquals(NavigatorList.PAGE_SIZE, list.rows.size)
        assertTrue(list.hasMorePages)
    }

    @Test
    fun awaitingMyReviewIssueModeListThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            Fixtures.navigatorList(
                scope = NavigatorScope.Account,
                mode = NavigatorMode.ISSUES,
                filter = NavigatorFilter.AWAITING_MY_REVIEW,
                state = ListState.LOADED,
                now = now,
            )
        }
    }

    @Test
    fun awaitingMyReviewPrModeListWorksAndAllRowsReviewRequired() {
        val list = Fixtures.navigatorList(
            scope = NavigatorScope.Account,
            mode = NavigatorMode.PRS,
            filter = NavigatorFilter.AWAITING_MY_REVIEW,
            state = ListState.LOADED,
            now = now,
        )
        val rows = (list.rows as NavigatorRows.Prs).rows
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.reviewState == ReviewState.REVIEW_REQUIRED })
        assertTrue(rows.all { it.state == "open" })
        assertTrue(rows.none { it.isDraft })
    }

    @Test
    fun openFilterProducesOnlyOpenRows() {
        for (mode in listOf(NavigatorMode.ISSUES, NavigatorMode.PRS)) {
            val list = Fixtures.navigatorList(
                scope = NavigatorScope.Account,
                mode = mode,
                filter = NavigatorFilter.OPEN,
                state = ListState.PAGED,
                now = now,
            )
            when (val rows = list.rows) {
                is NavigatorRows.Issues -> assertTrue(rows.rows.all { it.state == "open" })
                is NavigatorRows.Prs -> assertTrue(rows.rows.all { it.state == "open" })
            }
        }
    }

    @Test
    fun mineFilterRowsAreAssignedToYou() {
        val list = Fixtures.navigatorList(
            scope = NavigatorScope.Account,
            mode = NavigatorMode.ISSUES,
            filter = NavigatorFilter.MINE,
            state = ListState.LOADED,
            now = now,
        )
        val rows = (list.rows as NavigatorRows.Issues).rows
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.assignee == "octodev" || it.author == "octodev" })
    }

    @Test
    fun mentionsFilterTitlesMentionYou() {
        val list = Fixtures.navigatorList(
            scope = NavigatorScope.Account,
            mode = NavigatorMode.ISSUES,
            filter = NavigatorFilter.MENTIONS,
            state = ListState.LOADED,
            now = now,
        )
        val rows = (list.rows as NavigatorRows.Issues).rows
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.title.contains("@octodev") })
    }

    @Test
    fun lastGoodNavigatorListHasBasisAndPastObservedAt() {
        val list = Fixtures.navigatorList(
            scope = NavigatorScope.Account,
            mode = NavigatorMode.PRS,
            filter = NavigatorFilter.OPEN,
            state = ListState.LAST_GOOD,
            now = now,
        )
        assertEquals(ValueBasis.LAST_GOOD, list.valueBasis)
        assertTrue(list.observedAt!!.isBefore(now))
    }

    @Test
    fun emptyStateListHasNoRowsAndExactBasis() {
        val list = Fixtures.navigatorList(
            scope = NavigatorScope.Account,
            mode = NavigatorMode.ISSUES,
            filter = NavigatorFilter.OPEN,
            state = ListState.EMPTY,
            now = now,
        )
        assertEquals(0, list.rows.size)
        assertEquals(ValueBasis.EXACT, list.valueBasis)
    }

    @Test
    fun everyLoadedNavigatorRowTextIsClean() {
        for (mode in listOf(NavigatorMode.ISSUES, NavigatorMode.PRS)) {
            for (filter in NavigatorFilter.entries) {
                if (filter == NavigatorFilter.AWAITING_MY_REVIEW && mode == NavigatorMode.ISSUES) continue

                val list = Fixtures.navigatorList(
                    scope = NavigatorScope.Account,
                    mode = mode,
                    filter = filter,
                    state = ListState.LOADED,
                    now = now,
                )
                when (val rows = list.rows) {
                    is NavigatorRows.Issues -> rows.rows.forEach { row ->
                        assertTrue(Sanitize.isClean(row.title))
                        assertTrue(Sanitize.isClean(row.author))
                        row.labels.forEach { assertTrue(Sanitize.isClean(it)) }
                    }
                    is NavigatorRows.Prs -> rows.rows.forEach { row ->
                        assertTrue(Sanitize.isClean(row.title))
                        assertTrue(Sanitize.isClean(row.author))
                        row.labels.forEach { assertTrue(Sanitize.isClean(it)) }
                    }
                }
            }
        }
    }
}
