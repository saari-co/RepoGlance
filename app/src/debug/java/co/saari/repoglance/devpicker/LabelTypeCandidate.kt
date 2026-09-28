package co.saari.repoglance.devpicker

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import co.saari.repoglance.R
import co.saari.repoglance.ui.theme.LabelStyle
import co.saari.repoglance.ui.theme.LabelType

/**
 * Five candidates for the `family-look` grill, round 2, slot
 * `label-typography`: how RepoGlance sets its labels (section heads, chips,
 * age / rate-limit lines, button labels). Body text stays M3 in all five.
 *
 * Each is a [LabelType] fed through the production seam
 * (ui/theme/LabelType.kt); A is the empty type, so it is today's app.
 * Intercom's label numbers come from its docs/design/intercom-page.md:
 * section heads 11 px / 600 / uppercase / 0.12 em, chips 13.5 px / 500,
 * buttons 600. Space Grotesk is instanced from Intercom's OFL variable font
 * into the debug source set only (licence in debug assets).
 */
internal enum class LabelTypeCandidate(
    val letter: String,
    val shortName: String,
    val description: String,
    val type: LabelType,
) {
    M3_DEFAULT(
        "A",
        "M3 default",
        "Today: section heads titleSmall, chips and pills labelLarge, ages bodySmall, banners labelMedium, " +
            "buttons labelLarge. System font, sentence case, no tracking.",
        LabelType.Default,
    ),
    SYSTEM_CAPS(
        "B",
        "system caps",
        "Intercom's label structure on the system font: section heads 11 sp / 600 / UPPERCASE / 0.12 em, " +
            "chips 13.5 sp / 500, ages 11 sp / 500 lightly tracked, buttons 15 sp / 600. No bundled font.",
        LabelType(
            section = LabelStyle(sys(11, FontWeight.SemiBold, 0.12), uppercase = true),
            chip = LabelStyle(sys(13.5, FontWeight.Medium, 0.0)),
            meta = LabelStyle(sys(11, FontWeight.Medium, 0.03)),
            button = LabelStyle(sys(15, FontWeight.SemiBold, 0.0)),
        ),
    ),
    GROTESK(
        "C",
        "Space Grotesk labels",
        "Intercom's own label face, bundled for labels only: section heads 11 sp / 600 / UPPERCASE / 0.12 em, " +
            "chips 13.5 sp / 500, ages 12 sp / 500, buttons 15 sp / 600. Adds ~90 KB per weight and an OFL file.",
        LabelType(
            section = LabelStyle(grotesk(11, FontWeight.SemiBold, 0.12), uppercase = true),
            chip = LabelStyle(grotesk(13.5, FontWeight.Medium, 0.0)),
            meta = LabelStyle(grotesk(12, FontWeight.Medium, 0.01)),
            button = LabelStyle(grotesk(15, FontWeight.SemiBold, 0.0)),
        ),
    ),
    MONO(
        "D",
        "mono labels",
        "Terminal texture, echoing Intercom's Space Mono code face via the system monospace: section heads " +
            "12 sp / 600, chips 12.5 sp, ages 11 sp, buttons 14 sp / 500, all monospaced. No bundled font.",
        LabelType(
            section = LabelStyle(mono(12, FontWeight.SemiBold)),
            chip = LabelStyle(mono(12.5, FontWeight.Normal)),
            meta = LabelStyle(mono(11, FontWeight.Normal)),
            button = LabelStyle(mono(14, FontWeight.Medium)),
        ),
    ),
    EXPRESSIVE(
        "E",
        "expressive sentence",
        "Google-app emphasis on the system font, sentence case: section heads 16 sp / 700 tightened, chips " +
            "14 sp / 600, ages 12 sp / 500 with tabular figures, buttons 16 sp / 700. No bundled font.",
        LabelType(
            section = LabelStyle(sys(16, FontWeight.Bold, -0.01)),
            chip = LabelStyle(sys(14, FontWeight.SemiBold, 0.0)),
            meta = LabelStyle(sys(12, FontWeight.Medium, 0.0).copy(fontFeatureSettings = "tnum")),
            button = LabelStyle(sys(16, FontWeight.Bold, 0.0)),
        ),
    ),
}

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
)

private fun sys(size: Double, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

private fun sys(size: Int, weight: FontWeight, tracking: Double) = sys(size.toDouble(), weight, tracking)

private fun grotesk(size: Double, weight: FontWeight, tracking: Double) =
    sys(size, weight, tracking).copy(fontFamily = SpaceGrotesk)

private fun grotesk(size: Int, weight: FontWeight, tracking: Double) = grotesk(size.toDouble(), weight, tracking)

private fun mono(size: Double, weight: FontWeight) = sys(size, weight, 0.0).copy(fontFamily = FontFamily.Monospace)

private fun mono(size: Int, weight: FontWeight) = mono(size.toDouble(), weight)
