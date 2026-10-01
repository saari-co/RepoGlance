package co.saari.repoglance.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import co.saari.repoglance.state.ThemeChoice
import co.saari.repoglance.state.ThemePrefs
import co.saari.repoglance.ui.theme.LabelRole
import co.saari.repoglance.ui.theme.LabelText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ThemeSettingItem(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var choice by remember { mutableStateOf<ThemeChoice?>(null) }
    LaunchedEffect(context) {
        choice = withContext(Dispatchers.IO) { ThemePrefs.choice(context) }
    }
    var choosing by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        headline = "Theme",
        supporting = choice?.label.orEmpty(),
        onClick = { if (choice != null) choosing = true },
        modifier = modifier.testTag("repoglance:settings-theme"),
    )
    val current = choice
    if (choosing && current != null) {
        ThemeDialog(
            selected = current,
            onChoose = { next ->
                choosing = false
                if (next != current) {
                    choice = next
                    ThemePrefs.choose(context, next)
                }
            },
            onDismiss = { choosing = false },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ThemeDialog(selected: ThemeChoice, onChoose: (ThemeChoice) -> Unit, onDismiss: () -> Unit) {
    var pending by rememberSaveable(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeChoice.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = option == pending,
                                role = Role.RadioButton,
                                onClick = { pending = option },
                            )
                            .testTag("repoglance:theme-${option.name.lowercase()}"),
                    ) {
                        RadioButton(selected = option == pending, onClick = null)
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onChoose(pending) },
                modifier = Modifier.testTag("repoglance:theme-ok"),
            ) { LabelText("OK", LabelRole.BUTTON) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("repoglance:theme-cancel"),
            ) { LabelText("Cancel", LabelRole.BUTTON) }
        },
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("repoglance:theme-dialog"),
    )
}
