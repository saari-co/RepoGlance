package co.saari.repoglance.state

import co.saari.repoglance.data.LiveIssue
import co.saari.repoglance.data.LivePullRequest
import co.saari.repoglance.widget.WidgetRow
import co.saari.repoglance.widget.WidgetRowKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LiveRowsStoreTest {
    private val now = Instant.parse("2026-09-19T00:00:00Z")

    @Test
    fun rowsRoundTripThroughTheStoreFormat() {
        val rows = listOf(
            WidgetRow(WidgetRowKind.ISSUE, 41, "Widget shows stale counts", now.minusSeconds(60), "https://example/41"),
            WidgetRow(WidgetRowKind.PR, 39, "Add tile lock guard", now.minusSeconds(120), "https://example/39"),
        )
        assertEquals(rows, LiveRowsStore.decode(LiveRowsStore.encode(rows)))
    }

    @Test
    fun rowsFromKeepsTheNewestTenOfEachKindNewestFirst() {
        val issues = (1..12).map { issue(it, now.minusSeconds(it * 10L)) }
        val prs = (1..12).map { pr(100 + it, now.minusSeconds(it * 10L + 5)) }
        val rows = LiveRowsStore.rowsFrom(issues, prs)
        assertEquals(LiveRowsStore.MAX_ROWS_PER_KIND * 2, rows.size)
        assertEquals(LiveRowsStore.MAX_ROWS_PER_KIND, rows.count { it.kind == WidgetRowKind.ISSUE })
        assertEquals(listOf(1, 101, 2, 102, 3), rows.take(5).map { it.number })
    }

    @Test
    fun aKindThatDominatesRecencyCannotStarveTheOtherKind() {
        val prs = (1..10).map { pr(100 + it, now.minusSeconds(it.toLong())) }
        val issues = (1..3).map { issue(it, now.minusSeconds(3600L + it)) }
        val rows = LiveRowsStore.rowsFrom(issues, prs)
        assertEquals(3, rows.count { it.kind == WidgetRowKind.ISSUE })
        assertEquals(3, LiveRowsStore.decode(LiveRowsStore.encode(rows))!!.count { it.kind == WidgetRowKind.ISSUE })
    }

    @Test
    fun aPartialOrFailedFetchNeverReplacesSavedRows() {
        assertNull(LiveRowsStore.replacementRows(null, listOf(pr(1, now))))
        assertNull(LiveRowsStore.replacementRows(listOf(issue(1, now)), null))
        assertNull(LiveRowsStore.replacementRows(null, null))
        assertEquals(emptyList<WidgetRow>(), LiveRowsStore.replacementRows(emptyList(), emptyList()))
    }

    @Test
    fun theViewModelOnlySavesRowsThroughReplacementRows() {
        var dir: java.nio.file.Path? = java.nio.file.Paths.get("").toAbsolutePath()
        while (dir != null && !java.nio.file.Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        val source = String(
            java.nio.file.Files.readAllBytes(
                requireNotNull(dir).resolve("app/src/main/java/co/saari/repoglance/RepoGlanceViewModel.kt"),
            ),
            Charsets.UTF_8,
        )
        assertEquals(1, Regex("LiveRowsStore\\.replacementRows\\(").findAll(source).count())
        assertEquals(0, Regex("LiveRowsStore\\.rowsFrom\\(").findAll(source).count())
    }

    @Test
    fun unknownVersionDecodesToNothingRatherThanStaleRows() {
        assertNull(LiveRowsStore.decode("""{"version":99,"rows":[]}"""))
    }

    private fun issue(number: Int, updatedAt: Instant) = LiveIssue(
        number = number, title = "Issue $number", author = "bobby", assignee = null, labels = emptyList(),
        commentCount = 0, updatedAt = updatedAt, htmlUrl = "https://example/i/$number",
    )

    private fun pr(number: Int, updatedAt: Instant) = LivePullRequest(
        number = number, title = "PR $number", author = "bobby", assignee = null, labels = emptyList(),
        isDraft = false, reviewRequestedFromViewer = false, updatedAt = updatedAt, htmlUrl = "https://example/p/$number",
    )
}
