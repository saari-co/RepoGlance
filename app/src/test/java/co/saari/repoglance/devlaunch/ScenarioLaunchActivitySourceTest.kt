package co.saari.repoglance.devlaunch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioLaunchActivitySourceTest {

    @Test
    fun launcherPreferenceWritesRunOffTheMainThreadBeforeTheNextScreen() {
        val source = String(
            Files.readAllBytes(root().resolve("app/src/debug/java/co/saari/repoglance/devlaunch/ScenarioLaunchActivity.kt")),
            Charsets.UTF_8,
        )
        val onCreate = source.substringAfter("override fun onCreate(").substringBefore("\n    }\n")
        val io = onCreate.substringAfter("withContext(Dispatchers.IO) {").substringBefore("\n            }\n")
        assertTrue("the scenario write runs on Dispatchers.IO", io.contains("AppPrefs.setSelectedScenario("))
        assertTrue("the probe write runs on Dispatchers.IO", io.contains("RefreshProbe.arm("))
        val outsideIo = onCreate.replace(io, "")
        assertTrue(!outsideIo.contains("AppPrefs.") && !outsideIo.contains("RefreshProbe."))
        val afterIo = onCreate.substringAfter("withContext(Dispatchers.IO) {").substringAfter("\n            }\n")
        assertTrue("the next screen starts after the writes", afterIo.contains("startActivity(next)"))
    }

    private fun root(): Path {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }
}
