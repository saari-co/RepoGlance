package co.saari.repoglance

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.ui.HomeScreen
import co.saari.repoglance.ui.LiveRepoGlanceScreen
import co.saari.repoglance.ui.NavigatorScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.EXTRA_NAVIGATOR_MODE
import co.saari.repoglance.widget.EXTRA_REPO_FULL
import co.saari.repoglance.widget.WidgetRefresh
import co.saari.repoglance.widget.navigatorModeFromExtra
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val fixtureNavigatorScope = mutableStateOf<NavigatorScope?>(null)
    private val fixtureNavigatorMode = mutableStateOf(NavigatorMode.BOTH)
    private val fixtureNavigatorRouteToken = mutableStateOf(0)
    private lateinit var liveModel: RepoGlanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        liveModel = ViewModelProvider(this)[RepoGlanceViewModel::class.java]

        fixtureNavigatorScope.value = resolveFixtureScopeFromIntent(intent)
        fixtureNavigatorMode.value = navigatorModeFromExtra(intent?.getStringExtra(EXTRA_NAVIGATOR_MODE))

        setContent {
            RepoGlanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val currentFixtureScope = fixtureNavigatorScope.value
                    if (currentFixtureScope != null) {
                        key(fixtureNavigatorRouteToken.value) {
                            FixtureRoot(currentFixtureScope, fixtureNavigatorMode.value)
                        }
                    } else {
                        LiveRepoGlanceScreen(
                            state = liveModel.liveState.value,
                            selectedRepository = liveModel.selectedRepository.value,
                            contentState = liveModel.repositoryContent.value,
                            connectionReady = liveModel.deviceFlowReady,
                            onConnectGitHub = liveModel::beginGitHubAuthorization,
                            onCopyCodeAndOpenGitHub = ::copyCodeAndOpenGitHub,
                            onCancelGitHubAuthorization = liveModel::cancelGitHubAuthorization,
                            onRetry = liveModel::refreshCatalog,
                            onSelectRepository = liveModel::selectRepository,
                            onBackToRepositories = liveModel::backToRepositories,
                            onRefreshRepository = liveModel::refreshSelectedRepository,
                            onChooseRepositories = ::openInstallationSettings,
                            onSignOut = liveModel::signOut,
                        )
                    }
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFixtureIntent(intent)
    }

    private fun openGitHubVerification(verificationUri: String) {
        CustomTabsIntent.Builder().setShowTitle(true).build()
            .launchUrl(this, Uri.parse(verificationUri))
    }

    private fun copyCodeAndOpenGitHub(userCode: String, verificationUri: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("GitHub device code", userCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show()
        openGitHubVerification(verificationUri)
    }

    private fun openInstallationSettings() {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(
            this,
            Uri.parse("https://github.com/apps/repoglance-by-saari/installations/new"),
        )
    }

    private fun resolveFixtureScopeFromIntent(intent: Intent?): NavigatorScope.Repo? {
        return intent
            ?.getStringExtra(EXTRA_REPO_FULL)
            ?.let { NavigatorScopeCodec.decode("REPO", it) }
            ?.takeIf { it is NavigatorScope.Repo } as? NavigatorScope.Repo
    }

    private fun handleFixtureIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_MAIN) {
            fixtureNavigatorScope.value = null
            fixtureNavigatorRouteToken.value += 1
            return
        }
        val nextScope = resolveFixtureScopeFromIntent(intent) ?: return
        fixtureNavigatorScope.value = nextScope
        fixtureNavigatorMode.value = navigatorModeFromExtra(intent.getStringExtra(EXTRA_NAVIGATOR_MODE))
        fixtureNavigatorRouteToken.value += 1
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
