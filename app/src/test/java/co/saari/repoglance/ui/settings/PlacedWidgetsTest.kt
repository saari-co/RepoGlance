package co.saari.repoglance.ui.settings

import co.saari.repoglance.LiveUiState
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.sample.SampleAccount
import co.saari.repoglance.widget.RepoWidgetConfig
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlacedWidgetsTest {
    private val repoGlance = RepoWidgetConfig(RepoRef("saari-co", "RepoGlance"), NavigatorMode.BOTH)

    @Test
    fun repositoryWidgetsComeFirstInPlacementOrderThenPinnedRepos() {
        val placed = placedWidgets(
            repositoryIds = listOf(42, 7, 42),
            pinnedReposIds = listOf(9),
            config = { if (it == 7) repoGlance else null },
            pinCount = 3,
        )
        assertEquals(
            listOf(
                PlacedWidget.Repository(7, repoGlance),
                PlacedWidget.Repository(42, null),
                PlacedWidget.PinnedRepos(9, 3),
            ),
            placed,
        )
    }

    @Test
    fun rowsNameTheRepositoryAndItsFeedOrSayNotSetUp() {
        val configured = PlacedWidget.Repository(7, repoGlance)
        assertEquals("saari-co/RepoGlance", configured.headline())
        assertEquals("Repository widget · Issues and PRs", configured.supporting())

        val prsOnly = PlacedWidget.Repository(8, repoGlance.copy(mode = NavigatorMode.PRS))
        assertEquals("Repository widget · PRs", prsOnly.supporting())

        val unconfigured = PlacedWidget.Repository(42, null)
        assertEquals("Repository widget", unconfigured.headline())
        assertEquals("Not set up · tap to choose a repository", unconfigured.supporting())
    }

    @Test
    fun pinnedReposRowCountsPinsAndSaysSoWhenThereAreNone() {
        assertEquals("Pinned repos widget", PlacedWidget.PinnedRepos(9, 0).headline())
        assertEquals("No pins yet · pin repositories in the list", PlacedWidget.PinnedRepos(9, 0).supporting())
        assertEquals("1 pinned repository", PlacedWidget.PinnedRepos(9, 1).supporting())
        assertEquals("12 pinned repositories", PlacedWidget.PinnedRepos(9, 12).supporting())
    }

    @Test
    fun widgetKindsUseTheNamesTheMaintainerChose() {
        assertEquals(listOf("Repository widget", "Pinned repos widget"), WidgetKind.entries.map { it.title })
        WidgetKind.entries.forEach { kind ->
            assertFalse("no user-facing 'stack'", kind.title.contains("stack", ignoreCase = true))
            assertFalse(kind.description.contains("stack", ignoreCase = true))
        }
    }

    @Test
    fun settingsAccountFollowsTheSessionAndSampleMode() {
        val ready = LiveUiState.Ready(
            catalog = SampleAccount.catalog(Instant.EPOCH),
            observedAt = Instant.EPOCH,
            rateLimit = SampleAccount.RATE_LIMIT,
        )
        assertEquals(SettingsAccount.LIVE, settingsAccount(ready, sampleMode = false))
        assertEquals(SettingsAccount.SAMPLE, settingsAccount(ready, sampleMode = true))
        assertEquals(SettingsAccount.SIGNED_OUT, settingsAccount(LiveUiState.SignedOut, sampleMode = false))
        assertEquals(SettingsAccount.SIGNED_OUT, settingsAccount(LiveUiState.Checking, sampleMode = false))
        assertEquals(SettingsAccount.LIVE, settingsAccount(LiveUiState.LoadingRepositories, sampleMode = false))
        assertEquals(
            "a failed refresh keeps the session",
            SettingsAccount.LIVE,
            settingsAccount(LiveUiState.Failure("offline"), sampleMode = false),
        )
        assertEquals(
            "a rejected token has no session left to manage",
            SettingsAccount.SIGNED_OUT,
            settingsAccount(LiveUiState.Failure("sign in again", needsNewSignIn = true), sampleMode = false),
        )
    }
}
