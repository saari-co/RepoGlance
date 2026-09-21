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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.ui.HomeScreen
import co.saari.repoglance.ui.LiveRepoGlanceScreen
import co.saari.repoglance.ui.NavigatorScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.EXTRA_LIVE_CATALOG
import co.saari.repoglance.widget.EXTRA_LIVE_REPO_FULL
import co.saari.repoglance.widget.EXTRA_NAVIGATOR_MODE
import co.saari.repoglance.widget.EXTRA_REPO_FULL
import co.saari.repoglance.widget.WidgetRefresh
import co.saari.repoglance.widget.navigatorModeFromExtra
import kotlinx.coroutines.launch

internal const val REPOGLANCE_INSTALLATION_SETTINGS_URL =
    "https://github.com/apps/repoglance-by-saari/installations/new"
private const val STATE_REFRESH_CATALOG_AFTER_GITHUB_ACCESS =
    "refreshCatalogAfterGitHubAccess"
private const val STATE_RETURN_AFTER_GITHUB_VERIFICATION =
    "returnAfterGitHubVerification"

class MainActivity : ComponentActivity() {
    private val fixtureNavigatorScope = mutableStateOf<NavigatorScope?>(null)
    private val fixtureNavigatorMode = mutableStateOf(NavigatorMode.BOTH)
    private val fixtureNavigatorRouteToken = mutableIntStateOf(0)
    private lateinit var liveModel: RepoGlanceViewModel
    private var refreshCatalogAfterGitHubAccess = false
    private var returnAfterGitHubVerification = false

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        liveModel = ViewModelProvider(this)[RepoGlanceViewModel::class.java]
        refreshCatalogAfterGitHubAccess =
            savedInstanceState?.getBoolean(STATE_REFRESH_CATALOG_AFTER_GITHUB_ACCESS) == true
        returnAfterGitHubVerification =
            savedInstanceState?.getBoolean(STATE_RETURN_AFTER_GITHUB_VERIFICATION) == true
        lifecycleScope.launch {
            liveModel.deviceAuthorizationCommitted.collect { returnFromGitHubVerification() }
        }

        fixtureNavigatorScope.value = resolveFixtureScopeFromIntent(intent)
        fixtureNavigatorMode.value = navigatorModeFromExtra(intent?.getStringExtra(EXTRA_NAVIGATOR_MODE))
        handleLiveIntent(intent)

        setContent {
            RepoGlanceTheme {
                Surface(

                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val currentFixtureScope = fixtureNavigatorScope.value
                    if (currentFixtureScope != null) {
                        key(fixtureNavigatorRouteToken.intValue) {
                            FixtureRoot(currentFixtureScope, fixtureNavigatorMode.value)
                        }
                    } else {
                        LiveRepoGlanceScreen(
                            state = liveModel.liveState.value,
                            selectedRepository = liveModel.selectedRepository.value,
                            contentState = liveModel.repositoryContent.value,
                            connectionReady = liveModel.deviceFlowReady,
                            onConnectGitHub = ::connectGitHub,
                            onCopyCodeAndOpenGitHub = ::copyCodeAndOpenGitHub,
                            onCancelGitHubAuthorization = liveModel::cancelGitHubAuthorization,
                            onRetry = liveModel::refreshCatalog,
                            onSelectRepository = liveModel::selectRepository,
                            onBackToRepositories = liveModel::backToRepositories,
                            onRefreshRepository = liveModel::refreshSelectedRepository,
                            onManageGitHubAccess = ::openInstallationSettings,
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
        handleLiveIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            STATE_REFRESH_CATALOG_AFTER_GITHUB_ACCESS,
            refreshCatalogAfterGitHubAccess,
        )
        outState.putBoolean(
            STATE_RETURN_AFTER_GITHUB_VERIFICATION,
            returnAfterGitHubVerification,
        )
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::liveModel.isInitialized) {
            liveModel.resumeGitHubAuthorization()
            if (refreshCatalogAfterGitHubAccess) {
                refreshCatalogAfterGitHubAccess = false
                liveModel.refreshCatalog()
            }
        }
    }

    private fun openGitHubVerification(verificationUri: String) {
        returnAfterGitHubVerification = true
        CustomTabsIntent.Builder().setShowTitle(true).build()
            .launchUrl(this, Uri.parse(verificationUri))
    }

    private fun connectGitHub() {
        returnAfterGitHubVerification = false
        liveModel.beginGitHubAuthorization()
    }

    private fun returnFromGitHubVerification() {
        if (!returnAfterGitHubVerification) return
        returnAfterGitHubVerification = false
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
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
        refreshCatalogAfterGitHubAccess = true
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(
            this,
            Uri.parse(REPOGLANCE_INSTALLATION_SETTINGS_URL),
        )
    }

    private fun resolveFixtureScopeFromIntent(intent: Intent?): NavigatorScope.Repo? {
        return intent
            ?.getStringExtra(EXTRA_REPO_FULL)
            ?.let { NavigatorScopeCodec.decode("REPO", it) }
            ?.takeIf { it is NavigatorScope.Repo } as? NavigatorScope.Repo
    }

    private fun handleLiveIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_LIVE_CATALOG, false) == true) {
            fixtureNavigatorScope.value = null
            liveModel.backToRepositories()
            return
        }
        val full = intent?.getStringExtra(EXTRA_LIVE_REPO_FULL) ?: return
        fixtureNavigatorScope.value = null
        liveModel.openRepositoryByName(full)
    }

    private fun handleFixtureIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_MAIN) {
            fixtureNavigatorScope.value = null
            fixtureNavigatorRouteToken.intValue += 1
            return
        }
        val nextScope = resolveFixtureScopeFromIntent(intent) ?: return
        fixtureNavigatorScope.value = nextScope
        fixtureNavigatorMode.value = navigatorModeFromExtra(intent.getStringExtra(EXTRA_NAVIGATOR_MODE))
        fixtureNavigatorRouteToken.intValue += 1
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
