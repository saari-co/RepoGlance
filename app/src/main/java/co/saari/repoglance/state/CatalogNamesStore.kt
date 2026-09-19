package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object CatalogNamesStore {
    private const val PREFS_NAME = "repoglance_catalog_names"
    private const val KEY_NAMES = "names"
    private const val KEY_VIEWER = "viewer"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, names: List<String>, viewerLogin: String) {
        prefs(context).edit()
            .putString(KEY_NAMES, JSONArray(names).toString())
            .putString(KEY_VIEWER, viewerLogin)
            .apply()
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
