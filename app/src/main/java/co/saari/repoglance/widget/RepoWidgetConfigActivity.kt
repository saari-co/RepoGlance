@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package co.saari.repoglance.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import java.time.Instant
import kotlinx.coroutines.launch

/** System-launched configuration and reconfiguration surface for one repo
 * widget instance. A canceled activity leaves the widget unconfigured; Save
 * commits only this launcher's app-widget ID and then renders that ID. */
class RepoWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider
        if (provider != ComponentName(this, RepoWidgetReceiver::class.java)) {
            finish()
            return
        }
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(intent) ?: run {
            finish()
            return
        }

        val repos = WidgetFixtureData.availableSnapshots(Instant.now()).map { it.repo }
        if (repos.isEmpty()) {
            finish()
            return
        }
        val saved = RepoWidgetConfigStore.load(this, appWidgetId)
        val initialRepo = saved?.repo?.takeIf { it in repos } ?: repos.first()
        val initialMode = saved?.mode ?: NavigatorMode.BOTH

        enableEdgeToEdge()
        setContent {
            RepoGlanceTheme {
                RepoWidgetConfigScreen(
                    repos = repos,
                    initialRepo = initialRepo,
                    initialMode = initialMode,
                    onSave = { repo, mode ->
                        RepoWidgetConfigStore.save(this, appWidgetId, RepoWidgetConfig(repo, mode))
                        lifecycleScope.launch {
                            RepoWidget().update(this@RepoWidgetConfigActivity, glanceId)
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RepoWidgetConfigScreen(
    repos: List<RepoRef>,
    initialRepo: RepoRef,
    initialMode: NavigatorMode,
    onSave: (RepoRef, NavigatorMode) -> Unit,
) {
    var selectedRepoFull by rememberSaveable { mutableStateOf(initialRepo.full) }
    var selectedMode by rememberSaveable { mutableStateOf(initialMode) }
    var repoMenuExpanded by remember { mutableStateOf(false) }
    val selectedRepo = repos.firstOrNull { it.full == selectedRepoFull } ?: initialRepo

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                Text("Configure widget", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Choose one repository and the feed this widget should show.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.padding(top = 8.dp))
                ExposedDropdownMenuBox(
                    expanded = repoMenuExpanded,
                    onExpandedChange = { repoMenuExpanded = it },
                ) {
                    TextField(
                        value = selectedRepo.full,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repository") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repoMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = repoMenuExpanded,
                        onDismissRequest = { repoMenuExpanded = false },
                    ) {
                        repos.forEach { repo ->
                            DropdownMenuItem(
                                text = { Text(repo.full) },
                                onClick = {
                                    selectedRepoFull = repo.full
                                    repoMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text("Feed", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(NavigatorMode.ISSUES, NavigatorMode.BOTH, NavigatorMode.PRS).forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                            label = { Text(mode.name) },
                        )
                    }
                }
                Text(
                    "Fixture data only in this build. BOTH is a single recently updated feed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onSave(selectedRepo, selectedMode) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save widget")
                }
            }
        }
    }
}
