package co.saari.repoglance.widget

import android.content.Context
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef

data class RepoWidgetConfig(
    val repo: RepoRef,
    val mode: NavigatorMode,
)

/** Private, per-app-widget configuration. App-widget IDs are assigned by the
 * launcher, so every placed widget gets independent repository and mode keys. */
object RepoWidgetConfigStore {
    private const val PREFS_NAME = "repo_widget_configs"
    private const val REPO_SUFFIX = ".repo"
    private const val MODE_SUFFIX = ".mode"

    fun load(context: Context, appWidgetId: Int): RepoWidgetConfig? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decode(
            repoFull = prefs.getString(key(appWidgetId, REPO_SUFFIX), null),
            modeName = prefs.getString(key(appWidgetId, MODE_SUFFIX), null),
        )
    }

    fun save(context: Context, appWidgetId: Int, config: RepoWidgetConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key(appWidgetId, REPO_SUFFIX), config.repo.full)
            .putString(key(appWidgetId, MODE_SUFFIX), config.mode.name)
            .apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key(appWidgetId, REPO_SUFFIX))
            .remove(key(appWidgetId, MODE_SUFFIX))
            .apply()
    }

    internal fun decode(repoFull: String?, modeName: String?): RepoWidgetConfig? {
        val parts = repoFull?.split('/', limit = 2) ?: return null
        if (parts.size != 2) return null
        val repo = runCatching { RepoRef(parts[0], parts[1]) }.getOrNull() ?: return null
        return RepoWidgetConfig(repo, navigatorModeFromExtra(modeName))
    }

    private fun key(appWidgetId: Int, suffix: String): String = "widget.$appWidgetId$suffix"
}
