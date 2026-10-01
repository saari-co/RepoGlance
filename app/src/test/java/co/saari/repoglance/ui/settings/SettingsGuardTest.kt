package co.saari.repoglance.ui.settings

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGuardTest {
    private val settingsDir = "app/src/main/java/co/saari/repoglance/ui/settings"
    private val settings = source("$settingsDir/SettingsScreen.kt")
    private val widgets = source("$settingsDir/WidgetsScreen.kt")
    private val placed = source("$settingsDir/PlacedWidgets.kt")
    private val pinning = source("$settingsDir/WidgetPinning.kt")
    private val screen = source("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt")
    private val activity = source("app/src/main/java/co/saari/repoglance/MainActivity.kt")

    @Test
    fun settingsAndWidgetsNeverWriteToGitHubOrTheStores() {
        val forbidden = listOf(
            "GitHubApiClient",
            "apiClient",
            "HttpTransport",
            "LiveRefresh",
            "BackgroundRefresh",
            ".save(",
            "addPin",
            "togglePin",
            "removePins",
            "addLivePin",
            "removeLivePins",
            "clearAll",
            ".edit",
            "signOut",
        )
        for ((name, text) in listOf("SettingsScreen" to settings, "WidgetsScreen" to widgets, "PlacedWidgets" to placed, "WidgetPinning" to pinning)) {
            for (call in forbidden) assertFalse("$name must stay read-only: $call", text.contains(call))
        }
        assertTrue("Disconnect is the one session action and the activity owns it", activity.contains("liveModel.signOut()"))
    }

    @Test
    fun theConnectScreenMenuOffersOnlySettings() {
        val connect = section(screen, "private fun ConnectGitHubScreen(", "private fun SampleModeBar(")
        assertTrue(connect.contains("AppMenu(\n            onOpenWidgets = null,\n            onOpenSettings = onOpenSettings,"))
        val widgetsRow = settings.indexOf("headline = \"Widgets\"")
        assertTrue(widgetsRow >= 0)
        assertTrue(
            "Settings hides Widgets when there is nothing a widget could show",
            settings.lastIndexOf("if (account != SettingsAccount.SIGNED_OUT) {", widgetsRow) >= 0,
        )
    }

    @Test
    fun pinRequestsGoThroughOneGuardedHelper() {
        val request = section(pinning, "suspend fun request(context: Context, kind: WidgetKind): Boolean {", "\n    fun setupIntent(")
        val guard = request.indexOf("if (!manager.isRequestPinAppWidgetSupported) return false")
        assertTrue(guard >= 0)
        assertTrue(guard < request.indexOf("requestPinGlanceAppWidget("))
        assertTrue(
            "the repository widget opens its setup after the launcher adds it",
            request.contains("Intent(context, RepoWidgetConfigActivity::class.java)") && request.contains("FLAG_MUTABLE"),
        )
        val main = Paths.get("app/src/main/java")
        val callers = Files.walk(repositoryRoot().resolve(main)).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }
                .filter { path ->
                    val text = Files.readAllBytes(path).toString(Charsets.UTF_8)
                    text.contains(".requestPinAppWidget(") || text.contains(".requestPinGlanceAppWidget(")
                }
                .map { it.fileName.toString() }
                .toList()
        }
        assertEquals(listOf("WidgetPinning.kt"), callers)
    }

    @Test
    fun theWidgetsScreenReadsTheHomeScreenOffTheMainThreadOnEveryResume() {
        val screenBody = section(widgets, "fun WidgetsScreen(", "\n@Composable\nfun WidgetsContent(")
        assertTrue(screenBody.contains("LifecycleResumeEffect(Unit)"))
        assertTrue(screenBody.contains("withContext(Dispatchers.IO) { loadWidgetsUiState(context) }"))
        assertTrue(
            "placed widgets are unknown until read, never an empty list",
            screenBody.contains("placed = null"),
        )
        assertTrue(
            "pin support is known before the first frame, so Add never flashes on a launcher that cannot pin",
            screenBody.contains("pinSupported = WidgetPinning.isSupported(context)"),
        )
        assertTrue(widgets.contains("placed == null -> item(key = \"placed-loading\")"))
        val loader = section(placed, "internal fun loadWidgetsUiState(context: Context): WidgetsUiState {", "\n}\n")
        assertTrue("sample widgets list their sample setups", loader.contains("if (sample) SampleModeStore.widgetConfig(context, id)"))
        assertTrue(loader.contains("if (sample) SampleModeStore.pins(context) else AppPrefs.livePins(context)"))
    }

    @Test
    fun aRecreatedActivityKeepsSettingsOpenAndWidgetTapsCloseIt() {
        assertTrue(activity.contains("outState.putStringArray(\n            STATE_SETTINGS_DESTINATIONS,"))
        assertTrue(activity.contains("?.getStringArray(STATE_SETTINGS_DESTINATIONS)"))
        val live = section(activity, "private fun handleLiveIntent(intent: Intent?) {", "\n    }\n\n")
        assertEquals(
            "a widget or tile tap shows the repository, not Settings",
            2,
            Regex(Regex.escape("settingsDestinations.value = emptyList()")).findAll(live).count(),
        )
    }

    @Test
    fun theLauncherPickerUsesTheSameWidgetNames() {
        val manifest = source("app/src/main/AndroidManifest.xml")
        val strings = source("app/src/main/res/values/strings.xml")
        assertTrue(manifest.contains("android:name=\".widget.RepoWidgetReceiver\"\n            android:exported=\"true\"\n            android:label=\"@string/widget_repo_label\""))
        assertTrue(manifest.contains("android:name=\".widget.StackWidgetReceiver\"\n            android:exported=\"true\"\n            android:label=\"@string/widget_stack_label\""))
        assertTrue(strings.contains("<string name=\"widget_repo_label\">Repository</string>"))
        assertTrue(strings.contains("<string name=\"widget_stack_label\">Pinned repos</string>"))
        val userFacing = Regex("<string name=\"widget_[a-z_]+\">([^<]+)</string>").findAll(strings).map { it.groupValues[1] }.toList()
        assertTrue(userFacing.size >= 4)
        userFacing.forEach { assertFalse("no user-facing 'stack': $it", it.contains("stack", ignoreCase = true)) }
    }

    @Test
    fun theWidgetsScreenShowsTheLockedPreviewTiles() {
        val content = section(widgets, "fun WidgetsContent(", "\n@Composable\nprivate fun WidgetTiles(")
        assertTrue("widgets-look-047 locked C preview tiles", content.contains("item(key = \"previews\") { WidgetTiles(state.pinSupported, onAdd) }"))
        assertTrue(content.indexOf("WidgetTiles(") < content.indexOf("SettingsSectionHead(WIDGETS_PLACED_HEAD)"))
        val tiles = section(widgets, "private fun WidgetTiles(", "\nprivate fun LazyListScope.placedRows(")
        assertTrue(tiles.contains("WidgetKind.entries.forEach"))
        assertTrue(tiles.contains("WidgetSketch(kind)"))
        assertTrue("each Add names its widget for TalkBack", tiles.contains("contentDescription = \"Add \${kind.title}\""))
        assertTrue("Add is hidden where the launcher cannot pin; the how-to shows instead", tiles.contains("if (pinSupported) {"))
        assertTrue(content.contains("if (!state.pinSupported) item(key = \"how-to\") { WidgetsNote(WIDGETS_HOW_TO) }"))
        assertFalse("the round's seam is gone once locked", widgets.contains("LocalWidgetsLook"))
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
