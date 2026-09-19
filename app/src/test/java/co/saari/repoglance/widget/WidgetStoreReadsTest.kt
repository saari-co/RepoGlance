package co.saari.repoglance.widget

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val STORE_READS = listOf(
    "AppPrefs.",
    "LiveSnapshotStore.",
    "LiveRowsStore.",
    "RateLimitStore.",
    "RepoWidgetConfigStore.",
    "CatalogNamesStore.",
    "widgetClock(",
    "Instant.now(",
)

internal fun assertWidgetReadsStayOutOfComposition(source: String, reader: String) {
    val glance = source.substringAfter("override suspend fun provideGlance(").substringBefore("\n    }\n")
    val composition = glance.substringAfter("provideContent {")
    assertTrue("provideGlance must read through $reader", glance.contains(reader))
    assertTrue("the first read must be dispatched off the main thread", glance.contains("readWidgetStores(read)"))
    assertTrue("a redraw must re-read the stores", composition.contains("redrawnWidgetData(initial, read)"))
    for (read in STORE_READS) {
        assertFalse("composition must not read $read", composition.contains(read))
    }
    val readerBody = source.substringAfter("internal fun $reader").substringBefore("\n}\n")
    assertTrue("$reader must be a plain function, not composable", readerBody.isNotEmpty())
    assertFalse("$reader must not be composable", source.substringBefore("internal fun $reader").trimEnd().endsWith("@Composable"))
}

class WidgetStoreReadsTest {

    @Test
    fun storeReadsRunOnTheIoDispatcher() {
        val caller = Thread.currentThread()
        val reader = runBlocking { readWidgetStores { Thread.currentThread() } }
        assertNotEquals(caller, reader)
        assertTrue(reader.name, reader.name.startsWith("DefaultDispatcher-worker"))
    }

    @Test
    fun aRedrawBumpReReadsTheStoresOffTheMainThread() {
        val source = String(Files.readAllBytes(root().resolve("app/src/main/java/co/saari/repoglance/widget/WidgetRefresh.kt")), Charsets.UTF_8)
        val helper = source.substringAfter("internal fun <T> redrawnWidgetData(")
        assertTrue(helper.contains("LaunchedEffect(currentState(WidgetRefresh.REDRAW_KEY)) { data = readWidgetStores(currentRead) }"))
        assertTrue(source.contains("withContext(Dispatchers.IO)"))
    }

    @Test
    fun theRepoWidgetReadsItsStoresOffTheMainThreadAndOutsideComposition() {
        val source = String(Files.readAllBytes(root().resolve("app/src/main/java/co/saari/repoglance/widget/RepoWidget.kt")), Charsets.UTF_8)
        assertWidgetReadsStayOutOfComposition(source, "readRepoWidgetData(")
    }

    private fun root(): Path {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }
}
