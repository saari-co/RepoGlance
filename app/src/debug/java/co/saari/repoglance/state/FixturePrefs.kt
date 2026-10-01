package co.saari.repoglance.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.core.content.edit
import co.saari.repoglance.fixtures.FixtureScenario

// Fixture-route prefs: the scenario switcher and the fixture-home pins. Only
// the debug fixture route, the debug scenario launcher and the debug pickers
// use them, so release AppPrefs carries none of them. They stay AppPrefs
// extensions on the same prefs file and keys, so existing debug installs keep
// their scenario and pins. Guarded by FixtureRouteGuardTest and
// FixtureRouteReleaseTest.

private const val KEY_SCENARIO = "selected_scenario"
private const val KEY_PINNED = "pinned_repos"

fun AppPrefs.selectedScenario(context: Context): FixtureScenario {
    val stored = prefs(context).getString(KEY_SCENARIO, null) ?: return FixtureScenario.MIXED
    return runCatching { FixtureScenario.valueOf(stored) }.getOrDefault(FixtureScenario.MIXED)
}

fun AppPrefs.setSelectedScenario(context: Context, scenario: FixtureScenario) {
    prefs(context).edit { putString(KEY_SCENARIO, scenario.name) }
}

fun AppPrefs.pinnedRepos(context: Context): Set<String> =
    prefs(context).getStringSet(KEY_PINNED, emptySet()).orEmpty().toSet()

fun AppPrefs.togglePin(context: Context, repoFull: String) {
    val current = pinnedRepos(context)
    val next = if (repoFull in current) current - repoFull else current + repoFull
    prefs(context).edit { putStringSet(KEY_PINNED, next) }
}

@Composable
fun AppPrefs.rememberScenario(context: Context): State<FixtureScenario> =
    rememberPrefsState(context, KEY_SCENARIO) { selectedScenario(context) }

@Composable
fun AppPrefs.rememberPinnedRepos(context: Context): State<Set<String>> =
    rememberPrefsState(context, KEY_PINNED) { pinnedRepos(context) }
