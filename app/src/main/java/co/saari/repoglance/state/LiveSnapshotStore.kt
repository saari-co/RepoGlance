package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import org.json.JSONObject
import java.time.Instant

object LiveSnapshotStore {

    private const val PREFS_NAME = "repoglance_live_snapshots"
    private const val VERSION = 1

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, snapshot: RepoSnapshot) {
        prefs(context).edit().putString(snapshot.repo.full, encode(snapshot)).apply()
    }

    fun load(context: Context, repo: RepoRef): RepoSnapshot? =
        prefs(context).getString(repo.full, null)?.let { decode(repo, it) }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    internal fun encode(snapshot: RepoSnapshot): String = JSONObject().apply {
        put("version", VERSION)
        put("basis", snapshot.valueBasis.name)
        put("openIssues", snapshot.openIssues ?: JSONObject.NULL)
        put("openPrs", snapshot.openPrs ?: JSONObject.NULL)
        put("awaitingReview", snapshot.prsAwaitingMyReview ?: JSONObject.NULL)
        put("observedAt", snapshot.observedAt?.toString() ?: JSONObject.NULL)
        put("pushedAt", snapshot.pushedAt?.toString() ?: JSONObject.NULL)
    }.toString()

    internal fun decode(repo: RepoRef, raw: String): RepoSnapshot? = try {
        val json = JSONObject(raw)
        if (json.optInt("version") != VERSION) {
            null
        } else {
            val basis = ValueBasis.valueOf(json.getString("basis"))
            RepoSnapshot(
                repo = repo,
                openPrs = json.intOrNull("openPrs"),
                prsAwaitingMyReview = json.intOrNull("awaitingReview"),
                openIssues = json.intOrNull("openIssues"),
                defaultBranchCi = CiState.UNKNOWN,
                latestRelease = null,
                pushedAt = json.instantOrNull("pushedAt"),
                valueBasis = basis,
                observedAt = json.instantOrNull("observedAt"),
                rateLimit = RateLimitBucket.UNKNOWN,
            )
        }
    } catch (_: Exception) {
        null
    }

    private fun JSONObject.intOrNull(name: String): Int? =
        if (isNull(name)) null else getInt(name)

    private fun JSONObject.instantOrNull(name: String): Instant? =
        if (isNull(name)) null else Instant.parse(getString(name))
}
