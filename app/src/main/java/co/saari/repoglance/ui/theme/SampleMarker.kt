package co.saari.repoglance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class SampleMarker {
    CHIP_ROW,
    BANNER,
    ;

    companion object {
        val Default = BANNER
    }
}

val LocalSampleMarker = staticCompositionLocalOf { SampleMarker.Default }

@Composable
fun sampleTone(): StatusTone = StatusTone(
    ink = MaterialTheme.colorScheme.tertiary,
    container = MaterialTheme.colorScheme.tertiaryContainer,
    onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
)
