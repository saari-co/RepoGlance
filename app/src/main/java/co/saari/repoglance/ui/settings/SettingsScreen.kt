package co.saari.repoglance.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import co.saari.repoglance.LiveUiState
import co.saari.repoglance.link.GitHubLinks
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.ui.theme.LabelRole
import co.saari.repoglance.ui.theme.LabelText

enum class SettingsAccount { LIVE, SAMPLE, SIGNED_OUT }

enum class SettingsDestination { SETTINGS, WIDGETS }

internal const val SETTINGS_TEST_TAG = "repoglance:settings"
internal const val SETTINGS_BACK_TEST_TAG = "repoglance:settings-back"
internal const val SETTINGS_WIDGETS_TEST_TAG = "repoglance:settings-widgets"
internal const val SETTINGS_MANAGE_ACCESS_TEST_TAG = "repoglance:settings-manage-access"
internal const val SETTINGS_DISCONNECT_TEST_TAG = "repoglance:settings-disconnect"
internal const val DISCONNECT_CONFIRM_TEST_TAG = "repoglance:disconnect-confirm"
internal const val SETTINGS_VERSION_TEST_TAG = "repoglance:settings-version"
internal const val SETTINGS_PRIVACY_TEST_TAG = "repoglance:settings-privacy"
internal const val SETTINGS_SOURCE_TEST_TAG = "repoglance:settings-source"
internal const val PRIVACY_POLICY_URL = "https://saari-co.github.io/RepoGlance/privacy/"
internal val SOURCE_CODE_REPO = RepoRef("saari-co", "RepoGlance")

internal fun settingsAccount(state: LiveUiState, sampleMode: Boolean): SettingsAccount = when {
    sampleMode -> SettingsAccount.SAMPLE
    state is LiveUiState.Failure -> if (state.needsNewSignIn) SettingsAccount.SIGNED_OUT else SettingsAccount.LIVE
    state is LiveUiState.SignedOut ||
        state is LiveUiState.Checking ||
        state is LiveUiState.RequestingDeviceCode ||
        state is LiveUiState.AwaitingDeviceAuthorization -> SettingsAccount.SIGNED_OUT
    else -> SettingsAccount.LIVE
}

@Composable
fun SettingsScreen(
    account: SettingsAccount,
    versionName: String,
    onBack: () -> Unit,
    onOpenWidgets: () -> Unit,
    onManageGitHubAccess: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    appearance: (@Composable () -> Unit)? = null,
) {
    var confirmDisconnect by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    if (confirmDisconnect) {
        DisconnectDialog(
            onDismiss = { confirmDisconnect = false },
            onConfirm = {
                confirmDisconnect = false
                onDisconnect()
            },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(SETTINGS_TEST_TAG),
        topBar = { SettingsTopBar("Settings", SETTINGS_BACK_TEST_TAG, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            if (account != SettingsAccount.SIGNED_OUT) {
                item(key = "widgets") {
                    SettingsRow(
                        headline = "Widgets",
                        supporting = "Add RepoGlance widgets to your home screen and change what they show",
                        icon = Icons.Outlined.Widgets,
                        onClick = onOpenWidgets,
                        modifier = Modifier.testTag(SETTINGS_WIDGETS_TEST_TAG),
                    )
                }
            }
            if (appearance != null) {
                item(key = "appearance-head") { SettingsSectionHead("Appearance") }
                item(key = "appearance") { appearance() }
            }
            if (account == SettingsAccount.LIVE) {
                item(key = "github-head") { SettingsSectionHead("GitHub") }
                item(key = "manage-access") {
                    SettingsRow(
                        headline = "Manage GitHub access",
                        supporting = "Choose which repositories RepoGlance can read, on GitHub",
                        icon = Icons.Outlined.ManageAccounts,
                        trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                        trailingDescription = "Opens GitHub",
                        onClick = onManageGitHubAccess,
                        modifier = Modifier.testTag(SETTINGS_MANAGE_ACCESS_TEST_TAG),
                    )
                }
                item(key = "disconnect") {
                    SettingsRow(
                        headline = "Disconnect GitHub",
                        supporting = "Remove the GitHub session and saved data from this phone",
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        onClick = { confirmDisconnect = true },
                        modifier = Modifier.testTag(SETTINGS_DISCONNECT_TEST_TAG),
                    )
                }
            }
            item(key = "about-head") { SettingsSectionHead("About") }
            item(key = "version") {
                SettingsRow(
                    headline = "Version",
                    supporting = "RepoGlance $versionName",
                    icon = Icons.Outlined.Info,
                    modifier = Modifier.testTag(SETTINGS_VERSION_TEST_TAG),
                )
            }
            item(key = "privacy") {
                SettingsRow(
                    headline = "Privacy policy",
                    icon = Icons.Outlined.PrivacyTip,
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    trailingDescription = "Opens in your browser",
                    onClick = { onOpenLink(PRIVACY_POLICY_URL) },
                    modifier = Modifier.testTag(SETTINGS_PRIVACY_TEST_TAG),
                )
            }
            item(key = "source") {
                SettingsRow(
                    headline = "Source code",
                    supporting = SOURCE_CODE_REPO.full,
                    icon = Icons.Outlined.Code,
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    trailingDescription = "Opens GitHub",
                    onClick = { onOpenLink(GitHubLinks.repo(SOURCE_CODE_REPO)) },
                    modifier = Modifier.testTag(SETTINGS_SOURCE_TEST_TAG),
                )
            }
        }
    }
}

@Composable
private fun DisconnectDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.popupResourceIds(),
        title = { Text("Disconnect RepoGlance?") },
        text = { Text("This removes the GitHub session from this phone. You can connect again anytime.") },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(DISCONNECT_CONFIRM_TEST_TAG)) {
                LabelText("Disconnect GitHub", LabelRole.BUTTON)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { LabelText("Cancel", LabelRole.BUTTON) }
        },
    )
}
