package co.saari.repoglance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.ui.HomeScreen
import co.saari.repoglance.ui.NavigatorScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.EXTRA_REPO_FULL
import co.saari.repoglance.widget.EXTRA_NAVIGATOR_MODE
import co.saari.repoglance.widget.WidgetRefresh
import co.saari.repoglance.widget.navigatorModeFromExtra
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A widget row tap launches MainActivity with this extra set (see
        // widget/WidgetActions.kt) instead of a raw ACTION_VIEW Intent from
        // Glance — pre-scoping the navigator to that repo. Only read on a
        // fresh launch (no onNewIntent handling in v0.1); a widget tap while
        // the app is already running opens another activity instance, which
        // is standard-launch-mode default behavior, not specially handled.
        val widgetRepoFull = intent?.getStringExtra(EXTRA_REPO_FULL)
        val initialScope = widgetRepoFull
            ?.let { NavigatorScopeCodec.decode("REPO", it) }
            ?.takeIf { it is NavigatorScope.Repo }
        val initialMode = navigatorModeFromExtra(intent?.getStringExtra(EXTRA_NAVIGATOR_MODE))

        setContent {
            RepoGlanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot(initialNavigatorScope = initialScope, initialNavigatorMode = initialMode)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(initialNavigatorScope: NavigatorScope?, initialNavigatorMode: NavigatorMode) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isHome by rememberSaveable { mutableStateOf(initialNavigatorScope == null) }
    var scopeKind by rememberSaveable {
        mutableStateOf(initialNavigatorScope?.let(NavigatorScopeCodec::kindOf) ?: "ACCOUNT")
    }
    var scopeValue by rememberSaveable {
        mutableStateOf(initialNavigatorScope?.let(NavigatorScopeCodec::valueOf) ?: "")
    }
    var modeName by rememberSaveable {
        mutableStateOf(if (initialNavigatorScope == null) NavigatorMode.BOTH.name else initialNavigatorMode.name)
    }

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
