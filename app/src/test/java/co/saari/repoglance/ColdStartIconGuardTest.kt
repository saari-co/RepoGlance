package co.saari.repoglance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class ColdStartIconGuardTest {
    private val androidNs = "http://schemas.android.com/apk/res/android"
    private val res = "app/src/main/res"

    @Test
    fun applicationDeclaresTheAdaptiveLauncherIconAndTheSystemFollowingTheme() {
        val application = manifest().getElementsByTagName("application").item(0) as Element
        assertEquals("@mipmap/ic_launcher", application.getAttributeNS(androidNs, "icon"))
        assertEquals("@mipmap/ic_launcher", application.getAttributeNS(androidNs, "roundIcon"))
        assertEquals("@style/Theme.RepoGlance", application.getAttributeNS(androidNs, "theme"))
    }

    @Test
    fun launcherIconHasBackgroundForegroundAndMonochromeLayers() {
        val icon = parse("$res/mipmap-anydpi/ic_launcher.xml").documentElement
        assertEquals("adaptive-icon", icon.tagName)
        val layers = mapOf(
            "background" to "@drawable/ic_launcher_background",
            "foreground" to "@drawable/ic_launcher_foreground",
            "monochrome" to "@drawable/ic_launcher_monochrome",
        )
        for ((tag, drawable) in layers) {
            val layer = icon.getElementsByTagName(tag).item(0) as Element?
            assertTrue("the adaptive icon has a $tag layer", layer != null)
            assertEquals(drawable, layer!!.getAttributeNS(androidNs, "drawable"))
        }
    }

    @Test
    fun startWindowBackgroundMatchesTheDynamicAppBackgroundInBothModes() {
        assertEquals("@android:color/system_neutral1_10", windowBackground("values"))
        assertEquals("@android:color/system_neutral1_900", windowBackground("values-night"))
        assertEquals("@android:color/system_background_light", windowBackground("values-v34"))
        assertEquals("@android:color/system_background_dark", windowBackground("values-night-v34"))
    }

    @Test
    fun eachStartWindowFolderUsesTheParentForItsMode() {
        val light = "@android:style/Theme.Material.Light.NoActionBar"
        val dark = "@android:style/Theme.Material.NoActionBar"
        assertEquals(light, theme("values").getAttribute("parent"))
        assertEquals(light, theme("values-v34").getAttribute("parent"))
        assertEquals(dark, theme("values-night").getAttribute("parent"))
        assertEquals(dark, theme("values-night-v34").getAttribute("parent"))
    }

    @Test
    fun quickSettingsTileUsesTheRingedGlyphInManifestAndService() {
        val nodes = manifest().getElementsByTagName("service")
        val tile = (0 until nodes.length).map { nodes.item(it) as Element }
            .single { it.getAttributeNS(androidNs, "name") == ".tile.RepoGlanceTileService" }
        assertEquals("@drawable/ic_repoglance_glyph", tile.getAttributeNS(androidNs, "icon"))
        val service = readText(root().resolve("app/src/main/java/co/saari/repoglance/tile/RepoGlanceTileService.kt"))
        assertTrue(service.contains("Icon.createWithResource(this, R.drawable.ic_repoglance_glyph)"))
        assertTrue("the tile no longer uses the in-app mark", !service.contains("ic_repoglance_mark"))
    }

    private fun theme(folder: String): Element {
        val styles = parse("$res/$folder/themes.xml").getElementsByTagName("style")
        return (0 until styles.length).map { styles.item(it) as Element }
            .single { it.getAttribute("name") == "Theme.RepoGlance" }
    }

    private fun windowBackground(folder: String): String {
        val items = theme(folder).getElementsByTagName("item")
        val background = (0 until items.length).map { items.item(it) as Element }
            .single { it.getAttribute("name") == "android:windowBackground" }
        return background.textContent.trim()
    }

    private fun manifest(): Document = parse("app/src/main/AndroidManifest.xml")

    private fun parse(relative: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(root().resolve(relative).toFile())
    }

    private fun root(): Path {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }

    private fun readText(path: Path): String = String(Files.readAllBytes(path), Charsets.UTF_8)
}
