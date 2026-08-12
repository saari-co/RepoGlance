package co.saari.repoglance.data

import co.saari.repoglance.auth.GitHubAuthConfig
import co.saari.repoglance.auth.GitHubOAuthClient
import co.saari.repoglance.auth.GitHubSession
import co.saari.repoglance.auth.GitHubUserToken
import co.saari.repoglance.auth.TokenStore
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubApiClientTest {
    private val now = Instant.parse("2026-08-12T10:00:00Z")
    private val clock = MutableClock(now)

    @Test
    fun loadCatalogFollowsLinkHeaderPaginationAndParsesNullableInstallationOwner() {
        val transport = FakeTransport().apply {
            enqueue(
                "/user",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("999"),
                    ),
                    body = """{"login":"viewer","avatar_url":"https://example.com/v.png"}""",
                ),
            )
            enqueue(
                "/user/installations?per_page=100",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "Link" to listOf("""<https://api.github.com/user/installations?per_page=100&page=2>; rel="next""""),
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("998"),
                    ),
                    body = """{"installations":[{"id":1,"account":{"login":"org-alpha","type":"Organization"}}]}""",
                ),
            )
            enqueue(
                "/user/installations?per_page=100&page=2",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("997"),
                    ),
                    body = """{"installations":[{"id":2,"account":{"type":"Enterprise"}}]}""",
                ),
            )
            enqueue(
                "/user/installations/1/repositories?per_page=100",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "Link" to listOf("""<https://api.github.com/user/installations/1/repositories?per_page=100&page=2>; rel="next""""),
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("996"),
                    ),
                    body = """{"repositories":[{"id":11,"full_name":"zeta/repo-a","private":true,"archived":false,"pushed_at":"2026-08-01T00:00:00Z"}]}""",
                ),
            )
            enqueue(
                "/user/installations/1/repositories?per_page=100&page=2",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("995"),
                    ),
                    body = """{"repositories":[{"id":12,"full_name":"alpha/repo-b","private":false,"archived":false,"pushed_at":null}]}""",
                ),
            )
            enqueue(
                "/user/installations/2/repositories?per_page=100",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("994"),
                    ),
                    body = """{"repositories":[{"id":13,"full_name":"alpha/repo-c","private":false,"archived":true,"pushed_at":"2026-08-01T00:00:00Z"}]}""",
                ),
            )
        }
        val client = GitHubApiClient(
            session = testSession(),
            transport = transport,
            clock = clock,
        )

        val result = client.loadCatalog()
        assertTrue(result is GitHubApiResult.Success)
        val catalog = (result as GitHubApiResult.Success).value
        assertEquals("viewer", catalog.viewer.login)
        assertEquals(listOf("org-alpha", null), catalog.installations.map { it.ownerLogin })
        assertEquals(
            listOf("alpha/repo-b", "alpha/repo-c", "zeta/repo-a"),
            catalog.repositories.map { it.ref.full },
        )
        assertEquals(
            listOf(
                "/user",
                "/user/installations?per_page=100",
                "/user/installations?per_page=100&page=2",
                "/user/installations/1/repositories?per_page=100",
                "/user/installations/1/repositories?per_page=100&page=2",
                "/user/installations/2/repositories?per_page=100",
            ),
            transport.requests,
        )
    }

    @Test
    fun loadRepositoryContentReturnsPartialPagesAndParsesReviewersAndMixedLabelPayloads() {
        val repository = LiveRepository(
            id = 99L,
            ref = RepoRef("octocat", "api"),
            isPrivate = true,
            isArchived = false,
            pushedAt = null,
        )
        val transport = FakeTransport().apply {
            enqueue(
                "/repos/octocat/api/issues?state=open&sort=updated&direction=desc&per_page=30",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "Link" to listOf("""<https://api.github.com/repos/octocat/api/issues?state=open&sort=updated&direction=desc&per_page=30&page=2>; rel="next""""),
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("1000"),
                    ),
                    body = """[
                      {"number":7,"title":"open issue","user":{"login":"octocat"},"assignee":null,"labels":["bug",{"name":"docs"}],"comments":4,"updated_at":"2026-08-11T10:00:00Z","html_url":"https://github.com/octocat/api/issues/7"},
                      {"number":8,"title":"filtered pull","user":null,"labels":[{"name":"ignore"}],"updated_at":"2026-08-11T10:00:00Z","html_url":"https://github.com/octocat/api/issues/8","pull_request":{"url":"x"}}
                    ]""",
                ),
            )
            enqueue(
                "/repos/octocat/api/pulls?state=open&sort=updated&direction=desc&per_page=30",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("1000"),
                    ),
                    body = """[
                      {"number":42,"title":"sample pr","user":null,"assignee":{"login":"alice"},"labels":[{"name":"enhancement"}],"draft":null,"requested_reviewers":["alice",{"login":"viewer"}],"updated_at":"2026-08-11T10:05:00Z","html_url":"https://github.com/octocat/api/pull/42"}
                    ]""",
                ),
            )
        }
        val client = GitHubApiClient(
            session = testSession(),
            transport = transport,
            clock = clock,
        )

        val result = client.loadRepositoryContent(repository, viewerLogin = "viewer")
        assertTrue(result.issues is GitHubApiResult.Success)
        assertTrue(result.pullRequests is GitHubApiResult.Success)

        val issues = (result.issues as GitHubApiResult.Success).value
        assertEquals(1, issues.rows.size)
        assertTrue(issues.hasMorePages)
        assertEquals(listOf("bug", "docs"), issues.rows.first().labels)
        assertEquals("octocat", issues.rows.first().author)

        val pulls = (result.pullRequests as GitHubApiResult.Success).value
        assertFalse(pulls.hasMorePages)
        assertEquals(1, pulls.rows.size)
        assertFalse(pulls.rows.first().isDraft)
        assertTrue(pulls.rows.first().reviewRequestedFromViewer)
    }

    @Test
    fun loadCatalogMarksRateLimitFrom403WithResetHeader() {
        val reset = now.plusSeconds(120)
        val transport = FakeTransport().apply {
            enqueue(
                "/user",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("1000"),
                    ),
                    body = """{"login":"viewer"}""",
                ),
            )
            enqueue(
                "/user/installations?per_page=100",
                HttpResponse(
                    statusCode = 403,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("0"),
                        "X-RateLimit-Reset" to listOf(reset.epochSecond.toString()),
                    ),
                    body = """{"installations":[]}""",
                ),
            )
        }
        val client = GitHubApiClient(
            session = testSession(),
            transport = transport,
            clock = clock,
        )
        val result = client.loadCatalog()
        assertTrue(result is GitHubApiResult.Failure)
        val failure = (result as GitHubApiResult.Failure)
        assertEquals(403, failure.statusCode)
        assertEquals(RateLimitBucket.EXHAUSTED, failure.rateLimit.bucket)
        assertEquals(reset, failure.rateLimit.resetsAt)
    }

    @Test
    fun repositoryContentRespectsRateLimitBackoffUntilRetryWindow() {
        val transport = FakeTransport().apply {
            enqueue(
                "/repos/octocat/api/issues?state=open&sort=updated&direction=desc&per_page=30",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("1000"),
                    ),
                    body = """[]""",
                ),
            )
            enqueue(
                "/repos/octocat/api/pulls?state=open&sort=updated&direction=desc&per_page=30",
                HttpResponse(
                    statusCode = 429,
                    headers = mapOf(
                        "Retry-After" to listOf("5"),
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("0"),
                    ),
                    body = """[]""",
                ),
            )
            enqueue(
                "/repos/octocat/api/issues?state=open&sort=updated&direction=desc&per_page=30",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("1000"),
                    ),
                    body = """[]""",
                ),
            )
            enqueue(
                "/repos/octocat/api/pulls?state=open&sort=updated&direction=desc&per_page=30",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("1000"),
                    ),
                    body = """[]""",
                ),
            )
        }
        val repository = LiveRepository(
            id = 99L,
            ref = RepoRef("octocat", "api"),
            isPrivate = true,
            isArchived = false,
            pushedAt = null,
        )
        val client = GitHubApiClient(
            session = testSession(),
            transport = transport,
            clock = clock,
        )

        val firstAttempt = client.loadRepositoryContent(repository, viewerLogin = "viewer")
        val firstPulls = (firstAttempt.pullRequests as GitHubApiResult.Failure)
        assertEquals(429, firstPulls.statusCode)
        assertEquals(RateLimitBucket.EXHAUSTED, firstPulls.rateLimit.bucket)

        val secondAttempt = client.loadRepositoryContent(repository, viewerLogin = "viewer")
        val secondPulls = (secondAttempt.pullRequests as GitHubApiResult.Failure)
        assertEquals(429, secondPulls.statusCode)
        assertEquals(RateLimitBucket.EXHAUSTED, secondPulls.rateLimit.bucket)

        clock.advanceBySeconds(5)
        val thirdAttempt = client.loadRepositoryContent(repository, viewerLogin = "viewer")
        assertTrue(thirdAttempt.pullRequests is GitHubApiResult.Success)

        assertEquals(
            listOf(
                "/repos/octocat/api/issues?state=open&sort=updated&direction=desc&per_page=30",
                "/repos/octocat/api/pulls?state=open&sort=updated&direction=desc&per_page=30",
                "/repos/octocat/api/issues?state=open&sort=updated&direction=desc&per_page=30",
                "/repos/octocat/api/pulls?state=open&sort=updated&direction=desc&per_page=30",
            ),
            transport.requests,
        )
    }

    @Test
    fun permissionDeniedWithoutRateLimitSignalsDoesNotStartBackoff() {
        val transport = FakeTransport().apply {
            enqueue(
                "/user",
                HttpResponse(
                    statusCode = 403,
                    headers = emptyMap(),
                    body = """{"message":"Resource not accessible by integration"}""",
                ),
            )
            enqueue(
                "/user",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("999"),
                    ),
                    body = """{"login":"viewer"}""",
                ),
            )
            enqueue(
                "/user/installations?per_page=100",
                HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "X-RateLimit-Limit" to listOf("1000"),
                        "X-RateLimit-Remaining" to listOf("998"),
                    ),
                    body = """{"installations":[]}""",
                ),
            )
        }
        val client = GitHubApiClient(testSession(), transport, clock)

        val denied = client.loadCatalog() as GitHubApiResult.Failure
        assertEquals(RateLimitBucket.UNKNOWN, denied.rateLimit.bucket)
        assertEquals("GitHub did not allow this read", denied.message)
        assertTrue(client.loadCatalog() is GitHubApiResult.Success)
        assertEquals(3, transport.requests.size)
    }

    private fun testSession(): GitHubSession = GitHubSession(
        tokenStore = FakeTokenStore(
            GitHubUserToken(
                accessToken = "token",
                refreshToken = null,
                accessTokenExpiresAt = null,
                refreshTokenExpiresAt = null,
                tokenType = "bearer",
            ),
        ),
        oauthClient = GitHubOAuthClient(
            config = GitHubAuthConfig(
                clientId = "id",
                clientSecret = "secret",
                callbackUrl = "https://example.com/callback",
            ),
            transport = HttpTransport { error("OAuth not expected") },
            clock = clock,
        ),
        clock = clock,
    )

    private class FakeTokenStore(var value: GitHubUserToken?) : TokenStore {
        override fun read(): GitHubUserToken? = value
        override fun write(token: GitHubUserToken) {
            value = token
        }
        override fun clear() {
            value = null
        }
    }

    private class FakeTransport : HttpTransport {
        private val responses = linkedMapOf<String, ArrayDeque<HttpResponse>>()
        val requests = mutableListOf<String>()

        fun enqueue(path: String, response: HttpResponse) {
            responses.getOrPut(normalizePath(path)) { ArrayDeque() }.add(response)
        }

        override fun execute(request: HttpRequest): HttpResponse {
            val path = normalizePath(request.url)
            requests.add(path)
            val queued = responses[normalizePath(path)] ?: throw AssertionError("No response for $path")
            require(queued.isNotEmpty()) { "No response left for $path" }
            return queued.removeFirst()
        }

        private fun normalizePath(urlOrPath: String): String {
            return if (urlOrPath.startsWith("http")) {
                val uri = URI(urlOrPath)
                if (uri.rawQuery == null) uri.path else "${uri.path}?${uri.rawQuery}"
            } else {
                urlOrPath
            }
        }
    }

    private class MutableClock(private var current: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
        override fun instant(): Instant = current
        fun advanceBySeconds(seconds: Long) {
            current = current.plusSeconds(seconds)
        }
    }
}
