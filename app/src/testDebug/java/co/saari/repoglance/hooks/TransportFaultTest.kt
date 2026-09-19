package co.saari.repoglance.hooks

import co.saari.repoglance.auth.GitHubAuthConfig
import co.saari.repoglance.auth.GitHubDeviceFlowClient
import co.saari.repoglance.auth.GitHubSession
import co.saari.repoglance.auth.GitHubUserToken
import co.saari.repoglance.auth.TokenStore
import co.saari.repoglance.data.GitHubApiClient
import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.HttpRequest
import co.saari.repoglance.data.HttpResponse
import co.saari.repoglance.data.HttpTransport
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportFaultTest {
    private val now = Instant.parse("2026-09-19T16:00:00Z")
    private val reset = now.plusSeconds(180)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val request = HttpRequest("GET", "https://api.github.com/repos/saari-co/RepoGlance")
    private val realBody = """{"open_issues_count":7,"pushed_at":"2026-09-19T15:00:00Z"}"""

    private class CountingTransport(private val response: HttpResponse) : HttpTransport {
        var calls = 0
        override fun execute(request: HttpRequest): HttpResponse {
            calls += 1
            return response
        }
    }

    private fun realTransport() = CountingTransport(
        HttpResponse(
            statusCode = 200,
            headers = mapOf(
                "X-RateLimit-Limit" to listOf("5000"),
                "x-ratelimit-remaining" to listOf("4990"),
                "X-RateLimit-Reset" to listOf(now.plusSeconds(3600).epochSecond.toString()),
                "ETag" to listOf("\"abc\""),
            ),
            body = realBody,
        ),
    )

    @Test
    fun disarmedPassesTheRealResponseThrough() {
        val real = realTransport()
        val response = TransportFault.respond(null, request, real)
        assertEquals(1, real.calls)
        assertEquals(realBody, response.body)
        assertEquals("4990", response.header("X-RateLimit-Remaining"))
    }

    @Test
    fun lowCallsGitHubAndRewritesOnlyTheRateLimitHeaders() {
        val real = realTransport()
        val response = TransportFault.respond(TransportFault.Armed(TransportFault.Kind.LOW, reset), request, real)
        assertEquals(1, real.calls)
        assertEquals(200, response.statusCode)
        assertEquals(realBody, response.body)
        assertEquals("\"abc\"", response.header("ETag"))
        assertEquals(TransportFault.LOW_REMAINING.toString(), response.header("X-RateLimit-Remaining"))
        assertEquals(1, response.headers.keys.count { it.equals("X-RateLimit-Remaining", ignoreCase = true) })
        assertEquals(reset.epochSecond.toString(), response.header("X-RateLimit-Reset"))
    }

    @Test
    fun exhaustedNeverReachesGitHub() {
        val real = realTransport()
        val response = TransportFault.respond(TransportFault.Armed(TransportFault.Kind.EXHAUSTED, reset), request, real)
        assertEquals(0, real.calls)
        assertEquals(403, response.statusCode)
        assertEquals("0", response.header("X-RateLimit-Remaining"))
        assertEquals(reset.epochSecond.toString(), response.header("X-RateLimit-Reset"))
    }

    @Test
    fun theFaultExpiresAtItsResetTime() {
        val armed = TransportFault.Armed(TransportFault.Kind.EXHAUSTED, reset)
        assertSame(armed, TransportFault.active(armed, now))
        assertNull(TransportFault.active(armed, reset))
        assertNull(TransportFault.active(null, now))
    }

    @Test
    fun theApiClientReadsLowFromTheRewrittenHeaders() {
        val result = metadataThrough(TransportFault.Armed(TransportFault.Kind.LOW, reset), realTransport())
        assertTrue(result is GitHubApiResult.Success)
        val success = result as GitHubApiResult.Success
        assertEquals(RateLimitBucket.LOW, success.rateLimit.bucket)
        assertEquals(TransportFault.LOW_REMAINING, success.rateLimit.remaining)
        assertEquals(reset, success.rateLimit.resetsAt)
        assertEquals(7, success.value.openIssuesAndPullRequests)
    }

    @Test
    fun theApiClientReadsExhaustedWithTheResetTime() {
        val real = realTransport()
        val result = metadataThrough(TransportFault.Armed(TransportFault.Kind.EXHAUSTED, reset), real)
        assertTrue(result is GitHubApiResult.Failure)
        val failure = result as GitHubApiResult.Failure
        assertEquals(RateLimitBucket.EXHAUSTED, failure.rateLimit.bucket)
        assertEquals(reset, failure.rateLimit.resetsAt)
        assertEquals("GitHub's rate limit is exhausted", failure.message)
        assertEquals(0, real.calls)
    }

    private fun metadataThrough(armed: TransportFault.Armed, real: HttpTransport) = GitHubApiClient(
        session = session(),
        transport = HttpTransport { TransportFault.respond(armed, it, real) },
        clock = clock,
    ).loadRepositoryMetadata(
        LiveRepository(id = 1L, ref = RepoRef("saari-co", "RepoGlance"), isPrivate = false, isArchived = false, pushedAt = null),
    )

    private fun session() = GitHubSession(
        tokenStore = object : TokenStore {
            private var value: GitHubUserToken? = GitHubUserToken(
                accessToken = "token",
                refreshToken = null,
                accessTokenExpiresAt = null,
                refreshTokenExpiresAt = null,
                tokenType = "bearer",
            )
            override fun read(): GitHubUserToken? = value
            override fun write(token: GitHubUserToken) {
                value = token
            }
            override fun clear() {
                value = null
            }
        },
        deviceFlowClient = GitHubDeviceFlowClient(
            config = GitHubAuthConfig(clientId = "id"),
            transport = HttpTransport { error("OAuth not expected") },
            clock = clock,
        ),
        clock = clock,
    )
}
