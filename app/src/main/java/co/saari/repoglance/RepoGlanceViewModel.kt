package co.saari.repoglance

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.saari.repoglance.auth.AuthorizationCommitGate
import co.saari.repoglance.auth.DeviceFlowPollWakeSignal
import co.saari.repoglance.auth.GitHubAuthConfig
import co.saari.repoglance.auth.GitHubAuthException
import co.saari.repoglance.auth.GitHubDeviceFlowClient
import co.saari.repoglance.auth.GitHubDeviceFlowPoller
import co.saari.repoglance.auth.GitHubSession
import co.saari.repoglance.auth.SecureTokenStore
import co.saari.repoglance.data.GitHubApiClient
import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.data.LiveRepositoryCatalog
import co.saari.repoglance.data.LiveRepositoryContent
import co.saari.repoglance.data.LiveSnapshotFactory
import co.saari.repoglance.data.RateLimitSnapshot
import co.saari.repoglance.data.sessionInvalidationFailure
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.widget.WidgetRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class RepoGlanceViewModel(application: Application) : AndroidViewModel(application) {
    val liveState = mutableStateOf<LiveUiState>(LiveUiState.Checking)
    val selectedRepository = mutableStateOf<LiveRepository?>(null)
    val repositoryContent = mutableStateOf<ContentUiState>(ContentUiState.Idle)

    private val authConfig = GitHubAuthConfig(
        clientId = BuildConfig.GITHUB_APP_CLIENT_ID,
    )
    private val deviceFlowClient = GitHubDeviceFlowClient(authConfig)
    private val session = GitHubSession(
        tokenStore = SecureTokenStore(application),
        deviceFlowClient = deviceFlowClient,
    )
    private val deviceFlowPollWakeSignal = DeviceFlowPollWakeSignal()
    private val deviceFlowPoller = GitHubDeviceFlowPoller(
        gateway = deviceFlowClient,
        wait = deviceFlowPollWakeSignal::await,
    )
    private val authorizationCommitGate = AuthorizationCommitGate()
    private val apiClient = GitHubApiClient(session)
    private val sessionDispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val catalogGeneration = AtomicInteger(0)
    private val repositoryContentGeneration = AtomicInteger(0)
    private var bootstrapJob: Job? = null
    private var authorizationJob: Job? = null
    private var catalogLoadJob: Job? = null
    private var repositoryContentLoadJob: Job? = null

    val deviceFlowReady: Boolean
        get() = authConfig.isReady

    init {
        bootstrapSessionState()
    }

    fun beginGitHubAuthorization() {
        if (!authConfig.isReady) {
            liveState.value = LiveUiState.Failure(
                message = "This build is missing its public GitHub App client ID.",
                needsNewSignIn = true,
            )
            return
        }
        authorizationJob?.cancel()
        val requestGeneration = authorizationCommitGate.nextGeneration()
        liveState.value = LiveUiState.RequestingDeviceCode
        authorizationJob = viewModelScope.launch {
            try {
                val authorization = withContext(Dispatchers.IO) { deviceFlowClient.begin() }
                currentCoroutineContext().ensureActive()
                liveState.value = LiveUiState.AwaitingDeviceAuthorization(
                    userCode = authorization.userCode,
                    verificationUri = authorization.verificationUri,
                    expiresAt = authorization.expiresAt,
                )
                val token = withContext(Dispatchers.IO) {
                    deviceFlowPoller.awaitToken(authorization)
                }
                currentCoroutineContext().ensureActive()
                val committed = withContext(sessionDispatcher) {
                    authorizationCommitGate.commit(requestGeneration) { session.acceptDeviceToken(token) }
                }
                if (!committed) return@launch
                liveState.value = LiveUiState.Connecting
                refreshCatalog()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: GitHubAuthException) {
                if (!authorizationCommitGate.isCurrent(requestGeneration)) return@launch
                val needsNewSignIn = failure.needsNewSignIn || !hasSavedSession()
                if (!authorizationCommitGate.isCurrent(requestGeneration)) return@launch
                liveState.value = LiveUiState.Failure(
                    message = failure.message ?: "GitHub sign-in failed",
                    needsNewSignIn = needsNewSignIn,
                )
            } catch (_: Exception) {
                if (!authorizationCommitGate.isCurrent(requestGeneration)) return@launch
                val needsNewSignIn = !hasSavedSession()
                if (!authorizationCommitGate.isCurrent(requestGeneration)) return@launch
                liveState.value = LiveUiState.Failure(
                    message = "Could not start GitHub sign-in right now",
                    needsNewSignIn = needsNewSignIn,
                )
            }
        }
    }

    fun cancelGitHubAuthorization() {
        authorizationJob?.cancel()
        authorizationJob = null
        authorizationCommitGate.invalidate(::clearSavedSession)
        liveState.value = LiveUiState.SignedOut
    }

    fun resumeGitHubAuthorization() {
        if (
            liveState.value is LiveUiState.AwaitingDeviceAuthorization &&
            authorizationJob?.isActive == true
        ) {
            deviceFlowPollWakeSignal.wake()
        }
    }

    fun refreshCatalog() {
        liveState.value = LiveUiState.LoadingRepositories
        val requestGeneration = catalogGeneration.incrementAndGet()
        catalogLoadJob?.cancel()
        catalogLoadJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { apiClient.loadCatalog() }
            if (requestGeneration != catalogGeneration.get()) return@launch
            when (result) {
                is GitHubApiResult.Success -> liveState.value = LiveUiState.Ready(
                    catalog = result.value,
                    observedAt = result.observedAt,
                    rateLimit = result.rateLimit,
                )
                is GitHubApiResult.Failure -> {
                    if (result.needsNewSignIn) {
                        withContext(sessionDispatcher) { session.signOut() }
                        backToRepositories()
                    }
                    liveState.value = LiveUiState.Failure(
                        message = result.message,
                        needsNewSignIn = result.needsNewSignIn,
                        rateLimit = result.rateLimit,
                    )
                }
            }
        }
    }

    fun selectRepository(repository: LiveRepository) {
        selectedRepository.value = repository
        refreshSelectedRepository()
    }

    fun refreshSelectedRepository() {
        val repository = selectedRepository.value ?: return
        val catalog = (liveState.value as? LiveUiState.Ready)?.catalog ?: return
        repositoryContent.value = ContentUiState.Loading
        val requestGeneration = repositoryContentGeneration.incrementAndGet()
        repositoryContentLoadJob?.cancel()
        repositoryContentLoadJob = viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                apiClient.loadRepositoryContent(repository, catalog.viewer.login)
            }
            if (
                requestGeneration != repositoryContentGeneration.get() ||
                selectedRepository.value?.id != repository.id
            ) {
                return@launch
            }
            val invalidSession = content.sessionInvalidationFailure()
            if (invalidSession != null) {
                withContext(sessionDispatcher) { session.signOut() }
                selectedRepository.value = null
                repositoryContent.value = ContentUiState.Idle
                liveState.value = LiveUiState.Failure(
                    message = invalidSession.message,
                    needsNewSignIn = true,
                    rateLimit = invalidSession.rateLimit,
                )
            } else {
                repositoryContent.value = ContentUiState.Ready(content)
                persistLiveSnapshot(repository, content)
            }
        }
    }

    private suspend fun persistLiveSnapshot(
        repository: LiveRepository,
        content: LiveRepositoryContent,
    ) {
        val context = getApplication<Application>()
        val issues = (content.issues as? GitHubApiResult.Success)?.value
        val prSuccess = content.pullRequests as? GitHubApiResult.Success

        withContext(Dispatchers.IO) {
            val metadata = if (LiveSnapshotFactory.needsRepositoryMetadata(issues, prSuccess?.value)) {
                apiClient.loadRepositoryMetadata(repository) as? GitHubApiResult.Success
            } else {
                null
            }
            LiveSnapshotFactory.build(
                repository = repository,
                metadata = metadata?.value,
                issues = issues,
                pullRequests = prSuccess?.value,
                previous = LiveSnapshotStore.load(context, repository.ref),

                observedAt = metadata?.observedAt ?: prSuccess?.observedAt ?: Instant.now(),
                rateLimit = metadata?.rateLimit?.bucket
                    ?: prSuccess?.rateLimit?.bucket
                    ?: RateLimitBucket.UNKNOWN,
            ).also { LiveSnapshotStore.save(context, it) }
        }
        WidgetRefresh.updateAll(context)
    }

    fun backToRepositories() {
        selectedRepository.value = null
        repositoryContent.value = ContentUiState.Idle
        repositoryContentLoadJob?.cancel()
        repositoryContentGeneration.incrementAndGet()
    }

    fun signOut() {
        bootstrapJob?.cancel()
        authorizationJob?.cancel()
        authorizationJob = null
        authorizationCommitGate.invalidate(::clearSavedSession)
        catalogLoadJob?.cancel()
        catalogGeneration.incrementAndGet()
        backToRepositories()
        liveState.value = LiveUiState.SignedOut
    }

    private fun bootstrapSessionState() {
        bootstrapJob = viewModelScope.launch {
            if (hasSavedSession()) {
                refreshCatalog()
            } else {
                liveState.value = LiveUiState.SignedOut
            }
        }
    }

    private suspend fun hasSavedSession(): Boolean = withContext(sessionDispatcher) { session.hasSavedSession() }

    private fun clearSavedSession() {
        viewModelScope.launch(sessionDispatcher + NonCancellable) { session.signOut() }
    }

    override fun onCleared() {
        super.onCleared()
        sessionDispatcher.close()
    }
}

sealed interface LiveUiState {
    data object Checking : LiveUiState
    data object SignedOut : LiveUiState
    data object RequestingDeviceCode : LiveUiState
    data class AwaitingDeviceAuthorization(
        val userCode: String,
        val verificationUri: String,
        val expiresAt: Instant,
    ) : LiveUiState
    data object Connecting : LiveUiState
    data object LoadingRepositories : LiveUiState
    data class Ready(
        val catalog: LiveRepositoryCatalog,
        val observedAt: java.time.Instant,
        val rateLimit: RateLimitSnapshot,
    ) : LiveUiState
    data class Failure(
        val message: String,
        val needsNewSignIn: Boolean = false,
        val rateLimit: RateLimitSnapshot? = null,
    ) : LiveUiState
}

sealed interface ContentUiState {
    data object Idle : ContentUiState
    data object Loading : ContentUiState
    data class Ready(val content: LiveRepositoryContent) : ContentUiState
}
