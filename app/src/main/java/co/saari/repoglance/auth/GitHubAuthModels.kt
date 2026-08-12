package co.saari.repoglance.auth

import java.time.Instant

class GitHubAuthConfig(
    val clientId: String,
    val clientSecret: String,
    val callbackUrl: String,
) {
    val isReady: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank() && callbackUrl.isNotBlank()

    override fun toString(): String = "GitHubAuthConfig(clientId=$clientId, clientSecret=<redacted>, callbackUrl=$callbackUrl)"
}

class PendingAuthorization(
    val state: String,
    val codeVerifier: String,
    val authorizationUrl: String,
) {
    override fun toString(): String = "PendingAuthorization(<redacted>)"
}

class GitHubUserToken(
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAt: Instant?,
    val refreshTokenExpiresAt: Instant?,
    val tokenType: String,
) {
    override fun toString(): String = "GitHubUserToken(<redacted>)"
}

sealed class AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>()
    data class Failure(
        val message: String,
        val needsNewSignIn: Boolean = false,
    ) : AuthResult<Nothing>()
}

class GitHubAuthException(
    message: String,
    val needsNewSignIn: Boolean = false,
) : Exception(message)
