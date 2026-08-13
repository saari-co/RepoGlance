package co.saari.repoglance.ui

import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.model.RepoRef
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAccessUiSourceWiringTest {
    private val expectedInstallationSettingsUrl =
        "https://github.com/apps/repoglance-by-saari/installations/new"

    @Test
    fun loadedRepositoryHomeHasNoPersistentBottomBarRepoAndDisconnectControls() {
        val root = repositoryRoot()
        val screenSource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt"),
        )
        val loadedHomeSource = screenSource
            .substringAfter("private fun LiveRepositoryHome(")
            .substringBefore(
                "@Composable\nprivate fun LiveNavigator(",
            )

        assertFalse(
            "The loaded home must stop using Scaffold.bottomBar for GitHub access actions",
            loadedHomeSource.contains("bottomBar = {"),
        )
        assertFalse(
            "Repositories action is now expected in the header menu, not a persistent bar",
            loadedHomeSource.contains("Text(\"Repositories\")"),
        )
        assertFalse(
            "Disconnect action is now expected in the header menu, not a persistent bar",
            loadedHomeSource.contains("Text(\"Disconnect\")"),
        )
    }

    @Test
    fun loadedRepositoryHomeHeaderMenuExposesManageAndDisconnectLabels() {
        val root = repositoryRoot()
        val screenSource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt"),
        )
        val loadedHomeSource = screenSource
            .substringAfter("private fun LiveRepositoryHome(")
            .substringBefore(
                "@Composable\nprivate fun LiveNavigator(",
            )

        assertTrue(
            "Header menu must expose GitHub access management",
            loadedHomeSource.contains("Text(\"Manage GitHub access\")"),
        )
        assertTrue(
            "Header menu must expose disconnect action",
            loadedHomeSource.contains("Text(\"Disconnect GitHub\")"),
        )
        assertFalse(
            "Legacy disconnect dialog label must migrate to new header action label",
            loadedHomeSource.contains("Text(\"Disconnect GitHub?\")"),
        )
        assertTrue(
            "Expected overflow menu entry structure should be present in loaded home",
            loadedHomeSource.contains("DropdownMenuItem"),
        )
    }

    @Test
    fun installationManagementUrlIsExactAndTestable() {
        val root = repositoryRoot()
        val activitySource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/MainActivity.kt"),
        )
        val constantMatch = Regex("""REPOGLANCE_INSTALLATION_SETTINGS_URL\s*=\s*"([^"]+)"""").find(
            activitySource,
        )
        if (constantMatch != null) {
            assertEquals(expectedInstallationSettingsUrl, constantMatch.groupValues[1])
        } else {
            val installFunctionSource = activitySource
                .substringAfter("private fun openInstallationSettings()")
                .substringBefore("private fun resolveFixtureScopeFromIntent(")

            val parsedUrl = Regex("""Uri.parse\("([^"]+)"\)""").find(installFunctionSource)
            assertTrue("Installation-management URL should be hardcoded only as a fallback for testability", parsedUrl != null)
            assertEquals(expectedInstallationSettingsUrl, parsedUrl!!.groupValues[1])
        }
    }

    @Test
    fun loadedHomeOwnerChoicesComeFromDistinctRepositoryOwnersPlusAll() {
        val repositories = listOf(
            repository(1, "saari-co", "RepoGlance"),
            repository(2, "dinkuskit", "kit"),
            repository(3, "saari-co", "x-api"),
            repository(4, "saariuslystoned", "notes"),
            repository(5, "SAARI-CO", "case-variant"),
        )

        assertEquals(
            listOf("dinkuskit", "saari-co", "saariuslystoned"),
            availableRepositoryOwners(repositories),
        )

        val loadedHomeSource = loadedHomeSource()
        assertTrue(
            "All must be present as an explicit owner filter option",
            loadedHomeSource.contains("Text(\"Account or organization\")") &&
                loadedHomeSource.contains("text = { Text(\"All\") }") &&
                loadedHomeSource.contains("onSelectedOwnerChange(null)"),
        )
    }

    @Test
    fun loadedHomeOwnerFilterAppliesBeforeAndWithRepositorySearch() {
        val repositories = listOf(
            repository(1, "saari-co", "RepoGlance"),
            repository(2, "saari-co", "x-api"),
            repository(3, "dinkuskit", "RepoGlance-demo"),
        )

        assertEquals(
            listOf("saari-co/RepoGlance"),
            visibleRepositories(repositories, "saari-co", "glance").map { it.ref.full },
        )
        assertEquals(
            listOf("dinkuskit/RepoGlance-demo"),
            visibleRepositories(repositories, "dinkuskit", "REPOGLANCE").map { it.ref.full },
        )
    }

    @Test
    fun loadedHomeEmptyStateForOwnerAndSearchFiltersRemainsHonest() {
        val loadedHomeSource = loadedHomeSource()

        assertTrue(
            "Matching filter should still render the empty-search-no-matches state",
            loadedHomeSource.contains("if (matchingRepositories.isEmpty())") &&
                loadedHomeSource.contains("\"No repositories match\""),
        )
        assertTrue(
            "Empty repository case should remain a different branch than no-filter-match case",
            loadedHomeSource.contains("if (catalog.repositories.isEmpty())") &&
                loadedHomeSource.contains("} else {") &&
                loadedHomeSource.contains("if (matchingRepositories.isEmpty())"),
        )
    }

    @Test
    fun loadedHomeSelectedOwnerPersistsAcrossCatalogRefreshWithFallbackToAll() {
        val repositories = listOf(repository(1, "saari-co", "RepoGlance"))

        assertEquals("saari-co", retainedRepositoryOwner("SAARI-CO", repositories))
        assertNull(retainedRepositoryOwner("dinkuskit", repositories))

        val screenSource = readText(
            repositoryRoot().resolve("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt"),
        )
        val screenEntrySource = screenSource
            .substringAfter("fun LiveRepoGlanceScreen(")
            .substringBefore("private fun AwaitingGitHubScreen(")
        assertTrue(screenEntrySource.contains("var selectedOwner by rememberSaveable"))
        assertTrue(screenEntrySource.contains("retainedRepositoryOwner(selectedOwner"))
    }

    @Test
    fun returnFromAccessManagementRefreshesCatalogWithoutBreakingResume() {
        val root = repositoryRoot()
        val activitySource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/MainActivity.kt"),
        )
        val onResumeSource = activitySource
            .substringAfter("override fun onResume()")
            .substringBefore("private fun openGitHubVerification")

        assertTrue(onResumeSource.contains("liveModel.resumeGitHubAuthorization()"))
        assertTrue(
            "Access-management return refresh must be gated through resume-safe state",
            onResumeSource.contains("refreshCatalogAfterGitHubAccess"),
        )
        assertTrue(
            "Catalog refresh on access-management return should be wired from onResume",
            onResumeSource.contains("liveModel.refreshCatalog()"),
        )
        assertTrue(
            "Device-flow resume should still run before any access-management refresh",
            onResumeSource.indexOf("liveModel.resumeGitHubAuthorization()") <
                onResumeSource.indexOf("liveModel.refreshCatalog()"),
        )
    }

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")

    private fun readText(path: Path): String = Files.readAllBytes(path).toString(Charsets.UTF_8)

    private fun repository(id: Long, owner: String, name: String): LiveRepository = LiveRepository(
        id = id,
        ref = RepoRef(owner, name),
        isPrivate = false,
        isArchived = false,
        pushedAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun loadedHomeSource(): String {
        val root = repositoryRoot()
        val screenSource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt"),
        )
        return screenSource
            .substringAfter("private fun LiveRepositoryHome(")
            .substringBefore(
                "@Composable\nprivate fun LiveNavigator(",
            )
    }
}
