package co.saari.repoglance.devlaunch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcaseLaunchTest {
    @Test
    fun entryDropsPinsAndWidgetSetupsThatAreNotTheShowcasePersonas() {
        val mixed =
            setOf("saari-co/rocket", "saltmarsh-io/rocket", "dinkuskit/infra", "ferrywood/infra", "someone/else")
        assertEquals(setOf("saari-co/rocket", "dinkuskit/infra", "someone/else"), ShowcaseLaunch.stalePins(mixed))
        val showcaseOnly = setOf("saltmarsh-io/rocket", "elin-tidewater/dotfiles")
        assertEquals(emptySet<String>(), ShowcaseLaunch.stalePins(showcaseOnly))
        val all = mapOf(
            "active" to true,
            "widget.7.repo" to "saari-co/rocket",
            "widget.7.mode" to "BOTH",
            "widget.9.repo" to "saltmarsh-io/rocket",
            "widget.9.mode" to "ISSUES",
            "widget.11.repo" to 42,
        )
        assertEquals(setOf("widget.7.repo", "widget.7.mode"), ShowcaseLaunch.staleWidgetKeys(all))
    }

    @Test
    fun theKeyLayoutMatchesSampleModeStore() {
        val store = source("app/src/main/java/co/saari/repoglance/state/SampleModeStore.kt")
        val launch = source("app/src/debug/java/co/saari/repoglance/devlaunch/ShowcaseLaunch.kt")
        val keys = listOf(
            "KEY_PINS = \"pins\"",
            "WIDGET_PREFIX = \"widget.\"",
            "REPO_SUFFIX = \".repo\"",
            "MODE_SUFFIX = \".mode\"",
        )
        for (line in keys) {
            assertTrue("store declares $line", store.contains(line))
            assertTrue("launch mirrors $line", launch.contains(line))
        }
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
