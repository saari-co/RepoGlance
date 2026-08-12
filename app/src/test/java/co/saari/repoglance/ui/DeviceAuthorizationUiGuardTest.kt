package co.saari.repoglance.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAuthorizationUiGuardTest {
    @Test
    fun deviceAuthorizationRequiresAnExplicitCopyAndOpenTap() {
        val root = repositoryRoot()
        val screenSource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt"),
        )
        val authorizationScreen = screenSource.substringAfter("private fun AwaitingGitHubScreen(")
            .substringBefore("private fun ConnectGitHubScreen(")

        assertFalse(
            "The authorization screen must never launch GitHub as a composition side effect",
            authorizationScreen.contains("LaunchedEffect"),
        )
        assertTrue(
            "The code must stay visible while the user decides when to continue",
            authorizationScreen.contains("Text(userCode"),
        )
        assertTrue(
            "The primary action must explain that it copies and opens GitHub",
            authorizationScreen.contains("Copy code & open GitHub"),
        )
        assertTrue(
            "The screen must explain that the copied code can be pasted",
            authorizationScreen.contains("copies this code so you can paste it on GitHub"),
        )
        assertTrue(
            "Only the button tap may invoke the copy-and-open callback",
            authorizationScreen.contains(
                "onClick = { onCopyCodeAndOpenGitHub(userCode, verificationUri) }",
            ),
        )
        assertTrue(
            "The user must still be able to cancel the pending authorization",
            authorizationScreen.contains("TextButton(onClick = onCancel) { Text(\"Cancel sign-in\") }"),
        )
        assertEquals(
            "The copy-and-open callback must have exactly one invocation in the UI source",
            1,
            screenSource.windowed("onCopyCodeAndOpenGitHub(".length)
                .count { it == "onCopyCodeAndOpenGitHub(" },
        )
    }

    @Test
    fun explicitActionCopiesSensitiveCodeBeforeOpeningTheExactUri() {
        val root = repositoryRoot()
        val activitySource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/MainActivity.kt"),
        )
        val action = activitySource.substringAfter(
            "private fun copyCodeAndOpenGitHub(userCode: String, verificationUri: String)",
        ).substringBefore("private fun openInstallationSettings()")

        assertTrue(action.contains("ClipData.newPlainText(\"GitHub device code\", userCode)"))
        assertTrue(action.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU"))
        assertTrue(action.contains("putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)"))
        assertTrue(action.contains("clipboard.setPrimaryClip(clip)"))
        assertTrue(action.contains("openGitHubVerification(verificationUri)"))
        assertTrue(
            "The code must be copied before the unchanged verification URI is opened",
            action.indexOf("clipboard.setPrimaryClip(clip)") <
                action.indexOf("openGitHubVerification(verificationUri)"),
        )
    }

    @Test
    fun returningFromGitHubWakesThePendingAuthorizationPoll() {
        val root = repositoryRoot()
        val activitySource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/MainActivity.kt"),
        )
        val viewModelSource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/RepoGlanceViewModel.kt"),
        )
        val pollerSource = readText(
            root.resolve("app/src/main/java/co/saari/repoglance/auth/GitHubDeviceFlowClient.kt"),
        )

        val onResume = activitySource.substringAfter("override fun onResume()")
            .substringBefore("private fun openGitHubVerification")
        assertTrue(onResume.contains("liveModel.resumeGitHubAuthorization()"))

        val resumeAuthorization = viewModelSource.substringAfter("fun resumeGitHubAuthorization()")
            .substringBefore("fun refreshCatalog()")
        assertTrue(resumeAuthorization.contains("LiveUiState.AwaitingDeviceAuthorization"))
        assertTrue(resumeAuthorization.contains("authorizationJob?.isActive == true"))
        assertTrue(resumeAuthorization.contains("deviceFlowPollWakeSignal.wake()"))

        val poller = pollerSource.substringAfter("class GitHubDeviceFlowPoller(")
            .substringBefore("class AuthorizationCommitGate")
        assertTrue(
            "Every early resume wake must recheck the minimum polling instant",
            poller.contains("waitUntil(nextPollAt, authorization.expiresAt)"),
        )
        assertTrue(
            "The next request must be scheduled from the prior request completion",
            poller.contains("nextPollAt = clock.instant().plusMillis(secondsToMillis(intervalSeconds))"),
        )
    }

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")

    private fun readText(path: Path): String = Files.readAllBytes(path).toString(Charsets.UTF_8)
}
