package co.saari.repoglance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import co.saari.repoglance.ui.theme.ControlChip
import co.saari.repoglance.ui.theme.LabelRole
import co.saari.repoglance.ui.theme.LabelText
import co.saari.repoglance.ui.theme.PrimaryButton
import co.saari.repoglance.ui.theme.StatusBanner
import co.saari.repoglance.ui.theme.sampleTone

internal const val SAMPLE_WORD = "SAMPLE"
internal const val SAMPLE_BANNER_TEXT = "These repositories are made up. Sign in to see your own GitHub."
internal const val SAMPLE_SIGN_IN = "Sign in with GitHub"

@Composable
internal fun SampleBanner(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    StatusBanner(
        tone = sampleTone(),
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag(SAMPLE_BAR_TEST_TAG),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LabelText(SAMPLE_WORD, LabelRole.SECTION, modifier = Modifier.testTag(SAMPLE_CHIP_TEST_TAG))
            Spacer(Modifier.height(4.dp))
            Text(SAMPLE_BANNER_TEXT, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(onClick = onSignIn, modifier = Modifier.testTag(SAMPLE_SIGN_IN_TEST_TAG)) {
                LabelText(SAMPLE_SIGN_IN, LabelRole.BUTTON)
            }
        }
    }
}

@Composable
internal fun SampleChipRow(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().testTag(SAMPLE_BAR_TEST_TAG),
    ) {
        ControlChip(
            label = { LabelText(SAMPLE_WORD, LabelRole.CHIP) },
            modifier = Modifier.testTag(SAMPLE_CHIP_TEST_TAG),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Made-up repositories, not your GitHub",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSignIn, modifier = Modifier.testTag(SAMPLE_SIGN_IN_TEST_TAG)) {
            LabelText(SAMPLE_SIGN_IN, LabelRole.BUTTON)
        }
    }
}
