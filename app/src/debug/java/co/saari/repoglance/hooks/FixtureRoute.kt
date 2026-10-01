package co.saari.repoglance.hooks

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.ui.HomeScreen
import co.saari.repoglance.ui.NavigatorScreen
import co.saari.repoglance.widget.WidgetRefresh
import co.saari.repoglance.widget.navigatorModeFromExtra
import kotlinx.coroutines.launch

// Debug-only fixture route: the fixture home and the fixture navigator.
// They are test screens, not user surfaces (maintainer decision 2026-10-01,
// PR #55), so they live in the debug source set. MainActivity opens this
// route when an intent carries EXTRA_REPO_FULL, which only
// ScenarioLaunchActivity's `navigator` screen sends. The release flavour is
// a no-op: a release build ignores the extra and stays on the live entry.
// Guarded by FixtureRouteGuardTest and FixtureRouteReleaseTest.

internal const val EXTRA_REPO_FULL: String = "repo_full"

internal const val EXTRA_NAVIGATOR_MODE: String = "navigator_mode"

class FixtureRoute {
    private val scope = mutableStateOf<NavigatorScope?>(null)
    private val mode = mutableStateOf(NavigatorMode.BOTH)
    private val routeToken = mutableIntStateOf(0)

    val isOpen: Boolean get() = scope.value != null

    fun onCreate(intent: Intent?) {
        scope.value = resolveScope(intent)
        mode.value = navigatorModeFromExtra(intent?.getStringExtra(EXTRA_NAVIGATOR_MODE))
    }

    fun onNewIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_MAIN) {
            scope.value = null
            routeToken.intValue += 1
            return
        }
        val nextScope = resolveScope(intent) ?: return
        scope.value = nextScope
        mode.value = navigatorModeFromExtra(intent.getStringExtra(EXTRA_NAVIGATOR_MODE))
        routeToken.intValue += 1
    }

    fun close() {
        scope.value = null
    }

    @Composable
    fun Content() {
        val currentScope = scope.value ?: return
        key(routeToken.intValue) {
            FixtureRoot(currentScope, mode.value)
        }
    }

    private fun resolveScope(intent: Intent?): NavigatorScope.Repo? {
        return intent
            ?.getStringExtra(EXTRA_REPO_FULL)
            ?.let { NavigatorScopeCodec.decode("REPO", it) }
            ?.takeIf { it is NavigatorScope.Repo } as? NavigatorScope.Repo
    }
}

@Composable
private fun FixtureRoot(initialNavigatorScope: NavigatorScope, initialNavigatorMode: NavigatorMode) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isHome by rememberSaveable { mutableStateOf(false) }
    var scopeKind by rememberSaveable { mutableStateOf(NavigatorScopeCodec.kindOf(initialNavigatorScope)) }
    var scopeValue by rememberSaveable { mutableStateOf(NavigatorScopeCodec.valueOf(initialNavigatorScope)) }
    var modeName by rememberSaveable { mutableStateOf(initialNavigatorMode.name) }

    val scenarioState = AppPrefs.rememberScenario(context)
    val pinnedState = AppPrefs.rememberPinnedRepos(context)

    fun refreshWidgets() {
        coroutineScope.launch { WidgetRefresh.updateAll(context) }
    }

    fun openNavigator(scope: NavigatorScope, mode: NavigatorMode = NavigatorMode.BOTH) {
        scopeKind = NavigatorScopeCodec.kindOf(scope)
        scopeValue = NavigatorScopeCodec.valueOf(scope)
        modeName = mode.name
        isHome = false
    }

    if (isHome) {
        HomeScreen(
            scenario = scenarioState.value,
            onScenarioChange = { newScenario ->
                AppPrefs.setSelectedScenario(context, newScenario)
                refreshWidgets()
            },
            pinnedRepos = pinnedState.value,
            onTogglePin = { repoFull ->
                AppPrefs.togglePin(context, repoFull)
                refreshWidgets()
            },
            onOpenNavigator = { openNavigator(NavigatorScope.Account) },
            onOpenRepo = { ref -> openNavigator(NavigatorScope.Repo(ref)) },
        )
    } else {
        val scope = remember(scopeKind, scopeValue) { NavigatorScopeCodec.decode(scopeKind, scopeValue) }
        NavigatorScreen(
            scenario = scenarioState.value,
            initialScope = scope,
            initialMode = navigatorModeFromExtra(modeName),
            onBackToHome = { isHome = true },
        )
    }
}
