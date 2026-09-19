package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object CatalogNamesStore {
    private const val PREFS_NAME = "repoglance_catalog_names"
    private const val KEY_NAMES = "names"
    private const val KEY_VIEWER = "viewer"
    private const val KEY_PUSHED_AT = "pushedAt"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, names: List<String>, viewerLogin: String, pushedAt: Map<String, Instant>) {
        prefs(context).edit()
            .putString(KEY_NAMES, JSONArray(names).toString())
            .putString(KEY_VIEWER, viewerLogin)
            .putString(KEY_PUSHED_AT, encodePushedAt(pushedAt))
            .apply()
    }

    fun pushedAt(context: Context): Map<String, Instant> =
        decodePushedAt(prefs(context).getString(KEY_PUSHED_AT, null))

    internal fun encodePushedAt(pushedAt: Map<String, Instant>): String =
        JSONObject().apply { pushedAt.forEach { (name, instant) -> put(name, instant.toString()) } }.toString()

    internal fun decodePushedAt(raw: String?): Map<String, Instant> {
        if (raw == null) return emptyMap()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        return json.keys().asSequence()
            .mapNotNull { name -> runCatching { name to Instant.parse(json.getString(name)) }.getOrNull() }
            .toMap()
    }

    fun viewerLogin(context: Context): String? =
        prefs(context).getString(KEY_VIEWER, null)?.takeIf { it.isNotBlank() }

    fun load(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_NAMES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
