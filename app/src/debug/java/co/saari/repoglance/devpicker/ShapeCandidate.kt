package co.saari.repoglance.devpicker

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import co.saari.repoglance.ui.theme.CardFill
import co.saari.repoglance.ui.theme.ControlShape
import co.saari.repoglance.ui.theme.Edge
import co.saari.repoglance.ui.theme.Fill

/**
 * Five candidates for the `family-look` grill, round 3, slot
 * `shape-and-control-language`: the shape, fill and edge of chips, status
 * pills, buttons, banners and cards/rows. Each is a [ControlShape] fed through
 * the production seam (ui/theme/ControlShape.kt); A is ControlShape.Material,
 * the pre-lock app; the maintainer locked C on 2026-09-28 and it is now
 * ControlShape.Tonal, the production default (secondary buttons borderless). Swarm Intercom's numbers come from its
 * docs/design/intercom-page.md: capsules and chips 999 px, cards 20 px,
 * outlined buttons with a 1.5 px state-colour edge at 65 %.
 */
internal enum class ShapeCandidate(
    val letter: String,
    val shortName: String,
    val description: String,
    val shape: ControlShape,
) {
    M3_DEFAULT(
        "A",
        "M3 default",
        "Today: 8 dp outlined assist chips, tonal capsule status pills, filled capsule buttons, square tonal " +
            "banners, 12 dp elevated cards.",
        ControlShape.Material,
    ),
    INTERCOM_OUTLINE(
        "B",
        "outlined capsules",
        "Intercom's language: every chip, pill, button and banner is a capsule with no fill and a 1.5 dp edge " +
            "in its state colour (primary for buttons). Cards 20 dp, flat, hairline edge.",
        ControlShape(
            chip = CircleShape, chipEdge = Edge.HAIRLINE, chipFill = Fill.OUTLINED,
            pill = CircleShape, pillEdge = Edge.STATE, pillFill = Fill.OUTLINED,
            button = CircleShape, buttonFill = Fill.OUTLINED,
            banner = CircleShape, bannerEdge = Edge.STATE, bannerFill = Fill.OUTLINED,
            card = RoundedCornerShape(20.dp), cardFill = CardFill.OUTLINED,
        ),
    ),
    GOOGLE_TONAL(
        "C",
        "Google tonal",
        "Gemini / Google-app softness: borderless tonal chips (8 dp), tonal capsule pills, tonal capsule " +
            "buttons, 16 dp tonal banners, 24 dp flat tonal cards. No outlines anywhere.",
        ControlShape.Tonal,
    ),
    TERMINAL_SQUARE(
        "D",
        "squared terminal",
        "Matches the mono labels: 4 dp hairline chips, 4 dp pills filled with a state edge, 6 dp filled " +
            "buttons, square banners with a state edge, 8 dp flat hairline cards.",
        ControlShape(
            chip = RoundedCornerShape(4.dp), chipEdge = Edge.HAIRLINE, chipFill = Fill.OUTLINED,
            pill = RoundedCornerShape(4.dp), pillEdge = Edge.STATE, pillFill = Fill.TONAL,
            button = RoundedCornerShape(6.dp), buttonFill = Fill.FILLED,
            banner = RoundedCornerShape(0.dp), bannerEdge = Edge.STATE, bannerFill = Fill.TONAL,
            card = RoundedCornerShape(8.dp), cardFill = CardFill.OUTLINED,
        ),
    ),
    FILLED_CAPSULE(
        "E",
        "filled capsules + chamfer",
        "Solid capsules everywhere: tonal capsule chips, tonal capsule pills with a state edge, filled " +
            "capsule buttons, capsule tonal banners, and chamfered 12 dp elevated cards as the one hard edge.",
        ControlShape(
            chip = CircleShape, chipEdge = Edge.NONE, chipFill = Fill.TONAL,
            pill = CircleShape, pillEdge = Edge.STATE, pillFill = Fill.TONAL,
            button = CircleShape, buttonFill = Fill.FILLED,
            banner = CircleShape, bannerEdge = Edge.NONE, bannerFill = Fill.TONAL,
            card = CutCornerShape(12.dp), cardFill = CardFill.ELEVATED,
        ),
    ),
}
