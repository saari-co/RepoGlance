@file:OptIn(ExperimentalComposeUiApi::class)

package co.saari.repoglance.devlaunch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import co.saari.repoglance.ui.FinishingSignInScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme

// Debug-only: holds the production post-token sign-in screens open so
// verify-repoglance can dump and capture them. In the real app
// LiveUiState.Connecting lasts as long as the Keystore commit and
// LoadingCatalogAfterSignIn as long as the first catalog request, both too
// short to catch, and reaching them for real needs a human-gated sign-in.
// Opens on "Finishing sign-in…"; a tap anywhere advances to "Loading your
// repositories…" through the same composable call, which is how the real
// transition keeps the mark's rings running instead of restarting them.
// Never ships: debug source set only.
//
//   bin/verify-repoglance launch MIXED signin-finishing
class SignInFinishingPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepoGlanceTheme {
                var loadingRepositories by rememberSaveable { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true }
                        .clickable { loadingRepositories = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FinishingSignInScreen(loadingRepositories = loadingRepositories)
                }
            }
        }
    }
}
