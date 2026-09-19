package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import co.saari.repoglance.data.LiveIssue
import co.saari.repoglance.data.LivePullRequest
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.widget.WidgetRow
import co.saari.repoglance.widget.WidgetRowKind
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object LiveRowsStore {
    private const val PREFS_NAME = "repoglance_live_rows"
    private const val VERSION = 1
    const val MAX_ROWS = 10

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, repo: RepoRef, rows: List<WidgetRow>) {
        prefs(context).edit().putString(repo.full, encode(rows)).apply()
    }

    fun load(context: Context, repo: RepoRef): List<WidgetRow> =
        prefs(context).getString(repo.full, null)?.let(::decode).orEmpty()

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun rowsFrom(issues: List<LiveIssue>?, pullRequests: List<LivePullRequest>?): List<WidgetRow> {
        val issueRows = issues.orEmpty().map {
            WidgetRow(WidgetRowKind.ISSUE, it.number, it.title, it.updatedAt, it.htmlUrl)
        }
        val prRows = pullRequests.orEmpty().map {
            WidgetRow(WidgetRowKind.PR, it.number, it.title, it.updatedAt, it.htmlUrl)
        }
        return (issueRows + prRows).sortedByDescending { it.updatedAt }.take(MAX_ROWS)
    }

    internal fun encode(rows: List<WidgetRow>): String = JSONObject().apply {
        put("version", VERSION)
        put(
            "rows",
            JSONArray().apply {
                rows.take(MAX_ROWS).forEach { row ->
                    put(
                        JSONObject().apply {
                            put("kind", row.kind.name)
                            put("number", row.number)
                            put("title", row.title)
                            put("updatedAt", row.updatedAt.toString())
                            put("url", row.url)
                        },
                    )
                }
            },
        )
    }.toString()

    internal fun decode(raw: String): List<WidgetRow>? = try {
        val json = JSONObject(raw)
        if (json.optInt("version") != VERSION) {
            null
        } else {
            val array = json.getJSONArray("rows")
            List(array.length()) { index ->
                val row = array.getJSONObject(index)
                WidgetRow(
                    kind = WidgetRowKind.valueOf(row.getString("kind")),
                    number = row.getInt("number"),
                    title = row.getString("title"),
                    updatedAt = Instant.parse(row.getString("updatedAt")),
                    url = row.getString("url"),
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}
