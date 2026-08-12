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
        val store = FakeTokenStore(token("current", "refresh", now.plusSeconds(3600), now.plusSeconds(7200)))
        val transport = HttpTransport { error("network should not be called") }

        assertEquals("current", session(store, transport).accessToken())
    }

    @Test
    fun expiringDeviceFlowTokenIsRefreshedAndReplaced() {
        val store = FakeTokenStore(token("old", "refresh", now.plusSeconds(30), now.plusSeconds(7200)))
        val transport = HttpTransport {
            HttpResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = """{
                  "access_token":"new",
                  "refresh_token":"new-refresh",
                  "expires_in":28800,
                  "refresh_token_expires_in":15811200,
                  "token_type":"bearer"
                }""",
            )
        }

        assertEquals("new", session(store, transport).accessToken())
        assertEquals("new", store.value?.accessToken)
        assertEquals("new-refresh", store.value?.refreshToken)
    }

    @Test
    fun nonExpiringDeviceFlowTokenDoesNotNeedRefreshMaterial() {
        val store = FakeTokenStore(token("current", null, null, null))

        assertEquals("current", session(store, HttpTransport { error("unused") }).accessToken())
    }

    @Test
    fun signOutClearsOnlyLocalTokenStore() {
        val store = FakeTokenStore(token("current", null, null, null))

        session(store, HttpTransport { error("unused") }).signOut()

        assertNull(store.value)
    }

    @Test
    fun invalidRefreshRequiresNewSignInWithoutReflectingServerDescription() {
        val store = FakeTokenStore(token("old", "refresh", now.minusSeconds(1), now.plusSeconds(7200)))
        val transport = HttpTransport {
            HttpResponse(
                statusCode = 400,
                headers = emptyMap(),
                body = """{"error":"bad_refresh_token","error_description":"untrusted detail"}""",
            )
        }

        val failure = assertThrows(GitHubAuthException::class.java) { session(store, transport).accessToken() }

        assertTrue(failure.needsNewSignIn)
        assertFalse(failure.message.orEmpty().contains("untrusted"))
    }

    private fun session(store: TokenStore, transport: HttpTransport) = GitHubSession(
        tokenStore = store,
        deviceFlowClient = GitHubDeviceFlowClient(GitHubAuthConfig("public-client-id"), transport, clock),
        clock = clock,
    )

    private fun token(
        access: String,
        refresh: String?,
        accessExpiry: Instant?,
        refreshExpiry: Instant?,
    ) = GitHubUserToken(access, refresh, accessExpiry, refreshExpiry, "bearer")

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
