package co.saari.repoglance.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import co.saari.repoglance.fixtures.FixtureScenario

/**
 * Thin SharedPreferences wrapper for Slice 2's on-device state: the
 * fixture-scenario switcher and the pinned-repo set (repos stored as
 * [co.saari.repoglance.model.RepoRef.full] strings). Two keys don't need
 * DataStore.
 */
object AppPrefs {
    private const val PREFS_NAME = "repoglance"
    private const val KEY_SCENARIO = "selected_scenario"
    private const val KEY_PINNED = "pinned_repos"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun selectedScenario(context: Context): FixtureScenario {
        val stored = prefs(context).getString(KEY_SCENARIO, null) ?: return FixtureScenario.MIXED
        return runCatching { FixtureScenario.valueOf(stored) }.getOrDefault(FixtureScenario.MIXED)
    }

    fun setSelectedScenario(context: Context, scenario: FixtureScenario) {
        prefs(context).edit().putString(KEY_SCENARIO, scenario.name).apply()
    }

    fun pinnedRepos(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PINNED, emptySet()).orEmpty().toSet()

    /** Toggles [repoFull] in the pinned set and persists the result. */
    fun togglePin(context: Context, repoFull: String) {
        val current = pinnedRepos(context)
        val next = if (repoFull in current) current - repoFull else current + repoFull
        prefs(context).edit().putStringSet(KEY_PINNED, next).apply()
    }

    /** Recomposes on [KEY_SCENARIO] changes, including ones made outside
     *  the calling composable (e.g. another screen's setter). */
    @Composable
    fun rememberScenario(context: Context): State<FixtureScenario> =
        rememberPrefsState(context, KEY_SCENARIO) { selectedScenario(context) }

    /** Recomposes on [KEY_PINNED] changes. */
    @Composable
    fun rememberPinnedRepos(context: Context): State<Set<String>> =
        rememberPrefsState(context, KEY_PINNED) { pinnedRepos(context) }

    @Composable
    private fun <T> rememberPrefsState(context: Context, key: String, read: () -> T): State<T> {
        val state = remember { mutableStateOf(read()) }
        DisposableEffect(context, key) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) state.value = read()
            }
            val p = prefs(context)
            p.registerOnSharedPreferenceChangeListener(listener)
            onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return state
    }
}
