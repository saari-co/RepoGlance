package co.saari.repoglance.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ThemeChoiceGuardTest {
    private val androidNs = "http://schemas.android.com/apk/res/android"
    private val main = "app/src/main/java/co/saari/repoglance"
    private val prefs = source("$main/state/ThemePrefs.kt")
    private val setting = source("$main/ui/settings/ThemeSetting.kt")

    @Test
    fun theChoiceIsAppliedAsPerAppNightModeAfterItIsStored() {
        val choose = section(prefs, "fun choose(context: Context, choice: ThemeChoice) {", "\n    }")
        val store = choose.indexOf("putString(KEY_CHOICE, choice.name)")
        val apply = choose.indexOf("setApplicationNightMode(choice.nightMode)")
        assertTrue("the choice is stored", store >= 0)
        assertTrue("the choice is applied through UiModeManager", apply >= 0)
        assertTrue("stored first, so the recreated screen reads the new value", store < apply)
    }

    @Test
    fun theAppThemeFollowsTheConfigurationAndNeverReadsTheChoiceItself() {
        val theme = source("$main/ui/theme/Theme.kt")
        assertTrue(
            "per-app night mode reaches Compose through the configuration",
            theme.contains("darkTheme: Boolean = isSystemInDarkTheme()"),
        )
        for (file in listOf("$main/ui/theme/Theme.kt", "$main/MainActivity.kt")) {
            val text = source(file)
            assertFalse("$file must not override the theme in Compose", text.contains("ThemePrefs"))
            assertFalse("$file must not override the theme in Compose", text.contains("ThemeChoice"))
        }
    }

    @Test
    fun activitiesRecreateOnANightModeChangeSoTheSystemBarsFollow() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder()
            .parse(repositoryRoot().resolve("app/src/main/AndroidManifest.xml").toFile())
        val activities = manifest.getElementsByTagName("activity")
        assertTrue(activities.length > 0)
        for (index in 0 until activities.length) {
            val activity = activities.item(index) as Element
            val changes = activity.getAttributeNS(androidNs, "configChanges")
            assertFalse(
                "${activity.getAttributeNS(androidNs, "name")} must not handle uiMode itself",
                changes.contains("uiMode"),
            )
        }
    }

    @Test
    fun widgetsAndTheTileStayOnTheSystemTheme() {
        val look = source("$main/widget/WidgetLook.kt")
        assertTrue(look.contains("dynamicLightColorScheme(context)"))
        assertTrue(look.contains("dynamicDarkColorScheme(context)"))
        for (folder in listOf("widget", "tile")) {
            Files.list(repositoryRoot().resolve("$main/$folder")).use { files ->
                files.filter { it.toString().endsWith(".kt") }.forEach { file ->
                    val text = Files.readAllBytes(file).toString(Charsets.UTF_8)
                    assertFalse("$file must not read the in-app theme", text.contains("ThemePrefs"))
                    assertFalse("$file must not read the in-app theme", text.contains("ThemeChoice"))
                }
            }
        }
    }

    @Test
    fun signingOutAndLeavingSampleModeKeepTheChoice() {
        for (file in listOf(
            "$main/RepoGlanceViewModel.kt",
            "$main/state/SampleModeStore.kt",
            "$main/state/AppPrefs.kt",
            "$main/auth/GitHubDeviceFlowClient.kt",
        )) {
            val text = source(file)
            assertFalse("$file must not touch the theme choice", text.contains("ThemePrefs"))
            assertFalse("$file must not touch the theme choice", text.contains("repoglance_theme"))
        }
        assertEquals("repoglance_theme", section(prefs, "internal const val PREFS_NAME = \"", "\"").trim())
    }

    @Test
    fun theSettingSitsInTheSharedSettingsAppearanceSlot() {
        val activity = source("$main/MainActivity.kt")
        assertEquals(1, Regex(Regex.escape("appearance = { ThemeSettingItem() }")).findAll(activity).count())
        val screen = source("$main/ui/settings/SettingsScreen.kt")
        assertTrue(screen.contains("SettingsSectionHead(\"Appearance\")"))
    }

    @Test
    fun theSettingReadsTheStoredChoiceOffTheMainThread() {
        assertEquals(1, Regex(Regex.escape("ThemePrefs.choice(context)")).findAll(setting).count())
        assertTrue(setting.contains("withContext(Dispatchers.IO) { ThemePrefs.choice(context) }"))
    }

    @Test
    fun theSettingKeepsItsStableHandles() {
        for (tag in listOf(
            "repoglance:settings-theme",
            "repoglance:theme-dialog",
            "repoglance:theme-ok",
            "repoglance:theme-cancel",
            "repoglance:theme-\${option.name.lowercase()}",
        )) {
            assertTrue("missing $tag", setting.contains("\"$tag\""))
        }
        assertTrue(setting.contains("headline = \"Theme\""))
        assertTrue(setting.contains("Text(\"Choose theme\")"))
        assertTrue(setting.contains("role = Role.RadioButton"))
        assertTrue(
            "a dialog is its own window, so it publishes its tags as resource ids itself",
            setting.contains(".semantics { testTagsAsResourceId = true }"),
        )
        assertTrue("a radio tap only marks the choice", setting.contains("onClick = { pending = option }"))
        assertTrue("OK applies it", setting.contains("onClick = { onChoose(pending) }"))
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
