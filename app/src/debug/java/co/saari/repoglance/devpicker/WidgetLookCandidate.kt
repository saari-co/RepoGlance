package co.saari.repoglance.devpicker

import co.saari.repoglance.widget.FreshnessStyle
import co.saari.repoglance.widget.WidgetLook

/**
 * Five candidates for the `family-look` grill, round 4, slot
 * `widget-freshness-and-labels`: how the repo and stack widgets colour their
 * freshness line (stale, last good, rate limited, no data) and whether their
 * labels take the app's mono face. Each is a [WidgetLook] fed through the
 * production seam (widget/WidgetLook.kt); A is WidgetLook.Material, the
 * pre-lock widgets. The maintainer asked for hybrid H (C compact, D tall and
 * stack) and locked it on 2026-09-28; it is now WidgetLook.Family, the
 * production default. Family roles: rate limited = failing red, last good = working
 * amber, no data = neutral; fresh stays onSurfaceVariant. CI on widgets is
 * deferred (the live store has no CI).
 */
internal enum class WidgetLookCandidate(
    val letter: String,
    val shortName: String,
    val description: String,
    val look: WidgetLook,
) {
    M3_DEFAULT(
        "A",
        "M3 error",
        "Today: any stale line is M3 error red and bold, on the system face. The tall header line is never coloured.",
        WidgetLook.Material,
    ),
    INK_MONO(
        "B",
        "family ink + mono",
        "Stale lines take the family ink (amber last good, red rate limited, neutral no data), bold, and all " +
            "widget labels are mono. The tall header line is coloured too.",
        WidgetLook(mono = true, freshness = FreshnessStyle.TONE_INK, staleBold = true),
    ),
    CAPSULE_MONO(
        "C",
        "tonal capsule + mono",
        "Stale lines sit in a tonal capsule (family container, on-container text, regular weight), matching " +
            "the app's pills; mono labels.",
        WidgetLook(mono = true, freshness = FreshnessStyle.TONE_CAPSULE, staleBold = false),
    ),
    INK_SYSTEM(
        "D",
        "family ink, system face",
        "Family ink as in B, bold, but labels stay on the system face: colour changes, type does not.",
        WidgetLook(mono = false, freshness = FreshnessStyle.TONE_INK, staleBold = true),
    ),
    TINTED_HEADER(
        "E",
        "tinted header + mono",
        "The tall and stack header band takes the family container when stale or rate limited; row and " +
            "compact lines use the ink at regular weight; mono labels.",
        WidgetLook(mono = true, freshness = FreshnessStyle.TONE_HEADER, staleBold = false),
    ),
    HYBRID_C_COMPACT_D_TALL(
        "H",
        "C compact + D tall/stack",
        "Maintainer-requested hybrid (replaces the five for confirmation): compact widget = C (tonal capsule " +
            "freshness, regular weight, mono labels); tall and stack widgets = D (family ink, bold, system face).",
        WidgetLook.Family,
    ),
}

internal val ROUND_FIVE = WidgetLookCandidate.entries.filter { it.letter != "H" }
