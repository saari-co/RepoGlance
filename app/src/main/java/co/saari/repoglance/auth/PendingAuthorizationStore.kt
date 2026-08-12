package co.saari.repoglance.auth

import android.content.Context

class PendingAuthorizationStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(pending: PendingAuthorization) {
        preferences.edit()
            .putString(KEY_STATE, pending.state)
            .putString(KEY_VERIFIER, pending.codeVerifier)
            .apply()
    }

    fun verifierFor(expectedState: String): String? {
        val savedState = preferences.getString(KEY_STATE, null)
        val verifier = preferences.getString(KEY_VERIFIER, null)
        if (verifier == null || savedState == null || !constantTimeEquals(savedState, expectedState)) {
            return null
        }
        return verifier
    }

    fun clear() {
        preferences.edit().remove(KEY_STATE).remove(KEY_VERIFIER).apply()
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val leftBytes = left.toByteArray(Charsets.UTF_8)
        val rightBytes = right.toByteArray(Charsets.UTF_8)
        if (leftBytes.size != rightBytes.size) return false
        var difference = 0
        leftBytes.indices.forEach { index ->
            difference = difference or (leftBytes[index].toInt() xor rightBytes[index].toInt())
        }
        return difference == 0
    }

    private companion object {
        const val PREFERENCES_NAME = "repoglance_pending_github_authorization"
        const val KEY_STATE = "state"
        const val KEY_VERIFIER = "verifier"
    }
}
