package co.saari.repoglance.hooks

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureRouteGuardTest {
    @Test
    fun releaseSourcesNeitherReadTheFixtureExtrasNorNameTheFixtureScreens() {
        val shipped = shippedSources()
        assertTrue("the scan reads MainActivity", shipped.containsKey("app/src/main/java/co/saari/repoglance/MainActivity.kt"))
        assertTrue("the scan reads the release hooks", shipped.containsKey("app/src/release/java/co/saari/repoglance/hooks/FixtureRoute.kt"))
        val forbidden = listOf(
            Regex("(?<!live_)repo_full"),
            Regex("navigator_mode"),
            Regex("\\bEXTRA_REPO_FULL\\b"),
            Regex("\\bEXTRA_NAVIGATOR_MODE\\b"),
            Regex("\\bHomeScreen\\b"),
            Regex("\\bNavigatorScreen\\b"),
            Regex("\\bFixtureRoot\\b"),
        )
        val hits = shipped.flatMap { (path, text) ->
            forbidden.filter { it.containsMatchIn(text) }.map { "$path: ${it.pattern}" }
        }
        assertEquals("the fixture route is debug-only", emptyList<String>(), hits)
    }

    @Test
    fun releaseSourcesNameNoFixtureOnlyHelper() {
        val forbidden = listOf(
            Regex("\\bNavigatorScopeCodec\\b"),
            Regex("\\bCiSemanticRole\\b"),
            Regex("\\bStatusPill\\b"),
            Regex("\\b(?:selectedScenario|setSelectedScenario|rememberScenario|rememberPinnedRepos)\\b"),
            Regex("\\bAppPrefs\\.(?:pinnedRepos|togglePin)\\b"),
            Regex("\"(?:selected_scenario|pinned_repos)\""),
        )
        val hits = shippedSources().flatMap { (path, text) ->
            forbidden.filter { it.containsMatchIn(text) }.map { "$path: ${it.pattern}" }
        }
        assertEquals("the fixture helpers are debug-only", emptyList<String>(), hits)
    }

    @Test
    fun theFixtureHelpersLiveInTheDebugSourceSet() {
        for (file in listOf("state/NavigatorScopeCodec.kt", "state/FixturePrefs.kt", "render/CiSemanticRole.kt", "ui/theme/StatusPill.kt")) {
            assertTrue(file, Files.exists(repositoryRoot().resolve("app/src/debug/java/co/saari/repoglance/$file")))
        }
    }

    @Test
    fun theFixtureRouteAndItsScreensLiveInTheDebugSourceSet() {
        val debug = source("app/src/debug/java/co/saari/repoglance/hooks/FixtureRoute.kt")
        assertTrue(debug.contains("internal const val EXTRA_REPO_FULL: String = \"repo_full\""))
        assertTrue(debug.contains("HomeScreen(") && debug.contains("NavigatorScreen("))
        for (screen in listOf("HomeScreen.kt", "NavigatorScreen.kt")) {
            assertTrue(screen, Files.exists(repositoryRoot().resolve("app/src/debug/java/co/saari/repoglance/ui/$screen")))
        }
    }

    @Test
    fun theReleaseRouteIsANoOpThatNeverReadsAnIntent() {
        val release = source("app/src/release/java/co/saari/repoglance/hooks/FixtureRoute.kt")
        assertEquals(
            listOf("android.content.Intent", "androidx.compose.runtime.Composable"),
            Regex("^import (.+)$", RegexOption.MULTILINE).findAll(release).map { it.groupValues[1] }.toList(),
        )
        assertTrue(release.contains("val isOpen: Boolean get() = false"))
        assertFalse(release.contains("getStringExtra"))
        val functions = Regex("^\\s*fun .+$", RegexOption.MULTILINE).findAll(release).map { it.value.trim() }.toList()
        assertEquals(4, functions.size)
        functions.forEach { assertTrue("release hook does nothing: $it", it.endsWith(") = Unit")) }
    }

    companion object {
        val DEBUG_ONLY_CLASSES = listOf(
            "co.saari.repoglance.ui.HomeScreenKt",
            "co.saari.repoglance.ui.NavigatorScreenKt",
            "co.saari.repoglance.hooks.FixtureRouteKt",
            "co.saari.repoglance.state.NavigatorScopeCodec",
            "co.saari.repoglance.state.FixturePrefsKt",
            "co.saari.repoglance.render.CiSemanticRole",
            "co.saari.repoglance.ui.theme.StatusPillKt",
        )

        val DEBUG_ONLY_MEMBERS = listOf(
            MovedMembers(
                shippedHost = "co.saari.repoglance.state.AppPrefs",
                debugHolder = "co.saari.repoglance.state.FixturePrefsKt",
                names = setOf(
                    "selectedScenario",
                    "setSelectedScenario",
                    "pinnedRepos",
                    "togglePin",
                    "rememberScenario",
                    "rememberPinnedRepos",
                ),
            ),
            MovedMembers(
                shippedHost = "co.saari.repoglance.ui.theme.ControlShapeKt",
                debugHolder = "co.saari.repoglance.ui.theme.StatusPillKt",
                names = setOf("StatusPill"),
            ),
        )

        fun loads(name: String): Boolean =
            runCatching { Class.forName(name, false, FixtureRouteGuardTest::class.java.classLoader) }.isSuccess

        fun declaredMethodNames(name: String): Set<String> =
            Class.forName(name, false, FixtureRouteGuardTest::class.java.classLoader)
                .declaredMethods.mapTo(mutableSetOf()) { it.name }
    }

    data class MovedMembers(val shippedHost: String, val debugHolder: String, val names: Set<String>)

    private fun shippedSources(): Map<String, String> =
        kotlinSources("app/src/main/java") + kotlinSources("app/src/release/java")

    private fun kotlinSources(relative: String): Map<String, String> {
        val root = repositoryRoot()
        return Files.walk(root.resolve(relative)).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }.toList()
        }.associate { root.relativize(it).toString() to Files.readAllBytes(it).toString(Charsets.UTF_8) }
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
