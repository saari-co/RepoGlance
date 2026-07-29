package co.saari.repoglance.link

import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.model.RepoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GitHubLinksTest {

    private val ref = RepoRef("saari-co", "RepoGlance")

    @Test
    fun repoUrlExactMatch() {
        assertEquals("https://github.com/saari-co/RepoGlance", GitHubLinks.repo(ref))
    }

    @Test
    fun issuesUrlExactMatch() {
        assertEquals("https://github.com/saari-co/RepoGlance/issues", GitHubLinks.issues(ref))
    }

    @Test
    fun pullsUrlExactMatch() {
        assertEquals("https://github.com/saari-co/RepoGlance/pulls", GitHubLinks.pulls(ref))
    }

    @Test
    fun issueUrlExactMatch() {
        assertEquals("https://github.com/saari-co/RepoGlance/issues/12", GitHubLinks.issue(ref, 12))
    }

    @Test
    fun pullUrlExactMatch() {
        assertEquals("https://github.com/saari-co/RepoGlance/pull/34", GitHubLinks.pull(ref, 34))
    }

    @Test
    fun releaseUrlExactMatch() {
        assertEquals(
            "https://github.com/saari-co/RepoGlance/releases/tag/v1.2.0",
            GitHubLinks.release(ref, "v1.2.0"),
        )
    }

    @Test
    fun releaseTagWithSlashAndSpacePercentEncoded() {
        assertEquals(
            "https://github.com/saari-co/RepoGlance/releases/tag/beta%2F2%20rc",
            GitHubLinks.release(ref, "beta/2 rc"),
        )
    }

    @Test
    fun issueNumberMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) { GitHubLinks.issue(ref, 0) }
        assertThrows(IllegalArgumentException::class.java) { GitHubLinks.issue(ref, -1) }
    }

    @Test
    fun pullNumberMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) { GitHubLinks.pull(ref, 0) }
        assertThrows(IllegalArgumentException::class.java) { GitHubLinks.pull(ref, -1) }
    }

    @Test
    fun hostileOwnerConstructionThrows() {
        val hostileOwners = listOf(
            "..",
            "a/b",
            "evil.com@x",
            "-lead",
            "",
            " ",
            "réseau",
            "a".repeat(40),
        )
        for (owner in hostileOwners) {
            assertThrows(IllegalArgumentException::class.java) { RepoRef(owner, "RepoGlance") }
        }
    }

    @Test
    fun repoNameDotDotIsRejectedAtLinkLayerEvenThoughNamePatternAllowsIt() {
        // RepoRef's name charset permits a bare "..", so construction alone
        // does not catch this — GitHubLinks' belt-and-braces check must.
        val dotDotRef = RepoRef("saari-co", "..")
        assertThrows(IllegalArgumentException::class.java) { GitHubLinks.repo(dotDotRef) }
    }

    @Test
    fun fixtureDerivedLinksAreAlwaysSafe() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        for (scenario in FixtureScenario.entries) {
            for (snapshot in Fixtures.snapshots(scenario, now)) {
                val urls = mutableListOf(
                    GitHubLinks.repo(snapshot.repo),
                    GitHubLinks.issues(snapshot.repo),
                    GitHubLinks.pulls(snapshot.repo),
                    GitHubLinks.issue(snapshot.repo, 1),
                    GitHubLinks.pull(snapshot.repo, 1),
                )
                snapshot.latestRelease?.let { urls.add(GitHubLinks.release(snapshot.repo, it.tag)) }

                for (url in urls) {
                    assertTrue("$url should start with https://github.com/", url.startsWith("https://github.com/"))
                    assertFalse(url.contains(".."))
                    assertFalse(url.contains("@"))
                    assertFalse(url.contains("?"))
                    assertFalse(url.contains("#"))
                    assertFalse(url.contains(" "))
                }
            }
        }
    }
}
