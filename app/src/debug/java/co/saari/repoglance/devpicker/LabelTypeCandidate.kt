package co.saari.repoglance.devpicker

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import co.saari.repoglance.ui.theme.LabelStyle
import co.saari.repoglance.ui.theme.LabelType

/**
 * Five candidates for the `family-look` grill, round 2, slot
 * `label-typography`: how RepoGlance sets its labels (section heads, chips,
 * age / rate-limit lines, button labels). Body text stays M3 in all five.
 *
 * Each is a [LabelType] fed through the production seam
 * (ui/theme/LabelType.kt). A is LabelType.Material (the pre-lock app); the
 * maintainer locked D on 2026-09-28 and it is now LabelType.Mono, the
 * production default.
 * Intercom's label numbers come from its docs/design/intercom-page.md:
 * section heads 11 px / 600 / uppercase / 0.12 em, chips 13.5 px / 500,
 * buttons 600. Round 2 was captured with C in real Space Grotesk; the font
 * was removed after the lock.
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
        LabelType.Material,
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
        "Intercom's own label face for labels only: section heads 11 sp / 600 / UPPERCASE / 0.12 em, " +
            "chips 13.5 sp / 500, ages 12 sp / 500, buttons 15 sp / 600. Rejected; the font was removed after the " +
            "D lock, so this now renders on the system face.",
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
        LabelType.Mono,
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

private fun sys(size: Double, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

private fun sys(size: Int, weight: FontWeight, tracking: Double) = sys(size.toDouble(), weight, tracking)

private fun grotesk(size: Double, weight: FontWeight, tracking: Double) =
    sys(size, weight, tracking).copy(fontFamily = FontFamily.Default)

private fun grotesk(size: Int, weight: FontWeight, tracking: Double) = grotesk(size.toDouble(), weight, tracking)
