package co.saari.repoglance.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class NavigatorListInvariantsTest {

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val emptyRows = NavigatorRows.Issues(emptyList())
    private val populatedRows = NavigatorRows.Issues(
        listOf(
            IssueRow(
                number = 1,
                title = "Issue",
                state = "open",
                labels = emptyList(),
                author = "octodev",
                assignee = null,
                commentCount = 0,
                updatedAt = now,
            ),
        ),
    )

    private fun list(
        valueBasis: ValueBasis,
        rows: NavigatorRows,
        observedAt: Instant?,
    ) = NavigatorList(
        filter = NavigatorFilter.OPEN,
        rows = rows,
        valueBasis = valueBasis,
        observedAt = observedAt,
    )

    @Test
    fun unknownBasisRejectsPopulatedRows() {
        assertThrows(IllegalArgumentException::class.java) {
            list(ValueBasis.UNKNOWN, populatedRows, null)
        }
    }

    @Test
    fun unknownBasisRequiresNullObservedAt() {
        assertThrows(IllegalArgumentException::class.java) {
            list(ValueBasis.UNKNOWN, emptyRows, now)
        }
    }

    @Test
    fun exactAndLastGoodRequireObservedAt() {
        for (basis in listOf(ValueBasis.EXACT, ValueBasis.LAST_GOOD)) {
            assertThrows(IllegalArgumentException::class.java) {
                list(basis, populatedRows, null)
            }
        }
    }

    @Test
    fun validUnknownAndKnownListsConstruct() {
        assertEquals(0, list(ValueBasis.UNKNOWN, emptyRows, null).rows.size)
        assertEquals(1, list(ValueBasis.EXACT, populatedRows, now).rows.size)
        assertEquals(1, list(ValueBasis.LAST_GOOD, populatedRows, now).rows.size)
    }
}
