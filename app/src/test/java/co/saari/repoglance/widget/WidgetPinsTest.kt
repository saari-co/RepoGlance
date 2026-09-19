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
    fun widgetHeaderRoutesToTheLiveRepositoryNotTheFixtureNavigator() {
        var dir: java.nio.file.Path? = java.nio.file.Paths.get("").toAbsolutePath()
        while (dir != null && !java.nio.file.Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        val root = requireNotNull(dir)
        fun read(rel: String) = String(java.nio.file.Files.readAllBytes(root.resolve(rel)), Charsets.UTF_8)
        val widget = read("app/src/main/java/co/saari/repoglance/widget/RepoWidget.kt")
        val header = widget.substringAfter("private fun navigatorIntent(").substringBefore("private fun githubIntent(")
        assertEquals(true, header.contains("EXTRA_LIVE_REPO_FULL"))
        assertEquals(false, header.contains("EXTRA_REPO_FULL,") || header.contains("EXTRA_NAVIGATOR_MODE"))
        val rowTap = widget.substringAfter("private fun githubIntent(").substringBefore("\n\n")
        assertEquals(true, rowTap.contains("GitHubAppLauncher.intent("))
        assertEquals(false, rowTap.contains("Intent(Intent.ACTION_VIEW"))
        val activity = read("app/src/main/java/co/saari/repoglance/MainActivity.kt")
        assertEquals(true, activity.contains("EXTRA_LIVE_REPO_FULL") && activity.contains("openRepositoryByName("))
    }

    @Test
    fun releasedRepositoriesAreThoseNoOtherWidgetStillUses() {
        assertEquals(setOf("a/b"), WidgetPins.releasedRepositories(listOf("a/b", "c/d"), listOf("c/d")))
        assertEquals(emptySet<String>(), WidgetPins.releasedRepositories(emptyList(), listOf("c/d")))
    }
}
