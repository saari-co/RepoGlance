package co.saari.repoglance.auth

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object GitHubAuthorization {
    private const val AUTHORIZATION_ENDPOINT = "https://github.com/login/oauth/authorize"

    fun create(config: GitHubAuthConfig, secureRandom: SecureRandom = SecureRandom()): PendingAuthorization {
        require(config.clientId.isNotBlank()) { "GitHub client ID is required" }
        require(config.callbackUrl.isNotBlank()) { "GitHub callback URL is required" }

        val state = randomUrlSafe(secureRandom, 32)
        val verifier = randomUrlSafe(secureRandom, 64)
        return PendingAuthorization(
            state = state,
            codeVerifier = verifier,
            authorizationUrl = authorizationUrl(config, state, verifier),
        )
    }

    fun authorizationUrl(config: GitHubAuthConfig, state: String, codeVerifier: String): String {
        require(state.isNotBlank()) { "OAuth state is required" }
        require(codeVerifier.length in 43..128) { "PKCE verifier must be 43 to 128 characters" }
        val params = linkedMapOf(
            "client_id" to config.clientId,
            "redirect_uri" to config.callbackUrl,
            "state" to state,
            "code_challenge" to challenge(codeVerifier),
            "code_challenge_method" to "S256",
        )
        return AUTHORIZATION_ENDPOINT + "?" + formEncode(params)
    }

    fun challenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun formEncode(values: Map<String, String>): String = values.entries.joinToString("&") { (name, value) ->
        "${encode(name)}=${encode(value)}"
    }

    private fun randomUrlSafe(random: SecureRandom, size: Int): String {
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
