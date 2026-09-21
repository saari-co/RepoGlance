package co.saari.repoglance.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInReturnGuardTest {
    @Test
    fun committedTokenBringsTheAppBackOverTheVerificationTab() {
        val activity = source("app/src/main/java/co/saari/repoglance/MainActivity.kt")

        val onCreate = activity.substringAfter("override fun onCreate(").substringBefore("setContent {")
        assertTrue(
            "The commit signal must be collected in lifecycleScope directly: the activity is stopped " +
                "behind the Custom Tab when the token lands, so a repeatOnLifecycle collector would miss it",
            onCreate.contains("lifecycleScope.launch {") &&
                onCreate.contains("liveModel.deviceAuthorizationCommitted.collect { returnFromGitHubVerification() }"),
        )
        assertFalse(onCreate.contains("repeatOnLifecycle"))

        val returnFn = activity.substringAfter("private fun returnFromGitHubVerification()")
            .substringBefore("private fun copyCodeAndOpenGitHub(")
        assertTrue(
            "Only a sign-in whose verification tab this activity opened may bring the app forward",
            returnFn.contains("if (!returnAfterGitHubVerification) return"),
        )
        assertTrue(
            "The return is one-shot",
            returnFn.indexOf("returnAfterGitHubVerification = false") <
                returnFn.indexOf("startActivity("),
        )
        assertTrue(
            "An app already in front (code entered on another device, or tab closed by hand) must not relaunch",
            returnFn.contains("if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return"),
        )
        assertTrue(
            "The self-start must reuse the existing task so CLEAR_TOP removes the Custom Tab above it",
            returnFn.contains("Intent(this, MainActivity::class.java)") &&
                returnFn.contains("Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP") &&
                !returnFn.contains("FLAG_ACTIVITY_NEW_TASK"),
        )

        val openVerification = activity.substringAfter("private fun openGitHubVerification(")
            .substringBefore("private fun connectGitHub()")
        assertTrue(
            openVerification.indexOf("returnAfterGitHubVerification = true") <
                openVerification.indexOf(".launchUrl("),
        )
        val connect = activity.substringAfter("private fun connectGitHub()")
            .substringBefore("private fun returnFromGitHubVerification()")
        assertTrue(
            "A new sign-in starts without a stale return armed from an abandoned tab",
            connect.indexOf("returnAfterGitHubVerification = false") <
                connect.indexOf("liveModel.beginGitHubAuthorization()"),
        )
        assertTrue(activity.contains("onConnectGitHub = ::connectGitHub,"))
        assertTrue(
            "The armed return must survive activity recreation while the tab is open",
            activity.substringAfter("override fun onSaveInstanceState(")
                .contains("STATE_RETURN_AFTER_GITHUB_VERIFICATION"),
        )

        val manifest = source("app/src/main/AndroidManifest.xml")
        val mainActivity = manifest.substringAfter("android:name=\".MainActivity\"").substringBefore(">")
        assertTrue(
            "CLEAR_TOP clears the Custom Tab only because MainActivity is the singleTask root of its task",
            mainActivity.contains("android:launchMode=\"singleTask\""),
        )
    }

    @Test
    fun commitSignalFiresOnlyAfterTheTokenIsCommitted() {
        val viewModel = source("app/src/main/java/co/saari/repoglance/RepoGlanceViewModel.kt")
        val flow = viewModel.substringAfter("fun beginGitHubAuthorization()")
            .substringBefore("} catch (cancelled: CancellationException)")

        val connecting = flow.indexOf("liveState.value = LiveUiState.Connecting")
        val commit = flow.indexOf("authorizationCommitGate.commit(requestGeneration)")
        val notCommitted = flow.indexOf("if (!committed) return@launch")
        val signal = flow.indexOf("deviceAuthorizationCommits.trySend(Unit)")
        val load = flow.indexOf("loadCatalog(LiveUiState.LoadingCatalogAfterSignIn)")

        assertTrue(
            "Finishing sign-in covers the real Keystore commit, so it is shown before the commit runs",
            connecting in 0 until commit,
        )
        assertTrue(
            "The app may only come forward once the token is committed, never for a stale generation",
            commit < notCommitted && notCommitted < signal,
        )
        assertTrue(signal < load)
        assertTrue(
            "Catalog refresh outside sign-in keeps its own loading state",
            viewModel.substringAfter("fun refreshCatalog()").substringBefore("private fun loadCatalog(")
                .contains("loadCatalog(LiveUiState.LoadingRepositories)"),
        )
    }

    @Test
    fun postTokenSequenceHoldsOneMarkAcrossBothMessages() {
        val screen = source("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt")
        val routing = screen.substringAfter("fun LiveRepoGlanceScreen(").substringBefore("private fun AwaitingGitHubScreen(")

        assertTrue(
            "Both post-token states must share one call site so the ping rings keep running across " +
                "the message change instead of restarting",
            routing.contains(
                "LiveUiState.Connecting, LiveUiState.LoadingCatalogAfterSignIn -> FinishingSignInScreen(",
            ),
        )
        assertTrue(
            "The mark is scoped to sign-in: plain catalog loads keep their spinner",
            routing.contains("LiveUiState.LoadingRepositories -> CenteredStatus("),
        )

        val finishing = screen.substringAfter("internal fun FinishingSignInScreen(")
            .substringBefore("private fun MarkStatus(")
        assertTrue(finishing.contains("\"Finishing sign-in…\""))
        assertTrue(finishing.contains("\"Loading your repositories…\""))
        assertTrue(finishing.contains("MarkStatus("))

        val markStatus = screen.substringAfter("private fun MarkStatus(").substringBefore("private fun CenteredStatus(")
        assertTrue(markStatus.contains("CheckingMark("))
        assertFalse(
            "checking-splash-018 lock: the mark screens carry no spinner",
            markStatus.contains("CircularProgressIndicator"),
        )
    }

    @Test
    fun codeScreenTellsTheUserHowToComeBackByHand() {
        val screen = source("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt")
        val awaiting = screen.substringAfter("private fun AwaitingGitHubScreen(")
            .substringBefore("private fun ConnectGitHubScreen(")

        assertTrue(awaiting.contains("If it doesn't, close the GitHub tab."))
        assertTrue(awaiting.contains("Modifier.testTag(RETURN_INSTRUCTION_TEST_TAG)"))
        assertTrue(
            "The code stays on screen until the token lands",
            awaiting.contains("Text(userCode"),
        )
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
