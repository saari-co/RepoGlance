package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant

data class LatestPushRecord(val repoFull: String, val pushedAt: Instant, val observedAt: Instant)

object LatestPushStore {
    private const val PREFS_NAME = "repoglance_latest_push"
    private const val KEY_REPO = "repo"
    private const val KEY_PUSHED_AT = "pushed_at"
    private const val KEY_OBSERVED_AT = "observed_at"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, record: LatestPushRecord) {
        prefs(context).edit()
            .putString(KEY_REPO, record.repoFull)
            .putLong(KEY_PUSHED_AT, record.pushedAt.toEpochMilli())
            .putLong(KEY_OBSERVED_AT, record.observedAt.toEpochMilli())
            .apply()
    }

    fun load(context: Context): LatestPushRecord? {
        val p = prefs(context)
        val repo = p.getString(KEY_REPO, null) ?: return null
        if (!p.contains(KEY_PUSHED_AT) || !p.contains(KEY_OBSERVED_AT)) return null
        return LatestPushRecord(
            repoFull = repo,
            pushedAt = Instant.ofEpochMilli(p.getLong(KEY_PUSHED_AT, 0L)),
            observedAt = Instant.ofEpochMilli(p.getLong(KEY_OBSERVED_AT, 0L)),
        )
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
