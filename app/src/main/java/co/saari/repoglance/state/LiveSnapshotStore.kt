package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import java.time.Instant
import org.json.JSONObject

/**
 * Durable per-repository live counts, so a widget can render without a
 * network call. Only the fields the compact widget needs are persisted;
 * navigator rows stay out of storage.
 *
 * A record that cannot be read back exactly as it was written is discarded
 * rather than partially reconstructed — a half-restored snapshot would be a
 * count of unknown basis, which the truth rules forbid.
 */
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
        // Includes RepoSnapshot's own truth-rule IllegalArgumentException: a
        // record that no longer satisfies the invariants is not a snapshot.
        null
    }

    private fun JSONObject.intOrNull(name: String): Int? =
        if (isNull(name)) null else getInt(name)

    private fun JSONObject.instantOrNull(name: String): Instant? =
        if (isNull(name)) null else Instant.parse(getString(name))
}
