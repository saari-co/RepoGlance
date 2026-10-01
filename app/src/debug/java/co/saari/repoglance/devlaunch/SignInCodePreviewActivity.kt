@file:OptIn(ExperimentalComposeUiApi::class)

package co.saari.repoglance.devlaunch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import co.saari.repoglance.ui.AwaitingGitHubScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import java.time.Duration
import java.time.Instant

// Debug-only: holds the production device-code screen
// (LiveUiState.AwaitingDeviceAuthorization) open with a made-up code, for
// repoglance.com's imagery (showcase-048). The real screen shows a code
// GitHub issued for the maintainer's sign-in, which verify-repoglance's
// redaction guard refuses to dump or capture; this preview never asks GitHub
// for anything: FIXTURE_USER_CODE is a constant, the buttons do nothing, and
// the expiry is fifteen minutes from launch so the screen reads as fresh.
// Never ships: debug source set only.
//
//   bin/verify-repoglance launch MIXED signin-code
class SignInCodePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val expiresAt = Instant.now().plus(Duration.ofMinutes(FIXTURE_MINUTES))
        setContent {
            RepoGlanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AwaitingGitHubScreen(
                        userCode = FIXTURE_USER_CODE,
                        verificationUri = FIXTURE_VERIFICATION_URI,
                        expiresAt = expiresAt,
                        onCopyCodeAndOpenGitHub = { _, _ -> },
                        onCancel = {},
                    )
                }
            }
        }
    }

    companion object {
        const val FIXTURE_USER_CODE = "HK7N-4R2D"
        const val FIXTURE_VERIFICATION_URI = "https://github.com/login/device"
        const val FIXTURE_MINUTES = 15L
    }
}
