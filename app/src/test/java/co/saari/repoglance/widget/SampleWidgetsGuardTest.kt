package co.saari.repoglance.widget

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleWidgetsGuardTest {
    private val liveStores = listOf(
        "RepoWidgetConfigStore.",
        "LiveSnapshotStore.",
        "LiveRowsStore.",
        "RateLimitStore.",
        "AppPrefs.",
        "CatalogNamesStore.",
        "LatestPushStore.",
        "BackgroundRefresh.",
        "LiveRefresh.",
        "GitHubAppLauncher.",
    )
    private val sampleSources = listOf("SampleModeStore.", "SampleWidgetData.", "SampleAccount.")

    private val repoWidget = source("app/src/main/java/co/saari/repoglance/widget/RepoWidget.kt")
    private val stackWidget = source("app/src/main/java/co/saari/repoglance/widget/StackWidget.kt")
    private val setup = source("app/src/main/java/co/saari/repoglance/widget/RepoWidgetConfigActivity.kt")

    @Test
    fun widgetsPickSampleDataOnlyWhileSampleModeIsOn() {
        for ((widget, reader) in listOf(repoWidget to "readRepoWidgetData(", stackWidget to "readStackWidgetData(")) {
            val body = section(widget, "internal fun $reader", "\ninternal fun ")
            assertTrue("$reader branches on the sample flag", body.contains("if (SampleModeStore.isActive(context))"))
        }
    }

    @Test
    fun sampleWidgetsNeverReadTheLiveStores() {
        val readers = listOf(
            section(repoWidget, "internal fun readSampleRepoWidgetData(", "\n}\n"),
            section(stackWidget, "internal fun readSampleStackWidgetData(", "\n}\n"),
        )
        for (reader in readers) {
            for (store in liveStores) assertFalse("a sample widget must not read $store", reader.contains(store))
            assertTrue(reader.contains("SampleModeStore."))
            assertTrue("sample widgets say sample, never a clock", reader.contains("sample = true"))
            assertTrue("sample widgets never show a rate limit", reader.contains("rateLimitedUntil = null"))
        }
    }

    @Test
    fun signedInWidgetsNeverReadSampleData() {
        val readers = listOf(
            section(repoWidget, "internal fun readLiveRepoWidgetData(", "\n}\n"),
            section(stackWidget, "internal fun readLiveStackWidgetData(", "\n}\n"),
        )
        for (reader in readers) {
            for (sample in sampleSources) assertFalse("a live widget must not read $sample", reader.contains(sample))
            assertFalse(reader.contains("sample = true"))
        }
    }

    @Test
    fun sampleRowTapsOpenTheAppNeverGitHub() {
        val intent = section(repoWidget, "internal fun rowIntent(", "\n\n")
        assertTrue(intent.contains("if (freshness.sample) appIntent else githubIntent(row)"))
        val body = section(repoWidget, "private fun TallBody(", "\nprivate const val MAX_WIDGET_ROWS")
        assertTrue(body.contains("rowIntent(row, freshness, appIntent)"))
        assertFalse("rows reach GitHub only through rowIntent", body.contains("githubIntent("))
    }

    @Test
    fun theUnconfiguredWidgetOpensItsOwnSetupAndTheFixtureLabelIsGone() {
        assertEquals("RepoGlance", UNCONFIGURED_TITLE)
        assertEquals("Tap to choose a repository", UNCONFIGURED_PROMPT)
        assertFalse(repoWidget.contains("FIXTURE PREVIEW"))
        val intent = section(repoWidget, "internal fun widgetSetupIntent(", "\n\n")
        assertTrue(intent.contains("RepoWidgetConfigActivity::class.java"))
        assertTrue(intent.contains("putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)"))
        assertTrue("one pending intent per widget", intent.contains(".appendPath(appWidgetId.toString())"))
        val unconfigured = section(repoWidget, "private fun UnconfiguredContent(", "\n}\n")
        assertTrue(unconfigured.contains(".clickable(actionStartActivity(setupIntent))"))
        assertTrue(repoWidget.contains("UnconfiguredContent(widgetSetupIntent(context, appWidgetId))"))
    }

    @Test
    fun sampleSetupListsSampleReposPinsSampleAndNeverRefreshesFromGitHub() {
        val load = section(setup, "internal fun loadWidgetSetup(", "\ninternal const val")
        val sampleLoad = section(load, "if (SampleModeStore.isActive(context)) {", "} else {")
        assertTrue(sampleLoad.contains("SampleWidgetData.configurationList(SampleModeStore.pins(context)"))
        assertTrue(sampleLoad.contains("SampleModeStore.widgetConfig(context, appWidgetId)"))
        for (store in liveStores) assertFalse("sample setup must not read $store", sampleLoad.contains(store))

        val save = section(setup, "private fun saveSampleAndFinish(", "\n    private fun saveAndFinish(")
        assertTrue(save.contains("SampleModeStore.saveWidgetConfig("))
        assertTrue("saving pins the sample repository", save.contains("SampleModeStore.addPin(context, repo.full)"))
        assertTrue("repointing releases the old sample pin", save.contains("SampleModeStore.removePins("))
        assertTrue("the stack redraws with the new pin", save.contains("WidgetRefresh.updateAll(context)"))
        for (store in liveStores) assertFalse("sample save must not touch $store", save.contains(store))

        val create = section(setup, "override fun onCreate(", "\n    private fun showSetup(")
        assertTrue("setup reads its lists off the main thread", create.contains("withContext(Dispatchers.IO) { loadWidgetSetup("))
        assertFalse(create.contains("CatalogNamesStore.") || create.contains("SampleModeStore."))
    }

    @Test
    fun removingASampleWidgetReleasesItsSamplePin() {
        val receiver = source("app/src/main/java/co/saari/repoglance/widget/RepoWidgetReceiver.kt")
        val deleted = section(receiver, "override fun onDeleted(", "super.onDeleted(")
        assertTrue(deleted.contains("SampleModeStore.removeWidgetConfigs(context, appWidgetIds.toList())"))
        assertTrue(deleted.contains("WidgetPins.releasedRepositories(removedSample, SampleModeStore.widgetRepos(context))"))
        val after = receiver.substringAfter("super.onDeleted(context, appWidgetIds)")
        assertTrue("the stack drops the released pin at once", after.contains("WidgetRefresh.updateStacks(app)"))
    }

    @Test
    fun theTileReadsSampleDataOnlyInSampleModeAndNeverTheSavedRecordThere() {
        val service = source("app/src/main/java/co/saari/repoglance/tile/RepoGlanceTileService.kt")
        val listening = section(service, "override fun onStartListening()", "override fun onClick()")
        val sampleBranch = section(listening, "if (SampleModeStore.isActive(this)) {", "} else {")
        assertTrue(sampleBranch.contains("TileTexts.sample("))
        assertFalse(sampleBranch.contains("LatestPushStore."))
        val liveBranch = section(listening, "} else {", "main.post")
        assertTrue(liveBranch.contains("TileTexts.of(LatestPushStore.load(this)"))
        assertFalse(liveBranch.contains("SampleAccount."))
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
