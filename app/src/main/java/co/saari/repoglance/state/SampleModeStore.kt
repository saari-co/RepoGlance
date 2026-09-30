package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.edit
import co.saari.repoglance.widget.RepoWidgetConfig
import co.saari.repoglance.widget.RepoWidgetConfigStore

object SampleModeStore {
    internal const val PREFS_NAME = "repoglance_sample"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PINS = "pins"
    private const val WIDGET_PREFIX = "widget."
    private const val REPO_SUFFIX = ".repo"
    private const val MODE_SUFFIX = ".mode"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun enter(context: Context) {
        prefs(context).edit { putBoolean(KEY_ACTIVE, true) }
    }

    fun leave(context: Context) {
        prefs(context).edit { clear() }
    }

    fun pins(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PINS, emptySet()).orEmpty().toSet()

    fun togglePin(context: Context, repoFull: String) {
        val current = pins(context)
        val next = if (repoFull in current) current - repoFull else current + repoFull
        prefs(context).edit { putStringSet(KEY_PINS, next) }
    }

    fun addPin(context: Context, repoFull: String) {
        prefs(context).edit { putStringSet(KEY_PINS, pins(context) + repoFull) }
    }

    fun removePins(context: Context, repoFulls: Collection<String>) {
        if (repoFulls.isEmpty()) return
        prefs(context).edit { putStringSet(KEY_PINS, pins(context) - repoFulls.toSet()) }
    }

    fun widgetConfig(context: Context, appWidgetId: Int): RepoWidgetConfig? {
        val p = prefs(context)
        return RepoWidgetConfigStore.decode(
            repoFull = p.getString(widgetKey(appWidgetId, REPO_SUFFIX), null),
            modeName = p.getString(widgetKey(appWidgetId, MODE_SUFFIX), null),
        )
    }

    fun saveWidgetConfig(context: Context, appWidgetId: Int, config: RepoWidgetConfig) {
        prefs(context).edit {
            putString(widgetKey(appWidgetId, REPO_SUFFIX), config.repo.full)
            putString(widgetKey(appWidgetId, MODE_SUFFIX), config.mode.name)
        }
    }

    fun removeWidgetConfigs(context: Context, appWidgetIds: Collection<Int>) {
        if (appWidgetIds.isEmpty()) return
        prefs(context).edit {
            appWidgetIds.forEach {
                remove(widgetKey(it, REPO_SUFFIX))
                remove(widgetKey(it, MODE_SUFFIX))
            }
        }
    }

    fun widgetRepos(context: Context): List<String> =
        prefs(context).all
            .filterKeys { it.startsWith(WIDGET_PREFIX) && it.endsWith(REPO_SUFFIX) }
            .values
            .mapNotNull { it as? String }

    private fun widgetKey(appWidgetId: Int, suffix: String): String = "$WIDGET_PREFIX$appWidgetId$suffix"

    @Composable
    fun rememberPins(context: Context): State<Set<String>> {
        val state = remember { mutableStateOf(pins(context)) }
        DisposableEffect(context) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == KEY_PINS || changedKey == null) state.value = pins(context)
            }
            val p = prefs(context)
            p.registerOnSharedPreferenceChangeListener(listener)
            onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return state
    }
}
