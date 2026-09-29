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
    private val realOwners = listOf("saari-co", "dinkuskit", "saariuslystoned", "smokyproductcompany")

    @Test
    fun sampleDataNeverUsesTheMaintainersRealOwners() {
        val catalog = SampleAccount.catalog(now)
        val text = buildList {
            add(catalog.viewer.login)
            catalog.repositories.forEach { repository ->
                add(repository.ref.full)
                val content = SampleAccount.content(repository, now)
                issues(content).forEach { add(it.title); add(it.author); it.assignee?.let(::add); addAll(it.labels) }
                pullRequests(content).forEach { add(it.title); add(it.author); addAll(it.labels) }
            }
        }
        for (value in text) {
            for (owner in realOwners) {
                assertFalse("'$value' must not name the real owner $owner", value.contains(owner, ignoreCase = true))
            }
        }
    }

    @Test
    fun catalogIsAFictionalAccountWithSeveralOwnersAndVisibilities() {
        val repositories = SampleAccount.catalog(now).repositories
        assertTrue(repositories.size in 6..8)
        assertEquals(repositories.size, repositories.map { it.id }.distinct().size)
        assertEquals(setOf("acme", "octoco", SampleAccount.VIEWER_LOGIN), repositories.map { it.ref.owner }.toSet())
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
        assertTrue(all.flatMap(::pullRequests).any { it.reviewRequestedFromViewer })
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
