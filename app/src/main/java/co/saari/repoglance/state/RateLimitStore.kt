package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import co.saari.repoglance.data.RateLimitSnapshot
import co.saari.repoglance.model.RateLimitBucket
import java.time.Instant

data class RateLimitState(
    val bucket: RateLimitBucket,
    val resetsAt: Instant?,
)

object RateLimitStore {
    private const val PREFS_NAME = "repoglance_rate_limit"
    private const val KEY_BUCKET = "bucket"
    private const val KEY_RESETS_AT = "resetsAt"
    private const val EXHAUSTED_FALLBACK_SECONDS = 60L

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun record(context: Context, snapshot: RateLimitSnapshot, now: Instant) {
        val state = stateToRecord(snapshot, now) ?: return
        prefs(context).edit()
            .putString(KEY_BUCKET, state.bucket.name)
            .putString(KEY_RESETS_AT, state.resetsAt?.toString())
            .apply()
    }

    fun load(context: Context): RateLimitState? {
        val prefs = prefs(context)
        val bucket = prefs.getString(KEY_BUCKET, null)
            ?.let { name -> RateLimitBucket.entries.firstOrNull { it.name == name } }
            ?: return null
        val resetsAt = prefs.getString(KEY_RESETS_AT, null)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return RateLimitState(bucket, resetsAt)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    internal fun stateToRecord(snapshot: RateLimitSnapshot, now: Instant): RateLimitState? = when (snapshot.bucket) {
        RateLimitBucket.UNKNOWN -> null
        RateLimitBucket.EXHAUSTED -> RateLimitState(
            RateLimitBucket.EXHAUSTED,
            snapshot.resetsAt ?: now.plusSeconds(EXHAUSTED_FALLBACK_SECONDS),
        )
        else -> RateLimitState(snapshot.bucket, snapshot.resetsAt)
    }

    fun effectiveBucket(state: RateLimitState?, now: Instant): RateLimitBucket = when {
        state == null -> RateLimitBucket.UNKNOWN
        state.resetsAt != null && !now.isBefore(state.resetsAt) -> RateLimitBucket.UNKNOWN
        else -> state.bucket
    }

    fun exhaustedUntil(state: RateLimitState?, now: Instant): Instant? =
        state?.resetsAt?.takeIf { effectiveBucket(state, now) == RateLimitBucket.EXHAUSTED }
}
