package co.saari.repoglance.devpicker

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import co.saari.repoglance.ui.theme.StatusColors
import co.saari.repoglance.ui.theme.StatusShape
import co.saari.repoglance.ui.theme.StatusTone
import android.graphics.Color as AndroidColor

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
 * primary by half the difference, capped at 15 degrees. This picker does that
 * in HSV rather than HCT, close enough to judge the direction; the production
 * implementation after a lock would use the real harmoniser.
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
        override fun palette(scheme: ColorScheme, dark: Boolean) = StatusColors(
            ok = fixedTone(harmonise(EMERALD, scheme.primary), dark),
            working = fixedTone(harmonise(AMBER, scheme.primary), dark),
            failing = fixedTone(harmonise(RED, scheme.primary), dark),
            neutral = StatusTone(scheme.outline, scheme.surfaceVariant, scheme.onSurfaceVariant),
            shape = StatusShape.PILL,
        )
    },
    ;

    abstract fun palette(scheme: ColorScheme, dark: Boolean): StatusColors

    companion object {
        val EMERALD = Color(0xFF22C55E)
        val AMBER = Color(0xFFF59E0B)
        val RED = Color(0xFFEF4444)
        private const val MAX_ROTATION = 15f
        private const val HALF = 0.5f
        private const val FULL_TURN = 360f
        private const val HALF_TURN = 180f
        private const val CONTAINER_SAT_LIGHT = 0.28f
        private const val CONTAINER_VAL_LIGHT = 0.96f
        private const val ON_CONTAINER_SAT_LIGHT = 0.95f
        private const val ON_CONTAINER_VAL_LIGHT = 0.36f
        private const val CONTAINER_SAT_DARK = 0.55f
        private const val CONTAINER_VAL_DARK = 0.30f
        private const val ON_CONTAINER_SAT_DARK = 0.30f
        private const val ON_CONTAINER_VAL_DARK = 0.95f
        private const val INK_SAT_DARK_MAX = 0.75f

        private fun hsv(color: Color): FloatArray = FloatArray(3).also { AndroidColor.colorToHSV(color.toArgb(), it) }

        private fun fromHsv(h: Float, s: Float, v: Float): Color =
            Color(AndroidColor.HSVToColor(floatArrayOf((h + FULL_TURN) % FULL_TURN, s, v)))

        fun harmonise(design: Color, source: Color): Color {
            val d = hsv(design)
            val s = hsv(source)
            var diff = s[0] - d[0]
            if (diff > HALF_TURN) diff -= FULL_TURN
            if (diff < -HALF_TURN) diff += FULL_TURN
            val rotation = (kotlin.math.abs(diff) * HALF).coerceAtMost(MAX_ROTATION)
            val hue = d[0] + if (diff >= 0) rotation else -rotation
            return fromHsv(hue, d[1], d[2])
        }

        fun fixedTone(base: Color, dark: Boolean): StatusTone {
            val (h, s, v) = hsv(base)
            return if (dark) {
                StatusTone(
                    ink = fromHsv(h, s.coerceAtMost(INK_SAT_DARK_MAX), v),
                    container = fromHsv(h, CONTAINER_SAT_DARK, CONTAINER_VAL_DARK),
                    onContainer = fromHsv(h, ON_CONTAINER_SAT_DARK, ON_CONTAINER_VAL_DARK),
                )
            } else {
                StatusTone(
                    ink = base,
                    container = fromHsv(h, CONTAINER_SAT_LIGHT, CONTAINER_VAL_LIGHT),
                    onContainer = fromHsv(h, ON_CONTAINER_SAT_LIGHT, ON_CONTAINER_VAL_LIGHT),
                )
            }
        }
    }
}
