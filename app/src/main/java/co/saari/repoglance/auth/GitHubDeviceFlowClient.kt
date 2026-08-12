package co.saari.repoglance.auth

import co.saari.repoglance.data.HttpRequest
import co.saari.repoglance.data.HttpResponse
import co.saari.repoglance.data.HttpTransport
import co.saari.repoglance.data.UrlConnectionTransport
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject

interface GitHubDeviceAuthorizationGateway {
    fun poll(deviceCode: String): DevicePollResult
}

sealed interface DevicePollResult {
    data class Authorized(val token: GitHubUserToken) : DevicePollResult
    data object Pending : DevicePollResult
    data class SlowDown(val intervalSeconds: Long?) : DevicePollResult
}

class GitHubDeviceFlowClient(
    private val config: GitHubAuthConfig,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val clock: Clock = Clock.systemUTC(),
) : GitHubDeviceAuthorizationGateway {
    fun begin(): GitHubDeviceAuthorization {
        require(config.clientId.isNotBlank()) { "GitHub client ID is required" }
        val response = post(
            endpoint = DEVICE_CODE_ENDPOINT,
            parameters = mapOf("client_id" to config.clientId),
        )
        val json = response.jsonOrThrow("GitHub returned an unreadable device sign-in response")
        if (response.statusCode !in 200..299 || json.has("error")) {
            throw authFailure(json.optionalNonBlank("error"), response.statusCode)
        }

        val expiresIn = json.requiredPositiveLong("expires_in")
        val interval = json.requiredPositiveLong("interval")
        val verificationUri = json.requiredNonBlank("verification_uri").also(::requireVerificationUri)
        return GitHubDeviceAuthorization(
            deviceCode = json.requiredNonBlank("device_code"),
            userCode = json.requiredNonBlank("user_code"),
            verificationUri = verificationUri,
            expiresAt = clock.instant().plusSeconds(expiresIn),
            intervalSeconds = interval,
        )
    }

    override fun poll(deviceCode: String): DevicePollResult {
        require(deviceCode.isNotBlank()) { "GitHub device code is required" }
        val response = post(
            endpoint = TOKEN_ENDPOINT,
            parameters = linkedMapOf(
                "client_id" to config.clientId,
                "device_code" to deviceCode,
                "grant_type" to DEVICE_GRANT_TYPE,
            ),
        )
        val json = response.jsonOrThrow("GitHub returned an unreadable sign-in response")
        return when (val error = json.optionalNonBlank("error")) {
            null -> {
                if (response.statusCode !in 200..299) throw authFailure(null, response.statusCode)
                DevicePollResult.Authorized(parseToken(json))
            }
            "authorization_pending" -> DevicePollResult.Pending
            "slow_down" -> DevicePollResult.SlowDown(json.optionalPositiveLong("interval"))
            else -> throw authFailure(error, response.statusCode)
        }
    }

    fun refresh(refreshToken: String): GitHubUserToken {
        require(refreshToken.isNotBlank()) { "GitHub refresh token is required" }
        val response = post(
            endpoint = TOKEN_ENDPOINT,
            parameters = linkedMapOf(
                "client_id" to config.clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
            ),
        )
        val json = response.jsonOrThrow("GitHub returned an unreadable token refresh response")
        if (response.statusCode !in 200..299 || json.has("error")) {
            throw authFailure(json.optionalNonBlank("error"), response.statusCode)
        }
        return parseToken(json)
    }

    private fun parseToken(json: JSONObject): GitHubUserToken {
        val now = clock.instant()
        val accessExpiry = json.optionalPositiveLong("expires_in")?.let(now::plusSeconds)
        val refreshToken = json.optionalNonBlank("refresh_token")
        val refreshExpiry = json.optionalPositiveLong("refresh_token_expires_in")?.let(now::plusSeconds)
        if (accessExpiry != null && (refreshToken == null || refreshExpiry == null)) {
            throw GitHubAuthException("GitHub returned an incomplete expiring session", needsNewSignIn = true)
        }
        val tokenType = json.optionalNonBlank("token_type") ?: "bearer"
        if (!tokenType.equals("bearer", ignoreCase = true)) {
            throw GitHubAuthException("GitHub returned an unsupported token type", needsNewSignIn = true)
        }
        return GitHubUserToken(
            accessToken = json.requiredNonBlank("access_token"),
            refreshToken = refreshToken,
            accessTokenExpiresAt = accessExpiry,
            refreshTokenExpiresAt = refreshExpiry,
            tokenType = tokenType,
        )
    }

    private fun post(endpoint: String, parameters: Map<String, String>): HttpResponse {
        val body = formEncode(parameters).toByteArray(Charsets.UTF_8)
        return transport.execute(
            HttpRequest(
                method = "POST",
                url = endpoint,
                headers = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "User-Agent" to "RepoGlance-Android",
                ),
                body = body,
            ),
        )
    }

    private fun authFailure(error: String?, statusCode: Int): GitHubAuthException = GitHubAuthException(
        message = when (error) {
            "expired_token", "token_expired", "bad_verification_code", "incorrect_device_code" ->
                "The GitHub sign-in code expired. Please connect again."
            "access_denied" -> "GitHub sign-in was cancelled"
            "device_flow_disabled" -> "Device Flow is not enabled for the RepoGlance GitHub App"
            "bad_refresh_token", "invalid_grant" -> "Your GitHub session expired"
            "incorrect_client_credentials" -> "This build has an invalid GitHub App client ID"
            "unsupported_grant_type" -> "GitHub rejected the device sign-in request"
            else -> "GitHub sign-in failed ($statusCode)"
        },
        needsNewSignIn = error in TERMINAL_AUTH_ERRORS,
    )

    private fun requireVerificationUri(value: String) {
        val uri = runCatching { URI(value) }.getOrElse {
            throw GitHubAuthException("GitHub returned an invalid verification address")
        }
        if (
            uri.scheme != "https" ||
            uri.host != "github.com" ||
            uri.port != -1 ||
            uri.path != "/login/device" ||
            uri.rawQuery != null ||
            uri.rawFragment != null ||
            uri.userInfo != null
        ) {
            throw GitHubAuthException("GitHub returned an invalid verification address")
        }
    }

    private fun HttpResponse.jsonOrThrow(message: String): JSONObject =
        runCatching { JSONObject(body) }.getOrElse { throw GitHubAuthException(message) }

    private fun JSONObject.requiredNonBlank(name: String): String = optionalNonBlank(name)
        ?: throw GitHubAuthException("GitHub sign-in response omitted $name")

    private fun JSONObject.optionalNonBlank(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf(String::isNotBlank) else null

    private fun JSONObject.requiredPositiveLong(name: String): Long = optionalPositiveLong(name)
        ?: throw GitHubAuthException("GitHub sign-in response omitted $name")

    private fun JSONObject.optionalPositiveLong(name: String): Long? =
        if (has(name) && !isNull(name)) getLong(name).takeIf { it > 0L } else null

    companion object {
        const val DEVICE_CODE_ENDPOINT = "https://github.com/login/device/code"
        const val TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token"
        const val DEVICE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"

        fun formEncode(values: Map<String, String>): String = values.entries.joinToString("&") { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

        private val TERMINAL_AUTH_ERRORS = setOf(
            "expired_token",
            "token_expired",
            "bad_verification_code",
            "incorrect_device_code",
            "access_denied",
            "device_flow_disabled",
            "bad_refresh_token",
            "invalid_grant",
            "incorrect_client_credentials",
        )
    }
}

class GitHubDeviceFlowPoller(
    private val gateway: GitHubDeviceAuthorizationGateway,
    private val clock: Clock = Clock.systemUTC(),
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun awaitToken(authorization: GitHubDeviceAuthorization): GitHubUserToken {
        var intervalSeconds = authorization.intervalSeconds
        while (true) {
            val remainingMillis = Duration.between(clock.instant(), authorization.expiresAt).toMillis()
            if (remainingMillis <= 0L) throw expiredCode()
            val waitMillis = minOf(secondsToMillis(intervalSeconds), remainingMillis)
            try {
                wait(waitMillis)
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
            if (!clock.instant().isBefore(authorization.expiresAt)) throw expiredCode()

            when (val result = gateway.poll(authorization.deviceCode)) {
                is DevicePollResult.Authorized -> return result.token
                DevicePollResult.Pending -> Unit
                is DevicePollResult.SlowDown -> {
                    val requiredInterval = result.intervalSeconds ?: 0L
                    val slowedInterval = if (intervalSeconds > Long.MAX_VALUE - SLOW_DOWN_INCREMENT_SECONDS) {
                        Long.MAX_VALUE
                    } else {
                        intervalSeconds + SLOW_DOWN_INCREMENT_SECONDS
                    }
                    intervalSeconds = maxOf(slowedInterval, requiredInterval)
                }
            }
        }
    }

    private fun expiredCode(): GitHubAuthException = GitHubAuthException(
        "The GitHub sign-in code expired. Please connect again.",
        needsNewSignIn = true,
    )

    private fun secondsToMillis(seconds: Long): Long =
        seconds.coerceAtMost(Long.MAX_VALUE / 1_000L) * 1_000L

    private companion object {
        const val SLOW_DOWN_INCREMENT_SECONDS = 5L
    }
}

class AuthorizationCommitGate {
    private var generation = 0L

    @Synchronized
    fun nextGeneration(): Long {
        generation += 1L
        return generation
    }

    @Synchronized
    fun commit(expectedGeneration: Long, action: () -> Unit): Boolean {
        if (expectedGeneration != generation) return false
        action()
        return true
    }

    @Synchronized
    fun isCurrent(expectedGeneration: Long): Boolean = expectedGeneration == generation

    @Synchronized
    fun invalidate(action: () -> Unit) {
        generation += 1L
        action()
    }
}

class GitHubSession(
    private val tokenStore: TokenStore,
    private val deviceFlowClient: GitHubDeviceFlowClient,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Synchronized
    fun hasSavedSession(): Boolean = tokenStore.read() != null

    @Synchronized
    fun acceptDeviceToken(token: GitHubUserToken) {
        tokenStore.write(token)
    }

    @Synchronized
    fun accessToken(): String {
        val saved = tokenStore.read()
            ?: throw GitHubAuthException("Connect GitHub to continue", needsNewSignIn = true)
        val expiry = saved.accessTokenExpiresAt
        if (expiry == null || expiry.isAfter(clock.instant().plusSeconds(REFRESH_WINDOW_SECONDS))) {
            return saved.accessToken
        }
        val refreshToken = saved.refreshToken
            ?: throw GitHubAuthException("Your GitHub session expired", needsNewSignIn = true)
        if (saved.refreshTokenExpiresAt?.isAfter(clock.instant()) != true) {
            throw GitHubAuthException("Your GitHub session expired", needsNewSignIn = true)
        }
        return deviceFlowClient.refresh(refreshToken).also(tokenStore::write).accessToken
    }

    @Synchronized
    fun signOut() {
        tokenStore.clear()
    }

    private companion object {
        const val REFRESH_WINDOW_SECONDS = 5 * 60L
    }
}
