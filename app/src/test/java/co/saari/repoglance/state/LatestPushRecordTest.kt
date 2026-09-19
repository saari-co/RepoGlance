package co.saari.repoglance.state

import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.model.RepoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

class LatestPushRecordTest {
    private val now = Instant.parse("2026-09-18T12:00:00Z")
    private fun repo(full: String, id: Long, pushedAt: Instant?) = LiveRepository(
        id = id,
        ref = RepoRef(full.substringBefore('/'), full.substringAfter('/')),
        isPrivate = false,
        isArchived = false,
        pushedAt = pushedAt,
    )

    @Test
    fun emptyCatalogYieldsNoRecordSoTheStoreIsCleared() {
        assertNull(latestPushRecordFor(emptyList(), now))
    }

    @Test
    fun unknownOnlyCatalogYieldsNoRecordSoTheStoreIsCleared() {
        assertNull(latestPushRecordFor(listOf(repo("a/b", 1, null), repo("c/d", 2, null)), now))
    }

    @Test
    fun pushedCatalogYieldsTheNewestRepository() {
        val record = latestPushRecordFor(
            listOf(repo("a/b", 1, now.minusSeconds(60)), repo("c/d", 2, now.minusSeconds(10)), repo("e/f", 3, null)),
            now,
        )
        assertEquals(LatestPushRecord("c/d", now.minusSeconds(10), now), record)
    }

    @Test
    fun everySessionClearPathAlsoClearsTheTileRecord() {
        val source = readText(repositoryRoot().resolve("app/src/main/java/co/saari/repoglance/RepoGlanceViewModel.kt"))
        val helper = source.substringAfter("fun clearSessionAndTileRecord()").substringBefore("\n    }\n")
        assertTrue("session clear must drop the tile record before the token", helper.contains("LatestPushStore.clear("))
        assertTrue(helper.indexOf("LatestPushStore.clear(") < helper.indexOf("session.signOut()"))
        assertTrue("session clear must drop live pins too", helper.contains("AppPrefs.clearLivePins("))
        for (call in listOf("LiveSnapshotStore.clear(", "LiveRowsStore.clear(", "RepoWidgetConfigStore.clearAll(", "CatalogNamesStore.clear(")) {
            assertTrue("session clear must drop widget-visible data: $call", helper.contains(call))
            assertTrue(helper.indexOf(call) < helper.indexOf("session.signOut()"))
        }
        assertTrue("placed widgets must re-render after the clear", helper.contains("WidgetRefresh.updateAll("))
        assertTrue(helper.indexOf("AppPrefs.clearLivePins(") < helper.indexOf("session.signOut()"))
        assertTrue(
            "no session-clear path may bypass the shared helper",
            Regex("session\\.signOut\\(\\)").findAll(source).count() == 1,
        )
        val storeReplace = source.substringAfter("private fun recordLatestPush(").substringBefore("\n    }\n")
        assertTrue("a catalog with no push record must clear, not keep, the tile record", storeReplace.contains("LatestPushStore.replace("))
    }

    private fun repositoryRoot(): Path {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }

    private fun readText(path: Path): String = String(Files.readAllBytes(path), Charsets.UTF_8)
}
