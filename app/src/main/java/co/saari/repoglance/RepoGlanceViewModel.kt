package co.saari.repoglance

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.saari.repoglance.auth.GitHubAuthConfig
import co.saari.repoglance.auth.GitHubAuthException
import co.saari.repoglance.auth.GitHubAuthorization
import co.saari.repoglance.auth.GitHubOAuthClient
import co.saari.repoglance.auth.GitHubSession
import co.saari.repoglance.auth.PendingAuthorizationStore
import co.saari.repoglance.auth.SecureTokenStore
import co.saari.repoglance.data.GitHubApiClient
import co.saari.repoglance.data.GitHubApiResult
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.data.LiveRepositoryCatalog
import co.saari.repoglance.data.LiveRepositoryContent
import co.saari.repoglance.data.RateLimitSnapshot
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepoGlanceViewModel(application: Application) : AndroidViewModel(application) {
    val liveState = mutableStateOf<LiveUiState>(LiveUiState.Checking)
    val selectedRepository = mutableStateOf<LiveRepository?>(null)
    val repositoryContent = mutableStateOf<ContentUiState>(ContentUiState.Idle)

    private val authConfig = GitHubAuthConfig(
        clientId = BuildConfig.GITHUB_APP_CLIENT_ID,
        clientSecret = BuildConfig.GITHUB_APP_CLIENT_SECRET,
        callbackUrl = BuildConfig.GITHUB_CALLBACK_URL,
    )
    private val pendingAuthorizationStore = PendingAuthorizationStore(application)
    private val session = GitHubSession(
        tokenStore = SecureTokenStore(application),
        oauthClient = GitHubOAuthClient(authConfig),
    )
    private val apiClient = GitHubApiClient(session)
    private val authBusy = AtomicBoolean(false)
    private val catalogGeneration = AtomicInteger(0)
    private val repositoryContentGeneration = AtomicInteger(0)
    private var catalogLoadJob: Job? = null
    private var repositoryContentLoadJob: Job? = null

    val credentialReady: Boolean
        get() = authConfig.isReady

    init {
        bootstrapSessionState()
    }

    fun beginGitHubAuthorization(): String? {
        if (!authConfig.isReady) {
            liveState.value = LiveUiState.Failure(
                "This local build still needs its GitHub App credential before sign-in can start.",
            )
            return null
        }
        val pending = GitHubAuthorization.create(authConfig)
        pendingAuthorizationStore.save(pending)
        liveState.value = LiveUiState.AwaitingBrowser
        return pending.authorizationUrl
    }

    fun handleAuthorizationCallback(uri: Uri): Boolean {
        if (!uri.isRepoGlanceCallback()) return false

        val states = uri.getQueryParameters("state")
        val codes = uri.getQueryParameters("code")
        val errors = uri.getQueryParameters("error")
        if (states.size != 1 || states.single().isBlank()) {
            return true
        }
        val verifier = pendingAuthorizationStore.verifierFor(states.single())
        if (verifier == null) {
            return true
        }
        val hasOneCode = codes.size == 1 && codes.single().isNotBlank()
        val hasOneError = errors.size == 1 && errors.single().isNotBlank()
        if (hasOneCode == hasOneError) {
            pendingAuthorizationStore.clear()
            liveState.value = LiveUiState.Failure(
                message = "The GitHub return link was incomplete. Please try again.",
                needsNewSignIn = true,
            )
            return true
        }
        if (hasOneError) {
            pendingAuthorizationStore.clear()
            liveState.value = LiveUiState.Failure(
                message = when (errors.single()) {
                    "access_denied" -> "GitHub sign-in was cancelled"
                    "redirect_uri_mismatch" -> "GitHub rejected the app return address"
                    else -> "GitHub did not complete sign-in"
                },
                needsNewSignIn = true,
            )
            return true
        }
        if (!authBusy.compareAndSet(false, true)) return true

        liveState.value = LiveUiState.Connecting
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    session.acceptAuthorizationCode(codes.single(), verifier)
                }
                pendingAuthorizationStore.clear()
                refreshCatalog()
            } catch (failure: GitHubAuthException) {
                pendingAuthorizationStore.clear()
                liveState.value = LiveUiState.Failure(
                    message = failure.message ?: "GitHub sign-in failed",
                    needsNewSignIn = failure.needsNewSignIn || !session.hasSavedSession(),
                )
            } catch (_: Exception) {
                pendingAuthorizationStore.clear()
                liveState.value = LiveUiState.Failure(
                    message = "Could not finish GitHub sign-in right now",
                    needsNewSignIn = true,
                )
            } finally {
                authBusy.set(false)
            }
        }
        return true
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
                    if (result.needsNewSignIn) session.signOut()
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
            repositoryContent.value = ContentUiState.Ready(content)
        }
    }

    fun backToRepositories() {
        selectedRepository.value = null
        repositoryContent.value = ContentUiState.Idle
        repositoryContentLoadJob?.cancel()
        repositoryContentGeneration.incrementAndGet()
    }

    fun signOut() {
        session.signOut()
        pendingAuthorizationStore.clear()
        catalogLoadJob?.cancel()
        catalogGeneration.incrementAndGet()
        backToRepositories()
        liveState.value = LiveUiState.SignedOut
    }

    private fun bootstrapSessionState() {
        if (session.hasSavedSession()) {
            refreshCatalog()
        } else {
            liveState.value = LiveUiState.SignedOut
        }
    }

    private fun Uri.isRepoGlanceCallback(): Boolean {
        val verifiedHttps = scheme == "https" && host == "repoglance.ztoned.com" && path == "/oauth/callback"
        val localFallback = scheme == "repoglance" && host == "oauth" && path == "/callback"
        return verifiedHttps || localFallback
    }
}

sealed interface LiveUiState {
    data object Checking : LiveUiState
    data object SignedOut : LiveUiState
    data object AwaitingBrowser : LiveUiState
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
