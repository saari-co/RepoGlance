package co.saari.repoglance.widget

import co.saari.repoglance.model.NavigatorMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WidgetPinsTest {
    @Test
    fun configurationListPutsPinsFirstThenTheCatalogOrderAndKeepsUnknownPins() {
        val names = listOf("a/newest", "b/second", "c/third")
        val list = WidgetPins.configurationList(names, setOf("c/third", "z/pinned-elsewhere")).map { it.full }
        assertEquals(listOf("z/pinned-elsewhere", "c/third", "a/newest", "b/second"), list)
    }

    @Test
    fun configurationListDropsMalformedNames() {
        assertEquals(emptyList<String>(), WidgetPins.configurationList(listOf("not-a-repo"), emptySet()).map { it.full })
    }

    @Test
    fun savedRowsAreFilteredByTheWidgetFeedMode() {
        val now = Instant.parse("2026-09-19T00:00:00Z")
        val rows = listOf(
            WidgetRow(WidgetRowKind.ISSUE, 1, "issue", now, "https://example/1"),
            WidgetRow(WidgetRowKind.PR, 2, "pr", now, "https://example/2"),
        )
        assertEquals(listOf(1), rowsForMode(rows, NavigatorMode.ISSUES).map { it.number })
        assertEquals(listOf(2), rowsForMode(rows, NavigatorMode.PRS).map { it.number })
        assertEquals(listOf(1, 2), rowsForMode(rows, NavigatorMode.BOTH).map { it.number })
    }

    @Test
    fun releasedRepositoriesAreThoseNoOtherWidgetStillUses() {
        assertEquals(setOf("a/b"), WidgetPins.releasedRepositories(listOf("a/b", "c/d"), listOf("c/d")))
        assertEquals(emptySet<String>(), WidgetPins.releasedRepositories(emptyList(), listOf("c/d")))
    }
}
