package co.saari.repoglance.link

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubAppLauncherTest {
    @Test
    fun standardLaunchStartsGitHubInItsTask() {
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, GitHubAppLauncher.flags(adjacent = false))
    }

    @Test
    fun adjacentLaunchAddsOnlyTheAdjacentRequest() {
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT,
            GitHubAppLauncher.flags(adjacent = true),
        )
    }
}
