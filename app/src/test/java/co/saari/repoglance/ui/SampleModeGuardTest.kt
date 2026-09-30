package co.saari.repoglance.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleModeGuardTest {
    private val viewModel = source("app/src/main/java/co/saari/repoglance/RepoGlanceViewModel.kt")
    private val screen = source("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt")
    private val activity = source("app/src/main/java/co/saari/repoglance/MainActivity.kt")

    private val setSampleMode = section(viewModel, "fun setSampleMode(active: Boolean) {", "\n    private fun loadCatalog(")

    @Test
    fun enteringSampleNeverReachesGitHubOrTheSessionOrTheLiveStores() {
        for (forbidden in listOf(
            "apiClient",
            "session.",
            "deviceFlowClient",
            "recordLatestPush",
            "LiveRefresh",
            "WidgetRefresh",
            "beginGitHubAuthorization",
            "signOut(",
            "clearSavedSession",
            "BackgroundRefresh",
        )) {
            assertFalse("setSampleMode must not touch $forbidden", setSampleMode.contains(forbidden))
        }
        val enter = section(setSampleMode, "if (active) {", "} else {")
        assertTrue(enter.contains("SampleAccount.catalog(now)"))
        assertTrue(enter.contains("rateLimit = SampleAccount.RATE_LIMIT"))
        assertTrue(enter.contains("SampleModeStore.enter(context)"))

        val refresh = section(viewModel, "fun refreshCatalog() {", "fun setSampleMode(")
        assertTrue(
            "a sample refresh returns before any network load",
            refresh.indexOf("if (sampleMode.value)") in 0 until refresh.indexOf("loadCatalog("),
        )
    }

    @Test
    fun sampleRepositoryContentReturnsBeforeTheNetworkTheSessionAndTheWidgetStores() {
        val refresh = section(viewModel, "fun refreshSelectedRepository() {", "\n    fun backToRepositories()")
        val sampleBranch = refresh.indexOf("if (sampleMode.value)")
        assertTrue(sampleBranch >= 0)
        assertTrue(sampleBranch < refresh.indexOf("session.generation()"))
        assertTrue(sampleBranch < refresh.indexOf("apiClient."))
        assertTrue(sampleBranch < refresh.indexOf("LiveRefresh.persist("))
        val branch = refresh.substring(sampleBranch).substringBefore("return\n")
        assertTrue(branch.contains("SampleAccount.content("))
        assertFalse(branch.contains("LiveRefresh"))
        assertFalse(branch.contains("WidgetRefresh"))
    }

    @Test
    fun sampleModePersistsUntilTheUserChoosesToSignIn() {
        val bootstrap = section(viewModel, "private fun bootstrapSessionState()", "private suspend fun hasSavedSession()")
        val stored = section(bootstrap, "val storedSample = withContext(sessionDispatcher) {", "}")
        assertTrue("the sample flag is read off the main thread", stored.contains("SampleModeStore.isActive(context)"))
        assertTrue(
            "the app prefs file loads off the main thread before any screen reads it",
            stored.contains("AppPrefs.preload(context)"),
        )
        val branches = section(bootstrap, "when {", "else -> liveState.value = LiveUiState.SignedOut")
        assertTrue(
            "a real session wins over a stored sample flag",
            branches.indexOf("hasSavedSession() ->") in 0 until branches.indexOf("storedSample -> setSampleMode(true)"),
        )
        assertTrue(
            "a stale sample flag is cleared when a real session wins",
            section(branches, "hasSavedSession() ->", "refreshCatalog()").contains("SampleModeStore.leave(context)"),
        )

        val transition = section(setSampleMode, "if (active != sampleMode.value) {", "}")
        assertTrue("entering or leaving sample drops any open repository", transition.contains("backToRepositories()"))
        assertTrue(transition.contains("pendingRepositoryFull = null"))
        assertTrue(
            "the transition is detected before the new mode is stored",
            setSampleMode.indexOf("if (active != sampleMode.value)") in 0 until
                setSampleMode.indexOf("sampleMode.value = active"),
        )

        val leave = section(setSampleMode, "} else {", "\n    }\n")
        assertTrue("leaving lands on the Connect screen", leave.contains("liveState.value = LiveUiState.SignedOut"))
        assertTrue(leave.contains("SampleModeStore.leave(context)"))
        assertTrue(setSampleMode.contains("sampleMode.value = active"))

        assertTrue(activity.contains("onExploreSampleData = { liveModel.setSampleMode(true) },"))
        assertTrue(activity.contains("onLeaveSampleData = { liveModel.setSampleMode(false) },"))
    }

    @Test
    fun sampleRowsNeverOpenGitHub() {
        val open = section(screen, "    fun open(url: String) {", "\n    }\n")
        val sampleBranch = open.indexOf("if (sampleMode)")
        assertTrue(sampleBranch >= 0)
        assertTrue(sampleBranch < open.indexOf("GitHubAppLauncher.open("))
        assertTrue(open.substring(sampleBranch).substringBefore("return").contains("SampleAccount.ITEM_NOTE"))
    }

    @Test
    fun sampleScreensCarryTheSampleMarkerAndHideLiveOnlyControls() {
        val home = section(screen, "private fun LiveRepositoryHome(", "private fun DisconnectDialog(")
        assertTrue(home.contains("if (sampleMode) SampleModeBar(onSignIn = onLeaveSampleData)"))
        assertTrue(home.contains("SampleModeStore.togglePin(context, repository.ref.full)"))
        val samplePinBranch = section(section(home, "onTogglePin = {", "},\n"), "if (sampleMode) {", "} else {")
        assertTrue(samplePinBranch.contains("SampleModeStore.togglePin(context, repository.ref.full)"))
        assertFalse("sample pin taps never refresh the live widgets", samplePinBranch.contains("WidgetRefresh"))
        assertFalse(samplePinBranch.contains("AppPrefs."))

        val title = section(screen, "private fun CatalogTitleRow(", "private fun CatalogRateLimitLine(")
        val liveChip = title.indexOf("LabelText(\"LIVE\"")
        assertTrue(liveChip >= 0)
        assertTrue("the LIVE chip is only for a real session", title.lastIndexOf("if (!sampleMode) {", liveChip) >= 0)
        val menu = title.indexOf("Text(\"Manage GitHub access\")")
        assertTrue(menu >= 0)
        val menuGuard = title.lastIndexOf("if (!sampleMode) {", menu)
        assertTrue("Manage GitHub access and Disconnect are hidden in sample mode", menuGuard > liveChip)

        val navigator = section(screen, "private fun LiveNavigator(", "private fun androidx.compose")
        assertTrue(navigator.contains("if (sampleMode) SampleModeBar(onSignIn = onLeaveSampleData)"))

        val connect = section(screen, "private fun ConnectGitHubScreen(", "private fun SampleModeBar(")
        val connectTag = connect.indexOf("repoglance:connect-github")
        assertTrue(connectTag >= 0)
        assertTrue(connectTag < connect.indexOf("EXPLORE_SAMPLE_TEST_TAG"))
    }

    @Test
    fun samplePinsLiveInTheirOwnStore() {
        val store = source("app/src/main/java/co/saari/repoglance/state/SampleModeStore.kt")
        val prefs = source("app/src/main/java/co/saari/repoglance/state/AppPrefs.kt")
        val sampleFile = Regex("PREFS_NAME = \"([^\"]+)\"").find(store)!!.groupValues[1]
        val appFile = Regex("PREFS_NAME = \"([^\"]+)\"").find(prefs)!!.groupValues[1]
        assertNotEquals(appFile, sampleFile)
        assertFalse(store.contains("live_pinned_repos"))
    }

    private fun section(text: String, start: String, end: String): String {
        val from = text.indexOf(start)
        assertTrue("missing anchor: $start", from >= 0)
        val body = text.substring(from + start.length)
        val to = body.indexOf(end)
        assertTrue("missing end anchor after $start: $end", to >= 0)
        return body.substring(0, to)
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
