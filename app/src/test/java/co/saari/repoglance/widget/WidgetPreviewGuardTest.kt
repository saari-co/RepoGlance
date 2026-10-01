package co.saari.repoglance.widget

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreviewGuardTest {
    private val widgetDir = "app/src/main/java/co/saari/repoglance/widget"
    private val preview = source("$widgetDir/WidgetPreview.kt")

    @Test
    fun bothWidgetsDeclareAStaticPreviewLayoutForTheLauncherPicker() {
        for ((info, layout) in listOf(
            "repo_widget_info" to "widget_preview_repository",
            "stack_widget_info" to "widget_preview_pinned",
        )) {
            val xml = source("app/src/main/res/xml/$info.xml")
            assertTrue("$info declares its previewLayout", xml.contains("android:previewLayout=\"@layout/$layout\""))
            val body = source("app/src/main/res/layout/$layout.xml")
            val texts = Regex("android:text=\"([^\"]*)\"").findAll(body).map { it.groupValues[1] }.toList()
            assertTrue(texts.isNotEmpty())
            texts.forEach { assertTrue("$layout text comes from widget_preview strings: $it", it.startsWith("@string/widget_preview_")) }
            assertTrue("$layout shows the sample marker", texts.contains("@string/widget_preview_sample"))
            val views = Regex("<([A-Za-z.]+)").findAll(body).map { it.groupValues[1] }.filterNot { it == "?xml" }.toSet()
            assertTrue("$layout uses RemoteViews-safe views only: $views", views.all { it in setOf("LinearLayout", "TextView") })
        }
    }

    @Test
    fun theStaticPreviewFollowsGlancesDynamicColoursInLightAndDark() {
        val light = colours(source("app/src/main/res/values/widget_preview.xml"))
        val dark = colours(source("app/src/main/res/values-night/widget_preview.xml"))
        assertEquals(light.keys, dark.keys)
        (light.values + dark.values).forEach { assertTrue("dynamic system colour: $it", it.startsWith("@android:color/system_")) }
        assertEquals("@android:color/system_accent3_100", light["widget_preview_tertiary_container"])
        assertEquals("@android:color/system_accent3_700", dark["widget_preview_tertiary_container"])
    }

    @Test
    fun bothWidgetsProvideTheirPreviewFromTheSharedSampleContent() {
        assertTrue(
            source("$widgetDir/RepoWidget.kt").contains(
                "override suspend fun providePreview(context: Context, widgetCategory: Int) =\n" +
                    "        provideWidgetPreview(context, WidgetPreviewKind.REPOSITORY)",
            ),
        )
        assertTrue(
            source("$widgetDir/StackWidget.kt").contains(
                "override suspend fun providePreview(context: Context, widgetCategory: Int) =\n" +
                    "        provideWidgetPreview(context, WidgetPreviewKind.PINNED_REPOS)",
            ),
        )
        assertTrue(preview.contains("override val stateDefinition: GlanceStateDefinition<*>? = null"))
        assertTrue(preview.contains("override suspend fun provideGlance(context: Context, id: GlanceId) = provideWidgetPreview(context, kind)"))
    }

    @Test
    fun thePreviewNeverReadsTheUsersStores() {
        val stores = listOf(
            "SampleModeStore",
            "LiveSnapshotStore",
            "LiveRowsStore",
            "RateLimitStore",
            "RepoWidgetConfigStore",
            "CatalogNamesStore",
            "AppPrefs",
        )
        stores.forEach { assertFalse("previews are sample data only: $it", preview.contains(it)) }
        val drawn = listOf(
            "RepoWidget.kt" to "internal fun CompactContent(",
            "RepoWidget.kt" to "private fun CompactSlot(",
            "RepoWidget.kt" to "private fun CompactCounts(",
            "RepoWidget.kt" to "private fun MergedCounts(",
            "RepoWidget.kt" to "internal fun LedgerRow(",
            "RepoWidget.kt" to "internal fun compactFreshnessLabel(",
            "RepoWidget.kt" to "internal fun widgetClock(",
            "StackWidget.kt" to "internal fun StackHeader(",
            "StackWidget.kt" to "internal fun StackRow(",
            "StackWidget.kt" to "internal fun stackRowAge(",
            "StackWidget.kt" to "internal fun stackRowCounts(",
            "StackWidget.kt" to "internal fun stackHeaderLabel(",
            "StackWidget.kt" to "object StackRows {",
        )
        for ((file, start) in drawn) {
            val text = source("$widgetDir/$file")
            val from = text.indexOf(start)
            assertTrue("missing anchor in $file: $start", from >= 0)
            val body = text.substring(from).substringBefore("\n}\n")
            stores.forEach { assertFalse("$start in $file draws a preview and must not read $it", body.contains(it)) }
        }
        for (file in listOf(
            "$widgetDir/WidgetLook.kt",
            "$widgetDir/SampleWidgetMarks.kt",
            "$widgetDir/SampleWidgetData.kt",
            "app/src/main/java/co/saari/repoglance/sample/SampleAccount.kt",
        )) {
            val text = source(file)
            stores.forEach { assertFalse("$file feeds the preview and must not read $it", text.contains(it)) }
        }
        assertTrue(preview.contains("freshness = WidgetFreshness(now = now, clock = clock, rateLimitedUntil = null, sample = true)"))
    }

    @Test
    fun everyAddHandsTheSheetThePreview() {
        val pinning = source("app/src/main/java/co/saari/repoglance/ui/settings/WidgetPinning.kt")
        assertTrue(pinning.contains("preview = WidgetSheetPreview(WidgetPreviewKind.REPOSITORY)"))
        assertTrue(pinning.contains("preview = WidgetSheetPreview(WidgetPreviewKind.PINNED_REPOS)"))
        assertEquals(2, Regex("requestPinGlanceAppWidget\\(").findAll(pinning).count())
        assertTrue("the sheet preview is composed off the main thread", pinning.contains("withContext(Dispatchers.Default) { pin(context, glance, kind) }"))
        val request = pinning.substringAfter("suspend fun request(").substringBefore("private suspend fun pin(")
        assertTrue(
            "a failed sheet request retries without a preview, and that retry cannot throw",
            request.contains(
                "} catch (_: Exception) {\n" +
                    "            runCatching { manager.requestPinAppWidget(provider(context, kind), null, callback(context, kind)) }\n" +
                    "                .getOrDefault(false)",
            ),
        )
        assertTrue(request.contains("} catch (cancelled: CancellationException) {\n            throw cancelled"))
    }

    @Test
    fun generatedPreviewsArePublishedOncePerStampForTheHomeScreenOnly() {
        val publish = preview.substringAfter("suspend fun publishIfNeeded(context: Context) {").substringBefore("\n    }\n")
        assertTrue(publish.contains("Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return"))
        assertTrue(publish.contains("isDue(published, prefs.getString(receiver.java.name, null), stamp())"))
        assertTrue(publish.contains("intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)"))
        assertTrue(
            "a rate-limited or failed call stores nothing and retries on the next start",
            publish.contains("if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {\n" +
                "                prefs.edit { putString(receiver.java.name, stamp()) }"),
        )
        assertEquals(
            "the provider lookup and setWidgetPreviews both run inside attempt",
            2,
            Regex("attempt \\{").findAll(publish).count(),
        )
        assertTrue(publish.contains("attempt { publishedState(context, receiver) }"))
        assertTrue(publish.contains("val result = attempt {\n                glance.setWidgetPreviews("))
        val attempt = preview.substringAfter("private suspend fun <T> attempt(").substringBefore("\n\n")
        assertTrue(attempt.contains("catch (cancelled: CancellationException) {\n            throw cancelled"))
        assertTrue(attempt.contains("catch (_: Exception) {\n            null"))
        assertTrue(preview.contains("fun stamp(): String = \"\${BuildConfig.VERSION_CODE}.\$LOOK_VERSION\""))
        val app = source("app/src/main/java/co/saari/repoglance/RepoGlanceApplication.kt")
        assertTrue(app.contains("CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { WidgetPreviews.publishIfNeeded(app) }"))
    }

    private fun colours(xml: String): Map<String, String> =
        Regex("<color name=\"([a-z_]+)\">([^<]+)</color>").findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
