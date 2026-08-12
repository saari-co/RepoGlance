package co.saari.repoglance.auth

import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAuthorizationTest {
    private val config = GitHubAuthConfig(
        clientId = "client id",
        clientSecret = "not-used-by-authorization-url",
        callbackUrl = "https://repoglance.ztoned.com/oauth/callback",
    )

    @Test
    fun challengeMatchesRfc7636Vector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            GitHubAuthorization.challenge(verifier),
        )
    }

    @Test
    fun authorizationUrlCarriesExactCallbackStateAndS256Challenge() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val url = GitHubAuthorization.authorizationUrl(config, "state value", verifier)
        val uri = URI(url)
        val query = uri.rawQuery.split('&').associate { parameter ->
            val (name, value) = parameter.split('=', limit = 2)
            URLDecoder.decode(name, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
        }

        assertEquals("https", uri.scheme)
        assertEquals("github.com", uri.host)
        assertEquals("client id", query["client_id"])
        assertEquals(config.callbackUrl, query["redirect_uri"])
        assertEquals("state value", query["state"])
        assertEquals("S256", query["code_challenge_method"])
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", query["code_challenge"])
        assertFalse(url.contains(config.clientSecret))
    }

    @Test
    fun generatedAuthorizationUsesIndependentUrlSafeStateAndVerifier() {
        val first = GitHubAuthorization.create(config)
        val second = GitHubAuthorization.create(config)

        assertTrue(first.state.matches(Regex("^[A-Za-z0-9_-]+$")))
        assertTrue(first.codeVerifier.matches(Regex("^[A-Za-z0-9_-]{43,128}$")))
        assertFalse(first.state == second.state)
        assertFalse(first.codeVerifier == second.codeVerifier)
    }

    @Test
    fun formEncodingUsesPercentEncodingRatherThanPlusForSpaces() {
        assertEquals("a%20b=c%2Bd%26e", GitHubAuthorization.formEncode(mapOf("a b" to "c+d&e")))
    }
}
