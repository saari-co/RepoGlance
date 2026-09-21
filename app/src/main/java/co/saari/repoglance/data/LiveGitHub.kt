package co.saari.repoglance.data

import android.content.Context
import co.saari.repoglance.BuildConfig
import co.saari.repoglance.auth.GitHubAuthConfig
import co.saari.repoglance.auth.GitHubDeviceFlowClient
import co.saari.repoglance.auth.GitHubSession
import co.saari.repoglance.auth.SecureTokenStore
import co.saari.repoglance.hooks.TransportFault

object LiveGitHub {

    class Services(
        val authConfig: GitHubAuthConfig,
        val deviceFlowClient: GitHubDeviceFlowClient,
        val session: GitHubSession,
        val apiClient: GitHubApiClient,
    )

    @Volatile
    private var services: Services? = null

    fun services(context: Context): Services = services ?: synchronized(this) {
        services ?: build(context.applicationContext).also { services = it }
    }

    private fun build(context: Context): Services {
        val authConfig = GitHubAuthConfig(clientId = BuildConfig.GITHUB_APP_CLIENT_ID)
        val deviceFlowClient = GitHubDeviceFlowClient(authConfig)
        val session = GitHubSession(
            tokenStore = SecureTokenStore(context),
            deviceFlowClient = deviceFlowClient,
        )
        val apiClient = GitHubApiClient(session, TransportFault.wrap(context, UrlConnectionTransport()))
        return Services(authConfig, deviceFlowClient, session, apiClient)
    }
}
