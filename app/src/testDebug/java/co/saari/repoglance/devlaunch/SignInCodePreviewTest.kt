package co.saari.repoglance.devlaunch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInCodePreviewTest {
    @Test
    fun thePreviewShowsAFixtureCodeAndNeverTalksToGitHub() {
        assertEquals("HK7N-4R2D", SignInCodePreviewActivity.FIXTURE_USER_CODE)
        assertTrue(Regex("[A-Z0-9]{4}-[A-Z0-9]{4}").matches(SignInCodePreviewActivity.FIXTURE_USER_CODE))
        val source = source("app/src/debug/java/co/saari/repoglance/devlaunch/SignInCodePreviewActivity.kt")
        val forbiddenCalls =
            listOf("DeviceFlow", "beginGitHubAuthorization", "RepoGlanceViewModel", "HttpURLConnection", "okhttp")
        for (forbidden in forbiddenCalls) {
            assertFalse("the preview must not reach the device flow: $forbidden", source.contains(forbidden))
        }
        assertTrue(source.contains("onCopyCodeAndOpenGitHub = { _, _ -> }"))
        assertTrue(source.contains("onCancel = {}"))
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
