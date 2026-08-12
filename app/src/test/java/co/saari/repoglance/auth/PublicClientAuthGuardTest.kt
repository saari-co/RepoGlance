package co.saari.repoglance.auth

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicClientAuthGuardTest {
    @Test
    fun trackedSourceHasNoObsoleteConfidentialOrCallbackAuthPath() {
        val root = repositoryRoot()
        val forbiddenMarkers = listOf(
            "CLIENT_" + "SECRET",
            "client_" + "secret",
            "GITHUB_CALLBACK_" + "URL",
            "REPOGLANCE_GITHUB_" + "CLIENT_" + "SECRET",
            "/oauth/" + "callback",
            "GitHub" + "OAuthClient",
            "Pending" + "AuthorizationStore",
        )
        val findings = mutableListOf<String>()
        guardedRoots(root).forEach { guardedRoot ->
            if (Files.isRegularFile(guardedRoot)) {
                findings += findingsIn(root, guardedRoot, forbiddenMarkers)
            } else if (Files.exists(guardedRoot)) {
                Files.walk(guardedRoot).use { paths ->
                    paths.forEach { path ->
                        if (Files.isRegularFile(path) && path.isGuardedTextFile()) {
                            findings += findingsIn(root, path, forbiddenMarkers)
                        }
                    }
                }
            }
        }

        assertTrue(findings.joinToString("\n"), findings.isEmpty())
        val callbackRoot = root.resolve("callback-site")
        val hasCallbackFiles = if (Files.exists(callbackRoot)) {
            Files.walk(callbackRoot).use { paths -> paths.anyMatch { path -> Files.isRegularFile(path) } }
        } else {
            false
        }
        assertFalse("The device flow does not use callback-site files", hasCallbackFiles)
    }

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")

    private fun guardedRoots(root: Path): List<Path> = listOf(
        root.resolve(".github"),
        root.resolve("app/src"),
        root.resolve("app/build.gradle.kts"),
        root.resolve("build.gradle.kts"),
        root.resolve("gradle"),
        root.resolve("gradle.properties"),
        root.resolve("settings.gradle.kts"),
    )

    private fun findingsIn(root: Path, path: Path, markers: List<String>): List<String> {
        val text = Files.readAllBytes(path).toString(Charsets.UTF_8)
        return markers.filter(text::contains).map { marker -> "${root.relativize(path)} contains $marker" }
    }

    private fun Path.isGuardedTextFile(): Boolean {
        val relativeParts = iterator().asSequence().map { it.fileName.toString() }.toList()
        if (relativeParts.any { it in setOf(".git", ".gradle", ".kotlin", "build") }) return false
        val fileName = fileName.toString()
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        return fileName == ".gitignore" || extension in setOf(
            "css",
            "html",
            "js",
            "json",
            "kt",
            "kts",
            "md",
            "properties",
            "toml",
            "xml",
            "yaml",
            "yml",
        )
    }
}
