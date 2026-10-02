package co.saari.repoglance.sample

import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.LiveRepositoryContent
import co.saari.repoglance.model.RateLimitBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SampleAccountTest {
    private val now = Instant.parse("2026-09-29T16:00:00Z")

    private val approvedSampleRepositories = listOf(
        "saari-co/rocket",
        "saari-co/api-server",
        "saari-co/mobile-app",
        "dinkuskit/infra",
        "dinkuskit/design-system",
        "saariuslystoned/dotfiles",
        "saari-co/legacy-site",
    )

    private val publicRealRepositories = listOf("saari-co/RepoGlance", "dinkuskit/blocks")

    @Test
    fun sampleRepositoriesAreExactlyTheApprovedNames() {
        assertEquals(
            "changing the sample set needs a fresh 404 check under the owner (see the sample-app-038 proof)",
            approvedSampleRepositories,
            SampleAccount.catalog(now).repositories.map { it.ref.full },
        )
        for (real in publicRealRepositories) {
            assertFalse(
                "a real repository must never carry sample numbers: $real",
                approvedSampleRepositories.any { it.equals(real, ignoreCase = true) },
            )
        }
    }

    @Test
    fun sampleOwnersAreTheMaintainersOwnAccounts() {
        val catalog = SampleAccount.catalog(now)
        assertEquals("saariuslystoned", catalog.viewer.login)
        assertEquals(setOf("saari-co", "dinkuskit", "saariuslystoned"), catalog.repositories.map { it.ref.owner }.toSet())
        val text = catalog.repositories.flatMap { repository ->
            val content = SampleAccount.content(repository, now)
            issues(content).map { it.title + it.author } + pullRequests(content).map { it.title + it.author }
        }
        assertFalse(text.any { it.contains("smokyproductcompany", ignoreCase = true) })
    }

    @Test
    fun everySamplePersonIsTheMaintainersOwnHandle() {
        for (repository in SampleAccount.catalog(now).repositories) {
            val content = SampleAccount.content(repository, now)
            issues(content).forEach {
                assertEquals(SampleAccount.VIEWER_LOGIN, it.author)
                assertTrue(it.assignee == null || it.assignee == SampleAccount.VIEWER_LOGIN)
            }
            pullRequests(content).forEach {
                assertEquals(SampleAccount.VIEWER_LOGIN, it.author)
                assertTrue(it.assignee == null || it.assignee == SampleAccount.VIEWER_LOGIN)
                assertFalse("GitHub cannot request a review from the PR's own author", it.reviewRequestedFromViewer)
            }
        }
    }

    private val showcaseRepositories = listOf(
        "saltmarsh-io/rocket",
        "saltmarsh-io/api-server",
        "saltmarsh-io/mobile-app",
        "ferrywood/infra",
        "ferrywood/design-system",
        "elin-tidewater/dotfiles",
        "saltmarsh-io/legacy-site",
    )

    @Test
    fun theShowcasePersonaIsTheSameCatalogUnderFictionalOwners() {
        val sample = SampleAccount.catalog(now)
        val showcase = SampleAccount.catalog(now, SamplePersona.SHOWCASE)
        assertEquals(
            "the showcase owners resolved 404 on GitHub on 2026-10-01 (showcase-048); changing them needs a fresh check",
            showcaseRepositories,
            showcase.repositories.map { it.ref.full },
        )
        assertEquals("elin-tidewater", showcase.viewer.login)
        assertTrue(showcase.repositories.none { it.ref.owner in setOf("saari-co", "dinkuskit", "saariuslystoned") })
        assertEquals(sample.repositories.map { it.ref.name }, showcase.repositories.map { it.ref.name })
        assertEquals(sample.repositories.map { it.id }, showcase.repositories.map { it.id })
        assertEquals(sample.repositories.map { it.pushedAt }, showcase.repositories.map { it.pushedAt })
        assertEquals(sample.repositories.map { it.isPrivate to it.isArchived }, showcase.repositories.map { it.isPrivate to it.isArchived })
        for ((real, fictional) in sample.repositories.zip(showcase.repositories)) {
            val realContent = SampleAccount.content(real, now)
            val content = SampleAccount.content(fictional, now)
            assertEquals(issues(realContent).map { it.number to it.title }, issues(content).map { it.number to it.title })
            assertEquals(pullRequests(realContent).map { it.number to it.title }, pullRequests(content).map { it.number to it.title })
            (issues(content).map { it.author } + pullRequests(content).map { it.author }).forEach { assertEquals("elin-tidewater", it) }
            issues(content).forEach { assertTrue(it.assignee == null || it.assignee == "elin-tidewater") }
        }
        assertEquals(SamplePersona.SHOWCASE, SamplePersona.of("saltmarsh-io"))
        assertEquals(SamplePersona.SHOWCASE, SamplePersona.of("Ferrywood"))
        assertEquals(SamplePersona.SAMPLE, SamplePersona.of("saari-co"))
        assertEquals(SamplePersona.SAMPLE, SamplePersona.of("someone-else"))
    }

    @Test
    fun catalogHasSeveralOwnersAndVisibilities() {
        val repositories = SampleAccount.catalog(now).repositories
        assertTrue(repositories.size in 6..8)
        assertEquals(repositories.size, repositories.map { it.id }.distinct().size)
        assertTrue(repositories.any { it.isPrivate })
        assertTrue(repositories.any { !it.isPrivate })
        assertTrue(repositories.any { it.isArchived })
        assertTrue("an unknown push time stays unknown", repositories.any { it.pushedAt == null })
        repositories.mapNotNull { it.pushedAt }.forEach { assertFalse(it.isAfter(now)) }
    }

    @Test
    fun contentLoadsForEveryRepositoryWithoutGitHubLinks() {
        for (repository in SampleAccount.catalog(now).repositories) {
            val content = SampleAccount.content(repository, now)
            assertEquals(repository, content.repository)
            val issues = issues(content)
            val pullRequests = pullRequests(content)
            val numbers = issues.map { it.number } + pullRequests.map { it.number }
            assertEquals("numbers are unique in ${repository.ref.full}", numbers.size, numbers.distinct().size)
            issues.forEach {
                assertEquals("", it.htmlUrl)
                assertFalse(it.updatedAt.isAfter(now))
            }
            pullRequests.forEach {
                assertEquals("", it.htmlUrl)
                assertFalse(it.updatedAt.isAfter(now))
            }
        }
        val all = SampleAccount.catalog(now).repositories.map { SampleAccount.content(it, now) }
        assertTrue(all.sumOf { issues(it).size } >= 10)
        assertTrue(all.flatMap(::pullRequests).any { it.isDraft })
    }

    @Test
    fun aRepositoryOutsideTheSampleSetIsUnavailableNotEmpty() {
        val outside = co.saari.repoglance.data.LiveRepository(
            id = 99L,
            ref = co.saari.repoglance.model.RepoRef("someone", "real-repo"),
            isPrivate = false,
            isArchived = false,
            pushedAt = null,
        )
        val content = SampleAccount.content(outside, now)
        for (result in listOf(content.issues, content.pullRequests)) {
            val failure = result as GitHubApiResult.Failure
            assertEquals(SampleAccount.OUTSIDE_SAMPLE, failure.message)
            assertEquals("no rate-limit state is claimed", RateLimitBucket.UNKNOWN, failure.rateLimit.bucket)
            assertFalse(failure.needsNewSignIn)
        }
    }

    @Test
    fun sampleRateLimitClaimsNoNumbers() {
        val rate = SampleAccount.RATE_LIMIT
        assertEquals(RateLimitBucket.OK, rate.bucket)
        assertNull(rate.remaining)
        assertNull(rate.limit)
        assertNull(rate.resetsAt)
    }

    @Test
    fun sampleDataIsDeterministic() {
        assertEquals(SampleAccount.catalog(now), SampleAccount.catalog(now))
        val first = SampleAccount.catalog(now).repositories.first()
        assertEquals(SampleAccount.content(first, now), SampleAccount.content(first, now))
    }

    private fun issues(content: LiveRepositoryContent) =
        (content.issues as GitHubApiResult.Success).value.rows

    private fun pullRequests(content: LiveRepositoryContent) =
        (content.pullRequests as GitHubApiResult.Success).value.rows
}
