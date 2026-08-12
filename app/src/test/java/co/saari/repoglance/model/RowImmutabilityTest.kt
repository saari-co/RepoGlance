package co.saari.repoglance.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class RowImmutabilityTest {

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val repo = RepoRef("saari-co", "RepoGlance")

    @Test
    fun issueRowDefensivelySnapshotsAndProtectsLabels() {
        val sourceLabels = mutableListOf("bug")
        val row = IssueRow(
            repo = repo,
            number = 1,
            title = "Issue",
            state = "open",
            labels = sourceLabels,
            author = "octodev",
            assignee = null,
            commentCount = 0,
            updatedAt = now,
        )
        val originalHash = row.hashCode()

        sourceLabels += "mutated"

        assertEquals(listOf("bug"), row.labels)
        assertEquals(originalHash, row.hashCode())
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (row.labels as MutableList<String>).add("blocked")
        }
    }

    @Test
    fun prRowDefensivelySnapshotsAndProtectsLabels() {
        val sourceLabels = mutableListOf("review")
        val row = PrRow(
            repo = repo,
            number = 2,
            title = "PR",
            state = "open",
            labels = sourceLabels,
            author = "octodev",
            assignee = null,
            commentCount = 0,
            updatedAt = now,
            isDraft = false,
            reviewState = ReviewState.REVIEW_REQUIRED,
            ciRollup = CiState.PASSING,
        )
        val originalHash = row.hashCode()

        sourceLabels += "mutated"

        assertEquals(listOf("review"), row.labels)
        assertEquals(originalHash, row.hashCode())
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (row.labels as MutableList<String>).add("blocked")
        }
    }
}
