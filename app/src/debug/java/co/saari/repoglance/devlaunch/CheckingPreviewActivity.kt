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
import co.saari.repoglance.ui.CheckingScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme

// Debug-only: holds the production Checking screen (LiveUiState.Checking)
// open so verify-repoglance can dump and capture it. In the real app that
// state lasts tens of milliseconds while the saved session is read on IO,
// too short to capture. Never ships: debug source set only.
//
//   bin/verify-repoglance launch MIXED checking
class CheckingPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepoGlanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CheckingScreen()
                }
            }
        }
    }
}
