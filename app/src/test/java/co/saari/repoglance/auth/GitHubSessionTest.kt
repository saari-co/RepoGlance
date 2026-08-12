package co.saari.repoglance.auth

import co.saari.repoglance.data.HttpResponse
import co.saari.repoglance.data.HttpTransport
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubSessionTest {
    private val now = Instant.parse("2026-08-12T17:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun currentTokenIsReturnedWithoutNetworkRefresh() {
        val store = FakeTokenStore(
            GitHubUserToken("current", "refresh", now.plusSeconds(3600), now.plusSeconds(7200), "bearer"),
        )
        val transport = HttpTransport { error("network should not be called") }
        val oauth = GitHubOAuthClient(config(), transport, clock)

        assertEquals("current", GitHubSession(store, oauth, clock).accessToken())
    }

    @Test
    fun expiringTokenIsRefreshedAndReplaced() {
        val store = FakeTokenStore(
            GitHubUserToken("old", "refresh", now.plusSeconds(30), now.plusSeconds(7200), "bearer"),
        )
        val transport = HttpTransport {
            HttpResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = """{"access_token":"new","refresh_token":"new-refresh","expires_in":28800,"refresh_token_expires_in":15811200,"token_type":"bearer"}""",
            )
        }
        val session = GitHubSession(store, GitHubOAuthClient(config(), transport, clock), clock)

        assertEquals("new", session.accessToken())
        assertEquals("new", store.value?.accessToken)
    }

    @Test
    fun signOutClearsOnlyLocalTokenStore() {
        val store = FakeTokenStore(
            GitHubUserToken("current", null, null, null, "bearer"),
        )
        val session = GitHubSession(store, GitHubOAuthClient(config(), HttpTransport { error("unused") }, clock), clock)

        session.signOut()

        assertNull(store.value)
    }

    @Test
    fun invalidRefreshRequiresNewSignInWithoutReflectingServerDescription() {
        val store = FakeTokenStore(
            GitHubUserToken("old", "refresh", now.minusSeconds(1), now.plusSeconds(7200), "bearer"),
        )
        val transport = HttpTransport {
            HttpResponse(
                statusCode = 400,
                headers = emptyMap(),
                body = """{"error":"bad_refresh_token","error_description":"untrusted detail"}""",
            )
        }
        val session = GitHubSession(store, GitHubOAuthClient(config(), transport, clock), clock)

        val failure = assertThrows(GitHubAuthException::class.java) { session.accessToken() }

        assertTrue(failure.needsNewSignIn)
        assertFalse(failure.message.orEmpty().contains("untrusted"))
    }

    private fun config() = GitHubAuthConfig(
        clientId = "client",
        clientSecret = "public-client-credential",
        callbackUrl = "https://repoglance.ztoned.com/oauth/callback",
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
}
