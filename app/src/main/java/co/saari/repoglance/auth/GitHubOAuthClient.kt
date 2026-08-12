package co.saari.repoglance.auth

import co.saari.repoglance.data.HttpRequest
import co.saari.repoglance.data.HttpTransport
import co.saari.repoglance.data.UrlConnectionTransport
import java.time.Clock
import java.time.Instant
import org.json.JSONObject

class GitHubOAuthClient(
    private val config: GitHubAuthConfig,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun exchangeCode(code: String, codeVerifier: String): GitHubUserToken = exchange(
        linkedMapOf(
            "client_id" to config.clientId,
            "client_secret" to requiredClientSecret(),
            "code" to code,
            "redirect_uri" to config.callbackUrl,
            "code_verifier" to codeVerifier,
        ),
    )

    fun refresh(refreshToken: String): GitHubUserToken = exchange(
        linkedMapOf(
            "client_id" to config.clientId,
            "client_secret" to requiredClientSecret(),
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
        ),
    )

    private fun exchange(parameters: Map<String, String>): GitHubUserToken {
        val body = GitHubAuthorization.formEncode(parameters).toByteArray(Charsets.UTF_8)
        val response = transport.execute(
            HttpRequest(
                method = "POST",
                url = TOKEN_ENDPOINT,
                headers = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "User-Agent" to "RepoGlance-Android",
                ),
                body = body,
            ),
        )
        val json = runCatching { JSONObject(response.body) }.getOrElse {
            throw GitHubAuthException("GitHub returned an unreadable sign-in response")
        }
        if (response.statusCode !in 200..299 || json.has("error")) {
            val error = json.optionalNonBlank("error")
            val needsNewSignIn = error in setOf(
                "bad_verification_code",
                "bad_refresh_token",
                "incorrect_client_credentials",
                "invalid_grant",
            )
            throw GitHubAuthException(
                message = when (error) {
                    "bad_verification_code" -> "GitHub sign-in expired. Please connect again."
                    "bad_refresh_token", "invalid_grant" -> "Your GitHub session expired"
                    "incorrect_client_credentials" -> "This RepoGlance build needs an updated GitHub credential"
                    else -> "GitHub sign-in failed (${response.statusCode})"
                },
                needsNewSignIn = needsNewSignIn,
            )
        }
        val accessToken = json.requiredNonBlank("access_token")
        val now = clock.instant()
        return GitHubUserToken(
            accessToken = accessToken,
            refreshToken = json.optionalNonBlank("refresh_token"),
            accessTokenExpiresAt = json.optionalPositiveLong("expires_in")?.let(now::plusSeconds),
            refreshTokenExpiresAt = json.optionalPositiveLong("refresh_token_expires_in")?.let(now::plusSeconds),
            tokenType = json.optionalNonBlank("token_type") ?: "bearer",
        )
    }

    private fun requiredClientSecret(): String = config.clientSecret.takeIf(String::isNotBlank)
        ?: throw GitHubAuthException("This build still needs its local GitHub App credential")

    private fun JSONObject.requiredNonBlank(name: String): String = optionalNonBlank(name)
        ?: throw GitHubAuthException("GitHub sign-in response omitted $name")

    private fun JSONObject.optionalNonBlank(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf(String::isNotBlank) else null

    private fun JSONObject.optionalPositiveLong(name: String): Long? =
        if (has(name) && !isNull(name)) getLong(name).takeIf { it > 0L } else null

    private companion object {
        const val TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token"
    }
}

class GitHubSession(
    private val tokenStore: TokenStore,
    private val oauthClient: GitHubOAuthClient,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Synchronized
    fun hasSavedSession(): Boolean = tokenStore.read() != null

    @Synchronized
    fun acceptAuthorizationCode(code: String, codeVerifier: String): GitHubUserToken =
        oauthClient.exchangeCode(code, codeVerifier).also(tokenStore::write)

    @Synchronized
    fun accessToken(): String {
        val saved = tokenStore.read() ?: throw GitHubAuthException("Connect GitHub to continue", needsNewSignIn = true)
        val expiry = saved.accessTokenExpiresAt
        if (expiry == null || expiry.isAfter(clock.instant().plusSeconds(REFRESH_WINDOW_SECONDS))) {
            return saved.accessToken
        }
        val refreshToken = saved.refreshToken
            ?: throw GitHubAuthException("Your GitHub session expired", needsNewSignIn = true)
        if (saved.refreshTokenExpiresAt?.isBefore(Instant.now(clock)) == true) {
            throw GitHubAuthException("Your GitHub session expired", needsNewSignIn = true)
        }
        return oauthClient.refresh(refreshToken).also(tokenStore::write).accessToken
    }

    @Synchronized
    fun signOut() {
        tokenStore.clear()
    }

    private companion object {
        const val REFRESH_WINDOW_SECONDS = 5 * 60L
    }
}
