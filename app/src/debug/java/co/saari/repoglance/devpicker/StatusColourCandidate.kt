package co.saari.repoglance.devpicker

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import co.saari.repoglance.ui.theme.FamilyStatus
import co.saari.repoglance.ui.theme.StatusColors
import co.saari.repoglance.ui.theme.StatusTone

/**
 * Five candidates for the `family-look` grill, slot `status-colour`: how
 * RepoGlance colours the meanings ok / working / failing / neutral. Each is a
 * pure function of the live Material 3 colour scheme, so the canvas keeps the
 * dynamic-colour base and the light/dark mode it is rendered in.
 *
 * Family hues are Swarm Intercom's state colours (docs/design/intercom-page.md
 * in that repo): emerald ok, amber working, sky accent. Intercom has no
 * failing state; RepoGlance adds one red. "Harmonised" follows Google's
 * guidance for custom semantic colours: the hue leans towards the dynamic
 * primary by half the difference, capped at 15 degrees.
 *
 * Round 1 was captured with candidates A-D as dots and E as tonal pills, and
 * with an HSV hue nudge. After the maintainer locked E (2026-09-22) the
 * production seam only renders pills and harmonises in CIELCh
 * (ui/theme/StatusColors.kt), so this file now feeds every candidate's palette
 * through that seam: the hues are the same, the shape is E's for all.
 */
internal enum class StatusColourCandidate(
    val letter: String,
    val shortName: String,
    val label: String,
    val description: String,
) {
    DYNAMIC_ROLES(
        "A",
        "dynamic roles",
        "A dynamic roles",
        "ok = primary, working = tertiary, failing = error, neutral = outline (today). " +
            "Meanings move with the wallpaper; only failing is fixed by Material.",
    ) {
        override fun palette(scheme: ColorScheme, dark: Boolean) = StatusColors(
            ok = StatusTone(scheme.primary, scheme.primaryContainer, scheme.onPrimaryContainer),
            working = StatusTone(scheme.tertiary, scheme.tertiaryContainer, scheme.onTertiaryContainer),
            failing = StatusTone(scheme.error, scheme.errorContainer, scheme.onErrorContainer),
            neutral = StatusTone(scheme.outline, scheme.surfaceVariant, scheme.onSurfaceVariant),
        )
    },
    FAMILY_FIXED(
        "B",
        "family fixed",
        "B family fixed",
        "Intercom's exact hues: emerald ok, amber working, plus a fixed red for failing; neutral = outline. " +
            "Identical in both apps on every wallpaper, so the family reads instantly but never blends.",
    ) {
        override fun palette(scheme: ColorScheme, dark: Boolean) = StatusColors(
            ok = fixedTone(EMERALD, dark),
            working = fixedTone(AMBER, dark),
            failing = fixedTone(RED, dark),
            neutral = StatusTone(scheme.outline, scheme.surfaceVariant, scheme.onSurfaceVariant),
        )
    },
    FAMILY_HARMONISED(
        "C",
        "family harmonised",
        "C family harmonised",
        "The same meanings as B, with each hue leaned towards the dynamic primary (Google's harmonise rule). " +
            "Same family, slightly different tint on every phone.",
    ) {
        override fun palette(scheme: ColorScheme, dark: Boolean) = StatusColors(
            ok = fixedTone(harmonise(EMERALD, scheme.primary), dark),
            working = fixedTone(harmonise(AMBER, scheme.primary), dark),
            failing = fixedTone(harmonise(RED, scheme.primary), dark),
            neutral = StatusTone(scheme.outline, scheme.surfaceVariant, scheme.onSurfaceVariant),
        )
    },
    INK_ONLY(
        "D",
        "ink only",
        "D ink only",
        "The mark's monochrome language: ok = on-surface ink, working = muted ink, neutral = outline. " +
            "Colour appears only when something is wrong (failing = error).",
    ) {
        override fun palette(scheme: ColorScheme, dark: Boolean) = StatusColors(
            ok = StatusTone(scheme.onSurface, scheme.surfaceVariant, scheme.onSurface),
            working = StatusTone(scheme.onSurfaceVariant, scheme.surfaceVariant, scheme.onSurfaceVariant),
            failing = StatusTone(scheme.error, scheme.errorContainer, scheme.onErrorContainer),
            neutral = StatusTone(scheme.outline, scheme.surfaceVariant, scheme.onSurfaceVariant),
        )
    },
    FAMILY_TONAL(
        "E",
        "family tonal",
        "E family tonal",
        "C's harmonised family hues applied as Material tonal surfaces: the status sits in a soft tinted pill " +
            "with dark ink, the way Google apps label state, instead of a saturated dot.",
    ) {
        override fun palette(scheme: ColorScheme, dark: Boolean) = FamilyStatus.colors(scheme)
    },
    ;

    abstract fun palette(scheme: ColorScheme, dark: Boolean): StatusColors

    companion object {
        val EMERALD = FamilyStatus.EMERALD
        val AMBER = FamilyStatus.AMBER
        val RED = FamilyStatus.RED

        fun harmonise(design: Color, source: Color): Color = FamilyStatus.harmonise(design, source)

        fun fixedTone(base: Color, dark: Boolean): StatusTone = FamilyStatus.tone(base, dark)
    }
}
