package co.saari.repoglance.data

import co.saari.repoglance.model.RepoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class CatalogOrderingTest {
    private val t0 = Instant.parse("2026-09-01T00:00:00Z")
    private fun repo(full: String, id: Long, pushedAt: Instant?) = LiveRepository(
        id = id,
        ref = RepoRef(full.substringBefore('/'), full.substringAfter('/')),
        isPrivate = false,
        isArchived = false,
        pushedAt = pushedAt,
    )

    private val repos = listOf(
        repo("zeta/old", 1, t0),
        repo("alpha/unknown", 2, null),
        repo("mid/newest", 3, t0.plusSeconds(3600)),
        repo("beta/unknown", 4, null),
        repo("gamma/tie", 5, t0),
    )

    @Test
    fun recentPutsNewestPushFirstAndUnknownLastLabelledNotZero() {
        val order = orderRepositories(repos, CatalogSort.RECENT).map { it.ref.full }
        assertEquals(listOf("mid/newest", "gamma/tie", "zeta/old", "alpha/unknown", "beta/unknown"), order)
        assertNull(orderRepositories(repos, CatalogSort.RECENT).last().pushedAt)
    }

    @Test
    fun alphabeticalIgnoresCaseAndPushTime() {
        val order = orderRepositories(repos, CatalogSort.ALPHABETICAL).map { it.ref.full }
        assertEquals(listOf("alpha/unknown", "beta/unknown", "gamma/tie", "mid/newest", "zeta/old"), order)
    }

    @Test
    fun mostRecentlyPushedSkipsUnknownAndIsNullWhenNothingHasAPushTime() {
        assertEquals("mid/newest", mostRecentlyPushed(repos)?.ref?.full)
        assertNull(mostRecentlyPushed(listOf(repo("a/b", 9, null))))
    }
}
