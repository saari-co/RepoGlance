package co.saari.repoglance.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class AppUpdateReceiverTest {
    private val androidNs = "http://schemas.android.com/apk/res/android"

    @Test
    fun manifestDeclaresTheUpdateReceiverForExactlyMyPackageReplaced() {
        val receiver = receivers().single { it.getAttributeNS(androidNs, "name") == ".widget.AppUpdateReceiver" }
        assertEquals("false", receiver.getAttributeNS(androidNs, "exported"))
        assertEquals(listOf("android.intent.action.MY_PACKAGE_REPLACED"), actionsOf(receiver))
    }

    @Test
    fun noOtherReceiverRedrawsOnClockLocaleOrBootBroadcasts() {
        val actions = receivers().flatMap { actionsOf(it) }.toSet()
        assertEquals(
            setOf("android.appwidget.action.APPWIDGET_UPDATE", "android.intent.action.MY_PACKAGE_REPLACED"),
            actions,
        )
        assertEquals(1, receivers().count { "android.intent.action.MY_PACKAGE_REPLACED" in actionsOf(it) })
    }

    @Test
    fun receiverRedrawsBothWidgetsFromSavedDataInsideGoAsync() {
        val source = readText(
            repositoryRoot().resolve("app/src/main/java/co/saari/repoglance/widget/AppUpdateReceiver.kt"),
        )
        val body = source.substringAfter("override fun onReceive(")
        assertTrue("only the app's own update is handled", body.contains("Intent.ACTION_MY_PACKAGE_REPLACED"))
        val async = body.indexOf("goAsync()")
        val redraw = body.indexOf("WidgetRefresh.updateAll(")
        val finish = body.indexOf("pending.finish()")
        assertTrue("the redraw runs after goAsync and before the pending result finishes", async in 0 until redraw && redraw < finish)
        assertTrue("finish runs even when the redraw fails", body.substringAfter("finally").contains("pending.finish()"))
        for (forbidden in listOf("GitHub", "Http", "BackgroundRefresh", "WakeLock", "Service", "WorkManager")) {
            assertFalse("the update redraw must not reach $forbidden", source.contains(forbidden))
        }
    }

    private fun receivers(): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(repositoryRoot().resolve("app/src/main/AndroidManifest.xml").toFile())
        val nodes = document.getElementsByTagName("receiver")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun actionsOf(receiver: Element): List<String> {
        val nodes = receiver.getElementsByTagName("action")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(androidNs, "name") }
    }

    private fun repositoryRoot(): Path {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }

    private fun readText(path: Path): String = String(Files.readAllBytes(path), Charsets.UTF_8)
}
