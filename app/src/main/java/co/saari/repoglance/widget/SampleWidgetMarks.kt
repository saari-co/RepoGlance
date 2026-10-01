package co.saari.repoglance.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.padding
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.saari.repoglance.ui.theme.LocalSampleMarker
import co.saari.repoglance.ui.theme.SampleMarker

internal const val SAMPLE_CAPSULE_TAG = "sample-capsule"

@Composable
internal fun sampleMarker(freshness: WidgetFreshness): SampleMarker? =
    if (freshness.sample) freshness.sampleMarker ?: LocalSampleMarker.current else null

@Composable
internal fun SampleCapsule(text: String, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text,
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onTertiaryContainer,
            fontSize = 8.sp,
            fontFamily = labelFamily(),
        ),
        modifier = modifier
            .background(GlanceTheme.colors.tertiaryContainer)
            .cornerRadius(8.dp)
            .padding(horizontal = 4.dp)
            .semantics { testTag = SAMPLE_CAPSULE_TAG },
    )
}
