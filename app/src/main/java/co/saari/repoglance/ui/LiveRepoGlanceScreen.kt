@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package co.saari.repoglance.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.saari.repoglance.ContentUiState
import co.saari.repoglance.LiveUiState
import co.saari.repoglance.data.CatalogSort
import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.LiveIssue
import co.saari.repoglance.data.LivePullRequest
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.data.LiveRepositoryCatalog
import co.saari.repoglance.data.LiveRepositoryContent
import co.saari.repoglance.data.RateLimitSnapshot
import co.saari.repoglance.data.orderCatalog
import co.saari.repoglance.link.GitHubAppLauncher
import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.render.Ages
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.ui.brand.CheckingMark
import co.saari.repoglance.widget.WidgetRefresh
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@Composable
fun LiveRepoGlanceScreen(
    state: LiveUiState,
    selectedRepository: LiveRepository?,
    contentState: ContentUiState,
    connectionReady: Boolean,
    onConnectGitHub: () -> Unit,
    onCopyCodeAndOpenGitHub: (String, String) -> Unit,
    onCancelGitHubAuthorization: () -> Unit,
    onRetry: () -> Unit,
    onSelectRepository: (LiveRepository) -> Unit,
    onBackToRepositories: () -> Unit,
    onRefreshRepository: () -> Unit,
    onManageGitHubAccess: () -> Unit,
    onSignOut: () -> Unit,
) {
    var selectedOwner by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state) {
        when (state) {
            LiveUiState.SignedOut -> selectedOwner = null
            is LiveUiState.Ready -> {
                selectedOwner = retainedRepositoryOwner(selectedOwner, state.catalog.repositories)
            }
            else -> Unit
        }
    }

    when (state) {
        LiveUiState.Checking -> CheckingScreen()
        LiveUiState.SignedOut -> ConnectGitHubScreen(connectionReady, onConnectGitHub)
        LiveUiState.RequestingDeviceCode -> CenteredStatus("Starting GitHub sign-in…", showProgress = true)
        is LiveUiState.AwaitingDeviceAuthorization -> AwaitingGitHubScreen(
            userCode = state.userCode,
            verificationUri = state.verificationUri,
            expiresAt = state.expiresAt,
            onCopyCodeAndOpenGitHub = onCopyCodeAndOpenGitHub,
            onCancel = onCancelGitHubAuthorization,
        )
        LiveUiState.Connecting, LiveUiState.LoadingCatalogAfterSignIn -> FinishingSignInScreen(
            loadingRepositories = state == LiveUiState.LoadingCatalogAfterSignIn,
        )
        LiveUiState.LoadingRepositories -> CenteredStatus("Loading your repositories…", showProgress = true)
        is LiveUiState.Failure -> FailureScreen(
            message = state.message,
            connectionReady = connectionReady,
            needsNewSignIn = state.needsNewSignIn,
            rateLimit = state.rateLimit,
            onRetry = onRetry,
            onConnectGitHub = onConnectGitHub,
        )
        is LiveUiState.Ready -> {
            if (selectedRepository == null) {
                LiveRepositoryHome(
                    catalog = state.catalog,
                    observedAt = state.observedAt,
                    rateLimit = state.rateLimit,
                    selectedOwner = retainedRepositoryOwner(selectedOwner, state.catalog.repositories),
                    onSelectedOwnerChange = { selectedOwner = it },
                    onSelectRepository = onSelectRepository,
                    onRefresh = onRetry,
                    onManageGitHubAccess = onManageGitHubAccess,
                    onSignOut = {
                        selectedOwner = null
                        onSignOut()
                    },
                )
            } else {
                LiveNavigator(
                    repository = selectedRepository,
                    contentState = contentState,
                    onBack = onBackToRepositories,
                    onRefresh = onRefreshRepository,
                )
            }
        }
    }
}

@Composable
private fun AwaitingGitHubScreen(
    userCode: String,
    verificationUri: String,
    expiresAt: Instant,
    onCopyCodeAndOpenGitHub: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    val now = rememberFreshnessNow()
    val minutesRemaining = ((Duration.between(now, expiresAt).seconds.coerceAtLeast(0L) + 59L) / 60L)
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Enter this code on GitHub", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(userCode, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                if (minutesRemaining > 0L) "Code expires in about $minutesRemaining min" else "Code expired",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "The button copies this code so you can paste it on GitHub.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onCopyCodeAndOpenGitHub(userCode, verificationUri) },
                enabled = minutesRemaining > 0L,
                modifier = Modifier.testTag("repoglance:copy-code"),
            ) {
                Text("Copy code & open GitHub")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "After you authorize, RepoGlance comes back here on its own. If it doesn't, close the GitHub tab.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(RETURN_INSTRUCTION_TEST_TAG),
            )
            TextButton(onClick = onCancel) { Text("Cancel sign-in") }
        }
    }
}

@Composable
private fun ConnectGitHubScreen(connectionReady: Boolean, onConnectGitHub: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RepoGlance", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                "Your repositories, issues, and pull requests at a glance.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Read-only · GitHub controls which repositories are shared",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onConnectGitHub,
                enabled = connectionReady,
                modifier = Modifier.testTag("repoglance:connect-github"),
            ) {
                Text("Connect GitHub")
            }
            if (!connectionReady) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "This build is missing its public GitHub App client ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
internal fun CheckingScreen(modifier: Modifier = Modifier) {
    MarkStatus(
        message = "Checking your GitHub session…",
        markTag = "repoglance:checking-mark",
        messageTag = "repoglance:checking-message",
        modifier = modifier,
    )
}

@Composable
internal fun FinishingSignInScreen(loadingRepositories: Boolean, modifier: Modifier = Modifier) {
    MarkStatus(
        message = if (loadingRepositories) "Loading your repositories…" else "Finishing sign-in…",
        markTag = SIGNIN_MARK_TEST_TAG,
        messageTag = SIGNIN_MESSAGE_TEST_TAG,
        modifier = modifier,
    )
}

@Composable
private fun MarkStatus(message: String, markTag: String, messageTag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CheckingMark(modifier = Modifier.size(96.dp).testTag(markTag))
            Spacer(Modifier.height(20.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(messageTag),
            )
        }
    }
}

@Composable
private fun CenteredStatus(message: String, showProgress: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showProgress) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FailureScreen(
    message: String,
    connectionReady: Boolean,
    needsNewSignIn: Boolean,
    rateLimit: RateLimitSnapshot?,
    onRetry: () -> Unit,
    onConnectGitHub: () -> Unit,
) {
    val now = rememberFreshnessNow()
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RepoGlance could not refresh", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
            rateLimit?.waitLabel(now)?.let { waitLabel ->
                Spacer(Modifier.height(6.dp))
                Text(
                    waitLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            if (!needsNewSignIn) {
                Button(onClick = onRetry) { Text("Try again") }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = onConnectGitHub, enabled = connectionReady) {
                Text(if (needsNewSignIn) "Reconnect GitHub" else "Start a new sign-in")
            }
        }
    }
}

internal const val REPO_SEARCH_TEST_TAG = "repoglance:repo-search"
internal const val SIGNIN_MARK_TEST_TAG = "repoglance:signin-mark"
internal const val SIGNIN_MESSAGE_TEST_TAG = "repoglance:signin-message"
internal const val RETURN_INSTRUCTION_TEST_TAG = "repoglance:signin-return-instruction"

internal const val OWNER_FILTER_TEST_TAG = "repoglance:owner-filter"
internal const val CATALOG_SORT_RECENT_TEST_TAG = "repoglance:catalog-sort-recent"
internal const val CATALOG_SORT_ALPHA_TEST_TAG = "repoglance:catalog-sort-alpha"

@Composable
private fun LiveRepositoryHome(
    catalog: LiveRepositoryCatalog,
    observedAt: Instant,
    rateLimit: RateLimitSnapshot,
    selectedOwner: String?,
    onSelectedOwnerChange: (String?) -> Unit,
    onSelectRepository: (LiveRepository) -> Unit,
    onRefresh: () -> Unit,
    onManageGitHubAccess: () -> Unit,
    onSignOut: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var confirmSignOut by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var ownerMenuExpanded by remember { mutableStateOf(false) }
    val ownerOptions = remember(catalog.repositories) { availableRepositoryOwners(catalog.repositories) }
    val context = LocalContext.current
    val widgetScope = rememberCoroutineScope()
    val sort by AppPrefs.rememberCatalogSort(context)
    val pins by AppPrefs.rememberLivePins(context)
    val matchingRepositories = remember(catalog.repositories, selectedOwner, query, sort, pins) {
        visibleRepositories(catalog.repositories, selectedOwner, query, sort, pins)
    }
    val now = rememberFreshnessNow()

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Disconnect RepoGlance?") },
            text = { Text("This removes the GitHub session from this phone. You can connect again anytime.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text("Disconnect GitHub") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            item(key = "repository-header") {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RepoGlance", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "@${catalog.viewer.login} · ${catalog.repositories.size} repositories · " +
                                        Ages.updatedLabel(observedAt, now),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text("LIVE") },
                                modifier = Modifier.testTag("repoglance:live"),
                            )
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.testTag("repoglance:refresh-repositories"),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh repositories")
                            }
                            Box {
                                IconButton(onClick = { accountMenuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Account and access settings",
                                    )
                                }
                                DropdownMenu(
                                    expanded = accountMenuExpanded,
                                    onDismissRequest = { accountMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Manage GitHub access") },
                                        onClick = {
                                            accountMenuExpanded = false
                                            onManageGitHubAccess()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Disconnect GitHub") },
                                        onClick = {
                                            accountMenuExpanded = false
                                            confirmSignOut = true
                                        },
                                    )
                                }
                            }
                        }
                        val rateText = when (rateLimit.bucket) {
                            RateLimitBucket.OK -> rateLimit.remaining?.let { "GitHub rate limit: $it remaining" }
                            RateLimitBucket.LOW -> "GitHub rate limit is low: ${rateLimit.remaining ?: "?"} remaining"
                            RateLimitBucket.EXHAUSTED -> listOfNotNull(
                                "GitHub rate limit is exhausted",
                                rateLimit.waitLabel(now),
                            ).joinToString(" · ")
                            RateLimitBucket.UNKNOWN -> "GitHub rate limit is unknown"
                        }
                        rateText?.let {
                            Text(
                                it,
                                modifier = Modifier.testTag("repoglance:rate-limit"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rateLimit.bucket == RateLimitBucket.EXHAUSTED) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (catalog.repositories.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = ownerMenuExpanded,
                                onExpandedChange = { ownerMenuExpanded = it },
                            ) {
                                TextField(
                                    value = selectedOwner ?: "All",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Account or organization") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerMenuExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag(OWNER_FILTER_TEST_TAG),
                                )
                                ExposedDropdownMenu(
                                    expanded = ownerMenuExpanded,
                                    onDismissRequest = { ownerMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All") },
                                        onClick = {
                                            onSelectedOwnerChange(null)
                                            ownerMenuExpanded = false
                                        },
                                    )
                                    ownerOptions.forEach { owner ->
                                        DropdownMenuItem(
                                            text = { Text(owner) },
                                            onClick = {
                                                onSelectedOwnerChange(owner)
                                                ownerMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                label = { Text("Find a repository") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag(REPO_SEARCH_TEST_TAG),
                            )
                            Spacer(Modifier.height(8.dp))
                            CatalogSortToggle(sort) { AppPrefs.setCatalogSort(context, it) }
                        }
                    }
                }
            }
            if (catalog.repositories.isEmpty()) {
                item(key = "empty-repositories") {
                    Column(
                        modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("No repositories are shared with RepoGlance")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onManageGitHubAccess) { Text("Choose repositories on GitHub") }
                    }
                }
            } else {
                if (matchingRepositories.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("No repositories match")
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    onSelectedOwnerChange(null)
                                    query = ""
                                },
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
                items(matchingRepositories, key = { it.id }) { repository ->
                    ElevatedCard(
                        onClick = { onSelectRepository(repository) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    repository.ref.full,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                PinToggle(
                                    repoFull = repository.ref.full,
                                    pinned = repository.ref.full in pins,
                                    onToggle = {
                                        AppPrefs.toggleLivePin(context, repository.ref.full)
                                        widgetScope.launch { WidgetRefresh.updateAll(context) }
                                    },
                                )
                            }
                            Text(
                                buildString {
                                    append(if (repository.isPrivate) "Private" else "Public")
                                    if (repository.isArchived) append(" · Archived")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                repository.pushedAt?.let { Ages.updatedLabel(it, now) } ?: "Push time unknown",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun availableRepositoryOwners(repositories: List<LiveRepository>): List<String> =
    repositories
        .map { it.ref.owner }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

internal fun visibleRepositories(
    repositories: List<LiveRepository>,
    selectedOwner: String?,
    query: String,
    sort: CatalogSort = CatalogSort.RECENT,
    pinned: Set<String> = emptySet(),
): List<LiveRepository> = orderCatalog(repositories, sort, pinned)
    .filter { selectedOwner == null || it.ref.owner.equals(selectedOwner, ignoreCase = true) }
    .filter { it.ref.full.contains(query.trim(), ignoreCase = true) }

@Composable
private fun PinToggle(repoFull: String, pinned: Boolean, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.testTag("repoglance:pin-$repoFull"),
    ) {
        Icon(
            if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (pinned) "Unpin $repoFull" else "Pin $repoFull",
            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CatalogSortToggle(sort: CatalogSort, onSortChange: (CatalogSort) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Sort",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = sort == CatalogSort.RECENT,
            onClick = { onSortChange(CatalogSort.RECENT) },
            label = { Text("Recent push") },
            modifier = Modifier.testTag(CATALOG_SORT_RECENT_TEST_TAG),
        )
        Spacer(Modifier.width(6.dp))
        FilterChip(
            selected = sort == CatalogSort.ALPHABETICAL,
            onClick = { onSortChange(CatalogSort.ALPHABETICAL) },
            label = { Text("A to Z") },
            modifier = Modifier.testTag(CATALOG_SORT_ALPHA_TEST_TAG),
        )
    }
}

internal fun retainedRepositoryOwner(
    selectedOwner: String?,
    repositories: List<LiveRepository>,
): String? = selectedOwner?.let { owner ->
    repositories.firstOrNull { it.ref.owner.equals(owner, ignoreCase = true) }?.ref?.owner
}

@Composable
private fun LiveNavigator(
    repository: LiveRepository,
    contentState: ContentUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var modeName by rememberSaveable(repository.id) { mutableStateOf(NavigatorMode.BOTH.name) }
    var query by rememberSaveable(repository.id) { mutableStateOf("") }
    val mode = NavigatorMode.valueOf(modeName)
    val context = LocalContext.current
    val now = rememberFreshnessNow()

    fun open(url: String) {
        if (!GitHubAppLauncher.open(context, url, adjacent = true)) {
            Toast.makeText(context, "GitHub app is not available", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            item(key = "live-controls") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onBack, modifier = Modifier.testTag("repoglance:live-home")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Home")
                        }
                        Text(
                            repository.ref.full,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("repoglance:refresh-repository"),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh repository")
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search loaded rows") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            stickyHeader(key = "live-mode") {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) {
                        NavigatorMode.entries.forEachIndexed { index, candidate ->
                            SegmentedButton(
                                selected = mode == candidate,
                                onClick = { modeName = candidate.name },
                                shape = SegmentedButtonDefaults.itemShape(index, NavigatorMode.entries.size),
                                label = { Text(candidate.name) },
                                modifier = Modifier.testTag("repoglance:live-mode-${candidate.name}"),
                            )
                        }
                    }
                }
            }

            when (contentState) {
                ContentUiState.Idle, ContentUiState.Loading -> item {
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Loading live GitHub data…")
                        }
                    }
                }
                is ContentUiState.Ready -> {
                    val content = contentState.content
                    if (mode == NavigatorMode.ISSUES || mode == NavigatorMode.BOTH) {
                        liveIssueSection(content, query, now, ::open)
                    }
                    if (mode == NavigatorMode.PRS || mode == NavigatorMode.BOTH) {
                        livePullRequestSection(content, query, now, ::open)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.liveIssueSection(
    content: LiveRepositoryContent,
    query: String,
    now: Instant,
    onOpen: (String) -> Unit,
) {
    item { Text("Issues", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp)) }
    when (val result = content.issues) {
        is GitHubApiResult.Failure -> item { FailureRow(result.message, result.rateLimit, now) }
        is GitHubApiResult.Success -> {
            item { FreshnessRow(result.observedAt, result.rateLimit, now) }
            if (result.value.hasMorePages) item { PartialPageRow(result.value.rows.size) }
            val rows = result.value.rows.filter { it.title.contains(query.trim(), ignoreCase = true) }
            if (rows.isEmpty()) {
                item {
                    EmptyLiveRows(
                        if (result.value.hasMorePages) "No matches in the loaded issues" else "No open issues match",
                    )
                }
            }
            items(rows, key = { "live-issue-${it.number}" }) { issue ->
                LiveIssueRow(issue, now, onOpen)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.livePullRequestSection(
    content: LiveRepositoryContent,
    query: String,
    now: Instant,
    onOpen: (String) -> Unit,
) {
    item { Text("PRs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp)) }
    when (val result = content.pullRequests) {
        is GitHubApiResult.Failure -> item { FailureRow(result.message, result.rateLimit, now) }
        is GitHubApiResult.Success -> {
            item { FreshnessRow(result.observedAt, result.rateLimit, now) }
            if (result.value.hasMorePages) item { PartialPageRow(result.value.rows.size) }
            val rows = result.value.rows.filter { it.title.contains(query.trim(), ignoreCase = true) }
            if (rows.isEmpty()) {
                item {
                    EmptyLiveRows(
                        if (result.value.hasMorePages) {
                            "No matches in the loaded pull requests"
                        } else {
                            "No open pull requests match"
                        },
                    )
                }
            }
            items(rows, key = { "live-pr-${it.number}" }) { pullRequest ->
                LivePullRequestRow(pullRequest, now, onOpen)
            }
        }
    }
}

@Composable
private fun LiveIssueRow(issue: LiveIssue, now: Instant, onOpen: (String) -> Unit) {
    Surface(onClick = { onOpen(issue.htmlUrl) }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                "#${issue.number}  ${Sanitize.displayText(issue.title)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            val comments = issue.commentCount?.let { "$it comments" } ?: "comments —"
            Text(
                "by ${Sanitize.displayText(issue.author)} · $comments · " + Ages.updatedLabel(issue.updatedAt, now),
                style = MaterialTheme.typography.bodySmall,
            )
            if (issue.labels.isNotEmpty()) {
                Text(
                    issue.labels.joinToString(", ") { Sanitize.displayText(it) },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun LivePullRequestRow(pullRequest: LivePullRequest, now: Instant, onOpen: (String) -> Unit) {
    Surface(onClick = { onOpen(pullRequest.htmlUrl) }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${pullRequest.number}  ${Sanitize.displayText(pullRequest.title)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (pullRequest.isDraft) AssistChip(onClick = {}, label = { Text("Draft") })
                if (pullRequest.reviewRequestedFromViewer) {
                    AssistChip(onClick = {}, label = { Text("Review requested") })
                }
            }
            Text(
                "by ${Sanitize.displayText(pullRequest.author)} · ${Ages.updatedLabel(pullRequest.updatedAt, now)}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (pullRequest.labels.isNotEmpty()) {
                Text(
                    pullRequest.labels.joinToString(", ") { Sanitize.displayText(it) },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun FreshnessRow(observedAt: Instant, rateLimit: RateLimitSnapshot, now: Instant) {
    val rateText = when (rateLimit.bucket) {
        RateLimitBucket.OK -> rateLimit.remaining?.let { "Rate limit $it remaining" }
        RateLimitBucket.LOW -> "Rate limit low · ${rateLimit.remaining ?: "?"} remaining"
        RateLimitBucket.EXHAUSTED -> "Rate limit exhausted"
        RateLimitBucket.UNKNOWN -> "Rate limit unknown"
    }
    Text(
        listOfNotNull(Ages.updatedLabel(observedAt, now), rateText).joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = if (rateLimit.bucket == RateLimitBucket.EXHAUSTED) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun FailureRow(message: String, rateLimit: RateLimitSnapshot, now: Instant) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (rateLimit.bucket != RateLimitBucket.UNKNOWN) {
            Text("Rate limit: ${rateLimit.bucket.name}", style = MaterialTheme.typography.bodySmall)
        }
        rateLimit.waitLabel(now)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("No current value", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyLiveRows(message: String) {
    Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PartialPageRow(loadedCount: Int) {
    Text(
        "Showing the $loadedCount most recently updated · more available on GitHub",
        modifier = Modifier.padding(bottom = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun RateLimitSnapshot.waitLabel(now: Instant): String? {
    if (bucket != RateLimitBucket.EXHAUSTED) return null
    val reset = resetsAt ?: return "GitHub asked RepoGlance to pause before retrying"
    val seconds = Duration.between(now, reset).seconds.coerceAtLeast(0)
    return when {
        seconds == 0L -> "GitHub's retry window has arrived"
        seconds < 90L -> "Retry in about a minute"
        seconds < 90L * 60L -> "Retry in about ${(seconds + 59L) / 60L} minutes"
        else -> "Retry in about ${(seconds + 3_599L) / 3_600L} hours"
    }
}
