package co.saari.repoglance.auth

import java.time.Instant

class GitHubAuthConfig(
    val clientId: String,
) {
    val isReady: Boolean
        get() = clientId.isNotBlank()

    override fun toString(): String = "GitHubAuthConfig(clientId=$clientId)"
}

class GitHubDeviceAuthorization(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresAt: Instant,
    val intervalSeconds: Long,
) {
    override fun toString(): String =
        "GitHubDeviceAuthorization(userCode=<redacted>, deviceCode=<redacted>, " +
            "verificationUri=$verificationUri, expiresAt=$expiresAt, intervalSeconds=$intervalSeconds)"
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
