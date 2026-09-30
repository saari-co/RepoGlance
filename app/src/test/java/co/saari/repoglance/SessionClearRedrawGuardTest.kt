package co.saari.repoglance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SessionClearRedrawGuardTest {
    private val viewModel = source("app/src/main/java/co/saari/repoglance/RepoGlanceViewModel.kt")
    private val ioRedraw = "withContext(Dispatchers.IO) { WidgetRefresh.updateAll(context) }"

    @Test
    fun theSignOutRedrawSurvivesOnClearedClosingTheSessionDispatcher() {
        val cleared = section(viewModel, "override fun onCleared() {", "\n    }\n")
        assertTrue("the premise: onCleared closes the session dispatcher", cleared.contains("sessionDispatcher.close()"))

        val helper = section(viewModel, "private suspend fun clearSessionAndTileRecord() {", "\n    override fun onCleared()")
        assertEquals("the session clear redraws once", 1, occurrences(helper, "WidgetRefresh."))
        assertTrue(
            "the sign-out redraw runs on Dispatchers.IO, so its suspensions never resume onto the closed session dispatcher",
            helper.contains(ioRedraw),
        )
        assertTrue(
            "widgets redraw after the stores are cleared, outside the session lock",
            helper.substringAfter("session.signOut {").substringAfter("\n        }\n").contains(ioRedraw),
        )
    }

    @Test
    fun everySessionClearRunsTheHelperOnTheSessionDispatcherPastCancellation() {
        val launched = section(viewModel, "private fun clearSavedSession() {", "\n    }\n")
        assertTrue(launched.contains("viewModelScope.launch(sessionDispatcher + NonCancellable) { clearSessionAndTileRecord() }"))
        val awaited = section(
            viewModel,
            "private suspend fun clearSavedSessionNow() = withContext(sessionDispatcher + NonCancellable) {",
            "\n    }\n",
        )
        assertTrue(awaited.contains("clearSessionAndTileRecord()"))
        assertEquals("only these two paths reach the helper", 3, occurrences(viewModel, "clearSessionAndTileRecord()"))
    }

    @Test
    fun noViewModelRedrawRunsDirectlyOnTheSessionDispatcher() {
        val bare = viewModel.replace(ioRedraw, "")
        assertEquals(
            "a ViewModel redraw runs inside withContext(Dispatchers.IO) or on viewModelScope's main dispatcher, " +
                "never directly on sessionDispatcher",
            1,
            occurrences(bare, "WidgetRefresh."),
        )
        val content = section(viewModel, "fun refreshSelectedRepository() {", "\n    fun backToRepositories()")
        val mainLaunch = section(content, "repositoryContentLoadJob = viewModelScope.launch {", "\n        }\n")
        assertTrue("the one bare redraw follows a repository persist on the main dispatcher", mainLaunch.contains("WidgetRefresh."))
    }

    private fun occurrences(text: String, needle: String): Int = Regex(Regex.escape(needle)).findAll(text).count()

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
